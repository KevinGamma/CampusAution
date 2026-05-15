package com.campus.auction.dto;

import lombok.Data;

/**
 * Request body for PUT /users/profile.
 * Both fields are optional — null means "leave unchanged".
 */
@Data
public class UpdateProfileRequest {

    /** New avatar URL path, e.g. "/api/images/uuid.jpg". Null = keep existing. */
    private String avatarUrl;

    /** New personal bio text. Null = keep existing. */
    private String bio;
}
