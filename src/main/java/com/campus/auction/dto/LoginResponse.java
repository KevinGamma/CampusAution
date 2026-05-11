package com.campus.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Payload returned by {@code POST /users/login}.
 * The frontend stores {@code token} in localStorage and reads {@code user.role}
 * to gate UI features (e.g. admin moderation panel).
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    /** JWT Bearer token — include in every subsequent request as {@code Authorization: Bearer <token>}. */
    private String token;

    /** Safe user projection (no password). */
    private UserDTO user;
}
