package com.manerfan.booster.bootstrap.spring;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.manerfan.booster.bootstrap.service.PluginService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.aop.support.AopUtils;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

/**
 * PluginServiceInterceptor
 *
 * <pre>
 *      代理插件方法执行，在执行插件方法前后进行 ClassLoader 切换
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Slf4j
public class PluginServiceInterceptor<T extends PluginService> implements MethodInterceptor, PluginServiceWrapper {
    @Getter
    private final T target;
    @Getter
    private final Class<?> targetClass;
    @Getter
    private final ClassLoader targetClassLoader;

    private final Set<String> loggedMethodNames = Stream.of("init", "start", "stop").collect(Collectors.toSet());

    public PluginServiceInterceptor(T target) {
        this.target = Objects.requireNonNull(target);

        //ClassLoader classLoader = AopUtils.getTargetClass(target).getClassLoader();
        //if (classLoader instanceof PluginClassLoader) {
        //    classLoader = ((PluginClassLoader)classLoader).getPluginModule().getPluginClassLoader();
        //}
        //this.targetClassLoader = classLoader;
        this.targetClassLoader = AopUtils.getTargetClass(target).getClassLoader();
        this.targetClass = target.getClass();
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        // 方法的声明类
        val declaringClass = method.getDeclaringClass();

        // PluginServiceWrapper - 直接 invoke，不需要切换 ClassLoader
        if (PluginServiceWrapper.class.isAssignableFrom(declaringClass)) {
            return method.invoke(this, args);
        }

        // PluginLifeCycle - 生命周期调用日志
        val methodName = method.getName();
        if (loggedMethodNames.contains(methodName)) {
            log.info("[ManerFan Booster Bootstrap] LifeCycle {}#{}", targetClass.getName(), methodName);
        }

        // 当前的 ClassLoader
        ClassLoader sourceClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 切换为插件的 ClassLoader
            Thread.currentThread().setContextClassLoader(targetClassLoader);
            // 执行目标方法
            return methodProxy.invoke(target, args);
        } finally {
            // 恢复为当前的 ClassLoader
            // SecurityException setContextClassLoader
            Thread.currentThread().setContextClassLoader(sourceClassLoader);
        }
    }
}