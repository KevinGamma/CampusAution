package com.campus.auction.resource;

import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.common.Result;
import com.campus.auction.dto.AuctionResponse;
import com.campus.auction.dto.LoginRequest;
import com.campus.auction.dto.LoginResponse;
import com.campus.auction.dto.MyBidSummary;
import com.campus.auction.dto.PurchasedItem;
import com.campus.auction.dto.RechargeRequest;
import com.campus.auction.dto.RegisterRequest;
import com.campus.auction.dto.ReviewResponse;
import com.campus.auction.dto.SellerProfileResponse;
import com.campus.auction.dto.UpdateProfileRequest;
import com.campus.auction.dto.UserDTO;
import com.campus.auction.enums.UserRole;
import com.campus.auction.service.AuctionService;
import com.campus.auction.service.ReviewService;
import com.campus.auction.service.UserService;
import com.campus.auction.utils.JwtUtils;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Jersey (JAX-RS) resource replacing the former UserController.
 *
 * <p>Cross-cutting concerns are handled by the Jersey infrastructure:
 * <ul>
 *   <li>{@code @RoleAccess} enforcement + JWT auth — {@link com.campus.auction.filter.JwtSecurityFilter}.</li>
 *   <li>{@code Result.code} → HTTP status-line sync — {@link com.campus.auction.filter.ResultHttpStatusFilter}.</li>
 *   <li>Exception translation — {@link com.campus.auction.filter.ServiceExceptionMapper}
 *       and {@link com.campus.auction.filter.GeneralExceptionMapper}.</li>
 * </ul>
 */
@Component
@Path("/api/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class UserResource {

    private final UserService    userService;
    private final ReviewService  reviewService;
    private final AuctionService auctionService;
    private final JwtUtils       jwtUtils;

    /** POST /api/v1/users/login — public, no token required. */
    @POST
    @Path("/login")
    public Result<LoginResponse> login(LoginRequest request) {
        UserDTO user  = userService.login(request.getUsername(), request.getPassword());
        String  token = jwtUtils.generateToken(user);
        return Result.ok(new LoginResponse(token, user));
    }

    /** POST /api/v1/users/register — public, no token required. */
    @POST
    @Path("/register")
    public Result<UserDTO> register(RegisterRequest request) {
        // Result.created() carries code=201; ResultHttpStatusFilter syncs the HTTP status to match.
        return Result.created(userService.register(request.getUsername(), request.getPassword()));
    }

    /** POST /api/v1/users/recharge — add funds to the current user's balance. */
    @POST
    @Path("/recharge")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<UserDTO> recharge(RechargeRequest request) {
        return Result.ok(userService.recharge(request.getAmount()));
    }

    /** PATCH /api/v1/users/profile — update the current user's avatar URL and/or bio. */
    @PATCH
    @Path("/profile")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<UserDTO> updateProfile(UpdateProfileRequest request) {
        return Result.ok(userService.updateProfile(request.getAvatarUrl(), request.getBio()));
    }

    /** GET /api/v1/users/{id} — public seller/user profile. */
    @GET
    @Path("/{id}")
    public Result<SellerProfileResponse> getSellerProfile(@PathParam("id") Long id) {
        return Result.ok(userService.getSellerProfile(id));
    }

    /** GET /api/v1/users/{id}/reviews — all reviews received by a specific user, newest first. */
    @GET
    @Path("/{id}/reviews")
    public Result<List<ReviewResponse>> getReviews(@PathParam("id") Long id) {
        return Result.ok(reviewService.getReviewsByReviewee(id));
    }

    /** GET /api/v1/users/me/auctions — auctions created by the authenticated user, with bid counts. */
    @GET
    @Path("/me/auctions")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<List<AuctionResponse>> getMyAuctions() {
        return Result.ok(auctionService.listMyItems().stream()
                .map(a -> AuctionResponse.of(a, auctionService.countBids(a.getId())))
                .toList());
    }

    /** GET /api/v1/users/me/bids — auctions the authenticated user has bid on, with their top bid. */
    @GET
    @Path("/me/bids")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<List<MyBidSummary>> getMyBids() {
        return Result.ok(auctionService.listMyBids());
    }

    /** GET /api/v1/users/me/orders — all items purchased by the current user, newest first. */
    @GET
    @Path("/me/orders")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<List<PurchasedItem>> getMyOrders() {
        return Result.ok(auctionService.listPurchased());
    }
}
