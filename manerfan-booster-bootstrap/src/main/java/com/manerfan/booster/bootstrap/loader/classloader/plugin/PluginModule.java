package com.manerfan.booster.bootstrap.loader.classloader.plugin;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.vavr.control.Try;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;
import org.springframework.boot.loader.archive.Archive;

/**
 * PluginModule
 *
 * <pre>
 *     插件模块
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Getter
public class PluginModule {
    /**
     * plugin module archive
     */
    private final Archive archive;

    /**
     * plugin classloader
     */
    private final PluginClassLoader pluginClassLoader;

    private final Map<String, Class<?>> cachedClasses = new HashMap<>();

    public PluginModule(Archive archive, PluginClassLoader pluginClassLoader) throws ClassNotFoundException {
        this.archive = archive;
        this.pluginClassLoader = pluginClassLoader;
        this.pluginClassLoader.setPluginModule(this);

        cachePluginServiceClasses();
    }

    public String getName() {
        return Optional.ofNullable(archive).map(ac ->
            Try.of(() -> Paths.get(ac.getUrl().toURI()).getFileName().toString()).getOrNull()
        ).orElse(null);
    }

    private synchronized void cachePluginServiceClasses() throws ClassNotFoundException {
        if (MapUtils.isEmpty(cachedClasses)) {
            synchronized (cachedClasses) {
                cachedClasses.putAll(this.pluginClassLoader.exportPluginServiceClasses());
            }
        }
    }

    public Class<?> loadCachedClass(String name) {
        return cachedClasses.get(name);
    }
}
