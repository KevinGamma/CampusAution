package com.campus.auction.controller;

import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.common.Result;
import com.campus.auction.dto.SeedResult;
import com.campus.auction.entity.Auction;
import com.campus.auction.enums.UserRole;
import com.campus.auction.service.AuctionService;
import com.campus.auction.service.DataSeederService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class DataSeederController {

    private final DataSeederService dataSeederService;
    private final AuctionService    auctionService;

    /**
     * GET /api/v1/admin/auctions — every auction in the system regardless of status (ADMIN only).
     * Used by the admin dashboard moderation table.
     */
    @GetMapping("/auctions")
    @RoleAccess(UserRole.ADMIN)
    public Result<List<Auction>> listAllAuctions() {
        return Result.ok(auctionService.listAll());
    }

    /**
     * POST /api/v1/admin/seed-data — generate synthetic users and auctions in bulk.
     * {@code directPercent} controls what fraction of per-user items are DIRECT-sale (0–100).
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
