package com.campus.auction.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.common.Result;
import com.campus.auction.context.UserContext;
import com.campus.auction.dto.AuctionFilter;
import com.campus.auction.dto.AuctionResponse;
import com.campus.auction.dto.AuctionStatusRequest;
import com.campus.auction.dto.CreateAuctionRequest;
import com.campus.auction.dto.CreateOrderRequest;
import com.campus.auction.dto.PageResponse;
import com.campus.auction.dto.PlaceBidRequest;
import com.campus.auction.dto.UpdateAuctionRequest;
import com.campus.auction.entity.Auction;
import com.campus.auction.entity.Bid;
import com.campus.auction.entity.Order;
import com.campus.auction.enums.UserRole;
import com.campus.auction.exception.ServiceException;
import com.campus.auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    /** POST /api/v1/auctions — publish a new auction. */
    @PostMapping
    @RoleAccess(UserRole.STUDENT)
    public ResponseEntity<Result<AuctionResponse>> createAuction(@RequestBody CreateAuctionRequest request) {
        Auction auction = auctionService.createAuction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.created(AuctionResponse.of(auction, 0)));
    }

    /**
     * GET /api/v1/auctions — paginated active listings with optional filtering and sorting.
     *
     * <p>All parameters are optional. Defaults: page=1, size=10, sort=newest-first.
     */
    @GetMapping
    public Result<PageResponse<AuctionResponse>> listActive(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String saleType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size) {

        AuctionFilter filter = new AuctionFilter();
        filter.setKeyword(keyword);
        filter.setCategory(category);
        filter.setSaleType(saleType);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setSortBy(sortBy);
        filter.setOrder(order);
        filter.setCreatorId(creatorId);
        filter.setPage(page);
        filter.setSize(size);

        Page<Auction> auctionPage = auctionService.listActive(filter);
        List<AuctionResponse> items = auctionPage.getRecords().stream()
                .map(a -> AuctionResponse.of(a, auctionService.countBids(a.getId())))
                .toList();
        return Result.ok(new PageResponse<>(items, auctionPage.getTotal(),
                auctionPage.getCurrent(), auctionPage.getSize()));
    }

    /**
     * GET /api/v1/auctions/{id} — auction detail with HATEOAS links and ETag caching.
     * Returns 304 Not Modified when the client's cached ETag is still valid.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<AuctionResponse>> getById(
            @PathVariable Long id,
            WebRequest webRequest) {

        Auction auction = auctionService.getAuctionById(id);
        String etag = etagOf(auction);

        if (webRequest.checkNotModified(etag)) {
            return null;
        }

        int bidCount = auctionService.countBids(id);
        return ResponseEntity.ok()
                .eTag(etag)
                .body(Result.ok(AuctionResponse.of(auction, bidCount)));
    }

    /**
     * PUT /api/v1/auctions/{id} — full content update of an ACTIVE listing owned by the caller.
     * title, description, category, quantity are always editable.
     * startPrice and endTime may only change on AUCTION items with zero bids.
     */
    @PutMapping("/{id}")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<Auction> updateAuction(
            @PathVariable Long id,
            @RequestBody UpdateAuctionRequest request) {
        return Result.ok(auctionService.updateAuction(id, request));
    }

    /**
     * PATCH /api/v1/auctions/{id}/status — state transition.
     * <ul>
     *   <li>{@code {"status":"CANCELLED"}} — owner cancels an ACTIVE listing.</li>
     *   <li>{@code {"status":"SOLD"}} — owner accepts the current highest bid, closing the auction.</li>
     * </ul>
     */
    @PatchMapping("/{id}/status")
    @RoleAccess(UserRole.STUDENT)
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody AuctionStatusRequest request) {
        if ("CANCELLED".equalsIgnoreCase(request.getStatus())) {
            auctionService.cancelAuction(id);
        } else if ("SOLD".equalsIgnoreCase(request.getStatus())) {
            auctionService.acceptCurrentHighestBid(id);
        } else {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "Invalid status transition. Allowed values: CANCELLED, SOLD");
        }
        return Result.empty();
    }

    /**
     * POST /api/v1/auctions/{id}/bids — submit a bid. Bidder identity comes from the JWT.
     * Returns 201 Created on success.
     */
    @PostMapping("/{id}/bids")
    @RoleAccess(UserRole.STUDENT)
    public ResponseEntity<Result<Bid>> placeBid(
            @PathVariable Long id,
            @RequestBody PlaceBidRequest request) {
        Long bidderId = UserContext.get().userId();
        Bid bid = auctionService.placeBid(id, bidderId, request.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.created(bid));
    }

    /** GET /api/v1/auctions/{id}/bids — all bids for an auction, newest first. */
    @GetMapping("/{id}/bids")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<List<Bid>> listBids(@PathVariable Long id) {
        return Result.ok(auctionService.listBids(id));
    }

    /**
     * POST /api/v1/auctions/{id}/orders — create an order for this auction.
     *
     * <ul>
     *   <li>No body or empty body → DIRECT-sale instant purchase by the current user.</li>
     *   <li>{@code {"bidId": 123}} → seller accepts that specific bid (AUCTION type).</li>
     *   <li>{@code {"acceptHighest": true}} → seller accepts the current highest bid.</li>
     * </ul>
     * Returns 201 Created with the persisted Order on success.
     */
    @PostMapping("/{id}/orders")
    @RoleAccess(UserRole.STUDENT)
    public ResponseEntity<Result<Order>> createOrder(
            @PathVariable Long id,
            @RequestBody(required = false) CreateOrderRequest request) {
        Order order;
        if (request != null && request.getBidId() != null) {
            order = auctionService.acceptBid(id, request.getBidId());
        } else if (request != null && Boolean.TRUE.equals(request.getAcceptHighest())) {
            order = auctionService.acceptCurrentHighestBid(id);
        } else {
            order = auctionService.buy(id);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.created(order));
    }

    /**
     * DELETE /api/v1/auctions/{id} — remove an auction and all its bids.
     * ADMIN may delete any; STUDENT may only delete their own.
     */
    @DeleteMapping("/{id}")
    @RoleAccess({UserRole.ADMIN, UserRole.STUDENT})
    public Result<Void> deleteAuction(@PathVariable Long id) {
        auctionService.deleteAuction(id);
        return Result.empty();
    }

    private static String etagOf(Auction a) {
        String fingerprint = a.getId() + "~" + a.getTitle() + "~" + a.getDescription()
                + "~" + a.getStartPrice() + "~" + a.getCurrentPrice()
                + "~" + a.getEndTime() + "~" + a.getStatus() + "~" + a.getCreatorId();
        return String.format("%08x", fingerprint.hashCode());
    }
}
