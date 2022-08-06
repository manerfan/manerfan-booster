package com.manerfan.booster.api.common.dto.request;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import com.manerfan.booster.api.common.dto.Dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Sort
 *
 * <pre>
 *     排序
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Sort extends Dto {
    /**
     * 属性排序
     */
    private Collection<Order> orders;

    public static Sort by(String... properties) {
        return by(Order.DEF_DIRECTION, properties);
    }

    public static Sort by(Direction direction, String... properties) {
        return by(direction, Arrays.asList(properties));
    }

    public static Sort by(Direction direction, Collection<String> properties) {
        if (Objects.isNull(properties) || properties.isEmpty()) {
            throw new IllegalArgumentException("You have to provide at least one property to sort by!");
        }

        return by(properties.stream()
            .map(prop -> Order.by(Objects.nonNull(direction) ? direction : Order.DEF_DIRECTION, prop))
            .collect(Collectors.toList()));
    }

    public static Sort by(Order... orders) {
        return by(Arrays.asList(orders));
    }

    public static Sort by(Collection<Order> orders) {
        if (Objects.isNull(orders) || orders.isEmpty()) {
            throw new IllegalArgumentException("You have to provide at least one order to sort by!");
        }

        return new Sort(orders);
    }

    /**
     * 排序
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        public static final Direction DEF_DIRECTION = Direction.ASC;

        /**
         * 顺序
         */
        private Direction direction;

        /**
         * 属性
         */
        private String property;

        public static Order by(Direction direction, String property) {
            return new Order(direction, property);
        }

        public static Order by(String property) {
            return by(DEF_DIRECTION, property);
        }

        public static Order asc(String property) {
            return by(Direction.ASC, property);
        }

        public static Order desc(String property) {
            return by(Direction.DESC, property);
        }
    }

    /**
     * 排序枚举
     */
    public static enum Direction {
        /**
         * 升序
         */
        ASC,

        /**
         * 降序
         */
        DESC;

        public boolean isAscending() {
            return this.equals(ASC);
        }

        public boolean isDescending() {
            return this.equals(DESC);
        }

        public static Direction fromString(String value) {
            try {
                return Direction.valueOf(value.toUpperCase(Locale.US));
            } catch (Exception ex) {
                throw new IllegalArgumentException(
                    String.format(
                        "Invalid value '%s' for orders given! Has to be either 'desc' or 'asc' (case insensitive).",
                        value),
                    ex
                );
            }
        }
    }
}
