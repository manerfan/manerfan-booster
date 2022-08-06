package com.manerfan.booster.api.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Response
 *
 * <pre>
 *     响应
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/5
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Response extends StatMonitor {
    /**
     * 默认成功的code
     */
    public static final String SUCCESS_CODE = "SUCCESS";

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误码
     */
    private String errorCode = SUCCESS_CODE;

    /**
     * 错误信息
     */
    private String errorMessage;

    public static Response buildSuccess() {
        return new Response(true, SUCCESS_CODE, null);
    }

    public static Response buildFailure(String errorCode, String errMessage) {
        return new Response(false, errorCode, errMessage);
    }
}
