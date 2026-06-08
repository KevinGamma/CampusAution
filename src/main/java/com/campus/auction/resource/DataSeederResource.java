package com.campus.auction.resource;

import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.common.Result;
import com.campus.auction.dto.SeedResult;
import com.campus.auction.entity.Auction;
import com.campus.auction.enums.UserRole;
import com.campus.auction.service.AuctionService;
import com.campus.auction.service.DataSeederService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Path("/api/v1/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class DataSeederResource {

    private final DataSeederService dataSeederService;
    private final AuctionService    auctionService;

    /** GET /api/v1/admin/auctions — every auction regardless of status (ADMIN only). */
    @GET
    @Path("/auctions")
    @RoleAccess(UserRole.ADMIN)
    public Result<List<Auction>> listAllAuctions() {
        return Result.ok(auctionService.listAll());
    }

    /** POST /api/v1/admin/seed-data — generate synthetic users and auctions in bulk. */
    @POST
    @Path("/seed-data")
    @RoleAccess(UserRole.ADMIN)
    public Result<SeedResult> seedData(
            @QueryParam("userCount")       @DefaultValue("10") int userCount,
            @QueryParam("auctionsPerUser") @DefaultValue("5")  int auctionsPerUser,
            @QueryParam("directPercent")   @DefaultValue("30") int directPercent,
            @QueryParam("directSaleCount") @DefaultValue("0")  int directSaleCount) {
        SeedResult result = dataSeederService.seed(
                userCount, auctionsPerUser,
                Math.max(0, Math.min(100, directPercent)),
                Math.max(0, directSaleCount));
        return Result.ok(result);
    }
}
