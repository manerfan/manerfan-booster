package com.manerfan.booster.bootstrap.loader.classloader;

import java.io.IOException;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.manerfan.booster.bootstrap.loader.classloader.plugin.PluginModule;
import com.manerfan.booster.bootstrap.loader.loaderutils.ClassLoaderUtils;
import lombok.Getter;

/**
 * PluginContainerClassLoader
 *
 * <pre>
 *     自定义 classloader
 *
 *     Class: AppClassLoader -> BizClassLoader -> Plugin Export PluginService Classes
 *     Resource: AppClassLoader -> BizClassLoader -> PluginClassLoader
 *
 *     BizClassLoader 通常为 PluginContainerClassLoader(spring-boot)
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public class PluginContainerClassLoader extends SecureClassLoader {
    /**
     * 插件 Module
     */
    @Getter
    private final List<PluginModule> pluginModules;

    private final ClassLoader[] allOrderedClassLoaders;

    public PluginContainerClassLoader(ClassLoader parentClassLoader, List<PluginModule> pluginModules) {
        super(parentClassLoader);
        this.pluginModules = pluginModules;
        this.allOrderedClassLoaders = Optional.ofNullable(pluginModules)
            .map(ms -> ms.stream().map(PluginModule::getPluginClassLoader))
            .orElse(Stream.empty())
            .toArray(ClassLoader[]::new);
    }

    /**
     * class 范围：application + plugin service classes
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return pluginModules.stream()
            .map(pluginModule -> pluginModule.loadCachedClass(name))
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(() -> new ClassNotFoundException(name));
    }

    /**
     * resource 范围：application + plugin modules
     */
    @Override
    protected URL findResource(String name) {
        return ClassLoaderUtils.getResource(name, allOrderedClassLoaders);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        return ClassLoaderUtils.getResources(name, allOrderedClassLoaders);
    }
}
