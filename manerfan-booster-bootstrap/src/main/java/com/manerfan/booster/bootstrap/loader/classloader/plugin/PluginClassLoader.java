package com.manerfan.booster.bootstrap.loader.classloader.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.manerfan.booster.bootstrap.service.PluginService;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.SetUtils;
import org.springframework.boot.loader.archive.Archive;

/**
 * PluginClassLoader
 *
 * <pre>
 *      插件 ClassLoader
 *      包含 插件自身及插件依赖
 *
 *      Class:      [BizClassLoader ->] AppClassLoader -> Plugin jars
 *      Resource:   AppClassLoader -> Plugin jars
 *
 *      BizClassLoader 通常为 PluginContainerClassLoader(spring-boot)
 *
 *      对于一些共用的包，优先使用BizClassLoader（包括主应用的依赖）
 *      由于主应用与插件并非完全隔离，而是相互有依赖
 *      否则容易引起同class被不同classloader加载的异常
 *
 *      Caused by: java.lang.LinkageError: loader constraint violation:
 *      when resolving method 'org.slf4j.ILoggerFactory org.slf4j.impl.StaticLoggerBinder.getLoggerFactory()'
 *      the class loader com.manerfan.message.boot.loader.classloader.plugin.PluginClassLoader @258e2e41 of the current class, org/slf4j/LoggerFactory,
 *      and the class loader org.springframework.boot.loader.LaunchedURLClassLoader @66048bfd for the method's defining class, org/slf4j/impl/StaticLoggerBinder,
 *      have different Class objects for the type org/slf4j/ILoggerFactory used in the signature
 *      (org.slf4j.LoggerFactory is in unnamed module of loader com.manerfan.message.boot.loader.classloader.plugin.PluginClassLoader @258e2e41, parent loader 'app'; org.slf4j.impl.StaticLoggerBinder is in unnamed module of loader org.springframework.boot.loader.LaunchedURLClassLoader @66048bfd, parent loader 'app')
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Slf4j
public class PluginClassLoader extends URLClassLoader {
    /**
     * 为防止类冲突，对于特殊的包，共享 parent classloader
     */
    private final Set<String> USING_PARENT_CL;

    /**
     * 尝试优先在 plugin classloader 中加载的包
     */
    private final Set<String> USING_PLUGIN_CL;

    /**
     * 关联的 plugin module
     */
    @Getter
    @Setter
    private PluginModule pluginModule;

    @Getter
    private final List<Archive> pluginArchives;
    private final List<Archive> libArchives;
    private final Set<String> services;

    /**
     * 应用自身 ClassLoader
     */
    private final ClassLoader parentClassLoader;

    /**
     * 构造PluginClassLoader
     *
     * @param pluginArchives    插件主体
     * @param libArchives       插件依赖
     * @param parentClassLoader 父级 classloader
     * @param services          暴露的服务
     * @param loadFromParentCl  指定从 parent classloader 加载的类
     * @param loadFromPluginCl  指定从 plugin classloader 加载的类，优先级高于 loadFromBizCl
     */
    public PluginClassLoader(
        List<Archive> pluginArchives, List<Archive> libArchives,
        ClassLoader parentClassLoader, Set<String> services,
        Set<String> loadFromParentCl, Set<String> loadFromPluginCl) {
        super(Stream.concat(pluginArchives.stream(), libArchives.stream())
            .map(libArchive -> Try.of(libArchive::getUrl).getOrNull())
            .filter(Objects::nonNull)
            .toArray(URL[]::new)
        );

        this.pluginArchives = pluginArchives;
        this.libArchives = libArchives;
        this.parentClassLoader = parentClassLoader;

        this.services = services;

        // 固定使用父级 classloader 加载的类，这里写死可能不太合适
        this.USING_PARENT_CL = SetUtils.union(Stream.of(
            "com.manerfan.booster",
            "com.alibaba.fastjson",
            "org.springframework",
            "org.slf4j"
        ).collect(Collectors.toSet()), SetUtils.emptyIfNull(loadFromParentCl));

        // 强制使用 plugin classloader 加载的类
        this.USING_PLUGIN_CL = SetUtils.emptyIfNull(loadFromPluginCl);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // 从已加载的类中返回，(由于以下直接使用了 findClass) 否则会引起 attempted duplicate class definition 异常
        val loadedClass = findLoadedClass(name);
        if (Objects.nonNull(loadedClass)) {
            return loadedClass;
        }

        // 如果在 USING_PLUGIN_CL 中，优先在 plugin classloader 中加载
        val attemptPluginCl = USING_PLUGIN_CL.stream().anyMatch(name::startsWith);
        val clazzAttemptPlugin = attemptPluginCl ? Try.of(() -> super.findClass(name)).getOrNull() : null;
        if (Objects.nonNull(clazzAttemptPlugin)) {
            return clazzAttemptPlugin;
        }

        // 如果在 USING_PARENT_CL 中，优先从 parent classloader 中加载
        val usingBizAppCl = USING_PARENT_CL.stream().anyMatch(name::startsWith);
        val clazzFromBiz = usingBizAppCl ? Try.of(() -> parentClassLoader.loadClass(name)).getOrNull() : null;
        if (Objects.nonNull(clazzFromBiz)) {
            return clazzFromBiz;
        }

        // 从 plugin classloader 中加载
        val clazzFromPlugin = Try.of(() -> super.findClass(name)).getOrNull();
        if (Objects.nonNull(clazzFromPlugin)) {
            return clazzFromPlugin;
        }

        // 父类加载器中加载
        return super.getParent().loadClass(name);
    }

    /**
     * 从 META-INF/services 中导出所有 PluginService Class
     *
     * @return key: class-name ; value: class
     */
    public Map<String, Class<?>> exportPluginServiceClasses() throws ClassNotFoundException {
        // 从META-INF/services中读取需要导出的类名
        val pluginServiceClasses = new HashMap<String, Class<?>>(32);
        for (String className : services) {
            // 3. 通过 ClassLoader 加载类，导出供 PluginContainerClassLoader 使用
            val clazz = this.loadClass(className);
            if (PluginService.class.isAssignableFrom(clazz)) {
                pluginServiceClasses.put(className, clazz);
            }
        }

        return pluginServiceClasses;
    }
}