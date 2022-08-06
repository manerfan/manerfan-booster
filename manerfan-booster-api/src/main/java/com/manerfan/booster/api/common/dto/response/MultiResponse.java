package com.manerfan.booster.api.common.dto.response;

import java.util.Collection;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * MultiResponse
 *
 * <pre>
 *     多值响应
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MultiResponse<T> extends Response {
    /**
     * 数据
     */
    private Collection<T> content;

    @Builder
    public MultiResponse(boolean success, String errorCode, String errorMessage, Collection<T> content) {
        super(success, errorCode, errorMessage);
        this.content = content;
    }

    public static <T> MultiResponse<T> buildSuccess(Collection<T> content) {
        return MultiResponse.<T>builder()
            .content(content)
            .success(true)
            .errorCode(Response.SUCCESS_CODE)
            .build();
    }

    @SuppressWarnings("rawtypes")
    public static MultiResponse buildFailure(String errorCode, String errorMessage) {
        return MultiResponse.builder()
            .success(false)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .build();
    }
}