package com.campus.auction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.auction.enums.UserRole;
import lombok.Data;

import java.math.BigDecimal;

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
}
