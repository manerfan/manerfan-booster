package com.manerfan.booster.api.common.dto.response;

import java.util.Collection;

import com.manerfan.booster.api.common.dto.request.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * PageResponse
 *
 * <pre>
 *     分页响应
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PageResponse<T> extends MultiResponse<T> {
    /**
     * 总数
     */
    private long total;

    /**
     * 页数请求参数
     */
    private PageRequest page;

    private PageResponse(
        boolean success, String errorCode, String errMessage,
        Collection<T> data, long total, PageRequest page) {
        super(success, errorCode, errMessage, data);
        this.total = total;
        this.page = page;
    }

    public static <T> PageResponse<T> buildSuccess(Collection<T> data, PageRequest page) {
        return new PageResponse<T>(true, Response.SUCCESS_CODE, null, data, -1, page);
    }

    public static <T> PageResponse<T> buildSuccess(Collection<T> data, long total, PageRequest page) {
        return new PageResponse<T>(true, Response.SUCCESS_CODE, null, data, total, page);
    }

    @SuppressWarnings("rawtypes")
    public static PageResponse buildFailure(String errorCode, String errMessage) {
        return new PageResponse<>(false, errorCode, errMessage, null, -1, null);
    }
}
