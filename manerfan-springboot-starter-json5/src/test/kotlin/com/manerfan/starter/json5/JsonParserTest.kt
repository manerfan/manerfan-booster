/*
 * ManerFan(http://manerfan.com). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.manerfan.starter.json5

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

@DisplayName("Json5")
class JsonParserTest {
    @Test
    @DisplayName("Number")
    fun numberTest() {
        val result = """
            {
                // 整型
                "intDec": +123_456, /* 10进制 */ intHex: 0x1a_FFfF, /* 16进制 */

                // 长整型
                longDec: 1_234_567_890, longHex: -0X1F_ffff_aBcD_EfeF,

                // 浮点型
                double1: .234, double2: +1.2e-2, double3: -5.6E3,

                // 大整型
                bigintDec: 123_456_789_123_456_789_123_456, bigintHex: -0xABC_FFFF_FFFF_FFFF_FFFF
            }
        """.trimIndent().parseMap()

        println(result)

        Assertions.assertEquals(result["intDec"], 123_456)
        Assertions.assertEquals(result["intHex"], 0x1a_FFfF)

        Assertions.assertEquals(result["longDec"], 1_234_567_890L)
        Assertions.assertEquals(result["longHex"], -0X1F_ffff_aBcD_EfeF)

        Assertions.assertEquals(result["double1"], .234)
        Assertions.assertEquals(result["double2"], 1.2e-2)
        Assertions.assertEquals(result["double3"], -5.6E3)

        Assertions.assertEquals(result["bigintDec"], BigInteger("123456789123456789123456"))
        Assertions.assertEquals(result["bigintHex"], BigInteger("-ABCFFFFFFFFFFFFFFFF", 16))
    }

    @Test
    @DisplayName("Literal")
    fun literalTest() {
        val result = """
            /**
             * 字面量包含
             * null
             * true | false
             * infinity | -infinity
             * nan
             */

            {
                nullVal1: null, nullVal2: NULL,
                infVal: Infinity, posInfVal: +infinity, negInfVal: -Infinity,
                nanVal: NaN
            }
        """.trimIndent().parseMap()

        println(result)

        Assertions.assertEquals(result["nullVal1"], null)
        Assertions.assertEquals(result["nullVal2"], null)

        Assertions.assertEquals(result["infVal"], Double.POSITIVE_INFINITY)
        Assertions.assertEquals(result["posInfVal"], Double.POSITIVE_INFINITY)
        Assertions.assertEquals(result["negInfVal"], Double.NEGATIVE_INFINITY)

        Assertions.assertEquals(result["nanVal"], Double.NaN)
    }

    @Test
    @DisplayName("String")
    fun stringTest() {
        val result = """
            {
                escapeUnicode: '\u4f60\u597d\uff01', escapeHex: "\x41\x42\x43\x21",
                escapeMixed: '示例: \u4f60\u597d，\x41\x42\x43!',
                break:
'
line1\
line2\nline3
'               ,

            }

            // 字符串主要增强了转义和折行
        """.trimIndent().parseMap()

        println(result)

        Assertions.assertEquals(result["escapeUnicode"], "你好！")
        Assertions.assertEquals(result["escapeHex"], "ABC!")
        Assertions.assertEquals(result["escapeMixed"], "示例: 你好，ABC!")

        Assertions.assertEquals(result["break"], "line1\nline2\nline3")
    }

    @Test
    @DisplayName("SubObjArray")
    fun subObjArrayTest() {
        val result = """
            [
                null,
                12,
                [
                    1,
                    {},
                    []
                ],
                {
                    num: [
                        -0X1F_ffff_aBcD_EfeF,
                        .7e-2,
                        NaN,
                    ],
                    str: {
                        normal: '
Hah,
stupid!
',
                        escape: 'I can say \x41\x42\x43!'
                    },
                    emptyObj: {}, emptyArray: []
                },
            ]
        """.trimIndent().parseMap()

        println(result)

        val array = result[JsonParser.DEFAULT_LIST_KEY] as ArrayList<*>

        Assertions.assertEquals(null, array[0])
        Assertions.assertEquals(12, array[1])

        Assertions.assertTrue(array[2] is ArrayList<*>)
        Assertions.assertTrue((array[2] as ArrayList<*>)[1] is Map<*, *>)
        Assertions.assertTrue(((array[2] as ArrayList<*>)[1] as Map<*, *>).isEmpty())

        Assertions.assertEquals("I can say ABC!", ((array[3] as Map<*, *>)["str"] as Map<*, *>)["escape"])

        Assertions.assertTrue(((array[3] as Map<*, *>)["emptyObj"] as Map<*, *>).isEmpty())
        Assertions.assertTrue(((array[3] as Map<*, *>)["emptyArray"] as ArrayList<*>).isEmpty())
    }

    @Test
    @DisplayName("FastJson")
    fun fastJsonTest() {
        val result = """
            {
                str: 'Hello Json5!',
                num: -12_345,
                doubleNum: .3e-2,
                sub: {
                    str:
'
\u4f60\u597d
不要闹
',
                    array: [123_456_789_123_456_789_123_456, -0xABC_FFFF_FFFF_FFFF_FFFF]
                }
            }
        """.trimIndent().parseObject(Obj::class.java)

        println(result)

        Assertions.assertEquals(Obj(
                "Hello Json5!",
                -12_345,
                0.3e-2,
                SubObj(
                        "你好\n不要闹",
                        arrayOf(
                                BigInteger("123456789123456789123456"),
                                BigInteger("-ABCFFFFFFFFFFFFFFFF", 16)
                        )
                )
        ), result)

    }
}

data class Obj(
        val str: String,
        val num: Int,
        val doubleNum: Double,
        val sub: SubObj
)

data class SubObj(
        val str: String,
        val array: Array<BigInteger>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubObj

        if (str != other.str) return false
        if (!Arrays.equals(array, other.array)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = str.hashCode()
        result = 31 * result + Arrays.hashCode(array)
        return result
    }
}