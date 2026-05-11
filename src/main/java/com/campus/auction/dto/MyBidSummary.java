package com.campus.auction.dto;

import com.campus.auction.entity.Auction;

import java.math.BigDecimal;

/** Pairs an auction the current user has bid on with their personal highest bid amount. */
public record MyBidSummary(Auction auction, BigDecimal myHighestBid) {}
