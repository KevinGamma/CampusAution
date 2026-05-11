package com.campus.auction.service;

import com.baomidou.mybatisplus.extension.service.IService;
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
}
