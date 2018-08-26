package com.manerfan.starter.json5

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RateLimiter测试")
class JsonParserTest {
    @DisplayName("ToMap测试")
    @Test
    fun toMapTest() {
        """
            {
                "a" : 12
            }
        """.trimIndent().parseMap()
    }
}