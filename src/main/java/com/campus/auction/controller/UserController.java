package com.campus.auction.controller;

import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.common.Result;
import com.campus.auction.dto.LoginRequest;
import com.campus.auction.dto.LoginResponse;
import com.campus.auction.dto.RechargeRequest;
import com.campus.auction.dto.RegisterRequest;
import com.campus.auction.dto.ReviewResponse;
import com.campus.auction.dto.SellerProfileResponse;
import com.campus.auction.dto.UpdateProfileRequest;
import com.campus.auction.dto.UserDTO;
import com.campus.auction.enums.UserRole;
import com.campus.auction.service.UserService;
import com.campus.auction.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import com.campus.auction.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService   userService;
    private final ReviewService reviewService;
    private final JwtUtils      jwtUtils;

    /**
     * POST /users/login  — public, no token required.
     *
     * <p>Request : {@code { "username": "alice", "password": "secret" }}
     * <p>200 OK   : {@code { "code": 200, "data": { "token": "eyJ...", "user": { id, username, balance, role } } }}
     * <p>401      : {@code { "code": 401, "msg": "Invalid username or password" }}
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        UserDTO user  = userService.login(request.getUsername(), request.getPassword());
        String  token = jwtUtils.generateToken(user);
        return Result.ok(new LoginResponse(token, user));
    }

    /**
     * POST /users/register — public, no token required.
     *
     * <p>Request : {@code { "username": "alice", "password": "secret" }}
     * <p>201 Created : {@code { "code": 201, "data": { id, username, balance, role } }}
     * <p>409 Conflict: {@code { "code": 409, "msg": "Username already exists" }}
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<UserDTO> register(@RequestBody RegisterRequest request) {
        return Result.created(userService.register(request.getUsername(), request.getPassword()));
    }

    /**
     * POST /users/recharge — add funds to the current user's balance.
     *
     * <p>Request : {@code { "amount": 100.00 }}
     * <p>200 OK   : {@code { "code": 200, "data": { id, username, balance, role } }}
     * <p>400      : amount not positive
     */
    @PostMapping("/recharge")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<UserDTO> recharge(@RequestBody RechargeRequest request) {
        return Result.ok(userService.recharge(request.getAmount()));
    }

    /**
     * PUT /users/profile — update the current user's avatar URL and/or bio.
     *
     * <p>Request : {@code { "avatarUrl": "/api/images/uuid.jpg", "bio": "Hi!" }}
     * <p>200 OK   : updated UserDTO
     *
     * <p>Both fields are optional; send only the ones you want to change.
     */
    @PutMapping("/profile")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<UserDTO> updateProfile(@RequestBody UpdateProfileRequest request) {
        return Result.ok(userService.updateProfile(request.getAvatarUrl(), request.getBio()));
    }

    /**
     * GET /users/{id} — public seller / user profile.
     *
     * <p>Returns the user's display name, avatar, bio, average review rating,
     * review count, and registration date. No authentication required.
     * <p>404 if the user does not exist.
     */
    @GetMapping("/{id}")
    public Result<SellerProfileResponse> getSellerProfile(@PathVariable Long id) {
        return Result.ok(userService.getSellerProfile(id));
    }

    /**
     * GET /users/{id}/reviews — all reviews received by a specific user, newest first.
     *
     * <p>Each entry is enriched with the reviewer's display name and avatar.
     * No authentication required — reviews are public.
     */
    @GetMapping("/{id}/reviews")
    public Result<List<ReviewResponse>> getReviews(@PathVariable Long id) {
        return Result.ok(reviewService.getReviewsByReviewee(id));
    }
}
