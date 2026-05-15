package com.campus.auction.service;

import com.campus.auction.dto.CreateReviewRequest;
import com.campus.auction.dto.ReviewResponse;
import com.campus.auction.entity.Review;

import java.util.List;

public interface ReviewService {

    /**
     * Submits a review for a completed transaction.
     *
     * <p>Business rules enforced:
     * <ul>
     *   <li>The order must exist.</li>
     *   <li>The current user must have been the buyer or the seller.</li>
     *   <li>Rating must be in the range 1–5.</li>
     *   <li>Each (order, reviewer) pair may only produce one review.</li>
     * </ul>
     *
     * @throws com.campus.auction.exception.ServiceException (404) if the order is not found.
     * @throws com.campus.auction.exception.ServiceException (403) if the caller was not involved.
     * @throws com.campus.auction.exception.ServiceException (400) if the rating is out of range.
     * @throws com.campus.auction.exception.ServiceException (409) if already reviewed.
     */
    Review createReview(CreateReviewRequest request);

    /**
     * Returns all reviews received by the specified user, newest first.
     * Enriches each review with the reviewer's display name and avatar URL.
     */
    List<ReviewResponse> getReviewsByReviewee(Long revieweeId);

    /**
     * Returns the arithmetic mean rating for the specified user,
     * or {@code null} when no reviews exist.
     */
    Double getAverageRating(Long revieweeId);

    /** Returns the total number of reviews received by the specified user. */
    long getReviewCount(Long revieweeId);
}
