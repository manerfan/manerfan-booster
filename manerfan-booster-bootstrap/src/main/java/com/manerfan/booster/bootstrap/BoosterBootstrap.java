package com.manerfan.booster.bootstrap;

import java.util.Arrays;

import com.manerfan.booster.bootstrap.loader.ReLaunchMainLauncher;
import io.vavr.control.Try;

/**
 * BoosterBootstrap
 *
 * <pre>
 *     启动器
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/3
 */
public class BoosterBootstrap {

    public static void run(String[] args) {
        if (!ReLaunchMainLauncher.needLoad(ReLaunchMainLauncher.class.getClassLoader())) {
            return;
        }

        // 创建新的 ClassLoader，重新执行入口函数
        new ReLaunchMainLauncher().launch(args, deduceMainApplicationClass().getName());
    }

    /**
     * 从栈里获取原始的main函数
     *
     * @return class which has main func
     */
    private static Class<?> deduceMainApplicationClass() {
        return Try.of(() -> Arrays.stream(new RuntimeException().getStackTrace())
            .filter(stackTraceElement -> "main".equals(stackTraceElement.getMethodName()))
            .findFirst()
            .map(stackTraceElement -> Try.of(() -> Class.forName(stackTraceElement.getClassName())).getOrNull())
            .orElse(null)
        ).getOrNull();
    }
}
