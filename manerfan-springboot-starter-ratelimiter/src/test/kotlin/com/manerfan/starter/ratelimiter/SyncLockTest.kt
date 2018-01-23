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
import org.springframework.stereotype.Component
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.ObjectUtils
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * @author manerfan
 * @date 2018/1/18
 */

@DisplayName("SyncLock同步锁测试")
@EnableAutoConfiguration
@EnableRedisRepositories
@SpringBootTest(classes = [RateLimiterAutoConfiguration::class])
@ExtendWith(SpringExtension::class)
class SyncLockTest {
    @Autowired
    lateinit var annotationTest: AnnotationTest

    @Autowired
    lateinit var syncLockFactory: SyncLockFactory

    @Autowired
    lateinit var redis: RedisTemplate<String, String>

    @BeforeEach
    fun beforeEach() {
        redis.execute { connection ->
            val keys = connection.keys("lock:test:*".toByteArray()).orEmpty().filterNotNull().toTypedArray()
            if (!ObjectUtils.isEmpty(keys)) {
                connection.del(*keys)
            }
        }
    }

    @DisplayName("SyncLockable注解测试")
    @Test
    fun annotationTest() {
        arrayOf(2000L, 5000L, 3000L, 4000L).map { sleep ->
            thread(true) {
                annotationTest.lock(sleep)
            }
        }.forEach { it.join() }
    }

    @DisplayName("lock测试")
    @Test
    fun lockTest() {
        val syncLock = syncLockFactory.build("lock:test:lock", 2, 5)

        println("===> " + System.currentTimeMillis())
        var startTime = System.currentTimeMillis()
        var t = thread(true) {
            syncLock.lock()
            Thread.sleep(1000)
            syncLock.unLock()
        }
        Thread.sleep(100)
        syncLock.lock()
        println("<=== " + System.currentTimeMillis())
        Assertions.assertTrue(System.currentTimeMillis() > startTime + 1000)
        syncLock.unLock()
        t.join()

        println("===> " + System.currentTimeMillis())
        startTime = System.currentTimeMillis()
        t = thread(true) {
            syncLock.lock()
            Thread.sleep(5000)
            syncLock.unLock()
        }
        Thread.sleep(100)
        syncLock.lock()
        println("<=== " + System.currentTimeMillis())
        Assertions.assertTrue(System.currentTimeMillis() > startTime + 2000)
        Assertions.assertTrue(System.currentTimeMillis() < startTime + 5000)
        syncLock.unLock()
        t.join()
    }

    @DisplayName("tryLock测试")
    @Test
    fun tryLockTest() {
        val syncLock = syncLockFactory.build("lock:test:tryLock", 5)
        var t = thread(true) {
            syncLock.lock()
            Thread.sleep(3000)
            syncLock.unLock()
        }

        Thread.sleep(100)
        Assertions.assertFalse(syncLock.tryLock())

        Thread.sleep(3000)
        Assertions.assertTrue(syncLock.tryLock())
        t.join()
    }

    @DisplayName("含过期时间的tryLock测试")
    @Test
    fun tryLockTimeoutTest() {
        val syncLock = syncLockFactory.build("lock:test:tryLock", 5)
        var t = thread(true) {
            syncLock.lock()
            Thread.sleep(5000)
            syncLock.unLock()
        }

        Thread.sleep(100)
        Assertions.assertFalse(syncLock.tryLock(3, TimeUnit.SECONDS))
        Assertions.assertTrue(syncLock.tryLock(2, TimeUnit.SECONDS))
        t.join()
    }
}

@Component
class AnnotationTest {
    var latestRun: Long = System.currentTimeMillis()
    var latestSleep: Long = 0

    @SyncLockable("lock:test:annotation", 10)
    fun lock(sleep: Long) {
        Assertions.assertTrue(latestRun + latestSleep < System.currentTimeMillis())

        latestRun = System.currentTimeMillis()
        latestSleep = sleep

        println("lock $sleep ${System.currentTimeMillis()}")
        Thread.sleep(sleep)
    }
}