package com.game.common.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页查询结果
 *
 * @param <T> 数据类型
 * @author GameServer
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码 (从 1 开始)
     */
    private int page;

    /**
     * 每页大小
     */
    private int size;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 当前页数据
     */
    private List<T> data;

    /**
     * 是否有下一页
     */
    private boolean hasNext;

    /**
     * 是否有上一页
     */
    private boolean hasPrev;

    public PageResult() {
        this.data = Collections.emptyList();
    }

    public PageResult(int page, int size, long total, List<T> data) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.data = data != null ? data : Collections.emptyList();
        this.totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        this.hasNext = page < totalPages;
        this.hasPrev = page > 1;
    }

    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(int page, int size, long total, List<T> data) {
        return new PageResult<>(page, size, total, data);
    }

    /**
     * 创建空结果
     */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(page, size, 0, Collections.emptyList());
    }

    /**
     * 创建单页结果 (不分页)
     */
    public static <T> PageResult<T> single(List<T> data) {
        int size = data != null ? data.size() : 0;
        return new PageResult<>(1, size, size, data);
    }

    /**
     * 转换数据类型
     */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        List<R> mappedData = this.data.stream()
                .map(mapper)
                .collect(Collectors.toList());
        return new PageResult<>(page, size, total, mappedData);
    }

    /**
     * 获取数据条数
     */
    public int getDataSize() {
        return data != null ? data.size() : 0;
    }

    /**
     * 判断是否为空
     */
    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }

    /**
     * 判断是否是第一页
     */
    public boolean isFirst() {
        return page <= 1;
    }

    /**
     * 判断是否是最后一页
     */
    public boolean isLast() {
        return page >= totalPages;
    }

    /**
     * 获取起始索引 (用于数据库查询)
     */
    public int getOffset() {
        return (page - 1) * size;
    }

    // ==================== 构建器 ====================

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private int page = 1;
        private int size = 20;
        private long total = 0;
        private List<T> data;

        public Builder<T> page(int page) {
            this.page = page;
            return this;
        }

        public Builder<T> size(int size) {
            this.size = size;
            return this;
        }

        public Builder<T> total(long total) {
            this.total = total;
            return this;
        }

        public Builder<T> data(List<T> data) {
            this.data = data;
            return this;
        }

        public PageResult<T> build() {
            return new PageResult<>(page, size, total, data);
        }
    }
}
