package com.campus.auction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Fields that may be updated on an existing listing. Null means "leave unchanged". */
@Data
public class UpdateAuctionRequest {
    private String        title;
    private String        description;
    private String        category;
    private Integer       quantity;
    private BigDecimal    startPrice;       // locked for AUCTION items that already have bids
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;          // locked for AUCTION items that already have bids
    private List<String>  imageUrls;
}
