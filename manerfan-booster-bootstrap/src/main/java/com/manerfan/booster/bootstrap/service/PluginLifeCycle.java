package com.manerfan.booster.bootstrap.service;

import com.manerfan.booster.bootstrap.exception.PluginException;

/**
 * PluginLifeCycle
 *
 * <pre>
 *     插件生命周期
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public interface PluginLifeCycle {
    /**
     * 初始化服务
     *
     * @param context 上下文 {@link PluginContext}
     * @throws PluginException
     */
    default void init(PluginContext context) throws PluginException {}

    /**
     * 启动服务
     *
     * @throws PluginException
     */
    default void start() throws PluginException {}

    /**
     * 停止服务，容器停止时会回调该方法
     *
     * @throws PluginException
     */
    default void stop() throws PluginException {}
}
