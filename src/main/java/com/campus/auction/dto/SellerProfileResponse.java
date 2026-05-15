package com.campus.auction.dto;

import java.time.LocalDateTime;

/**
 * Public profile of a seller / user, returned by GET /users/{id}.
 * No sensitive fields (password, balance) are exposed.
 */
public record SellerProfileResponse(
        Long id,
        String username,
        String avatarUrl,
        String bio,

        /** Arithmetic mean of all received review ratings. Null when no reviews exist yet. */
        Double averageRating,

        /** Total number of reviews the user has received. */
        long reviewCount,

        /** Account creation timestamp. May be null for legacy accounts without the column. */
        LocalDateTime createdAt
) {}
