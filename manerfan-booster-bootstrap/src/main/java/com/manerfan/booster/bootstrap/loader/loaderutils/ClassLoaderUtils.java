package com.manerfan.booster.bootstrap.loader.loaderutils;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.stream.Collectors;

import com.manerfan.booster.core.util.collection.StreamUtils;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import sun.misc.Unsafe;

/**
 * ClassLoaderUtils
 *
 * <pre>
 *     类加载工具
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Slf4j
public class ClassLoaderUtils {
    private static final String JDK_9_CL_PREFIX = "jdk.internal.loader.ClassLoaders$";

    /**
     * 获取 ClassLoader 的 Urls
     *
     * @param classLoader ClassLoader
     * @return Urls
     */
    @SuppressWarnings({"unchecked"})
    public static URL[] getUrls(ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader) {
            return ((URLClassLoader)classLoader).getURLs();
        }

        // >= jdk9
        if (classLoader.getClass().getName().startsWith(JDK_9_CL_PREFIX)) {
            return Try.of(() -> {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                Unsafe unsafe = (Unsafe)field.get(null);

                // jdk.internal.loader.ClassLoaders.AppClassLoader.ucp
                Field ucpField = classLoader.getClass().getDeclaredField("ucp");
                long ucpFieldOffset = unsafe.objectFieldOffset(ucpField);
                Object ucpObject = unsafe.getObject(classLoader, ucpFieldOffset);

                // jdk.internal.loader.URLClassPath.path
                Field pathField = ucpField.getType().getDeclaredField("path");
                long pathFieldOffset = unsafe.objectFieldOffset(pathField);
                ArrayList<URL> path = (ArrayList<URL>)unsafe.getObject(ucpObject, pathFieldOffset);

                return path.toArray(new URL[0]);
            }).onFailure(
                ex -> log.error("[ManerFan Booster Bootstrap] Get urls from {} Failed!", classLoader.getClass().getName(), ex)
            ).getOrNull();
        }

        return null;
    }

    /**
     * 在多个 ClassLoader 中依次尝试 loadClass
     *
     * @param name         类名
     * @param classLoaders {@link ClassLoader}
     * @return {@link Class}
     * @throws ClassNotFoundException
     */
    public static Class<?> loadClass(String name, ClassLoader... classLoaders) throws ClassNotFoundException {
        if (Objects.isNull(classLoaders) || classLoaders.length < 1) {
            throw new ClassNotFoundException(name);
        }

        return Arrays.stream(classLoaders).map(classLoader -> {
            try {
                return classLoader.loadClass(name);
            } catch (Exception ex) {
                // Do Nothing
                return null;
            }
        }).filter(Objects::nonNull).findFirst().orElseThrow(() -> new ClassNotFoundException(name));
    }

    /**
     * 在多个 ClassLoader 中依次尝试 getResource
     *
     * @param name 资源
     * @return {@link URL}
     */
    public static URL getResource(String name, ClassLoader... classLoaders) {
        if (Objects.isNull(classLoaders) || classLoaders.length < 1) {
            return null;
        }

        return Arrays.stream(classLoaders)
            .map(classLoader -> classLoader.getResource(name))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * 整合多个 ClassLoader 中的 getResources
     *
     * @param name         资源
     * @param classLoaders {@link ClassLoader}
     * @return {@link URL}
     */
    public static Enumeration<URL> getResources(String name, ClassLoader... classLoaders) {
        if (Objects.isNull(classLoaders) || classLoaders.length < 1) {
            return Collections.emptyEnumeration();
        }

        val resources = Arrays.stream(classLoaders)
            .map(classLoader -> Try.of(() -> classLoader.getResources(name)).getOrNull())
            .filter(Objects::nonNull)
            .flatMap(StreamUtils::enumeration2Stream)
            .collect(Collectors.toList());

        return Collections.enumeration(resources);
    }
}
