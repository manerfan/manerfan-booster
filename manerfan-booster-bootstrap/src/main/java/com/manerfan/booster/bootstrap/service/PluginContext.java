package com.manerfan.booster.bootstrap.service;

import javax.sql.DataSource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * PluginContext
 *
 * <pre>
 *     插件上下文
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginContext {
    /**
     * system properties + system environment + conf/*.properties
     */
    private ConfigurableEnvironment environment;

    /**
     * spring bean factory
     */
    private BeanFactory beanFactory;

    /**
     * 数据库 DataSource
     */
    private DataSource dataSource;

}
