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

    /**
     * Optional list of image URL paths uploaded before form submission,
     * e.g. ["/api/images/uuid1.jpg", "/api/images/uuid2.jpg"].
     */
    private java.util.List<String> imageUrls;
}
