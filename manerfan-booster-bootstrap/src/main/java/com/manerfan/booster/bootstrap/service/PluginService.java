package com.manerfan.booster.bootstrap.service;

/**
 * PluginService
 *
 * <pre>
 *     插件服务
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public interface PluginService extends PluginLifeCycle {
    /**
     * 服务名称
     *
     * @return 服务名称
     */
    String getName();

    /**
     * 服务唯一标识
     *
     * @return 服务唯一标识
     */
    String getKey();

    /**
     * 服务描述
     *
     * @return 服务描述
     */
    default String getDescription() {return null;}

    /**
     * 服务帮助文档
     *
     * @return 服务帮助文档
     */
    default String getDocUrl() {return null;}
}
