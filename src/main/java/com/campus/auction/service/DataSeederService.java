package com.campus.auction.service;

import com.campus.auction.dto.SeedResult;

public interface DataSeederService {

    /**
     * Generates {@code userCount} student users, {@code auctionsPerUser} auctions per user,
     * and {@code directSaleCount} standalone fixed-price direct-sale items.
     *
     * @param directPercent  percentage of the per-user auctions that should be DIRECT-sale (0–100).
     * @param directSaleCount additional standalone DIRECT-sale items created with fixed-price titles.
     */
    SeedResult seed(int userCount, int auctionsPerUser, int directPercent, int directSaleCount);
}
