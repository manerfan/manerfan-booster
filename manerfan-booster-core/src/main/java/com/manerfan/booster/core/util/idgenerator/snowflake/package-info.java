/**
 * 基于SnowFlake的分布式ID生成器
 *
 * <pre>
 *      - 轻量级全局ID生成器，不依赖任何中间件
 *      - 可使用2^44ms=557年
 *      - 可支持并发4096次/毫秒生成不重复
 *      - 使用本机IP作为WorkerID
 *      - 生成的ID总体有序
 *      - ID可反解（哪个IP在何时生成）
 *      - 格式化为24字符
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
package com.manerfan.booster.core.util.idgenerator.snowflake;