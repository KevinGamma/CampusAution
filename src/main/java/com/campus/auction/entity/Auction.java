package com.campus.auction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.auction.enums.AuctionStatus;
import com.campus.auction.enums.SaleType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("auctions")
public class Auction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private BigDecimal startPrice;

    private BigDecimal currentPrice;

    private LocalDateTime endTime;

    private AuctionStatus status;

    private Long creatorId;

    private Integer quantity;

    private String category;

    private SaleType saleType;
}
