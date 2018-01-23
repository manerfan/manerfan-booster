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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.base.Preconditions
import com.google.common.math.LongMath
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * 参考 com.google.common.util.concurrent.RateLimiter
 * [Guava RateLimiter源码解析](https://segmentfault.com/a/1190000012875897)
 *
 * @author manerfan
 * @date 2018/1/22
 */

/**
 * 基于Redis的分布式流量控制
 */
@Configuration
class RateLimiterConfiguration {

    /**
     * PermitsTemplate Bean
     *
     * @param redisConnectionFactory Spring RedisConnectionFactory
     * @return PermitsTemplate
     */
    @Bean
    fun permitsTemplate(
            redisConnectionFactory: RedisConnectionFactory): PermitsTemplate {
        val template = PermitsTemplate()
        template.connectionFactory = redisConnectionFactory
        return template
    }
}

/**
 * 操作令牌数据的Redis模板
 */
class PermitsTemplate : RedisTemplate<String, RedisPermits>() {
    private val objectMapper = jacksonObjectMapper()

    init {
        keySerializer = StringRedisSerializer()
        valueSerializer = object : RedisSerializer<RedisPermits> {
            override fun serialize(t: RedisPermits) = objectMapper.writeValueAsBytes(t)
            override fun deserialize(bytes: ByteArray?) = bytes?.let { objectMapper.readValue(it, RedisPermits::class.java) }
        }
    }
}

/**
 * 令牌桶数据模型
 *
 * @property maxPermits 最大存储令牌数
 * @property storedPermits 当前存储令牌数
 * @property intervalMillis 添加令牌时间间隔
 * @property nextFreeTicketMillis 下次请求可以获取令牌的起始时间，默认当前系统时间
 *
 * @constructor 构建Redis令牌数据模型
 */
class RedisPermits(
        val maxPermits: Long,
        var storedPermits: Long,
        val intervalMillis: Long,
        var nextFreeTicketMillis: Long = System.currentTimeMillis()
) {
    /**
     * 构建Redis令牌数据模型
     *
     * @param permitsPerSecond 每秒放入的令牌数
     * @param maxBurstSeconds maxPermits由此字段计算，最大存储maxBurstSeconds秒生成的令牌
     * @param nextFreeTicketMillis 下次请求可以获取令牌的起始时间，默认当前系统时间
     */
    constructor(permitsPerSecond: Double, maxBurstSeconds: Int = 60, nextFreeTicketMillis: Long = System.currentTimeMillis()) :
            this((permitsPerSecond * maxBurstSeconds).toLong(), permitsPerSecond.toLong(), (TimeUnit.SECONDS.toMillis(1) / permitsPerSecond).toLong(), nextFreeTicketMillis)

    /**
     * 计算redis-key过期时长（秒）
     *
     * @return redis-key过期时长（秒）
     */
    fun expires(): Long {
        val now = System.currentTimeMillis()
        return 2 * TimeUnit.MINUTES.toSeconds(1) + TimeUnit.MILLISECONDS.toSeconds(max(nextFreeTicketMillis, now) - now)
    }

    /**
     * if nextFreeTicket is in the past, reSync to now
     * 若当前时间晚于nextFreeTicketMicros，则计算该段时间内可以生成多少令牌，将生成的令牌加入令牌桶中并更新数据
     *
     * @return 是否更新
     */
    fun reSync(now: Long): Boolean {
        if (now > nextFreeTicketMillis) {
            storedPermits = min(maxPermits, storedPermits + (now - nextFreeTicketMillis) / intervalMillis)
            nextFreeTicketMillis = now
            return true
        }
        return false
    }
}

/**
 * RateLimiter工厂类
 */
@Component
class RateLimiterFactory {
    @Autowired
    private lateinit var permitsTemplate: PermitsTemplate

    @Autowired
    private lateinit var syncLockFactory: SyncLockFactory

    private val rateLimiterMap = mutableMapOf<String, RateLimiter>()

    /**
     * 创建RateLimiter
     *
     * @param key Redis key
     * @param permitsPerSecond 每秒放入的令牌数
     * @param maxBurstSeconds 最大存储maxBurstSeconds秒生成的令牌
     *
     * @return RateLimiter
     */
    @Synchronized
    fun build(key: String, permitsPerSecond: Double, maxBurstSeconds: Int = 60): RateLimiter {
        if (!rateLimiterMap.containsKey(key)) {
            rateLimiterMap[key] = RateLimiter(key, permitsPerSecond, maxBurstSeconds, permitsTemplate, syncLockFactory.build("$key:lock"))
        }
        return rateLimiterMap[key]!!
    }
}

/**
 * RateLimiter实现类
 *
 * @property key Redis key
 * @property permitsPerSecond 每秒放入的令牌数
 * @property maxBurstSeconds 最大存储maxBurstSeconds秒生成的令牌
 * @property permitsTemplate Redis操作模板
 * @property syncLock 分布式同步锁
 *
 * @constructor 请尽量避免直接构造该类，改用RateLimiterFactory创建
 *
 * @see [SyncLockFactory]
 */
class RateLimiter(
        private val key: String,
        private val permitsPerSecond: Double,
        private val maxBurstSeconds: Int = 60,
        private val permitsTemplate: PermitsTemplate,
        private val syncLock: SyncLock) {
    private val logger = LoggerFactory.getLogger(RateLimiter::class.java)

    /**
     * 生成并存储默认令牌桶
     */
    private fun putDefaultPermits(): RedisPermits {
        val permits = RedisPermits(permitsPerSecond, maxBurstSeconds)
        permitsTemplate.opsForValue().set(key, permits, permits.expires(), TimeUnit.SECONDS)
        return permits
    }

    /**
     * 获取/更新令牌桶
     */
    private var permits: RedisPermits
        get() = permitsTemplate.opsForValue()[key] ?: putDefaultPermits()
        set(permits) = permitsTemplate.opsForValue().set(key, permits, permits.expires(), TimeUnit.SECONDS)

    /**
     * Acquires the given number of tokens from this {@code RateLimiter}, blocking until the request
     * can be granted. Tells the amount of time slept, if any.
     *
     * @param tokens the number of tokens to acquire
     * @return time spent sleeping to enforce rate, in milliseconds; 0 if not rate-limited
     * @throws IllegalArgumentException if the requested number of token is negative or zero
     */
    @Throws(IllegalArgumentException::class)
    fun acquire(tokens: Long): Long {
        var milliToWait = reserve(tokens)
        logger.info("acquire for {}ms {}", milliToWait, Thread.currentThread().name)
        Thread.sleep(milliToWait)
        return milliToWait
    }

    /**
     * Acquires a single token from this {@code RateLimiter}, blocking until the request can be
     * granted. Tells the amount of time slept, if any.
     *
     * <p>This method is equivalent to {@code acquire(1)}.
     *
     * @return time spent sleeping to enforce rate, in milliseconds; 0 if not rate-limited
     */
    @Throws(IllegalArgumentException::class)
    fun acquire() = acquire(1)

    /**
     * Acquires the given number of tokens from this {@code RateLimiter} if it can be obtained
     * without exceeding the specified {@code timeout}, or returns {@code false} immediately (without
     * waiting) if the tokens would not have been granted before the timeout expired.
     *
     * @param tokens the number of tokens to acquire
     * @param timeout the maximum time to wait for the tokens. Negative values are treated as zero.
     * @param unit the time unit of the timeout argument
     * @return {@code true} if the tokens were acquired, {@code false} otherwise
     * @throws IllegalArgumentException if the requested number of token is negative or zero
     */
    @Throws(IllegalArgumentException::class)
    fun tryAcquire(tokens: Long, timeout: Long, unit: TimeUnit): Boolean {
        val timeoutMicros = max(unit.toMillis(timeout), 0)
        checkTokens(tokens)

        var milliToWait: Long
        try {
            syncLock.lock()
            if (!canAcquire(tokens, timeoutMicros)) {
                return false
            } else {
                milliToWait = reserveAndGetWaitLength(tokens)
            }
        } finally {
            syncLock.unLock()
        }
        Thread.sleep(milliToWait)

        return true
    }

    /**
     * Acquires a token from this {@code RateLimiter} if it can be obtained without exceeding the
     * specified {@code timeout}, or returns {@code false} immediately (without waiting) if the token
     * would not have been granted before the timeout expired.
     *
     * <p>This method is equivalent to {@code tryAcquire(1, timeout, unit)}.
     *
     * @param timeout the maximum time to wait for the token. Negative values are treated as zero.
     * @param unit the time unit of the timeout argument
     * @return {@code true} if the token was acquired, {@code false} otherwise
     * @throws IllegalArgumentException if the requested number of token is negative or zero
     */
    @Throws(IllegalArgumentException::class)
    fun tryAcquire(timeout: Long, unit: TimeUnit) = tryAcquire(1, timeout, unit)

    /**
     * 获取redis服务器时间
     */
    private val now get() = permitsTemplate.execute { it.time() } ?: System.currentTimeMillis()

    /**
     * Reserves the given number of tokens from this {@code RateLimiter} for future use, returning
     * the number of milliseconds until the reservation can be consumed.
     *
     * @param tokens the number of tokens to acquire
     * @return time in milliseconds to wait until the resource can be acquired, never negative
     * @throws IllegalArgumentException if the requested number of tokens is negative or zero
     */
    @Throws(IllegalArgumentException::class)
    private fun reserve(tokens: Long): Long {
        checkTokens(tokens)
        try {
            syncLock.lock()
            return reserveAndGetWaitLength(tokens)
        } finally {
            syncLock.unLock()
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun checkTokens(tokens: Long) {
        Preconditions.checkArgument(tokens > 0, "Requested tokens $tokens must be positive")
    }

    private fun canAcquire(tokens: Long, timeoutMillis: Long): Boolean {
        return queryEarliestAvailable(tokens) - timeoutMillis <= 0
    }

    /**
     * Returns the earliest milliseconds to wait that tokens are available (with one caveat).
     *
     * @param tokens the number of tokens to acquire
     * @return the milliseconds to wait that tokens are available, or, if tokens are available immediately, zero or positive
     */
    private fun queryEarliestAvailable(tokens: Long): Long {
        val n = now
        var permit = permits
        permit.reSync(n)

        val storedPermitsToSpend = min(tokens, permit.storedPermits) // 可以消耗的令牌数
        val freshPermits = tokens - storedPermitsToSpend // 需要等待的令牌数
        val waitMillis = freshPermits * permit.intervalMillis // 需要等待的时间

        return LongMath.saturatedAdd(permit.nextFreeTicketMillis - n, waitMillis)
    }

    /**
     * Reserves next ticket and returns the wait time that the caller must wait for.
     *
     * @param tokens the number of tokens to acquire
     * @return the required wait time, never negative
     */
    private fun reserveAndGetWaitLength(tokens: Long): Long {
        val n = now
        var permit = permits
        permit.reSync(n)

        val storedPermitsToSpend = min(tokens, permit.storedPermits) // 可以消耗的令牌数
        val freshPermits = tokens - storedPermitsToSpend // 需要等待的令牌数
        val waitMillis = freshPermits * permit.intervalMillis // 需要等待的时间

        permit.nextFreeTicketMillis = LongMath.saturatedAdd(permit.nextFreeTicketMillis, waitMillis)
        permit.storedPermits -= storedPermitsToSpend
        permits = permit

        return permit.nextFreeTicketMillis - n
    }
}
