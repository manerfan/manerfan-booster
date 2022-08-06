package com.manerfan.booster.core.util.collection;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * StreamUtils
 *
 * <pre>
 *     Steam工具
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public class StreamUtils {
    /**
     * {@link Iterator} 转 {@link Stream}
     *
     * @param iterator {@link Iterator}
     * @return {@link Stream}
     */
    public static <T> Stream<T> iterator2Stream(Iterator<T> iterator) {
        if (Objects.isNull(iterator)) {
            return Stream.empty();
        }

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    /**
     * {@link Iterator} 转 {@link List}
     *
     * @param iterator {@link Iterator}
     * @return {@link List}
     */
    public static <T> List<T> iterator2List(Iterator<T> iterator) {
        return iterator2Stream(iterator).collect(Collectors.toList());
    }

    /**
     * {@link Enumeration} 转 {@link Stream}
     *
     * @param enumeration {@link Enumeration}
     * @return {@link Stream}
     */
    public static <T> Stream<T> enumeration2Stream(Enumeration<T> enumeration) {
        if (Objects.isNull(enumeration)) {
            return Stream.empty();
        }

        EnumerationSpliterator<T> spliterator
            = new EnumerationSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED, enumeration);
        return StreamSupport.stream(spliterator, false);
    }

    /**
     * {@link Enumeration} 的 {@link Spliterator}
     */
    public static class EnumerationSpliterator<T> extends AbstractSpliterator<T> {
        private final Enumeration<T> enumeration;

        public EnumerationSpliterator(long est, int additionalCharacteristics, Enumeration<T> enumeration) {
            super(est, additionalCharacteristics);
            this.enumeration = enumeration;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (enumeration.hasMoreElements()) {
                action.accept(enumeration.nextElement());
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            while (enumeration.hasMoreElements()) {
                action.accept(enumeration.nextElement());
            }
        }
    }
}
