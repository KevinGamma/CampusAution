package com.campus.auction.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fields that may be updated on an existing listing. Null means "leave unchanged". */
@Data
public class UpdateAuctionRequest {
    private String        title;
    private String        description;
    private String        category;
    private Integer       quantity;
    private BigDecimal    startPrice;  // locked for AUCTION items that already have bids
    private LocalDateTime endTime;     // locked for AUCTION items that already have bids
}
