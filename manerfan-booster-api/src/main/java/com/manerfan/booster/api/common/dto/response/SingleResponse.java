package com.manerfan.booster.api.common.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * SingleResponse
 *
 * <pre>
 *     单值响应
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SingleResponse<T> extends Response {
    /**
     * 数据
     */
    private T content;

    @Builder
    public SingleResponse(boolean success, String errorCode, String errMessage, T content) {
        super(success, errorCode, errMessage);
        this.content = content;
    }

    public static <T> SingleResponse<T> buildSuccess(T content) {
        return SingleResponse.<T>builder()
            .content(content)
            .success(true)
            .errorCode(Response.SUCCESS_CODE)
            .build();
    }

    @SuppressWarnings("rawtypes")
    public static SingleResponse buildFailure(String errorCode, String errMessage) {
        return SingleResponse.builder()
            .success(false)
            .errorCode(errorCode)
            .errMessage(errMessage)
            .build();
    }
}
