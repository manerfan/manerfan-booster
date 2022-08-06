package com.manerfan.booster.bootstrap.loader;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;

import com.manerfan.booster.bootstrap.BoosterBootstrap;
import com.manerfan.booster.bootstrap.loader.loaderutils.ArchiveUtils;
import com.manerfan.booster.bootstrap.loader.loaderutils.IsolatedThreadGroup;
import com.manerfan.booster.bootstrap.loader.loaderutils.JavaAgentDetector;
import com.manerfan.booster.bootstrap.loader.loaderutils.PluginLoaderUtils;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.loader.archive.Archive;

/**
 * ReLaunchMainLauncher
 *
 * <pre>
 *      main relaunch
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Slf4j
public class ReLaunchMainLauncher {
    /**
     * relaunch 是否构建完成
     */
    private static volatile boolean relaunchCreated = false;
    private static final String RELAUNCH_CREATED_NAME = "relaunchCreated";

    private final Archive archive;

    public ReLaunchMainLauncher() {
        try {
            this.archive = ArchiveUtils.codeSourceArchive(getClass());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 判断是否需要加载
     *
     * @return true-false
     */
    public static boolean needLoad(ClassLoader classLoader) {
        return !getLoaderStatus(classLoader, RELAUNCH_CREATED_NAME);
    }

    /**
     * 标记插件加载状态为完成
     *
     * @param classLoader {@link ClassLoader}
     */
    public static void markLoaded(ClassLoader classLoader) {
        markLoaderStatus(classLoader, RELAUNCH_CREATED_NAME, true);
    }

    /**
     * 在指定 classloader 中设置(静态)属性值
     *
     * @param classLoader ClassLoader
     * @param fieldName   属性
     * @param value       值
     */
    static void markLoaderStatus(ClassLoader classLoader, String fieldName, Object value) {
        Try.run(() -> {
            Class<?> pluginLauncherClass = classLoader.loadClass(ReLaunchMainLauncher.class.getName());
            Field declaredField = pluginLauncherClass.getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            declaredField.set(null, value);
        });
    }

    /**
     * 在指定classloader中读取(静态)属性值
     *
     * @param classLoader ClassLoader
     * @param fieldName   属性
     * @return 属性值
     */
    static boolean getLoaderStatus(ClassLoader classLoader, String fieldName) {
        return Try.of(() -> {
            Class<?> pluginLauncherClass = classLoader.loadClass(ReLaunchMainLauncher.class.getName());
            Field declaredField = pluginLauncherClass.getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            return (Boolean)declaredField.get(null);
        }).getOrElse(false);
    }

    /**
     * 以Main函数ClassLoader的urls重新构造一个 ClassLoader，加载Pandora，再重新加载Main函数，
     * 解决部分类因为类型检测被提前加载的问题
     *
     * @param args      启动参数
     * @param mainClass 入口 class
     */
    public void launch(String[] args, String mainClass) {
        // reLaunch 里以一个新线程，新classloader启动main函数，并等待新的main函数线程退出
        reLaunch(args, mainClass, createClassLoader());

        // 执行到这里，新启动的main线程已经退出了，可以直接退出进程
        System.exit(0);
    }

    /**
     * 创建新的线程，在新的线程组中使用自定义 ClassLoader 重新 launch
     *
     * @param args        启动参数
     * @param mainClass   入口类
     * @param classLoader 自定义 ClassLoader
     */
    @SuppressWarnings("all")
    private void reLaunch(String[] args, String mainClass, ClassLoader classLoader) {
        IsolatedThreadGroup threadGroup = new IsolatedThreadGroup(mainClass);
        Thread launchThread = new Thread(threadGroup, new LaunchRunner(args, mainClass), "main");

        launchThread.setContextClassLoader(classLoader);
        launchThread.start();
        LaunchRunner.join(threadGroup);
        threadGroup.rethrowUncaughtException();
    }

    /**
     * 构建 ClassLoader
     *
     * @return {@link ClassLoader}
     */
    private ClassLoader createClassLoader() {
        val bizAppClassLoader = BoosterBootstrap.class.getClassLoader();

        // 构建 Plugin Container Classloader ClassLoader
        val reLaunchClassLoader = PluginLoaderUtils.createContainerClassLoader(bizAppClassLoader);

        // 标记插件加载完成
        ReLaunchMainLauncher.markLoaded(ReLaunchMainLauncher.class.getClassLoader());

        return reLaunchClassLoader;
    }

    /**
     * 清理 agent jar的url，防止出现agent jar里的类被重复加载的情况
     *
     * @param urls 原 ClassLoader 加载的 urls
     * @return 过滤后需要加载的 urls
     */
    private static URL[] cleanJavaAgentUrls(URL[] urls) {
        val javaAgentDetector = new JavaAgentDetector();
        return Arrays.stream(urls).filter(url -> !javaAgentDetector.isJavaAgentJar(url)).toArray(URL[]::new);
    }
}
