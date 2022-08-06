package com.manerfan.booster.bootstrap.spring;

/**
 * PluginServiceWrapper
 *
 * <pre>
 *      插件服务包装
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public interface PluginServiceWrapper {
    /**
     * 原实例
     *
     * @return 原实例
     */
    Object getTarget();

    /**
     * 原实例类
     *
     * @return 原实例类
     */
    Class<?> getTargetClass();

    /**
     * 原实例类加载器
     *
     * @return 原实例类加载器
     */
    ClassLoader getTargetClassLoader();
}
