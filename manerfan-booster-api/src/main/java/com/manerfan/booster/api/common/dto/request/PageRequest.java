package com.manerfan.booster.api.common.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;

/**
 * PageRequest
 *
 * <pre>
 *     分页请求
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageRequest extends Query {
    private static final int DEF_PAGE = 1;
    private static final int DEF_SIZE = 10;

    /**
     * 页数，起始 1
     */
    private int page = DEF_PAGE;

    /**
     * 个数
     */
    private int size = DEF_SIZE;

    /**
     * 排序
     */
    private Sort sort;

    /**
     * 是否需要总数
     */
    private boolean needTotalCount = true;

    public PageRequest(int page, int size, Sort sort, boolean needTotalCount) {
        this.page = page < 1 ? DEF_PAGE : page;
        this.size = size < 1 ? DEF_SIZE : size;
        this.sort = sort;
        this.needTotalCount = needTotalCount;
    }

    public PageRequest(int page, int size, boolean needTotalCount) {
        this.page = page < 1 ? DEF_PAGE : page;
        this.size = size < 1 ? DEF_SIZE : size;
        this.needTotalCount = needTotalCount;
    }

    public PageRequest(int page, int size) {
        this.page = page < 1 ? DEF_PAGE : page;
        this.size = size < 1 ? DEF_SIZE : size;
    }

    public PageRequest(int page) {
        this.page = page < 1 ? DEF_PAGE : page;
        this.size = DEF_SIZE;
    }

    public PageRequest() {
        this.page = DEF_PAGE;
        this.size = DEF_SIZE;
    }

    public int getOffset() {
        val offset = (page - 1) * size;
        return Math.max(offset, 0);
    }

    public int getPage() {
        return Math.max(page, 1);
    }

    public int getSize() {
        return Math.max(size, 1);
    }
}
