package com.manerfan.starter.json5

import java.lang.RuntimeException

typealias ResultType = Map<String, Any?>

class JsonParser(private val json: String) {

    companion object {
        final val DEFAULT_LIST_KEY = "list"
    }

    private val NULL = "null"
    private val BOOL_TRUE = "true"
    private val BOOL_FALSE = "false"
    private val NUM_INFINITY = "infinity"
    private val NUM_INFINITY_PSITIVE = "+infinity"
    private val NUM_INFINITY_NEGATIVE = "-infinity"
    private val NUM_NAN = "nan"

    private val lf: Char = 0x0A.toChar()
    private val cr: Char = 0x0D.toChar()
    private val cs: Char = 0x2028.toChar()
    private val ps: Char = 0x2029.toChar()

    private val terminals = arrayListOf(lf, cr, cs, ps)

    private val validChars = arrayListOf('"', '\'', '@', '#', '$', '_', '{', '}', ':', '[', ']', ',', '+', '-', '.')

    private val maxLen = json.length
    private var index = 0
    private var result = mutableMapOf<String, Any>()

    fun toMap(): ResultType = when (getTokenBegin()) {
        '{' -> {
            // 对象
            index++
            parseMap()
        }
        '[' -> {
            //数组
            index++
            mapOf(DEFAULT_LIST_KEY to parseList())
        }
        else -> throw ParseException("Invalid Character at position $index, Json Must start with '{' or '[' !", index)

    }

    /**
     * 将下一部分解析为Map
     */
    private fun parseMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        loop@ while (index < maxLen) {
            var c = getTokenBegin()

            if (c == '}') {
                // 对象结束
                index++
                return map
            }

            // 1. 解析key
            val key = extractKey()

            c = getTokenBegin()

            // 2. 解析value
            if (c != ':') {
                // key之后应该紧接着冒号
                throw ParseException("Invalid Character '$c' at position $index", index)
            }

            index++
            val value = extractValue()

            map[key] = value

            when (getTokenBegin()) {
                '}' -> {
                    // 结束了
                    index++
                    return map
                }
                ',' -> {
                    // 继续下一个属性
                    index++
                    continue@loop
                }
                else -> throw ParseException("Invalid Character $c at position $index", index)
            }
        }

        return map
    }

    /**
     * 将下一部分解析为Collection
     */
    private fun parseList(): Collection<Any?> {
        val list = mutableListOf<Any?>()

        loop@ while (index < maxLen) {
            when (getTokenBegin()) {
                ']' -> {
                    // 数组结束了
                    index++
                    return list
                }
                ',' -> {
                    // 一个元素结束了
                    index++
                    continue@loop
                }
                else -> list.add(extractValue())
            }
        }

        return list
    }

    /**
     * 寻找key
     */
    private fun extractKey(): String {
        val c = json[index]
        // 终止符
        val terminator = if (c == '"' || c == '\'') c else 0.toChar()

        val b = StringBuilder()
        while (index < maxLen) {
            var cn = json[index]

            if (cn == terminator) {
                // 找到终止符
                index++
                if (b.isEmpty()) {
                    // 这里应该是起始符
                    continue
                }
                return b.toString().trim()
            }

            if (0.toChar() == terminator && (cn == ':' || cn == '/' || cn.isWhitespace())) {
                // 结束
                return b.toString().trim()
            }

            if (cn == '\\') {
                // 转义
                cn = nextEscapedText()
            }

            if (0.toChar() == terminator && (cn == '\'' || cn == '"' || isLineTerminator(cn))) {
                // key的起始位置没有被引号加持,则在:之前不能有任何引号或者换行符
                throw ParseException("Invalid Character '$cn' for identifier '$b' at position $index", index)
            } else {
                b.append(cn)
            }

            index++
        }

        return b.toString().trim()
    }

    /**
     * 提取value
     */
    private fun extractValue() = when (getTokenBegin()) {
        '{' -> {
            index++
            parseMap()
        }
        '[' -> {
            index++
            parseList()
        }
        '"', '\'' -> extractString()
        else -> extractLiteral()
    }

    /**
     * 找到首个有意义的字符,并重置index
     * 忽略注释等字符
     */
    private fun getTokenBegin(): Char {
        while (index < maxLen - 1) {
            val c = json[index]

            when {
                c.isDefined() && !c.isWhitespace() -> return c
                c == '/' -> {
                    // 可能是注释
                    val n = json[index + 1]
                    when (n) {
                        // 单行注释
                        '/' -> skipToEndLine()
                        // 多行注释
                        '*' -> {
                            index += 2
                            skipToEndComment()
                        }
                    }
                }
            }
            index++
        }

        return 0.toChar()
    }

    /**
     * 跳到行尾
     */
    private fun skipToEndLine() {
        while (index < maxLen) {
            val c = json[index]
            if (isLineTerminator(c)) {
                return
            } else {
                index++
            }
        }
    }

    /**
     * 跳到注释尾
     */
    private fun skipToEndComment() {
        while (index < maxLen - 1) {
            val c = json[index++]
            val cn = json[index]
            if (c == '*' && cn == '/') {
                return
            }
        }
    }

    /**
     * 提取字面量
     * null | boolean | nan | number
     */
    @Throws(ParseException::class)
    private fun extractLiteral(): Any? {
        val b = StringBuilder()
        loop@ while (index < maxLen) {
            val c = json[index]

            when {
                // +出现在首尾,忽略之
                c == '+' && b.isEmpty() -> index++
                // 合法字符
                c.isLetterOrDigit() || c in arrayListOf('.', '+', '-') -> {
                    b.append(c)
                    index++
                }
                // 非法字符,结束循环
                else -> break@loop
            }
        }

        val literal = b.toString().trim().toLowerCase()

        return when (literal) {
            NULL -> null
            BOOL_TRUE -> true
            BOOL_FALSE -> false
            NUM_INFINITY, NUM_INFINITY_PSITIVE -> Double.POSITIVE_INFINITY
            NUM_INFINITY_NEGATIVE -> Double.NEGATIVE_INFINITY
            NUM_NAN -> Double.NaN
            else -> try {
                detectNumber(literal)
            } catch (ex: NumberFormatException) {
                throw ParseException("Invalid literal '$literal' at position $index", index)
            }
        }
    }

    /**
     * 尝试将字符串转为数字
     *
     * @param literal   字符串
     * @return Integer | Long | BigInteger
     */
    @Throws(NumberFormatException::class)
    private fun detectNumber(literal: String): Any {
        val hasDot = literal.contains('.')
        val hasE = literal.contains('e')
        val hasX = literal.contains('x')

        if (hasDot || (hasE && !hasX)) {
            // 包含小数点,或者包含对数e但不包含0x,就认为是double浮点型
            return literal.toDouble()
        }

        // Int.MAX_VALUE 2147483647 10 characters || 0x7FFF_FFFF 8 + 2 = 10 characters
        // Long.MAX_VALUE 9223372036854775807 19 characters || 0x7FFF_FFFF_FFFF_FFFF 16 + 2 = 18 characters

        // 为了防止溢出,这里简单设定
        // MaxIntLen == 9 for dec or 9 for hex
        // MaxLongLen == 18 for dec or 17 for hex
        // BigInt for else

        val negativeLen = if (literal.startsWith('-')) 1 else 0
        val maxIntLen = negativeLen + if (hasX) 9 else 9
        val maxLongLen = negativeLen + if (hasX) 17 else 18

        val len = literal.length
        return when {
            len <= maxIntLen -> literal.toInt()
            len <= maxLongLen -> literal.toLong()
            else -> literal.toBigInteger()
        }
    }

    /**
     * 提取字符串
     *
     * @return 目标字符串
     */
    private fun extractString(): String {
        // 终止符 " or '
        val terminator = json[index++]
        val b = StringBuilder()
        loop@ while (index < maxLen) {
            val c = json[index]
            when (c) {
                '\\' -> {
                    // 处理转义
                    b.append(nextEscapedText())
                    index++
                }
                terminator -> {
                    // 遇到了终止符,结束
                    index++
                    break@loop
                }
                else -> {
                    b.append(c)
                    index++
                }
            }
        }

        return b.toString().trim()
    }

    /**
     * 前一个字符是'\'时,需要转义字符
     */
    private fun nextEscapedText(): Char {
        val c = json[++index]
        return when (c) {
            'b' -> '\b'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'v' -> 0x000B.toChar()
            '0' -> 0x0000.toChar()
            '\'' -> '\''
            '"' -> '"'
            '\\' -> '\\'
            'u' -> {
                // unicode
                StringBuilder()
                        .append(json[++index])
                        .append(json[++index])
                        .append(json[++index])
                        .append(json[++index])
                        .toString().toInt(16).toChar()
            }
            'x', 'X' -> {
                // 16进制
                val bx = StringBuilder()
                // index will refer to the last accepted character, therefore we need to use +1
                var cx = json[index + 1]
                while (isHexadecimalChar(cx)) {
                    bx.append(cx)
                    cx = json[++index + 1]
                }
                bx.toString().toInt(16).toChar()
            }
            else -> c
        }
    }

    /**
     * 判断字符是否是16进制合法字符
     *
     * @param c 字符
     * @return true | false
     */
    private fun isHexadecimalChar(c: Char) = when (c) {
        in '0'..'9', in 'a'..'f', in 'A'..'F' -> true
        else -> false
    }

    /**
     * 判断字符是否为换行符
     *
     * @param c 字符
     * @return true | false
     */
    private fun isLineTerminator(c: Char) = c in terminals
}

class ParseException(message: String, val position: Int) : RuntimeException(message)
