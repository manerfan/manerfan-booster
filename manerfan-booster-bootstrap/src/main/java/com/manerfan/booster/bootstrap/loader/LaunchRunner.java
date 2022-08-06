package com.manerfan.booster.bootstrap.loader;

import java.lang.reflect.Method;
import java.util.Objects;

import lombok.AllArgsConstructor;

/**
 * LaunchRunner
 *
 * <pre>
 *      在一个新的线程中启动 main
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@AllArgsConstructor
public class LaunchRunner implements Runnable {
    /**
     * 启动参数
     */
    private final String[] args;

    /**
     * 入口 class
     */
    private final String startClassName;

    @Override
    public void run() {
        Thread thread = Thread.currentThread();
        ClassLoader classLoader = thread.getContextClassLoader();
        try {
            // 使用自定义 ClassLoader 加载入口类
            Class<?> startClass = classLoader.loadClass(this.startClassName);
            // 调用入口类 main 方法
            Method mainMethod = startClass.getMethod("main", String[].class);
            if (!mainMethod.isAccessible()) {
                // FIXME call canAccess(Object) when jdk version >= 9
                mainMethod.setAccessible(true);
            }
            mainMethod.invoke(null, new Object[] {this.args});
        } catch (NoSuchMethodException ex) {
            Exception wrappedEx = new Exception(
                "The specified mainClass doesn't contain a " + "main method with appropriate signature.", ex);
            thread.getThreadGroup().uncaughtException(thread, wrappedEx);
        } catch (Exception ex) {
            thread.getThreadGroup().uncaughtException(thread, ex);
        }
    }

    /**
     * 等待线程组中所有非Daemon线程结束
     *
     * @param threadGroup 线程组
     */
    public static void join(ThreadGroup threadGroup) {
        boolean hasNonDaemonThreads;
        do {
            hasNonDaemonThreads = false;

            // 拉出线程组中所有线程
            Thread[] threads = new Thread[threadGroup.activeCount()];
            threadGroup.enumerate(threads);

            // 等待线程组中所有非Daemon线程结束
            for (Thread thread : threads) {
                if (Objects.nonNull(thread) && !thread.isDaemon()) {
                    // 非Daemon线程
                    try {
                        hasNonDaemonThreads = true;
                        // 等待结束
                        thread.join();
                    } catch (InterruptedException ex) {
                        // 线程中断处理
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } while (hasNonDaemonThreads);
    }
}
