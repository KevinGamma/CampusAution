package com.campus.auction.dto;

import com.campus.auction.enums.SaleType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateAuctionRequest {
    private String title;
    private String description;
    private BigDecimal startPrice;
    private LocalDateTime endTime;
    private Integer quantity;
    private String category;
    private SaleType saleType;
}
