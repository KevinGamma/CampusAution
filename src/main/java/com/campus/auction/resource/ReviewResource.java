package com.campus.auction.resource;

import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.common.Result;
import com.campus.auction.dto.CreateReviewRequest;
import com.campus.auction.entity.Review;
import com.campus.auction.enums.UserRole;
import com.campus.auction.service.ReviewService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Path("/api/v1/reviews")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class ReviewResource {

    private final ReviewService reviewService;

    /**
     * POST /api/v1/reviews — submit a mutual review for a completed transaction.
     * The reviewee is derived automatically from the order's buyer/seller relationship.
     * Returns 201 Created; ResultHttpStatusFilter syncs the HTTP status from Result.code.
     */
    @POST
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<Review> createReview(CreateReviewRequest request) {
        return Result.created(reviewService.createReview(request));
    }
}
