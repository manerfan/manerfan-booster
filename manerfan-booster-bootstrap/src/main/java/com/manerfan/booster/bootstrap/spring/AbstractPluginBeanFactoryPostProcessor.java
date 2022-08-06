package com.manerfan.booster.bootstrap.spring;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import com.manerfan.booster.bootstrap.service.PluginLifeCycle;
import com.manerfan.booster.bootstrap.service.PluginService;
import com.manerfan.booster.core.util.collection.StreamUtils;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.Getter;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * AbstractPluginBeanFactoryPostProcessor
 *
 * <pre>
 *      注册插件
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public abstract class AbstractPluginBeanFactoryPostProcessor <T extends PluginService>
    implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    @Getter
    private ConfigurableEnvironment environment;

    @Override
    @SuppressWarnings("unchecked")
    public abstract void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;

    /**
     * 注册 plugin
     */
    protected void registerPlugins(BeanDefinitionRegistry beanDefinitionRegistry, Class<T> clazz) {
        registerPlugins(beanDefinitionRegistry, clazz, null, null);
    }

    /**
     * 注册 plugin
     */
    protected void registerPlugins(
        BeanDefinitionRegistry beanDefinitionRegistry, Class<T> clazz,
        String specifiedKey, Set<String> excludeKeys) {
        // SPI 加载
        val pluginLoader = ServiceLoader.load(clazz);

        // 创建代理并注册到 spring
        val specifiedKeyExists = StringUtils.isNotEmpty(specifiedKey);
        val excludeKeyExists = CollectionUtils.isNotEmpty(excludeKeys);
        val pluginServices = StreamUtils.iterator2Stream(pluginLoader.iterator())
            .filter(pluginService -> !excludeKeyExists || !excludeKeys.contains(pluginService.getKey()))
            .filter(pluginService ->
                !specifiedKeyExists || StringUtils.equalsIgnoreCase(pluginService.getKey(), specifiedKey))
            .map(pluginService -> wrapPluginService(clazz, pluginService))
            .peek(pluginService -> registerPluginBean(beanDefinitionRegistry, pluginService._2, pluginService._1))
            .map(pluginService -> pluginService._1)
            .collect(Collectors.toList());

        PluginServiceLoaderBanner.addLoadedPluginServices(clazz, pluginServices);
    }

    @SuppressWarnings("unchecked")
    private Tuple2<T, Class<T>> wrapPluginService(Class<T> superClass, T pluginService) {
        // 使用原始 cglib 而非 spring-cglib，避免（插件中引入spring包时）不必要的类冲突
        val enhancer = new Enhancer();
        enhancer.setSuperclass(pluginService.getClass());
        enhancer.setInterfaces(new Class[] {
            superClass, PluginService.class, PluginLifeCycle.class, PluginServiceWrapper.class});
        enhancer.setCallback(new PluginServiceInterceptor<>(pluginService));
        return Tuple.of((T)enhancer.create(), (Class<T>)pluginService.getClass());
    }

    /**
     * 将 plugin 注册为 bean
     */
    private void registerPluginBean(
        BeanDefinitionRegistry beanDefinitionRegistry, Class<T> clazz, T plugin) {
        beanDefinitionRegistry.registerBeanDefinition(
            generateBeanName(plugin),
            BeanDefinitionBuilder
                .genericBeanDefinition(clazz, () -> plugin)
                // com.manerfan.message.spi.service.PluginService#start
                .setInitMethodName("start")
                // com.manerfan.message.spi.service.PluginService#stop
                .setDestroyMethodName("stop")
                .getBeanDefinition()
        );
    }

    /**
     * 生成 bean name
     */
    private String generateBeanName(T plugin) {
        return String.format("%s#%s", plugin.getClass().getSimpleName(), plugin.getKey());
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {}

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment)environment;
    }
}
