package com.campus.auction.dto;

import com.campus.auction.entity.Auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pairs a purchased auction with the amount paid and when the transaction occurred. */
public record PurchasedItem(Auction auction, BigDecimal paidAmount, LocalDateTime purchasedAt) {}
