package com.campus.auction.dto;

import lombok.Data;

/**
 * Request body for PATCH /api/v1/auctions/{id}/status.
 * Allowed transitions: CANCELLED (owner cancels), SOLD (owner accepts highest bid).
 */
@Data
public class AuctionStatusRequest {
    private String status;
}
