package com.campus.auction.dto;

import java.time.LocalDateTime;

/**
 * Public projection of a {@link com.campus.auction.entity.Review},
 * enriched with the reviewer's display name and avatar URL.
 */
public record ReviewResponse(
        Long id,
        Long orderId,
        Long reviewerId,
        String reviewerName,
        String reviewerAvatar,
        Long revieweeId,
        Integer rating,
        String comment,
        LocalDateTime createTime
) {}
