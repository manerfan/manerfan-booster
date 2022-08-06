package com.manerfan.booster.api.common.dto.response;

import com.alibaba.fastjson.annotation.JSONField;

import com.manerfan.booster.api.common.dto.Dto;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * StatMonitor
 *
 * <pre>
 *     统计 监控
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/5
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StatMonitor extends Dto {
    /**
     * 执行时间(毫秒)
     */
    private long elapsedTime;

    /**
     * Trace ID
     */
    private String traceId;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 方法
     */
    private String method;

    /**
     * 携带的exception
     *
     * <p>
     * 用于信息统计，不做序列化
     * </p>
     */
    @Getter(AccessLevel.NONE)
    @JSONField(serialize = false, deserialize = false)
    private transient Throwable exception;

    public Throwable withException() {
        return exception;
    }
}
