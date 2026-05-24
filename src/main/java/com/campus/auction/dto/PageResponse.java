package com.campus.auction.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * Paginated response envelope returned by list endpoints.
 */
public record PageResponse<T>(List<T> items, long total, long page, long size) {

    public static <T> PageResponse<T> of(Page<T> p) {
        return new PageResponse<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }
}
