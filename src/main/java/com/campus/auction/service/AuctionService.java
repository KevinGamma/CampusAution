package com.campus.auction.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.auction.dto.AuctionFilter;
import com.campus.auction.dto.CreateAuctionRequest;
import com.campus.auction.dto.MyBidSummary;
import com.campus.auction.dto.PurchasedItem;
import com.campus.auction.dto.UpdateAuctionRequest;
import com.campus.auction.entity.Auction;
import com.campus.auction.entity.Bid;
import com.campus.auction.entity.Order;

import java.math.BigDecimal;
import java.util.List;

public interface AuctionService extends IService<Auction> {

    /**
     * Returns a paginated page of ACTIVE, non-expired auctions matching the filter criteria.
     * Every field of {@code filter} is optional — null / blank values are ignored.
     * Page number and size are taken from {@code filter.page} and {@code filter.size}.
     */
    Page<Auction> listActive(AuctionFilter filter);

    /**
     * Returns every auction in the system regardless of status.
     * Reserved for ADMIN use — protected at the controller level with {@code @RoleAccess}.
     */
    List<Auction> listAll();

    /**
     * Looks up an auction by id.
     *
     * @throws com.campus.auction.exception.ServiceException (404) if not found.
     */
    Auction getAuctionById(Long id);

    /**
     * Places a bid on an auction.
     *
     * @throws com.campus.auction.exception.ServiceException (403) if the auction is not in ACTIVE
     *         status, if the auction end_time has passed, or if the bidder is the creator.
     * @throws com.campus.auction.exception.ServiceException (409) if {@code amount} is not
     *         strictly greater than the current price.
     */
    Bid placeBid(Long auctionId, Long bidderId, BigDecimal amount);

    /**
     * Deletes an auction and its associated bids.
     *
     * <ul>
     *   <li>ADMIN — may delete any auction (moderation).</li>
     *   <li>STUDENT — may only delete auctions they created.</li>
     * </ul>
     *
     * @throws com.campus.auction.exception.ServiceException (404) if the auction does not exist.
     * @throws com.campus.auction.exception.ServiceException (403) if a STUDENT attempts to
     *         delete another user's auction.
     */
    void deleteAuction(Long auctionId);

    /**
     * Creates a new auction owned by the currently authenticated user.
     *
     * @throws com.campus.auction.exception.ServiceException (400) if endTime is not a future date.
     */
    Auction createAuction(CreateAuctionRequest request);

    /** Returns the number of bids placed on the given auction. */
    int countBids(Long auctionId);

    /** Returns every auction created by the currently authenticated user. */
    List<Auction> listMyItems();

    /**
     * Returns every auction the current user has placed at least one bid on,
     * paired with that user's highest bid amount for each auction.
     */
    List<MyBidSummary> listMyBids();

    /**
     * Returns all bids for a given auction, newest first.
     *
     * @throws com.campus.auction.exception.ServiceException (404) if the auction does not exist.
     */
    List<Bid> listBids(Long auctionId);

    /**
     * Owner accepts a specific bid, closing the auction immediately as SOLD.
     *
     * @throws com.campus.auction.exception.ServiceException (404) if auction or bid not found.
     * @throws com.campus.auction.exception.ServiceException (403) if caller is not the owner.
     * @throws com.campus.auction.exception.ServiceException (400) if auction is not ACTIVE.
     */
    Order acceptBid(Long auctionId, Long bidId);

    /**
     * Owner accepts the current highest bid, closing the auction as SOLD.
     *
     * @throws com.campus.auction.exception.ServiceException (404) if auction not found.
     * @throws com.campus.auction.exception.ServiceException (403) if caller is not the owner.
     * @throws com.campus.auction.exception.ServiceException (400) if auction is not ACTIVE,
     *         type is DIRECT, or no bids exist.
     */
    Order acceptCurrentHighestBid(Long auctionId);

    /**
     * Authenticated user purchases a DIRECT-sale item at its fixed price.
     *
     * @throws com.campus.auction.exception.ServiceException (400) if the item is an AUCTION type or not ACTIVE.
     * @throws com.campus.auction.exception.ServiceException (403) if the caller is the seller.
     * @throws com.campus.auction.exception.ServiceException (404) if the auction does not exist.
     */
    Order buy(Long auctionId);

    /**
     * Returns all items purchased by the current user (both AUCTION and DIRECT),
     * ordered newest-first by transaction date.
     */
    List<PurchasedItem> listPurchased();

    /**
     * Updates a listing owned by the current user.
     *
     * <ul>
     *   <li>title, description, category, quantity — always editable while ACTIVE.</li>
     *   <li>startPrice, endTime — editable only when the item is an AUCTION with zero bids.</li>
     *   <li>startPrice — always editable for DIRECT sale items.</li>
     * </ul>
     *
     * @throws com.campus.auction.exception.ServiceException (404) if not found.
     * @throws com.campus.auction.exception.ServiceException (403) if caller is not the creator.
     * @throws com.campus.auction.exception.ServiceException (400) if status is not ACTIVE, or if
     *         price/endTime changes are attempted on an AUCTION that already has bids.
     */
    Auction updateAuction(Long auctionId, UpdateAuctionRequest request);

    /**
     * Sets an ACTIVE listing to CANCELLED, removing it from the marketplace.
     *
     * @throws com.campus.auction.exception.ServiceException (404) if not found.
     * @throws com.campus.auction.exception.ServiceException (403) if caller is not the creator.
     * @throws com.campus.auction.exception.ServiceException (400) if status is not ACTIVE.
     */
    void cancelAuction(Long auctionId);

    /**
     * Called by the scheduler to auto-settle a single expired AUCTION.
     * Bypasses ownership/caller checks — for internal scheduler use only.
     * If the auction has bids, credits the seller and marks it SOLD.
     * If no bids exist, marks it CANCELLED.
     */
    void settleExpiredAuction(Long auctionId);
}
