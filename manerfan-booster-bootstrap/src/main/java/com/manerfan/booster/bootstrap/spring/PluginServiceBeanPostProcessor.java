package com.manerfan.booster.bootstrap.spring;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.manerfan.booster.bootstrap.loader.classloader.plugin.PluginClassLoader;
import com.manerfan.booster.bootstrap.loader.classloader.plugin.PluginModule;
import com.manerfan.booster.bootstrap.service.PluginContext;
import com.manerfan.booster.bootstrap.service.PluginService;
import com.manerfan.booster.core.util.collection.StreamUtils;
import io.vavr.Tuple;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.loader.archive.Archive.EntryFilter;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.InputStreamResource;

/**
 * PluginServiceBeanPostProcessor
 *
 * <pre>
 *     插件生命周期管理
 *
 *     postProcessBeforeInitialization  ->  {@link PluginService#init(PluginContext)}
 *     init-method                      ->  {@link PluginService#start()}
 *     destroy-method                   ->  {@link PluginService#stop()}
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Slf4j
public class PluginServiceBeanPostProcessor implements BeanPostProcessor, EnvironmentAware, BeanFactoryAware {
    private static final Set<String> PROPERTY_SUFFIX = Collections.singleton(".properties");
    private static final Set<String> YAML_SUFFIX = Stream.of(".yml", ".yaml").collect(Collectors.toSet());

    protected ConfigurableEnvironment appEnvironment;
    protected BeanFactory appBeanFactory;

    private final PropertiesPropertySourceLoader propertiesPropertySourceLoader = new PropertiesPropertySourceLoader();
    private final YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof PluginService)) {
            return bean;
        }

        val pluginService = (PluginService)bean;

        // 构建ConfigurableEnvironment
        val pluginServiceEnvironment = createPluginServiceEnvironment(appEnvironment, pluginService);
        val datasource = buildPluginDataSource();
        val pluginContext = PluginContext.builder()
            .environment(pluginServiceEnvironment)
            .dataSource(datasource)
            .beanFactory(appBeanFactory)
            .build();

        // 插件初始化
        Try.run(() -> pluginService.init(pluginContext)).onFailure(ex -> {
            throw new BeanInitializationException("[ManerFan Booster Bootstrap] Invoke PluginService init Error!", ex);
        });

        return bean;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.appEnvironment = (ConfigurableEnvironment)environment;
    }

    /**
     * 集成应用Environment，并读取插件Module下的配置文件
     *
     * @param appEnvironment 应用Environment
     * @param pluginService  {@link PluginService}
     * @return 插件Environment
     */
    private ConfigurableEnvironment createPluginServiceEnvironment(
        ConfigurableEnvironment appEnvironment, PluginService pluginService) {
        val pluginServiceEnvironment = buildPluginEnvironment();

        // 合并 appEnvironment
        pluginServiceEnvironment.merge(appEnvironment);
        // 相当于缓存的配置，会扰乱 Binder 逻辑
        pluginServiceEnvironment.getPropertySources().remove("configurationProperties");

        // 获取插件 Module
        val pluginModule = getPluginModule(pluginService);
        if (Objects.isNull(pluginModule) || Objects.isNull(pluginModule.getArchive())) {
            return pluginServiceEnvironment;
        }

        // 读取 Plugin Module 中的配置文件
        val pluginModulePropertySources = getPluginModuleProperties(
            pluginModule, PROPERTY_SUFFIX, propertiesPropertySourceLoader);
        val pluginModuleYamlSources = getPluginModuleProperties(
            pluginModule, YAML_SUFFIX, yamlPropertySourceLoader);

        // 添加到 pluginServiceEnvironment
        val propertySources = pluginServiceEnvironment.getPropertySources();
        if (propertySources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            // 添加到系统变量之后
            pluginModulePropertySources.forEach(ps ->
                propertySources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, ps));
            pluginModuleYamlSources.forEach(ps ->
                propertySources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, ps));
        } else {
            pluginModulePropertySources.forEach(propertySources::addFirst);
            pluginModuleYamlSources.forEach(propertySources::addFirst);
        }

        return pluginServiceEnvironment;
    }

    protected AbstractEnvironment buildPluginEnvironment() {
        return new StandardEnvironment();
    }

    protected DataSource buildPluginDataSource() {
        return null;
    }

    /**
     * 从{@link PluginModule}中获取配置
     *
     * @param pluginModel          {@link PluginModule}
     * @param supportFileSuffixes  支持的文件后缀
     * @param propertySourceLoader {@link PropertySourceLoader}
     * @return {@link PropertySource}
     */
    private List<PropertySource<?>> getPluginModuleProperties(
        PluginModule pluginModel, Set<String> supportFileSuffixes, PropertySourceLoader propertySourceLoader) {
        return Try.of(() -> {
            val moduleArchive = pluginModel.getArchive();
            val module = Paths.get(moduleArchive.getUrl().toURI()).getFileName();

            // 搜索所有配置文件
            EntryFilter propertyArchivesEntryFilter = entry -> {
                val inConfFolder = entry.getName().startsWith("config/");
                val propertiesFile = !entry.isDirectory() &&
                    supportFileSuffixes.stream().anyMatch(suffix -> entry.getName().endsWith(suffix));
                return inConfFolder && propertiesFile;
            };
            val propertyArchives = moduleArchive.getNestedArchives(null, propertyArchivesEntryFilter);

            // 读取所有配置文件
            val propertyContents = StreamUtils.iterator2Stream(propertyArchives)
                .map(archive -> Try.of(() -> Tuple.of(
                        // name
                        String.format("%s:%s", module, Paths.get(archive.getUrl().toURI()).getFileName()),
                        // inputStream
                        new InputStreamResource(archive.getUrl().openStream())
                    )).onFailure(ex -> log.warn("[ManerFan Booster Bootstrap] Load {} Error!", archive, ex))
                    .getOrNull())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            // 解析所有配置文件
            return propertyContents.stream()
                .flatMap(content -> Try.of(() -> {
                        val propertySources = propertySourceLoader.load(content._1, content._2);
                        return propertySources.stream();
                    }).onFailure(ex -> log.warn("[ManerFan Booster Bootstrap] Parse {} Error!", content._1, ex))
                    .getOrElse(Stream::empty))
                .collect(Collectors.toList());
        }).getOrElse(Collections.emptyList());
    }

    /**
     * 从{@link PluginService}获取{@link PluginModule}
     *
     * @param pluginService {@link PluginService}
     * @return {@link PluginModule}
     */
    private PluginModule getPluginModule(PluginService pluginService) {
        // 尝试拿到原始实例的类加载器（这里的pluginService多数为代理类）
        ClassLoader pluginServiceClassLoader;
        if (pluginService instanceof PluginServiceWrapper) {
            pluginServiceClassLoader = ((PluginServiceWrapper)pluginService).getTargetClassLoader();
        } else {
            pluginServiceClassLoader = AopUtils.getTargetClass(pluginService).getClassLoader();
        }

        // 尝试拿到PluginModule
        PluginModule pluginModule = null;
        if (pluginServiceClassLoader instanceof PluginClassLoader) {
            pluginModule = ((PluginClassLoader)pluginServiceClassLoader).getPluginModule();
        }
        return pluginModule;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.appBeanFactory = beanFactory;
    }
}
