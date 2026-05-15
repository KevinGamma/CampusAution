package com.campus.auction.dto;

import com.campus.auction.entity.User;
import com.campus.auction.enums.UserRole;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Public projection of {@link User} — password is intentionally excluded.
 * Returned to the frontend after a successful login so it can store the
 * user's identity and permission level (role).
 */
@Data
public class UserDTO {

    private Long id;

    private String username;

    private BigDecimal balance;

    private UserRole role;

    /** URL path of the user's avatar image. Null when not set. */
    private String avatarUrl;

    /** Short personal bio. Null when not set. */
    private String bio;

    /** Maps a {@link User} entity to a safe DTO, stripping the password. */
    public static UserDTO from(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setBalance(user.getBalance());
        dto.setRole(user.getRole());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setBio(user.getBio());
        return dto;
    }
}
