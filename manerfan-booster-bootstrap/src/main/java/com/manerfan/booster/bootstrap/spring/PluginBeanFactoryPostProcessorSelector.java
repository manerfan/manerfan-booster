package com.manerfan.booster.bootstrap.spring;

import java.util.Collections;
import java.util.Optional;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * PluginBeanFactoryPostProcessorSelector
 *
 * <pre>
 *
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public class PluginBeanFactoryPostProcessorSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return Optional.ofNullable(
                importingClassMetadata.getAllAnnotationAttributes(EnablePluginRegistry.class.getName()))
            .map(attrs -> attrs.get("beanFactoryPostProcessorClass"))
            .orElse(Collections.emptyList())
            .stream()
            .map(clazz -> (Class<?>)clazz)
            .map(Class::getName)
            .toArray(String[]::new);
    }
}
