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

package com.manerfan.starter.schedule

import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.function.Executable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.ObjectUtils
import java.util.*
import kotlin.concurrent.timer

/**
 * @author manerfan
 * @date 2018/1/24
 */

@DisplayName("ScheduleManager测试")
@EnableAutoConfiguration
@EnableRedisRepositories
@SpringBootTest(classes = [ScheduleManagerAutoConfiguration::class])
@ExtendWith(SpringExtension::class)
class ScheduleManagerTest {
    @Autowired
    lateinit var scheduleManager: ScheduleManager

    @Autowired
    lateinit var redis: RedisTemplate<String, String>

    @BeforeEach
    fun beforeEach() {
        redis.execute { connection ->
            val keys = connection.keys("scheduler:*".toByteArray()).orEmpty().filterNotNull().toTypedArray()
            if (!ObjectUtils.isEmpty(keys)) {
                connection.del(*keys)
            }
        }

        Job.init()
    }

    @DisplayName("args构造函数测试")
    @Test
    fun testArgs() {
        println("==== Test Args ====")

        Job.init()
        var job = scheduleManager.add(JobEntity(
                name = "job-test-args-1",
                className = Job::class.java.name,
                args = arrayOf("string-property-1", 1),
                cron = "*/2 * * * * ?"
        ))
        Thread.sleep(4000)
        scheduleManager.delete(job.uuid)
        Assertions.assertNotNull(Job.staticString)
        Assertions.assertNotNull(Job.staticInt)

        Job.init()
        job = scheduleManager.add(JobEntity(
                name = "job-test-args-2",
                className = Job::class.java.name,
                args = arrayOf("string-property-2"),
                cron = "*/2 * * * * ?"
        ))
        Thread.sleep(4000)
        scheduleManager.delete(job.uuid)
        Assertions.assertNotNull(Job.staticString)
        Assertions.assertNull(Job.staticInt)

        Job.init()
        job = scheduleManager.add(JobEntity(
                name = "job-test-args-3",
                className = Job::class.java.name,
                args = arrayOf(2),
                cron = "*/2 * * * * ?"
        ))
        Thread.sleep(4000)
        scheduleManager.delete(job.uuid)
        Assertions.assertNull(Job.staticString)
        Assertions.assertNotNull(Job.staticInt)
    }

    @DisplayName("vargs对象属性测试")
    @Test
    fun testVargs() {
        println("==== Test Vargs ====")

        Job.init()
        var job = scheduleManager.add(JobEntity(
                name = "job-test-vargs-1",
                className = Job::class.java.name,
                vars = mapOf("propStr" to "string-property-1", "propInt" to 1),
                cron = "*/2 * * * * ?"
        ))
        Thread.sleep(4000)
        scheduleManager.delete(job.uuid)
        Assertions.assertNotNull(Job.staticString)
        Assertions.assertNotNull(Job.staticInt)

        Job.init()
        job = scheduleManager.add(JobEntity(
                name = "job-test-vargs-2",
                className = Job::class.java.name,
                vars = mapOf("propStr" to "string-property-2"),
                cron = "*/2 * * * * ?"
        ))
        Thread.sleep(4000)
        scheduleManager.delete(job.uuid)
        Assertions.assertNotNull(Job.staticString)
        Assertions.assertNull(Job.staticInt)

        Job.init()
        job = scheduleManager.add(JobEntity(
                name = "job-test-vargs-3",
                className = Job::class.java.name,
                vars = mapOf("propInt" to 2),
                cron = "*/2 * * * * ?"
        ))
        Thread.sleep(4000)
        scheduleManager.delete(job.uuid)
        Assertions.assertNull(Job.staticString)
        Assertions.assertNotNull(Job.staticInt)
    }

    @DisplayName("cron表达式测试")
    @Test
    fun cronTest() {
        println("==== Test Cron ====")

        Job.init()
        var job = scheduleManager.add(JobEntity(
                name = "job-test-cron",
                className = Job::class.java.name,
                cron = "*/3 * * * * ?"
        ))
        Thread.sleep(10000)
        scheduleManager.delete(job.uuid)
        Job.timeArray.reduce { prev, time ->
            var diff = time - prev
            Assertions.assertAll(Executable { Assertions.assertTrue(diff > 2500) }, Executable { Assertions.assertTrue(diff < 3500) })
            time
        }
    }

    @DisplayName("fixedRate测试")
    @Test
    fun fixedRateTest() {
        Job.init()
        var job = scheduleManager.add(JobEntity(
                name = "job-test-cron",
                className = Job::class.java.name,
                fixedRate = 2000
        ))
        Thread.sleep(10000)
        scheduleManager.delete(job.uuid)
        Job.timeArray.reduce { prev, time ->
            var diff = time - prev
            Assertions.assertAll(Executable { Assertions.assertTrue(diff > 1500) }, Executable { Assertions.assertTrue(diff < 2500) })
            time
        }
    }

    @DisplayName("StartedAt EndedAt 测试")
    @Test
    fun startEndTest() {
        println("==== Test StartedAt EndedAt ====")

        Job.init()
        scheduleManager.add(JobEntity(
                name = "job-test-start-end",
                className = Job::class.java.name,
                startedAt = DateTime.now().plusSeconds(4),
                endedAt = DateTime.now().plusSeconds(8),
                fixedRate = 1000
        ))

        Thread.sleep(2000)
        Assertions.assertTrue(Job.timeArray.isEmpty())

        Thread.sleep(6000)
        Assertions.assertTrue(Job.timeArray.isNotEmpty())

        Thread.sleep(4000)
        Assertions.assertTrue(Job.timeArray.size <= 4)

        Job.timeArray.reduce { prev, time ->
            var diff = time - prev
            Assertions.assertAll(Executable { Assertions.assertTrue(diff > 500) }, Executable { Assertions.assertTrue(diff < 1500) })
            time
        }
    }

    @DisplayName("delete测试")
    @Test
    fun deleteTest() {
        println("==== Test Delete ====")

        Job.init()
        var job = scheduleManager.add(JobEntity(
                name = "job-test-delete",
                className = Job::class.java.name,
                cron = "*/1 * * * * ?"
        ))

        Thread.sleep(4000)
        scheduleManager.delete(job.uuid)

        val size = Job.timeArray.size

        Thread.sleep(6000)
        Assertions.assertTrue(Job.timeArray.size == size)
    }
}

class Job(propStr: String? = null, propInt: Int? = null) : Runnable {

    constructor(propStr: String) : this(propStr, null)
    constructor(propInt: Int) : this(null, propInt)
    constructor() : this(null, null)

    init {
        staticString = propStr
        staticInt = propInt
    }

    var propStr = propStr
        set(propStr) {
            field = propStr
            staticString = propStr
        }

    var propInt = propInt
        set(propInt) {
            field = propInt
            staticInt = propInt
        }

    companion object {
        var staticString: String? = null
        var staticInt: Int? = null
        var timeArray = emptyList<Long>().toMutableList()

        fun init() {
            staticString = null
            staticInt = null
            timeArray.clear()
        }
    }

    override fun run() {
        timeArray.add(System.currentTimeMillis())
        println("I'm Here! ${DateTime.now()} staticString:$staticString staticInt:$staticInt")
        println("timeArray: $timeArray")
    }
}
