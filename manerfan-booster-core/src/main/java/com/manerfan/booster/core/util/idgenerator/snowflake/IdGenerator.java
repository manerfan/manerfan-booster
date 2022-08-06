package com.manerfan.booster.core.util.idgenerator.snowflake;

/**
 * IdGenerator
 *
 * <pre>
 *      ID生成器
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public interface IdGenerator {
    /**
     * 生成ID
     *
     * @return {@link IdData}
     */
    IdData nextId();
}
