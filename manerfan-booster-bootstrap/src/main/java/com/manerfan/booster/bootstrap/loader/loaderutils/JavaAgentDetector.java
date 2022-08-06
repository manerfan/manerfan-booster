package com.manerfan.booster.bootstrap.loader.loaderutils;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * JavaAgentDetector
 *
 * <pre>
 *     通过 JVM 参数中的 -javaagent 探测 agent 包
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public class JavaAgentDetector {
    /**
     * java agent 参数的前缀
     */
    private static final String JAVA_AGENT_PREFIX = "-javaagent:";

    private final Set<URL> javaAgentJars;

    public JavaAgentDetector() {
        this.javaAgentJars = getJavaAgentJars(getInputArguments());
    }

    /**
     * 获取 JVM 参数
     */
    private static List<String> getInputArguments() {
        try {
            return AccessController.doPrivileged(
                (PrivilegedAction<List<String>>)() -> ManagementFactory.getRuntimeMXBean().getInputArguments());
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    /**
     * 获取 agent jars
     *
     * @param inputArguments JVM 参数
     * @return agent jars 列表
     */
    private Set<URL> getJavaAgentJars(List<String> inputArguments) {
        Set<URL> javaAgentJars = new HashSet<>();
        for (String argument : inputArguments) {
            String path = getJavaAgentJarPath(argument);
            if (Objects.nonNull(path)) {
                try {
                    // 添加到 agent jars 列表中
                    javaAgentJars.add(new File(path).getCanonicalFile().toURI().toURL());
                } catch (IOException ex) {
                    throw new IllegalStateException(
                        "Failed to determine canonical path of Java agent at path '" + path + "'");
                }
            }
        }
        return javaAgentJars;
    }

    /**
     * 从 JVM 参数中判断&提取 agent jar
     *
     * @param arg JVM 参数
     * @return agent jar 路径
     */
    private String getJavaAgentJarPath(String arg) {
        if (arg.startsWith(JAVA_AGENT_PREFIX)) {
            String path = arg.substring(JAVA_AGENT_PREFIX.length());
            int equalsIndex = path.indexOf('=');
            if (equalsIndex > -1) {
                path = path.substring(0, equalsIndex);
            }
            return path;
        }
        return null;
    }

    /**
     * 判断是否 java agent
     *
     * @param url jar url
     * @return true-false
     */
    public boolean isJavaAgentJar(URL url) {
        return this.javaAgentJars.contains(url);
    }
}
