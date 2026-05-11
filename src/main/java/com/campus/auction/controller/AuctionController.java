package com.campus.auction.controller;

import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.common.Result;
import com.campus.auction.dto.AuctionFilter;
import com.campus.auction.dto.AuctionResponse;
import com.campus.auction.dto.CreateAuctionRequest;
import com.campus.auction.dto.MyBidSummary;
import com.campus.auction.dto.PlaceBidRequest;
import com.campus.auction.dto.PurchasedItem;
import com.campus.auction.dto.UpdateAuctionRequest;
import com.campus.auction.entity.Auction;
import com.campus.auction.entity.Bid;
import com.campus.auction.entity.Order;
import com.campus.auction.enums.UserRole;
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
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    /**
     * GET /auctions/all — every auction in the system (ADMIN only).
     * Used by the admin dashboard to display the full moderation table.
     */
    @GetMapping("/all")
    @RoleAccess(UserRole.ADMIN)
    public Result<List<Auction>> listAll() {
        return Result.ok(auctionService.listAll());
    }

    /** POST /auctions — publish a new auction; creatorId and initial state set by the service. */
    @PostMapping
    @RoleAccess(UserRole.STUDENT)
    public ResponseEntity<Result<AuctionResponse>> createAuction(@RequestBody CreateAuctionRequest request) {
        Auction auction = auctionService.createAuction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.created(AuctionResponse.of(auction, 0)));
    }

    /**
     * GET /auctions — active, non-expired listings with optional full-text search,
     * price/date range filtering, and dynamic sorting.
     *
     * <p>All parameters are optional. Unrecognised {@code sortBy} values fall back
     * to newest-first ordering.
     */
    @GetMapping
    public Result<List<AuctionResponse>> listActive(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String saleType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order) {

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

        List<AuctionResponse> body = auctionService.listActive(filter)
                .stream()
                .map(a -> AuctionResponse.of(a, auctionService.countBids(a.getId())))
                .toList();
        return Result.ok(body);
    }

    /**
     * GET /auctions/{id} — auction detail with HATEOAS links and ETag caching.
     *
     * <p>On a match with {@code If-None-Match}, Spring sets status 304 via
     * {@code webRequest.checkNotModified} and we return {@code null} — no body is written.
     * On a miss we attach the computed ETag so the client can cache it.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<AuctionResponse>> getById(
            @PathVariable Long id,
            WebRequest webRequest) {

        Auction auction = auctionService.getAuctionById(id);
        String etag = etagOf(auction);

        // Returns true and sets 304 on the response when If-None-Match matches.
        if (webRequest.checkNotModified(etag)) {
            return null;
        }

        int bidCount = auctionService.countBids(id);
        return ResponseEntity.ok()
                .eTag(etag)
                .body(Result.ok(AuctionResponse.of(auction, bidCount)));
    }

    /**
     * POST /auctions/{id}/bids — submit a bid. STUDENT only; admins are system overseers,
     * not market participants.
     * Returns 201 Created on success; 403/404/409 are handled by GlobalExceptionHandler.
     */
    @PostMapping("/{id}/bids")
    @RoleAccess(UserRole.STUDENT)
    public ResponseEntity<Result<Bid>> placeBid(
            @PathVariable Long id,
            @RequestBody PlaceBidRequest request) {
        Bid bid = auctionService.placeBid(id, request.getBidderId(), request.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.created(bid));
    }

    /**
     * POST /auctions/{id}/buy — instant purchase of a DIRECT-sale item at its fixed price.
     * STUDENT only; admins may not participate in trading.
     * Returns the created Order record.
     */
    @PostMapping("/{id}/buy")
    @RoleAccess(UserRole.STUDENT)
    public Result<Order> buy(@PathVariable Long id) {
        return Result.ok(auctionService.buy(id));
    }

    /** GET /auctions/my-items — auctions created by the authenticated user, with bid counts. */
    @GetMapping("/my-items")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<List<AuctionResponse>> listMyItems() {
        return Result.ok(auctionService.listMyItems()
                .stream()
                .map(a -> AuctionResponse.of(a, auctionService.countBids(a.getId())))
                .toList());
    }

    /** GET /auctions/my-bids — auctions the authenticated user has bid on, with their top bid. */
    @GetMapping("/my-bids")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<List<MyBidSummary>> listMyBids() {
        return Result.ok(auctionService.listMyBids());
    }

    /** GET /auctions/purchased — all items bought by the current user, newest first. */
    @GetMapping("/purchased")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<List<PurchasedItem>> listPurchased() {
        return Result.ok(auctionService.listPurchased());
    }

    /**
     * PUT /auctions/{id} — update an ACTIVE listing owned by the current user.
     * title, description, category, quantity are always editable.
     * startPrice and endTime may only be changed when no bids exist (AUCTION items).
     */
    @PutMapping("/{id}")
    @RoleAccess(UserRole.STUDENT)
    public Result<Auction> updateAuction(
            @PathVariable Long id,
            @RequestBody UpdateAuctionRequest request) {
        return Result.ok(auctionService.updateAuction(id, request));
    }

    /**
     * POST /auctions/{id}/cancel — cancel an ACTIVE listing, removing it from the marketplace.
     * Only the listing's creator may cancel it.
     */
    @PostMapping("/{id}/cancel")
    @RoleAccess(UserRole.STUDENT)
    public Result<Void> cancelAuction(@PathVariable Long id) {
        auctionService.cancelAuction(id);
        return Result.empty();
    }

    /** GET /auctions/{id}/bids — all bids for an auction, newest first. */
    @GetMapping("/{id}/bids")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<List<Bid>> listBids(@PathVariable Long id) {
        return Result.ok(auctionService.listBids(id));
    }

    /**
     * POST /auctions/{auctionId}/accept-bid/{bidId} — owner accepts a bid, closing the auction as SOLD.
     * STUDENT only; admins manage the platform but do not own or sell items.
     * Returns the created Order record confirming the transaction.
     */
    @PostMapping("/{auctionId}/accept-bid/{bidId}")
    @RoleAccess(UserRole.STUDENT)
    public Result<Order> acceptBid(@PathVariable Long auctionId, @PathVariable Long bidId) {
        return Result.ok(auctionService.acceptBid(auctionId, bidId));
    }

    /**
     * DELETE /auctions/{id} — remove an auction and all its bids.
     *
     * <p>Role rules enforced by the service layer (caller identity comes from the JWT via
     * {@link com.campus.auction.context.UserContext}):
     * <ul>
     *   <li>ADMIN — may delete any auction for moderation.</li>
     *   <li>STUDENT — may only delete their own auction (403 otherwise).</li>
     * </ul>
     */
    @DeleteMapping("/{id}")
    @RoleAccess({UserRole.ADMIN, UserRole.STUDENT})
    public Result<Void> deleteAuction(@PathVariable Long id) {
        auctionService.deleteAuction(id);
        return Result.empty();
    }

    /**
     * Fingerprints every mutable field of an auction so that any change — a new bid raising
     * {@code currentPrice}, a status transition, etc. — produces a different ETag value.
     */
    private static String etagOf(Auction a) {
        String fingerprint = a.getId() + "~" + a.getTitle() + "~" + a.getDescription()
                + "~" + a.getStartPrice() + "~" + a.getCurrentPrice()
                + "~" + a.getEndTime() + "~" + a.getStatus() + "~" + a.getCreatorId();
        return String.format("%08x", fingerprint.hashCode());
    }
}
