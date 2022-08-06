package com.manerfan.booster.core.exception;

/**
 * SysException
 *
 * <pre>
 *     系统异常
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public class SysException extends ServiceException {
    private static final String DEF_ERR_MESSAGE = "SYSTEM_ERROR";

    public SysException(ErrorInfo errorInfo) {
        super(errorInfo);
    }

    public SysException(ErrorInfo errorInfo, Throwable throwable) {
        super(errorInfo, throwable);
    }

    public SysException(ErrorInfo errorInfo, String errorMessage) {
        super(errorInfo, errorMessage);
    }

    public SysException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public SysException(String errorCode, String errorMessage, Throwable throwable) {
        super(errorCode, errorMessage, throwable);
    }

    public static SysException build(ErrorInfo errorCode) {
        return new SysException(errorCode);
    }

    public static SysException build(ErrorInfo errorCode, String errorMessage) {
        return new SysException(errorCode, errorMessage);
    }

    public static SysException build(ErrorInfo errorCode, Throwable throwable) {
        return new SysException(errorCode, throwable);
    }

    public static SysException build(String errorCode, String errorMessage) {
        return new SysException(errorCode, errorMessage);
    }

    public static SysException build(String errorCode, String errorMessage, Throwable throwable) {
        return new SysException(errorCode, errorMessage, throwable);
    }
}