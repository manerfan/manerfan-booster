package com.manerfan.booster.core.exception.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RequestId
 *
 * <pre>
 *      标识 request id 参数
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestId {
}
