package com.manerfan.booster.core.exception.aspect;

import java.util.Objects;

import com.google.common.collect.Maps;
import com.manerfan.booster.core.exception.annotation.ExCacheAndLog;
import com.manerfan.booster.core.exception.annotation.ExCacheAndLog.LogLevel;
import io.leangen.geantyref.TypeFactory;
import io.vavr.control.Try;
import lombok.Builder;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.MergedAnnotation;

/**
 * ExCatchLogMeta
 *
 * <pre>
 *
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Getter
@Builder
public class ExCatchLogMeta {
    /**
     * logger name for class
     */
    private final Class<?> clazz;

    /**
     * logger name
     */
    private final String name;

    /**
     * logger level
     */
    private final ExCacheAndLog.LogLevel level;

    /**
     * logger
     */
    private final Logger logger;

    public static ExCatchLogMeta from(MergedAnnotation<ExCacheAndLog> mergedAnnotation) {
        if (Objects.isNull(mergedAnnotation)) {
            return null;
        }

        return Try.of(() -> {
            val annotationValues = Maps.<String, Object>newHashMap();
            annotationValues.put("logClass", mergedAnnotation.getClass("logClass"));
            annotationValues.put("logName", mergedAnnotation.getString("logName"));
            annotationValues.put("logLevel", mergedAnnotation.getEnum("logLevel", LogLevel.class));
            return from(TypeFactory.annotation(ExCacheAndLog.class, annotationValues));
        }).getOrElse((ExCatchLogMeta)null);
    }

    public static ExCatchLogMeta from(ExCacheAndLog exCacheAndLog) {
        if (Objects.isNull(exCacheAndLog)) {
            return null;
        }

        ExCatchLogMeta.ExCatchLogMetaBuilder builder = ExCatchLogMeta.builder();
        builder.level(exCacheAndLog.logLevel());

        if (StringUtils.isNotBlank(exCacheAndLog.logName())) {
            builder.name(exCacheAndLog.logName());
            builder.logger(LoggerFactory.getLogger(exCacheAndLog.logName()));
        } else if (Object.class != exCacheAndLog.logClass()) {
            builder.clazz(exCacheAndLog.logClass());
            builder.logger(LoggerFactory.getLogger(exCacheAndLog.logClass()));
        } else {
            return null;
        }

        return builder.build();
    }
}
