package com.campus.auction.controller;

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
import com.campus.auction.service.UserService;
import com.campus.auction.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import com.campus.auction.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService    userService;
    private final ReviewService  reviewService;
    private final AuctionService auctionService;
    private final JwtUtils       jwtUtils;

    /** POST /api/v1/users/login — public, no token required. */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        UserDTO user  = userService.login(request.getUsername(), request.getPassword());
        String  token = jwtUtils.generateToken(user);
        return Result.ok(new LoginResponse(token, user));
    }

    /** POST /api/v1/users/register — public, no token required. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<UserDTO> register(@RequestBody RegisterRequest request) {
        return Result.created(userService.register(request.getUsername(), request.getPassword()));
    }

    /** POST /api/v1/users/recharge — add funds to the current user's balance. */
    @PostMapping("/recharge")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<UserDTO> recharge(@RequestBody RechargeRequest request) {
        return Result.ok(userService.recharge(request.getAmount()));
    }

    /** PATCH /api/v1/users/profile — update the current user's avatar URL and/or bio. */
    @PatchMapping("/profile")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<UserDTO> updateProfile(@RequestBody UpdateProfileRequest request) {
        return Result.ok(userService.updateProfile(request.getAvatarUrl(), request.getBio()));
    }

    /** GET /api/v1/users/{id} — public seller/user profile. */
    @GetMapping("/{id}")
    public Result<SellerProfileResponse> getSellerProfile(@PathVariable Long id) {
        return Result.ok(userService.getSellerProfile(id));
    }

    /** GET /api/v1/users/{id}/reviews — all reviews received by a specific user, newest first. */
    @GetMapping("/{id}/reviews")
    public Result<List<ReviewResponse>> getReviews(@PathVariable Long id) {
        return Result.ok(reviewService.getReviewsByReviewee(id));
    }

    /** GET /api/v1/users/me/auctions — auctions created by the authenticated user, with bid counts. */
    @GetMapping("/me/auctions")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<List<AuctionResponse>> getMyAuctions() {
        return Result.ok(auctionService.listMyItems().stream()
                .map(a -> AuctionResponse.of(a, auctionService.countBids(a.getId())))
                .toList());
    }

    /** GET /api/v1/users/me/bids — auctions the authenticated user has bid on, with their top bid. */
    @GetMapping("/me/bids")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<List<MyBidSummary>> getMyBids() {
        return Result.ok(auctionService.listMyBids());
    }

    /** GET /api/v1/users/me/orders — all items purchased by the current user, newest first. */
    @GetMapping("/me/orders")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<List<PurchasedItem>> getMyOrders() {
        return Result.ok(auctionService.listPurchased());
    }
}
