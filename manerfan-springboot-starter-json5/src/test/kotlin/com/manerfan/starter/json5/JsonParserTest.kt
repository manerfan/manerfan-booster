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
                }
            ]
        """.trimIndent().parseMap()

        println(result)
    }

}