package com.campus.auction.dto;

import com.campus.auction.entity.Auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pairs a purchased auction with transaction metadata.
 * {@code orderId} and {@code sellerId} are exposed so the frontend can offer
 * the buyer a "leave a review" action for each completed purchase.
 */
public record PurchasedItem(
        Long orderId,
        Long sellerId,
        Auction auction,
        BigDecimal paidAmount,
        LocalDateTime purchasedAt
) {}
