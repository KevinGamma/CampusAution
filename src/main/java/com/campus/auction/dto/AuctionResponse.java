package com.campus.auction.dto;

import com.campus.auction.common.Link;
import com.campus.auction.entity.Auction;
import com.campus.auction.enums.AuctionStatus;
import com.campus.auction.enums.SaleType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class AuctionResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final BigDecimal startPrice;
    private final BigDecimal currentPrice;
    private final LocalDateTime endTime;
    private final AuctionStatus status;
    private final Long creatorId;
    private final Integer quantity;
    private final String category;
    private final SaleType saleType;
    private final int bidCount;

    @JsonProperty("_links")
    private final Map<String, Link> links;

    private AuctionResponse(Auction a, int bidCount, Map<String, Link> links) {
        this.id           = a.getId();
        this.title        = a.getTitle();
        this.description  = a.getDescription();
        this.startPrice   = a.getStartPrice();
        this.currentPrice = a.getCurrentPrice();
        this.endTime      = a.getEndTime();
        this.status       = a.getStatus();
        this.creatorId    = a.getCreatorId();
        this.quantity     = a.getQuantity();
        this.category     = a.getCategory();
        this.saleType     = a.getSaleType();
        this.bidCount     = bidCount;
        this.links        = links;
    }

    public static AuctionResponse of(Auction a, int bidCount) {
        String base = "/auctions/" + a.getId();
        Map<String, Link> links = new LinkedHashMap<>();
        links.put("self",        new Link(base));
        links.put("place_bid",   new Link(base + "/bids"));
        links.put("bid_history", new Link(base + "/bids"));
        return new AuctionResponse(a, bidCount, links);
    }
}
