package com.manerfan.booster.bootstrap.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * EnablePluginRegistry
 *
 * <pre>
 *      激活插件注册
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
    PluginBeanFactoryPostProcessorSelector.class,
    PluginServiceBeanPostProcessor.class
})
public @interface EnablePluginRegistry {
    Class<? extends AbstractPluginBeanFactoryPostProcessor> beanFactoryPostProcessorClass();
}
