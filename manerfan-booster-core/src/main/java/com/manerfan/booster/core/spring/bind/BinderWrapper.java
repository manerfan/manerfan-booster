package com.manerfan.booster.core.spring.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

import org.springframework.beans.PropertyValues;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySources;
import org.springframework.util.ClassUtils;

/**
 * BinderWrapper
 *
 * <pre>
 *      兼容 spring boot 1.x 和 spring boot 2.x 的 Binder
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/7
 */
public class BinderWrapper {
    private static boolean existBinder = false;
    private static boolean existRelaxedDataBinder = false;
    private static final ClassLoader CL = BinderWrapper.class.getClassLoader();

    private static final String BINDER_CLASS = "org.springframework.boot.context.properties.bind.Binder";
    private static final String RELAXED_BINDER_CLASS = "org.springframework.boot.bind.RelaxedDataBinder";
    private static final String PROPERTY_SOURCE_CLASS = "org.springframework.boot.bind.PropertySourcesPropertyValues";

    static {
        try {
            existBinder = ClassUtils.isPresent(BINDER_CLASS, CL);
        } catch (Throwable ignored) {}
        try {
            existRelaxedDataBinder = ClassUtils.isPresent(RELAXED_BINDER_CLASS, CL);
        } catch (Throwable ignored) {}
    }

    /**
     * Bind the specified target {@link Class} using this binder's {@link ConfigurationPropertySource property sources}
     *
     * @param environment the environment source (must have attached {@link ConfigurationPropertySources})
     * @param prefix      the configuration property name to bind
     * @param type        the target class
     * @return the bound value
     */
    public static <T> T bind(ConfigurableEnvironment environment, String prefix, Class<T> type) {
        if (existRelaxedDataBinder) {
            return relaxedDataBinderBind(environment, prefix, type);
        } else if (existBinder) {
            return binderBind(environment, prefix, type);
        } else {
            throw new IllegalStateException("Can not find " + BINDER_CLASS + " or " + RELAXED_BINDER_CLASS);
        }
    }

    private static Class<?> Binder_Class;
    private static Constructor<?> Binder_Constructor;
    private static Constructor<?> Property_Shource_Constructor;
    private static Class<?> Property_Source_Class;
    private static Method Binder_Method;

    /**
     * 适用于 spring boot 2.x 的 binder
     */
    private static <T> T binderBind(ConfigurableEnvironment environment, String prefix, Class<T> type) {
        try {
            return Binder.get(environment).bind(prefix, type).get();
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 适用于 spring boot 1.x 的 binder
     */
    private static <T> T relaxedDataBinderBind(ConfigurableEnvironment environment, String prefix, Class<T> type) {
        T instance;
        try {
            instance = type.newInstance();

            if (Objects.isNull(Binder_Class)) {
                Binder_Class = ClassUtils.forName(RELAXED_BINDER_CLASS, CL);
                Binder_Constructor = Binder_Class.getDeclaredConstructor(Object.class, String.class);
            }
            Object binder = Binder_Constructor.newInstance(instance, prefix);

            if (Objects.isNull(Property_Source_Class)) {
                Property_Source_Class = ClassUtils.forName(PROPERTY_SOURCE_CLASS, CL);
                Property_Shource_Constructor = Property_Source_Class.getDeclaredConstructor(PropertySources.class);
            }
            Object propertyValue = Property_Shource_Constructor.newInstance(environment.getPropertySources());

            if (Objects.isNull(Binder_Method)) {
                Binder_Method = Binder_Class.getMethod("bind", PropertyValues.class);
            }
            Binder_Method.invoke(binder, propertyValue);
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }

        return instance;
    }
}
