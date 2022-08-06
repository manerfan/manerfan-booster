package com.manerfan.booster.bootstrap.spring;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.manerfan.booster.bootstrap.loader.classloader.PluginContainerClassLoader;
import com.manerfan.booster.bootstrap.loader.loaderutils.ArchiveUtils;
import com.manerfan.booster.bootstrap.service.PluginService;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciithemes.a8.A8_Grids;
import io.vavr.control.Try;
import lombok.val;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.FileCopyUtils;

/**
 * PluginServiceLoaderBanner
 *
 * <pre>
 *      using spirng.factories org.springframework.boot.SpringApplicationRunListener
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public class PluginServiceLoaderBanner implements SpringApplicationRunListener {
    /**
     * 加载的插件
     */
    private static final Map<Class<? extends PluginService>, Collection<? extends PluginService>> PLUGIN_SERVICE_LOADED
        = Collections.synchronizedMap(new LinkedHashMap<>());

    public static void addLoadedPluginServices(
        Class<? extends PluginService> clazz, Collection<? extends PluginService> pluginServices) {
        PLUGIN_SERVICE_LOADED.put(clazz, pluginServices);
    }

    public PluginServiceLoaderBanner(SpringApplication application, String[] args) {}

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {
        // 打印 banner
        PluginServiceLoaderBanner.printBanner();
        // 打印加载的模块
        PluginServiceLoaderBanner.printPluginModules();

        System.out.println();
    }

    @Override
    public void started(ConfigurableApplicationContext context) {
        // 打印 banner
        PluginServiceLoaderBanner.printBanner();
        // 打印注册的插件
        PluginServiceLoaderBanner.printPluginService();

        System.out.println();
    }

    private static void printBanner() {
        System.out.printf("\n%s\n", getBanner());
    }

    /**
     * 打印加载的模块
     */
    private static void printPluginModules() {
        val classLoader = Thread.currentThread().getContextClassLoader();
        if (!(classLoader instanceof PluginContainerClassLoader)) {
            return;
        }
        System.out.println();

        val pluginModules = ((PluginContainerClassLoader)classLoader).getPluginModules();

        AsciiTable head = new AsciiTable() {{
            addRule();
            addRow("加载的模块");
            addRule();

            getContext().setGrid(A8_Grids.lineDoubleBlocks());
        }};
        System.out.println(head.render(100));

        pluginModules.forEach(pluginModule -> withArchivePath(pluginModule.getArchive(), modulePath -> {
            AsciiTable content = new AsciiTable() {{
                addRule();

                // 模块地址
                addRow(":: " +
                    modulePath.getParent().getFileName().toString() + File.separator +
                    modulePath.getFileName().toString());

                // 插件包
                addRow("");
                addRow("plugin archives:");
                pluginModule.getPluginClassLoader().getPluginArchives().forEach(pluginArchive ->
                    withArchivePath(pluginArchive, pluginPath -> addRow(pluginPath.getFileName().toString())));

                // 插件实现类
                addRow("");
                addRow("plugin service classes:");
                pluginModule.getCachedClasses().forEach((className, cachedClass) -> addRow(className));

                addRule();
            }};
            System.out.printf("\n%s\n", content.render(100));
        }));
    }

    /**
     * 打印注册的插件
     */
    private static void printPluginService() {
        System.out.println();

        AsciiTable head = new AsciiTable() {{
            addRule();
            addRow("注册的服务");
            addRule();

            getContext().setGrid(A8_Grids.lineDoubleBlocks());
        }};
        System.out.println(head.render(100));

        PLUGIN_SERVICE_LOADED.forEach((pluginServiceClass, pluginServices) -> {
            AsciiTable content = new AsciiTable() {{
                addRule();

                // SPI类
                addRow(":: " + pluginServiceClass.getName());

                pluginServices.forEach(pluginService -> {
                    Class<?> targetServiceClass = AopUtils.getTargetClass(pluginService);
                    if (pluginService instanceof PluginServiceWrapper) {
                        targetServiceClass = ((PluginServiceWrapper)pluginService).getTargetClass();
                    }
                    Class<?> finalServiceClass = targetServiceClass;
                    val targetArchive = Try.of(() -> ArchiveUtils.codeSourceArchive(finalServiceClass)).getOrNull();

                    addRow("");
                    // 插件key:插件名
                    addRow(pluginService.getKey() + ":" + pluginService.getName());
                    // 插件实现类
                    addRow(finalServiceClass.getName());
                    // 所在插件包
                    withArchivePath(targetArchive, path -> addRow(path.getFileName().toString()));
                });
                addRule();
            }};
            System.out.printf("\n%s\n", content.render(100));
        });
    }

    /**
     * 获取 banner 内容
     */
    private static String getBanner() {
        val bannerStream = PluginServiceLoaderBanner.class.getClassLoader().getResourceAsStream("bootstrap.txt");
        if (Objects.nonNull(bannerStream)) {
            return Try.of(() -> FileCopyUtils.copyToString(new InputStreamReader(bannerStream))).getOrElse("");
        } else {
            return "";
        }
    }

    private static void withArchivePath(Archive archive, Consumer<Path> consumer) {
        if (Objects.isNull(archive)) {
            return;
        }

        Try.of(() -> archive.getUrl().toURI()).map(Paths::get).onSuccess(consumer);
    }
}
