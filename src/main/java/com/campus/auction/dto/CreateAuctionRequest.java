package com.campus.auction.dto;

import com.campus.auction.enums.SaleType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateAuctionRequest {
    private String title;
    private String description;
    private BigDecimal startPrice;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
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
