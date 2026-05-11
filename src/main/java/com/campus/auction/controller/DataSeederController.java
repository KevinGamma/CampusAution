package com.campus.auction.controller;

import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.common.Result;
import com.campus.auction.dto.SeedResult;
import com.campus.auction.enums.UserRole;
import com.campus.auction.service.DataSeederService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DataSeederController {

    private final DataSeederService dataSeederService;

    /**
     * POST /admin/seed-data?userCount=10&auctionsPerUser=5&directPercent=30&directSaleCount=0
     *
     * <p>Generates synthetic student accounts and their auctions in bulk batches.
     * {@code directPercent} controls what fraction of per-user items are DIRECT-sale (0–100).
     * {@code directSaleCount} adds that many extra standalone fixed-price direct-sale listings.
     */
    @PostMapping("/seed-data")
    @RoleAccess(UserRole.ADMIN)
    public Result<SeedResult> seedData(
            @RequestParam(defaultValue = "10") int userCount,
            @RequestParam(defaultValue = "5")  int auctionsPerUser,
            @RequestParam(defaultValue = "30") int directPercent,
            @RequestParam(defaultValue = "0")  int directSaleCount) {
        SeedResult result = dataSeederService.seed(
                userCount, auctionsPerUser,
                Math.max(0, Math.min(100, directPercent)),
                Math.max(0, directSaleCount));
        return Result.ok(result);
    }
}
