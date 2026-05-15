package com.campus.auction.controller;

import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.common.Result;
import com.campus.auction.dto.CreateReviewRequest;
import com.campus.auction.entity.Review;
import com.campus.auction.enums.UserRole;
import com.campus.auction.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * POST /api/reviews — submit a mutual review for a completed transaction.
     *
     * <p>The caller must have been the buyer or seller on the specified order.
     * The reviewee is derived automatically:
     * <ul>
     *   <li>If the caller is the buyer  → the seller is reviewed.</li>
     *   <li>If the caller is the seller → the buyer is reviewed.</li>
     * </ul>
     *
     * <p>Returns 201 Created with the persisted Review on success.
     * Returns 400 for invalid rating, 403 if not a participant,
     * 404 if order not found, and 409 if already reviewed.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<Review> createReview(@RequestBody CreateReviewRequest request) {
        return Result.created(reviewService.createReview(request));
    }
}
