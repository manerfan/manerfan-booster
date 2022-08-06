package com.manerfan.booster.core.exception;

/**
 * BizException
 *
 * <pre>
 *     业务异常
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public class BizException extends ServiceException {
    public BizException(ErrorInfo errorInfo) {
        super(errorInfo);
    }

    public BizException(ErrorInfo errorInfo, Throwable throwable) {
        super(errorInfo, throwable);
    }

    public BizException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public BizException(ErrorInfo errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public BizException(String errorCode, String errorMessage, Throwable throwable) {
        super(errorCode, errorMessage, throwable);
    }

    public static BizException build(ErrorInfo errorCode) {
        return new BizException(errorCode);
    }

    public static BizException build(ErrorInfo errorCode, Throwable throwable) {
        return new BizException(errorCode, throwable);
    }

    public static BizException build(ErrorInfo errorCode, String errorMessage) {
        return new BizException(errorCode, errorMessage);
    }

    public static BizException build(String errorCode, String errorMessage) {
        return new BizException(errorCode, errorMessage);
    }

    public static BizException build(String errorCode, String errorMessage, Throwable throwable) {
        return new BizException(errorCode, errorMessage, throwable);
    }
}
