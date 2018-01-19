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

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author manerfan
 * @date 2018/1/18
 */

/**
 * SyncLock同步锁工厂类
 */
@Component
class SyncLockFactory {
    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    private val syncLockMap = mutableMapOf<String, SyncLock>()

    /**
     * 创建SyncLock
     *
     * @param key Redis key
     * @param expire Redis TTL/秒，默认10秒
     * @param safetyTime 安全时间/秒，为了防止程序异常导致死锁，在此时间后强制拿锁，默认 expire * 5 秒
     */
    @Synchronized
    fun build(key: String, expire: Long = 10 /* seconds */, safetyTime: Long = expire * 5/* seconds */): SyncLock {
        if (!syncLockMap.containsKey(key)) {
            syncLockMap[key] = SyncLock(key, stringRedisTemplate, expire, safetyTime)
        }
        return syncLockMap[key]!!
    }
}

/**
 * 同步锁
 *
 * @property key  Redis key
 * @property stringRedisTemplate RedisTemplate
 * @property expire Redis TTL/秒
 * @property safetyTime 安全时间/秒
 * @constructor 请尽量避免直接构造该类，改用SyncLockFactory创建
 *
 * @see [SyncLockFactory]
 */
class SyncLock(
        private val key: String,
        private val stringRedisTemplate: StringRedisTemplate,
        private val expire: Long,
        private val safetyTime: Long
) {
    private val logger = LoggerFactory.getLogger(SyncLock::class.java)

    private val value = "Sync Lock for $key"
    private val waitPer: Long = 10

    /**
     * 尝试获取锁（立即返回）
     *
     * @return 是否获取成功
     *
     * @see [lock]
     * @see [unLock]
     */
    fun tryLock(): Boolean {
        val locked = stringRedisTemplate.opsForValue().setIfAbsent(key, value) ?: false
        if (locked) {
            stringRedisTemplate.expire(key, expire, TimeUnit.SECONDS)
        }
        return locked
    }

    /**
     * 尝试获取锁，并至多等待timeout时长
     *
     * @param timeout 超时时长
     * @param unit 时间单位
     *
     * @return 是否获取成功
     *
     * @see [tryLock]
     * @see [lock]
     * @see [unLock]
     */
    fun tryLock(timeout: Long, unit: TimeUnit): Boolean {
        val waitMax = unit.toMillis(timeout)
        var waitAlready: Long = 0

        while (stringRedisTemplate.opsForValue().setIfAbsent(key, value) != true && waitAlready < waitMax) {
            Thread.sleep(waitPer)
            waitAlready += waitPer
        }

        if (waitAlready < waitMax) {
            stringRedisTemplate.expire(key, expire, TimeUnit.SECONDS)
            return true
        }
        return false
    }

    /**
     * 获取锁
     *
     * @see [unLock]
     */
    fun lock() {
        val uuid = UUID.randomUUID().toString()
        logger.debug("======>[{}] lock {}", uuid, key)

        val waitMax = TimeUnit.SECONDS.toMillis(safetyTime)
        var waitAlready: Long = 0

        while (stringRedisTemplate.opsForValue().setIfAbsent(key, value) != true && waitAlready < waitMax) {
            Thread.sleep(waitPer)
            waitAlready += waitPer
        }

        // stringRedisTemplate.expire(key, expire, TimeUnit.SECONDS)
        stringRedisTemplate.opsForValue().set(key, value, expire, TimeUnit.SECONDS)

        logger.debug("<======[{}] lock {} [{} ms]", uuid, key, waitAlready)
    }

    /**
     * 释放锁
     *
     * @see [lock]
     * @see [tryLock]
     */
    fun unLock() {
        stringRedisTemplate.delete(key)
    }
}

/**
 * 同步锁注解
 *
 * @property key Redis key
 * @property expire Redis TTL/秒，默认10秒
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ASyncLock(
        val key: String,
        val expire: Long = 10
)

/**
 * 同步锁注解处理
 */
@Aspect
@Component
class SyncLockHandle {
    @Autowired
    private lateinit var syncLockFactory: SyncLockFactory

    /**
     * 在方法上执行同步锁
     */
    @Around("@annotation(syncLock)")
    fun syncLock(jp: ProceedingJoinPoint, syncLock: ASyncLock): Any? {
        val lock = syncLockFactory.build(syncLock.key, syncLock.expire)

        try {
            lock.lock()
            return jp.proceed()
        } finally {
            lock.unLock()
        }
    }
}