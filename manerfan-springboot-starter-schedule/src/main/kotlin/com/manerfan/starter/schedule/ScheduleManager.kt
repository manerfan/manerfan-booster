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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.util.concurrent.ThreadFactoryBuilder
import net.greghaines.jesque.utils.ReflectionUtils
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.support.CronSequenceGenerator
import org.springframework.stereotype.Component
import org.springframework.util.ObjectUtils
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * 任务管理
 *
 * @author manerfan
 * @date 2018/1/24
 */

/**
 * 基于Redis的分布式任务管理
 */
@Configuration
@EnableScheduling
@ComponentScan(basePackages = ["com.manerfan.starter.schedule"])
class ScheduleManagerAutoConfiguration {
    private val availableProcessors = Runtime.getRuntime().availableProcessors()

    /**
     * 线程池
     *
     * @return ThreadPoolExecutor
     */
    @Bean
    @ConditionalOnMissingBean
    fun executorService() = ThreadPoolExecutor(
            availableProcessors * 16,
            availableProcessors * 32,
            5, TimeUnit.SECONDS,
            ArrayBlockingQueue(256),
            ThreadFactoryBuilder().setNameFormat("schedule-pool-%d").build())

    /**
     * JobTemplate Bean
     *
     * @param redisConnectionFactory Spring RedisConnectionFactory
     * @return JobTemplate
     */
    @Bean
    fun jobTemplate(redisConnectionFactory: RedisConnectionFactory): JobTemplate {
        val template = JobTemplate()
        template.connectionFactory = redisConnectionFactory
        template.setEnableTransactionSupport(true)
        return template
    }
}

/**
 * 操作任务数据的Redis模板
 */
class JobTemplate : RedisTemplate<String, JobEntity>() {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    init {
        keySerializer = StringRedisSerializer()
        valueSerializer = object : RedisSerializer<JobEntity> {
            override fun serialize(t: JobEntity) = objectMapper.writeValueAsBytes(t)
            override fun deserialize(bytes: ByteArray?) = bytes?.let { objectMapper.readValue(bytes, JobEntity::class.java) }
        }
    }
}

/**
 * Jackson Joda DateTime 序列化
 */
class DateTimeSerializer : JsonSerializer<DateTime>() {
    override fun serialize(date: DateTime, gen: JsonGenerator, arg: SerializerProvider?) {
        gen.writeNumber(date.millis)
    }
}

/**
 * Jackson Joda DateTime 反序列化
 */
class DateTimeDeserializer : JsonDeserializer<DateTime>() {
    override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): DateTime {
        return when (jsonParser.currentToken) {
            JsonToken.VALUE_NUMBER_INT -> DateTime(jsonParser.valueAsLong)
            JsonToken.VALUE_STRING -> DateTime.parse(jsonParser.text)
            else -> throw JsonMappingException.from(jsonParser, "Invalid Type ${jsonParser.currentToken.name}")
        }
    }
}

/**
 * 任务Job
 *
 * @property uuid 唯一标识
 * @property name 任务名
 * @property className 任务实现类路径
 * @property args 任务实现类构造函数参数 顺序必须与构造函数定义一致
 * @property vars 任务实现类属性值 key: 属性名 | value: 属性值，属性必须有setter方法
 * @property startedAt 任务的起始时间 默认从创建时刻开始
 * @property endedAt 任务的结束时间 默认无限期执行
 * @property cron 任务执行cron表达式
 * @property fixedRate 以该速率（毫秒）循环执行（若指定了cron，则该参数失效）
 * @property lastScheduledAt 记录上次执行任务的时间
 *
 * @constructor 创建任务Job
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class JobEntity(
        var uuid: String = UUID.randomUUID().toString(),
        var name: String? = null,

        var className: String,
        var args: Array<Any?>? = null,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) var vars: Map<String, Any?>? = null,

        @JsonDeserialize(using = DateTimeDeserializer::class)
        @JsonSerialize(using = DateTimeSerializer::class)
        var startedAt: DateTime? = null,

        @JsonDeserialize(using = DateTimeDeserializer::class)
        @JsonSerialize(using = DateTimeSerializer::class)
        var endedAt: DateTime? = null,

        cron: String? = null, //  优先使用此参数
        var fixedRate: Long? = null,
        var lastScheduledAt: Long? = null
) {
    init {
        if (null == cron && null == fixedRate) {
            throw IllegalArgumentException("Params cron and fixedRate should not be null both!")
        }
    }

    /**
     * cron表达式解析器
     */
    @JsonIgnore
    private var cronGenerator: CronSequenceGenerator? = cron?.let { CronSequenceGenerator(it, TimeZone.getTimeZone("Asia/Shanghai")) }

    /**
     * cron表达式
     */
    var cron = cron
        set(cron) {
            field = cron
            cronGenerator = cron?.let { CronSequenceGenerator(it, TimeZone.getTimeZone("Asia/Shanghai")) }
        }

    /**
     * 记录下次需要执行的时间
     */
    var nextScheduledAt: Long = -1
        private set

    /**
     * 计算并更新下次执行时间
     * 若指定endedAt且下次执行时间晚于endedAt，则说明任务已结束，并返回false
     *
     * @return 是否需要更新 | 是否已失效
     */
    fun updateNextScheduledAt(timestamp: Long = System.currentTimeMillis()): Boolean {
        val limit = startedAt?.let { max(it.millis, timestamp) } ?: timestamp

        nextScheduledAt = when {
            null != cronGenerator -> cronGenerator!!.next(Date(limit)).time
            null != fixedRate -> limit + fixedRate!!
            else -> nextScheduledAt
        }

        return endedAt?.let { it.millis > nextScheduledAt } ?: true
    }
}

/**
 * 任务管理
 */
@Component
class ScheduleManager {
    private val logger = LoggerFactory.getLogger(ScheduleManager::class.java)
    val objectMapper = jacksonObjectMapper()
    private val redisKeyPrefix = "scheduler"

    /**
     * SortedSet（有序集合）key前缀
     * member: job.uuid | score: job.nextScheduledAt
     */
    private val zKey = "$redisKeyPrefix:jobList".toByteArray()

    /**
     * Hash（哈希表）key前缀
     * field: job.uuid | value: job
     */
    private val hKey = "$redisKeyPrefix:job".toByteArray()

    @Autowired
    private lateinit var jobTemplate: JobTemplate

    @Autowired
    private lateinit var executorService: ExecutorService

    /**
     * 获取redis服务器时间
     */
    private val now get() = jobTemplate.execute { it.time() } ?: System.currentTimeMillis()

    /**
     * 任务Job执行器
     *
     * 每隔1秒运行一次
     * 1. 从SortedSet中将score在(0,now)之间的uuid取出
     * 2. 从Hash中将uuid对应的job取出
     * 3. 解析job，计算job的nextScheduledAt，并将job回写到redis中
     * 4. 执行job
     */
    @Scheduled(fixedRate = 1000) // 不使用cron是为了使集群中各节点执行时间随机分散开
    fun schedule() {

        /**
         * SortedSet（有序集合）中，member为job.uuid，score为job.nextScheduledAt
         * 将score在 (0, now) 之间的uuid取出
         * 其对应的即是现在需要执行的job
         */

        var connection = jobTemplate.connectionFactory.connection
        var keys: Set<ByteArray>?
        try {
            val now = System.currentTimeMillis().toDouble()
            connection.multi()
            connection.zRangeByScore(zKey, 0.0, now) // 将score在(0,now)之间的uuid取出
            connection.zRemRangeByScore(zKey, 0.0, now) // 同时从redis中删除
            keys = connection.exec()[0] as? Set<ByteArray>
        } finally {
            connection.close()
        }

        if (ObjectUtils.isEmpty(keys)) {
            return
        }

        /**
         * Hash（哈希表）中，field为job.uuid，value为job
         * 通过uuid将对应job取出
         */

        connection = jobTemplate.connectionFactory.connection
        var values: List<ByteArray>?
        try {
            connection.multi()
            connection.hMGet(hKey, *keys!!.toTypedArray()) // 将uuid对应的job取出
            connection.hDel(hKey, *keys.toTypedArray()) // 同时从redis中删除
            values = connection.exec()[0] as? List<ByteArray>
        } finally {
            connection.close()
        }

        if (ObjectUtils.isEmpty(values)) {
            return
        }

        // 解析jobs并回写到redis中
        val jobs = values!!.map {
            try {
                // 计算job的nextScheduledAt，并将其回写到redis中
                add(objectMapper.readValue(it, JobEntity::class.java))
            } catch (e: Exception) {
                logger.warn("JSON Parse Error {} {}", it.toString(), e.message)
                null
            }
        }

        // 执行jobs
        jobs.filterNotNull().forEach {
            var job = ReflectionUtils.createObject(Class.forName(it.className), it.args, it.vars)
            when (job) {
                is Runnable -> executorService.submit(job)
                else -> logger.warn("Job Must Implement Runnable {}", job)
            }
        }
    }

    /**
     * 添加任务Job
     *
     * 计算并更新job.nextScheduledAt
     * 若指定endedAt且nextScheduledAt晚于endedAt，则说明任务已结束，直接返回
     * 反之，将更新后的job存入redis
     *
     * @param job 任务
     *
     * @return job
     */
    fun add(job: JobEntity): JobEntity {
        if (!job.updateNextScheduledAt(now)) {
            logger.warn("Job is Arrived! {}", job.toString())
            // Update to DB
            return job
        }

        val connection = jobTemplate.connectionFactory.connection
        try {
            connection.multi()
            connection.hSet(hKey, job.uuid.toByteArray(), objectMapper.writeValueAsBytes(job))
            connection.zAdd(zKey, job.nextScheduledAt.toDouble(), job.uuid.toByteArray())
            connection.exec()
        } finally {
            connection.close()
        }

        return job
    }

    /**
     * 更新任务
     *
     * 1. 删除任务
     * 2. 计算并更新job.nextScheduledAt
     * 若指定endedAt且nextScheduledAt晚于endedAt，则说明任务已结束，直接返回
     * 反之，将更新后的job存入redis
     *
     * @param job 任务
     *
     * @return job
     */
    fun update(job: JobEntity): JobEntity {
        delete(job.uuid)
        return add(job)
    }

    /**
     * 删除任务
     *
     * 1. 从Hash中删除
     * 2. 从SortedSet中删除
     *
     * @param uuid 任务uuid
     */
    fun delete(uuid: String) {
        val connection = jobTemplate.connectionFactory.connection
        try {
            connection.multi()
            connection.hDel(hKey, uuid.toByteArray())
            connection.zRem(zKey, uuid.toByteArray())
            connection.exec()
        } finally {
            connection.close()
        }
    }
}



