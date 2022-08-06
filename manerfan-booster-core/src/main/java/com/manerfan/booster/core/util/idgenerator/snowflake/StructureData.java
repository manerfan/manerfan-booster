package com.manerfan.booster.core.util.idgenerator.snowflake;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * StructureData
 * <pre>
 *      生成的结构化ID数据
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
public class StructureData {
    /**
     * 时间序列
     */
    private final Long timestamp;

    /**
     * 机器码
     */
    private final Long workId;

    /**
     * 并发序列
     */
    private final Long sequence;
}
