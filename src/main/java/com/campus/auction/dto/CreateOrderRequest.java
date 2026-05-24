package com.campus.auction.dto;

import lombok.Data;

/**
 * Request body for POST /api/v1/auctions/{id}/orders.
 *
 * <ul>
 *   <li>No body / all-null fields → DIRECT-sale instant purchase by the current user.</li>
 *   <li>{@code bidId} set → seller accepts that specific bid.</li>
 *   <li>{@code acceptHighest: true} → seller accepts the current highest bid.</li>
 * </ul>
 */
@Data
public class CreateOrderRequest {
    private Long    bidId;
    private Boolean acceptHighest;
}
