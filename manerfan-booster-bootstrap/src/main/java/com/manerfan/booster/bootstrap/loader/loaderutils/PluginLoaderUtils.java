package com.manerfan.booster.bootstrap.loader.loaderutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.manerfan.booster.bootstrap.loader.classloader.PluginContainerClassLoader;
import com.manerfan.booster.bootstrap.loader.classloader.plugin.PluginClassLoader;
import com.manerfan.booster.bootstrap.loader.classloader.plugin.PluginModule;
import com.manerfan.booster.core.util.collection.StreamUtils;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.Archive.Entry;
import org.springframework.boot.loader.archive.Archive.EntryFilter;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.util.FileCopyUtils;

/**
 * PluginLoaderUtils
 *
 * <pre>
 *     插件加载工具
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Slf4j
public final class PluginLoaderUtils {
    /**
     * 插件所在完全路径
     */
    private static final String PLUGIN_LOCATION = "plugin.location";

    private static final String USER_HOME_PREFIX = "~";

    private PluginLoaderUtils() {}

    /**
     * 发现并加载插件
     *
     * @return {@link PluginModule}
     */
    public static List<PluginModule> loadPlugins(ClassLoader parentClassLoader) {
        try {
            // 查找 Plugin Root Archive
            val pluginRootArchive = PluginLoaderUtils.findExternalPluginRoot();
            if (Objects.isNull(pluginRootArchive)) {
                log.warn("[ManerFan Booster Bootstrap] Can not load plugins! Please check '-Dplugin.location=' !");
                return Collections.emptyList();
            }

            // 仅支持文件夹，暂不支持 fatjar
            if (!pluginRootArchive.isExploded()) {
                log.warn("[ManerFan Booster Bootstrap] Non-exploded Plugin Archive is not Supported! "
                    + "Put plugins into '-Dplugin.location='");
                return Collections.emptyList();
            }

            // 查找所有的插件
            val moduleArchives = pluginRootArchive.getNestedArchives(null, Entry::isDirectory);

            // 构建 Plugin Modules
            return StreamUtils.iterator2Stream(moduleArchives)
                .filter(Archive::isExploded)
                .map(archive -> PluginLoaderUtils.loadPlugin(archive, parentClassLoader))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Throwable ex) {
            throw new RuntimeException("[ManerFan Booster Bootstrap] load plugins error!", ex);
        }
    }

    /**
     * 构建插件容器Classloader
     * @param parentClassloader 父Classloader
     * @return Plugin Container Classloader
     */
    public static ClassLoader createContainerClassLoader(ClassLoader parentClassloader) {
        if (Objects.isNull(parentClassloader)) {
            parentClassloader = PluginLoaderUtils.class.getClassLoader();
        }

        // 加载插件
        val pluginModules = PluginLoaderUtils.loadPlugins(parentClassloader);

        // 构建 Plugin Container ClassLoader
        return new PluginContainerClassLoader(parentClassloader, pluginModules);
    }

    /**
     * 加载插件，创建独立的 ClassLoader 进行类隔离
     *
     * @return {@link PluginModule}
     */
    private static PluginModule loadPlugin(Archive archive, ClassLoader parentClassLoader) {
        return Try.of(() -> {
            val module = Paths.get(archive.getUrl().toURI()).getFileName();

            ////// 1. 插件自身包
            val pluginArchives = getPluginArchives(archive, module);
            if (pluginArchives.isEmpty()) {
                return null;
            }

            ////// 2. 插件依赖包
            val libArchives = getLibArchives(archive);

            ////// 3. classloader 配置
            val classloaderConfigs = getClassloaderConfigs(archive, module);

            ////// 4. 暴露的 services
            val services = getServices(archive, module);

            ////// 5. 构建 PluginModule
            val pluginClassLoader = new PluginClassLoader(
                pluginArchives, libArchives,
                parentClassLoader, services,
                classloaderConfigs._1, classloaderConfigs._2);
            return new PluginModule(archive, pluginClassLoader);
        }).onFailure(ex ->
            log.error("[ManerFan Booster Bootstrap] Plugin] Load Error! - {}", Try.of(archive::getUrl).getOrNull(), ex)
        ).getOrNull();
    }

    private static List<Archive> getLibArchives(Archive archive) throws IOException {
        // lib 中的所有 jar 包
        EntryFilter libArchivesEntryFilter = entry -> {
            val inLibFolder = entry.getName().startsWith("lib/");
            val jarFile = !entry.isDirectory() && entry.getName().endsWith(".jar");
            return inLibFolder && jarFile;
        };
        return StreamUtils.iterator2List(archive.getNestedArchives(null, libArchivesEntryFilter));
    }

    private static List<Archive> getPluginArchives(Archive archive, Path module) throws IOException {
        // jar 包 或者 classes 文件夹
        EntryFilter pluginArchivesEntryFilter = entry -> {
            val parent = Paths.get(entry.getName()).getParent();
            val inRoot = Objects.isNull(parent) || parent.endsWith(module);
            val jarFileOrClassesFolder = (!entry.isDirectory() && entry.getName().endsWith(".jar")) ||
                (entry.isDirectory() && entry.getName().endsWith("classes/"));
            return inRoot && jarFileOrClassesFolder;
        };
        return StreamUtils.iterator2List(archive.getNestedArchives(null, pluginArchivesEntryFilter));
    }

    @SneakyThrows
    private static Tuple2<Set<String>, Set<String>> getClassloaderConfigs(Archive archive, Path module) {
        EntryFilter classloaderEntryFilter = entry -> {
            val parent = Paths.get(entry.getName()).getParent();
            val inRoot = Objects.isNull(parent) || parent.endsWith(module);
            val classloaderProperties = !entry.isDirectory() &&
                "classloader.properties".equalsIgnoreCase(entry.getName());
            return inRoot && classloaderProperties;
        };
        val classloaderArchives = StreamUtils.iterator2List(
            archive.getNestedArchives(null, classloaderEntryFilter));

        if (CollectionUtils.isEmpty(classloaderArchives)) {
            return Tuple.of(Collections.emptySet(), Collections.emptySet());
        }

        val ac = classloaderArchives.get(0);

        val properties = new Properties();
        properties.load(ac.getUrl().openStream());

        val parenPackages = Arrays.stream(StringUtils.splitPreserveAllTokens(
                properties.getProperty("classloader.load.parent", StringUtils.EMPTY), ","))
            .map(StringUtils::trimToEmpty)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
        val pluginPackages = Arrays.stream(StringUtils.splitPreserveAllTokens(
                properties.getProperty("classloader.load.plugin", StringUtils.EMPTY), ","))
            .map(StringUtils::trimToEmpty)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());

        return Tuple.of(parenPackages, pluginPackages);
    }

    @SneakyThrows
    private static Set<String> getServices(Archive archive, Path module) {
        // services 中的所有文件
        EntryFilter serviceArchivesEntryFilter = entry -> {
            val inServicesFolder = entry.getName().startsWith("services/");
            val isFile = !entry.isDirectory();
            return inServicesFolder && isFile;
        };
        val serviceArchives = StreamUtils.iterator2List(
            archive.getNestedArchives(null, serviceArchivesEntryFilter));

        // 获取所有文件中的内容，一行一个类路径
        return CollectionUtils.emptyIfNull(serviceArchives).stream()
            .map(ac -> Try.of(() -> ac.getUrl().openStream()))
            .filter(Try::isSuccess)
            .map(Try::get)
            .flatMap(serviceInputStream -> Try.of(() ->
                    FileCopyUtils.copyToString(new InputStreamReader(serviceInputStream)))
                .map(content -> Stream.of(content.split("[\r\n]")))
                .onFailure(ex -> log.error("[ManerFan Booster Bootstrap] Read {} Error!", serviceInputStream, ex))
                .getOrNull())
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());

    }

    /**
     * 查找 Plugin Root Archive
     *
     * @return Plugin Root {@link Archive}
     */
    private static Archive findExternalPluginRoot() throws IOException {
        String pluginLocation = System.getProperty(PLUGIN_LOCATION);
        if (Objects.isNull(pluginLocation)) {
            return null;
        }

        File sar = new File(pluginLocation);
        if (!sar.exists()) {
            if (pluginLocation.trim().startsWith(USER_HOME_PREFIX)) {
                log.error(
                    "[ManerFan Booster Bootstrap] Please use full file path in '-Dplugin.location=', '~' only works in shell.");
                log.error(
                    "[ManerFan Booster Bootstrap] Try to set '-Dplugin.location={}'",
                    pluginLocation.replaceFirst("^~", System.getProperty("user.home")));
            }
            return null;
        }

        return sar.isDirectory() ? new ExplodedArchive(sar, false) : new JarFileArchive(sar);
    }
}
