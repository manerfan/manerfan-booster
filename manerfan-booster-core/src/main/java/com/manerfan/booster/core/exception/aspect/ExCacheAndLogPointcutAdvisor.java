package com.manerfan.booster.core.exception.aspect;

import com.manerfan.booster.core.exception.annotation.ExCacheAndLog;
import lombok.Getter;
import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

/**
 * ExCacheAndLogPointcutAdvisor
 *
 * <pre>
 *      ExCacheAndLog切面定义
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Getter
public class ExCacheAndLogPointcutAdvisor extends AbstractPointcutAdvisor {
    /**
     * 切面拦截
     */
    private final Advice advice;

    /**
     * 切面规则
     */
    private final Pointcut pointcut;

    public ExCacheAndLogPointcutAdvisor(BaseServiceContext serviceContext) {
        this.advice = buildAdvice(serviceContext);
        this.pointcut = buildPointcut();
    }

    private Advice buildAdvice(BaseServiceContext serviceContext) {
        return new ExCatchAndLogAdvice(serviceContext);
    }

    private Pointcut buildPointcut() {
        // 被ExCacheAndLog修饰的方法或类(中所有public方法)
        return new AnnotationMatchingPointcut(ExCacheAndLog.class, true);
    }
}