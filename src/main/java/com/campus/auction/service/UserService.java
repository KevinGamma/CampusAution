package com.campus.auction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.auction.dto.SellerProfileResponse;
import com.campus.auction.dto.UserDTO;
import com.campus.auction.entity.User;

import java.math.BigDecimal;

public interface UserService extends IService<User> {

    /**
     * Validates credentials and returns a safe DTO (no password) carrying the user's role.
     *
     * @throws com.campus.auction.exception.ServiceException (401) on unknown username
     *         or wrong password — same message in both cases to avoid user enumeration.
     */
    UserDTO login(String username, String password);

    /**
     * Registers a new user, always with the STUDENT role regardless of caller input.
     *
     * @throws com.campus.auction.exception.ServiceException (409) if the username is taken.
     */
    UserDTO register(String username, String password);

    /**
     * Adds {@code amount} to the current user's balance and returns the updated UserDTO.
     *
     * @throws com.campus.auction.exception.ServiceException (400) if {@code amount} is not positive.
     */
    UserDTO recharge(BigDecimal amount);

    /**
     * Updates the current user's avatar URL and/or bio.
     * Null arguments are ignored (field is left unchanged).
     *
     * @return the updated UserDTO (password excluded)
     */
    UserDTO updateProfile(String avatarUrl, String bio);

    /**
     * Returns the public profile of any user by ID, including their
     * average review rating and review count.
     *
     * @throws com.campus.auction.exception.ServiceException (404) if the user does not exist.
     */
    SellerProfileResponse getSellerProfile(Long userId);
}
