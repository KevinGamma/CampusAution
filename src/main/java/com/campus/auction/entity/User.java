package com.campus.auction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.auction.enums.UserRole;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private BigDecimal balance;

    /** Permission level — defaults to STUDENT for every new registration. */
    private UserRole role;

    /** URL path of the user's avatar image, e.g. "/api/images/uuid.jpg". Nullable. */
    private String avatarUrl;

    /** Short personal bio or introduction. Nullable. */
    private String bio;

    /** Account registration timestamp. Set by the DB default on INSERT. */
    private LocalDateTime createdAt;
}
