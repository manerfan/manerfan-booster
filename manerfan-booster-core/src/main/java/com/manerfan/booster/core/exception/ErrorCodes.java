package com.manerfan.booster.core.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ErrorCodes
 *
 * <pre>
 *     常规异常
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Getter
@AllArgsConstructor
public enum ErrorCodes implements ErrorInfo {
    /**
     * 参数不合法
     */
    ILLEGAL_ARGUMENT("参数不合法"),

    /**
     * 不可预知的系统异常
     */
    SYSTEM_ERROR("系统异常"),

    /**
     * 系统启动异常
     */
    BOOTSTRAP_ERROR("系统启动异常"),

    /**
     * 调用第三方接口产生的异常
     */
    API_3RD_ERROR("第三方接口异常");

    private final String errorMessage;

    @Override
    public String getErrorCode() {
        return this.name();
    }
}
