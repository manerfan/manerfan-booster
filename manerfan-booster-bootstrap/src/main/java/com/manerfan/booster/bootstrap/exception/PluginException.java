package com.manerfan.booster.bootstrap.exception;

import com.manerfan.booster.core.exception.ErrorInfo;
import com.manerfan.booster.core.exception.ServiceException;

/**
 * PluginException
 *
 * <pre>
 *     插件异常
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public class PluginException extends ServiceException {
    public PluginException(ErrorInfo errorInfo) {
        super(errorInfo);
    }

    public PluginException(ErrorInfo errorInfo, Throwable throwable) {
        super(errorInfo, throwable);
    }

    public PluginException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public PluginException(ErrorInfo errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public PluginException(String errorCode, String errorMessage, Throwable throwable) {
        super(errorCode, errorMessage, throwable);
    }

    public static PluginException build(ErrorInfo errorCode) {
        return new PluginException(errorCode);
    }

    public static PluginException build(ErrorInfo errorCode, Throwable throwable) {
        return new PluginException(errorCode, throwable);
    }

    public static PluginException build(ErrorInfo errorCode, String errorMessage) {
        return new PluginException(errorCode, errorMessage);
    }

    public static PluginException build(String errorCode, String errorMessage) {
        return new PluginException(errorCode, errorMessage);
    }

    public static PluginException build(String errorCode, String errorMessage, Throwable throwable) {
        return new PluginException(errorCode, errorMessage, throwable);
    }
}
