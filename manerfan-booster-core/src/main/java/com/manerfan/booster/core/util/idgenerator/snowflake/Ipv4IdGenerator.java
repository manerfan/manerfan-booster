package com.manerfan.booster.core.util.idgenerator.snowflake;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.vavr.control.Try;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

/**
 * Ipv4IdGenerator
 *
 * <pre>
 *      扩充snow flake算法，使用本机ipv4地址充当work id，生成整体有序的ID
 *      生成的ID可反向解析出现场机器IP、时间戳及并发度
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public class Ipv4IdGenerator implements IdGenerator {
    /**
     * ipv4分4段
     */
    private static final int IPV4_SEG_LEN = 4;

    /**
     * ipv4每段8位
     */
    private static final int IPV4_SEG_BITS = 8;

    /**
     * ipv4分隔符
     */
    private static final String IPV4_DELIMITER = ".";

    /**
     * ipv4匹配正则
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile("^(\\d){1,3}(\\.(\\d){1,3}){3}$");

    private static final Ipv4IdGenerator INSTANCE = new Ipv4IdGenerator();

    public static Ipv4IdGenerator singleInstance() {
        return INSTANCE;
    }

    private final IdWorker idWorker;

    private Ipv4IdGenerator() {
        idWorker = ipv4Address()
            .flatMap(Ipv4IdGenerator::ipv4ToLong)
            .map(IdWorker::new)
            .orElseThrow(() -> new IllegalStateException("Cannot Found a valid IPv4 HostAddress!"));
    }

    @Override
    public IdData nextId() {
        return idWorker.nextId();
    }

    /**
     * 获取首个非回环ip
     */
    public static Optional<String> ipv4Address() {
        return Try.of(NetworkInterface::getNetworkInterfaces)
            .map(Ipv4IdGenerator::enumeration2Stream)
            .map(networkInterfaceStream ->
                networkInterfaceStream.map(NetworkInterface::getInetAddresses)
                    .map(Ipv4IdGenerator::firstUnLoopbackAddress)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
            )
            .getOrElse(Optional.empty());
    }

    /**
     * 将 {@link Enumeration} 转为 {@link Stream}
     *
     * @param enumeration {@link Enumeration}
     * @param <T>         {@link T}
     * @return Stream
     */
    private static <T> Stream<T> enumeration2Stream(Enumeration<T> enumeration) {
        if (Objects.isNull(enumeration)) {
            return Stream.empty();
        }

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
            new Iterator<T>() {
                @Override
                public T next() {
                    return enumeration.nextElement();
                }

                @Override
                public boolean hasNext() {
                    return enumeration.hasMoreElements();
                }
            },
            Spliterator.ORDERED
        ), false);
    }

    /**
     * 在地址组内返回首个非回环（且IPv4）地址
     *
     * @param inetAddresses 地址组
     * @return 首个非回环地址 or null
     */
    private static Optional<String> firstUnLoopbackAddress(Enumeration<InetAddress> inetAddresses) {
        return enumeration2Stream(inetAddresses)
            .map(inetAddress -> {
                // 非回环地址 && IPv4格式
                val unLoopbackIPv4Address = !inetAddress.isLoopbackAddress()
                    && IPV4_PATTERN.matcher(inetAddress.getHostAddress()).matches();
                return unLoopbackIPv4Address ? inetAddress.getHostAddress() : null;
            })
            .filter(StringUtils::isNotBlank)
            .findFirst();
    }

    /**
     * 字符型IPV4（127.0.0.1）转为Long
     *
     * @param ipv4 字符型IPv4地址
     * @return Optional Long
     * @see #longToIPv4(Long)
     */
    static Optional<Long> ipv4ToLong(String ipv4) {
        if (StringUtils.isBlank(ipv4)) {
            return Optional.empty();
        }

        val segments = StringUtils.split(ipv4, IPV4_DELIMITER, IPV4_SEG_LEN);
        if (segments.length < IPV4_SEG_LEN) {
            return Optional.empty();

        }

        return Try.of(() -> Optional.of(
                IntStream.rangeClosed(0, IPV4_SEG_LEN - 1)
                    // 左移
                    .mapToObj(i ->
                        Long.parseLong(segments[i]) << ((IPV4_SEG_LEN - 1 - i) * IPV4_SEG_BITS)
                    )
                    // 或运算
                    .reduce(0L, (pre, post) ->
                        pre | post
                    )
            )
        ).getOrElse(Optional.empty());
    }

    /**
     * Long型IPV4转为字符（127.0.0.1）
     *
     * @param ipv4 Long型IPV4
     * @return Optional String
     * @see #ipv4ToLong(String)
     */
    static Optional<String> longToIPv4(Long ipv4) {
        if (Objects.isNull(ipv4) || ipv4 < 0) {
            return Optional.empty();
        }

        // 0000_0000 . 0000_0000 . 0000_0000 . 1111_1111
        val mask = 0x00_00_00_FFL;
        return Try.of(() -> Optional.of(
                IntStream.rangeClosed(0, IPV4_SEG_LEN - 1)
                    .boxed()
                    .sorted(Collections.reverseOrder())
                    // 计算各段
                    .mapToLong(i -> (ipv4 >> (i * IPV4_SEG_BITS)) & mask)
                    .mapToObj(Objects::toString)
                    // 拼接
                    .collect(Collectors.joining(IPV4_DELIMITER))
            )
        ).getOrElse(Optional.empty());
    }
}
