package com.manerfan.booster.core.exception;

/**
 * ErrorInfo
 *
 * 通用异常信息，不局限形式，可以为 Enum、POJO 甚至工具类
 *
 * <pre>
 *
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public interface ErrorInfo {
    /**
     * 默认成功的code
     */
    String SUCCESS_CODE =  "SUCCESS";

    /**
     * 获取异常码
     *
     * @return 异常码
     */
    String getErrorCode();

    /**
     * 获取异常信息
     *
     * @return 异常信息
     */
    String getErrorMessage();
}

