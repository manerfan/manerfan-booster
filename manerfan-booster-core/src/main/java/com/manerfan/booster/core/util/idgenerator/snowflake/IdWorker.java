package com.manerfan.booster.core.util.idgenerator.snowflake;

import lombok.extern.slf4j.Slf4j;

/**
 * IdWorker
 *
 * <pre>
 *      基于snowfake算法的Id生成器
 *
 *      |-- high --|---------- low ----------|
 *
 *      44bits时间戳-32bits工作机器id-12bits序列号
 *
 *      - 可用 2^44 ms = 557 year
 *      - 可启动 2^32 = 全IPv4 实例
 *      - 单实例可并发 2^12 = 4096 保证不重复
 *      - 总体有序
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Slf4j
public class IdWorker implements IdGenerator {
    private static final int MAX_TRY_TIMES = 10;

    /**
     * 起始时间
     */
    static final Long TWEPOCH = 1464652800000L;

    /**
     * 时间戳占用位数
     */
    private static final int TIMESTAMP_BITS = 44;

    /**
     * workerId占用位数
     */
    private static final int WORKER_ID_BITS = 32;

    /**
     * 序列号占用位数
     */
    private static final int SEQUENCE_BITS = 12;

    /**
     * 时间戳可以使用的最大数值，防溢出
     */
    static final long TIMESTAMP_MASK = mask(TIMESTAMP_BITS);

    /**
     * workerId可以使用的最大数值，防溢出
     */
    static final long WORKER_ID_MASK = mask(WORKER_ID_BITS);

    /**
     * 序列号可以使用的最大数值，防溢出
     */
    static final long SEQUENCE_MASK = mask(SEQUENCE_BITS);

    /**
     * 时间戳左移位数
     */
    private static final int TIMESTAMP_SHIFT = 0;

    /**
     * workerId左移位数
     */
    static final int WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 序列号左移位数
     */
    private static final int SEQUENCE_SHIFT = 0;

    /**
     * 高位，转16进制字符占用位数
     */
    final static int HIGH_CHARS_NUM = (int)Math.ceil(TIMESTAMP_BITS / 4.0);

    /**
     * 地位，转16进制字符占用位数
     */
    final static int LOW_CHARS_NUM = (int)Math.ceil((WORKER_ID_BITS + SEQUENCE_BITS) / 4.0);

    /**
     * 上次计算使用的时间戳
     */
    private volatile long lastTimestamp = -1L;

    /**
     * 工作/机器 唯一ID
     */
    private final long workerId;

    /**
     * 序列号
     */
    private volatile long sequence = 0;

    public IdWorker(long workerId) {
        this.workerId = workerId;

        if (workerId > WORKER_ID_MASK || workerId < 0) {
            log.error("[snowflake] workerId should not be greater than $maxWorkerIdMask or less than 0");
            throw new IllegalArgumentException(
                "[snowflake] workerId should not be greater than $maxWorkerIdMask or less than 0");
        }

        log.info("[snowflake] Worker Started. Timestamp left shift {}, workerId: {}", TWEPOCH, workerId);
    }

    @Override
    public synchronized IdData nextId() {
        // 自旋次数
        int tryTimes = 0;
        long currMilliseconds;

        do {
            tryTimes++;
            currMilliseconds = System.currentTimeMillis();

            // 计算sequence
            if (lastTimestamp == currMilliseconds) {
                // 同一毫秒内的请求
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence <= 0) {
                    // 同一毫秒内，已将sequence用完，等待下一毫秒
                    currMilliseconds = tilNextMillis(currMilliseconds);
                    sequence = 0;
                }
            } else {
                sequence = 0;
            }

            // Clock is Moved backwards
        } while (currMilliseconds < lastTimestamp);

        if (tryTimes > MAX_TRY_TIMES) {
            log.warn(
                "[snowflake] Clock is Moved backwards, and IdWorker try {} Times! "
                    + "It maybe ugly if this warning appear too much times", tryTimes);
        }

        lastTimestamp = currMilliseconds;

        long low = ((workerId & WORKER_ID_MASK) << WORKER_ID_SHIFT) | (sequence & SEQUENCE_MASK);
        long high = (currMilliseconds - TWEPOCH) & TIMESTAMP_MASK;
        return IdData.builder().high(high).low(low).build();
    }

    /**
     * 二进制 生成bit个1 e.g. 5 for 0b0001_1111
     *
     * @param bit 1的个数
     * @return 生成的二进制对应的long
     */
    private static long mask(int bit) {
        return ~(-1L << bit);
    }

    /**
     * 阻塞等待到下一时间点
     *
     * @param lastTimestamp 下一时间点
     * @return 等待结束时刻
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
