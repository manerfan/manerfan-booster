package com.manerfan.booster.core.exception.aspect;

import com.manerfan.booster.core.util.idgenerator.snowflake.Ipv4IdGenerator;

/**
 * BaseServiceContext
 *
 * <pre>
 *     服务上下文
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public interface BaseServiceContext {
    /**
     * 获取 trace id
     *
     * @return trace id
     */
    default String getTraceId() {
        return Ipv4IdGenerator.singleInstance().nextId().toHex();
    }

    /**
     * 获取客户端名称
     *
     * @return 客户端名称
     */
    default String getClientName() {
        return "";
    }
}
