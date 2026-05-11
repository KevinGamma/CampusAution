package com.campus.auction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bids")
public class Bid {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long auctionId;

    private Long bidderId;

    private BigDecimal amount;

    // `timestamp` is a reserved keyword in MySQL 8 — backtick-quote it in all generated SQL
    @TableField("`timestamp`")
    private LocalDateTime timestamp;
}
