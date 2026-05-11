package com.campus.auction.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Encapsulates every optional query parameter accepted by
 * {@code GET /auctions}. Null / blank values are ignored by the service layer
 * — only non-null values narrow the result set.
 */
@Data
public class AuctionFilter {

    /** Full-text search applied to title and description (LIKE %keyword%). */
    private String keyword;

    /** English category key, e.g. {@code "Electronics"}. Null = all categories. */
    private String category;

    /** {@code "AUCTION"} or {@code "DIRECT"}. Null = both types. */
    private String saleType;

    /** Inclusive lower bound on {@code currentPrice}. */
    private BigDecimal minPrice;

    /** Inclusive upper bound on {@code currentPrice}. */
    private BigDecimal maxPrice;

    /**
     * Inclusive lower bound on {@code endTime} (start-of-day).
     * Lets users find auctions whose closing date is on or after this date.
     */
    private LocalDate startDate;

    /**
     * Inclusive upper bound on {@code endTime} (end-of-day).
     * Lets users find auctions closing on or before this date.
     */
    private LocalDate endDate;

    /**
     * Column to sort by. Accepted values: {@code "currentPrice"}, {@code "endTime"}.
     * Any other value (including null) defaults to {@code id DESC} (newest first).
     */
    private String sortBy;

    /** Sort direction: {@code "ASC"} or {@code "DESC"}. Defaults to {@code "DESC"}. */
    private String order;
}
