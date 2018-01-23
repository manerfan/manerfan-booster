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

package com.manerfan.starter.ratelimiter

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.ObjectUtils
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * @author manerfan
 * @date 2018/1/18
 */

@DisplayName("RateLimiter测试")
@EnableAutoConfiguration
@EnableRedisRepositories
@SpringBootTest(classes = [RateLimiterAutoConfiguration::class])
@ExtendWith(SpringExtension::class)
class RateLimiterTest {
    @Autowired
    lateinit var rateLimiterFactory: RateLimiterFactory

    @Autowired
    lateinit var redis: RedisTemplate<String, String>

    @BeforeEach
    fun beforeEach() {
        redis.execute { connection ->
            val keys = connection.keys("ratelimiter:test:*".toByteArray()).orEmpty().filterNotNull().toTypedArray()
            if (!ObjectUtils.isEmpty(keys)) {
                connection.del(*keys)
            }
        }
    }

    @DisplayName("acquire")
    @Test
    fun acquireTest() {
        val ratelimiter = rateLimiterFactory.build("ratelimiter:test:acquire", 10.0, 2)
        Assertions.assertTrue(ratelimiter.acquire(5) < 1000)
        Assertions.assertTrue(ratelimiter.acquire(9) < 1000)

        Assertions.assertTrue(ratelimiter.acquire(15) > 1000)

        Thread.sleep(2000)

        Assertions.assertTrue(ratelimiter.acquire(15) < 1000)
        Assertions.assertTrue(ratelimiter.acquire(10) < 1000)

        Assertions.assertTrue(ratelimiter.acquire(15) > 1000)

        Assertions.assertTrue(ratelimiter.acquire(20) == 2000L)
        Assertions.assertTrue(ratelimiter.acquire(15) == 1500L)
    }

    @DisplayName("tryAcquire")
    @Test
    fun tryAcquireTest() {
        val ratelimiter = rateLimiterFactory.build("ratelimiter:test:tryAcquire", 10.0, 2)

        var t = thread(true) { ratelimiter.acquire(5) }
        Assertions.assertTrue(ratelimiter.tryAcquire(4, 1, TimeUnit.SECONDS))
        Assertions.assertTrue(ratelimiter.tryAcquire(21, 2, TimeUnit.SECONDS))
        Assertions.assertFalse(ratelimiter.tryAcquire(11, 1, TimeUnit.SECONDS))
        Assertions.assertTrue(ratelimiter.tryAcquire(10, 1500, TimeUnit.MILLISECONDS))
        t.join()

        Thread.sleep(2000)
        Assertions.assertTrue(ratelimiter.tryAcquire(15, 500, TimeUnit.MILLISECONDS))
        Assertions.assertTrue(ratelimiter.tryAcquire(5, 200, TimeUnit.MILLISECONDS))
    }
}