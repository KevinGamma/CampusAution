package com.campus.auction.controller;

import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.common.Result;
import com.campus.auction.dto.LoginRequest;
import com.campus.auction.dto.LoginResponse;
import com.campus.auction.dto.RechargeRequest;
import com.campus.auction.dto.RegisterRequest;
import com.campus.auction.dto.UserDTO;
import com.campus.auction.enums.UserRole;
import com.campus.auction.service.UserService;
import com.campus.auction.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtils    jwtUtils;

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
}
