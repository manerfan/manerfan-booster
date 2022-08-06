package com.manerfan.booster.core.exception;

import java.util.Objects;

import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * ServiceException
 *
 * <pre>
 *     统一异常
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Getter
@ToString
public class ServiceException extends RuntimeException {
    private static final String DEF_ERR_MESSAGE = "SERVICE_ERROR";

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    public ServiceException(ErrorInfo errorInfo) {
        this(errorInfo, null, null);
    }

    public ServiceException(ErrorInfo errorInfo, Throwable throwable) {
        super(Objects.isNull(errorInfo) ? DEF_ERR_MESSAGE : errorInfo.getErrorMessage(), throwable);

        if (Objects.isNull(errorInfo)) {
            return;
        }

        this.errorCode = errorInfo.getErrorCode();
        this.errorMessage = errorInfo.getErrorMessage();
    }

    public ServiceException(ErrorInfo errorInfo, String errorMessage) {
        this(errorInfo, errorMessage, null);
    }

    public ServiceException(ErrorInfo errorInfo, String errorMessage, Throwable throwable) {
        super(errorMessage, throwable);

        if (Objects.isNull(errorInfo)) {
            return;
        }

        this.errorCode = errorInfo.getErrorCode();
        this.errorMessage = StringUtils.defaultString(errorMessage, errorInfo.getErrorMessage());
    }

    public ServiceException(String errorCode, String errorMessage) {
        this(errorCode, errorMessage, null);
    }

    public ServiceException(String errorCode, String errorMessage, Throwable throwable) {
        super(errorMessage, throwable);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static ServiceException build(ErrorInfo errorCode) {
        return new ServiceException(errorCode);
    }

    public static ServiceException build(ErrorInfo errorCode, Throwable throwable) {
        return new ServiceException(errorCode, throwable);
    }

    public static ServiceException build(String errorCode, String errorMessage) {
        return new ServiceException(errorCode, errorMessage);
    }

    public static ServiceException build(String errorCode, String errorMessage, Throwable throwable) {
        return new ServiceException(errorCode, errorMessage, throwable);
    }
}
