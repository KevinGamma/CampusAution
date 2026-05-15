package com.campus.auction.dto;

import lombok.Data;

/**
 * Request body for POST /api/reviews.
 */
@Data
public class CreateReviewRequest {

    /** ID of the completed order being reviewed. */
    private Long orderId;

    /** Star rating: 1 (poor) to 5 (excellent). */
    private Integer rating;

    /** Optional text comment. May be null or blank. */
    private String comment;
}
