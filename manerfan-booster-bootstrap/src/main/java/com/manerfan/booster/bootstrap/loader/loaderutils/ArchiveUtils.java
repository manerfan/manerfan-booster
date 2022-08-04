package com.manerfan.booster.bootstrap.loader.loaderutils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Objects;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;

import io.vavr.control.Try;

/**
 * ArchiveUtils
 *
 * <pre>
 *     archive 工具
 * </pre>
 *
 * @author manerfan
 * @date 2022/08/03
 */
public class ArchiveUtils {
    /**
     * 从路径 path 构建 archive
     *
     * @param path 文件/目录 路径
     * @return {@link Archive}
     * @throws IOException
     */
    public static Archive fromPath(String path) throws IOException {
        return fromPath(path, true);
    }

    public static Archive fromPath(String path, boolean recursive) throws IOException {
        if (Objects.isNull(path) || path.trim().isEmpty()) {
            throw new IllegalStateException("Unable to determine code source archive");
        }

        File root = new File(path);
        if (!root.exists()) {
            throw new IllegalStateException("Unable to determine code source archive from " + root);
        }

        return (root.isDirectory() ? new ExplodedArchive(root, recursive) : new JarFileArchive(root));
    }

    /**
     * 转换为非递归 archive
     *
     * @param archive 待转换 archive
     * @return {@link Archive}
     */
    public static Archive convert2NonRecursive(Archive archive) {
        if (archive instanceof ExplodedArchive) {
            return Try.of(() -> fromPath(archive.getUrl().getPath(), false)).getOrElse(archive);
        }

        return archive;
    }

    /**
     * 当前 Class 对应 CodeSource 的 Archive
     *
     * @param clazz 需要查找 CodeSource 的 Class
     * @return {@link Archive}
     * @throws Exception
     */
    public static Archive codeSourceArchive(Class<?> clazz) throws Exception {
        ProtectionDomain protectionDomain = clazz.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
        String path = (location != null) ? location.getSchemeSpecificPart() : null;

        return ArchiveUtils.fromPath(path);
    }
}
