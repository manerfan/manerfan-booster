package com.manerfan.booster.core.exception.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.manerfan.booster.api.common.dto.response.Response;
import org.springframework.validation.annotation.Validated;

/**
 * ExCacheAndLog
 *
 * <pre>
 *      添加该注解的类，如果方法返回类型为{@link Response}，在抛出异常时会自动包装Response返回
 *
 *       e.g.
 *
 *       使用默认日志
 *       - @ExCacheAndLog
 *       public class WriteServiceImpl
 *
 *       使用WriteServiceImpl.class获取logger
 *       - @ExCacheAndLog(logClass = WriteServiceImpl.class)
 *       public class WriteServiceImpl
 *
 *       使用 logger "SYS"
 *       - @ExCacheAndLog(logName = "SYS")
 *       public class WriteServiceImpl
 *
 *       优先使用 logger "SYS"
 *       - @ExCacheAndLog(logClass = WriteServiceImpl.class, logName = "SYS")
 *       public class WriteServiceImpl
 *
 *       设置日志级别
 *       - @ExCacheAndLog(logName = "SYS", logLevel = LogLevel.WARN)
 *       public class WriteServiceImpl
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Validated
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExCacheAndLog {
    /**
     * logger class
     * <p>
     * 如果指定该参数，则使用该class获取logger，否则使用logName获取logger (若两者均指定，则优先使用logName)
     * </p>
     */
    Class<?> logClass() default Object.class;

    /**
     * logger name
     * <p>
     * 如果指定该参数，则使用该name获取logger，否则使用logClass获取logger (若两者均不指定，则不输出日志)
     * </p>
     */
    String logName() default "";

    /**
     * log level
     * <p>
     * 日志级别，默认 {@link LogLevel#WARN}
     * </p>
     */
    LogLevel logLevel() default LogLevel.WARN;

    /**
     * 日志级别
     */
    public static enum LogLevel {
        /**
         * trace
         */
        TRACE,

        /**
         * debug
         */
        DEBUG,

        /**
         * info
         */
        INFO,

        /**
         * warn
         */
        WARN,

        /**
         * error
         */
        ERROR,

        /**
         * fatal
         */
        FATAL,

        /**
         * 关闭
         */
        OFF;
    }
}
