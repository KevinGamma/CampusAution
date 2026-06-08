package com.campus.auction.resource;

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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@Path("/api/v1/auctions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class AuctionResource {

    private final AuctionService auctionService;
    private final ObjectMapper   objectMapper;   // needed for optional body in createOrder

    /** POST /api/v1/auctions — publish a new auction. */
    @POST
    @RoleAccess(UserRole.STUDENT)
    public Result<AuctionResponse> createAuction(CreateAuctionRequest request) {
        Auction auction = auctionService.createAuction(request);
        // Result.created() carries code=201; ResultHttpStatusFilter syncs the HTTP status.
        return Result.created(AuctionResponse.of(auction, 0));
    }

    /**
     * GET /api/v1/auctions — paginated active listings with optional filtering and sorting.
     * All parameters optional; defaults: page=1, size=10.
     * Date params accept ISO-8601 format (yyyy-MM-dd) via LocalDateParamConverter.
     */
    @GET
    public Result<PageResponse<AuctionResponse>> listActive(
            @QueryParam("keyword")    String keyword,
            @QueryParam("category")   String category,
            @QueryParam("saleType")   String saleType,
            @QueryParam("minPrice")   BigDecimal minPrice,
            @QueryParam("maxPrice")   BigDecimal maxPrice,
            @QueryParam("startDate")  LocalDate startDate,
            @QueryParam("endDate")    LocalDate endDate,
            @QueryParam("sortBy")     String sortBy,
            @QueryParam("order")      String order,
            @QueryParam("creatorId")  Long creatorId,
            @QueryParam("page")  @DefaultValue("1")  int page,
            @QueryParam("size")  @DefaultValue("10") int size) {

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
     * GET /api/v1/auctions/{id} — auction detail with ETag conditional caching.
     * Returns 304 Not Modified when the client's If-None-Match matches the current ETag.
     * Returns Response directly to carry both the ETag header and the Result body.
     */
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id, @Context Request request) {
        Auction auction = auctionService.getAuctionById(id);
        EntityTag etag = new EntityTag(etagOf(auction));

        // Evaluate If-None-Match / If-Match preconditions.
        Response.ResponseBuilder notModified = request.evaluatePreconditions(etag);
        if (notModified != null) {
            return notModified.build(); // 304 No Body
        }

        int bidCount = auctionService.countBids(id);
        // ResultHttpStatusFilter reads the Result entity and sets status 200 — consistent.
        return Response.ok(Result.ok(AuctionResponse.of(auction, bidCount)))
                       .tag(etag)
                       .build();
    }

    /**
     * PUT /api/v1/auctions/{id} — full content update of an ACTIVE listing.
     * startPrice and endTime only changeable on AUCTION items with zero bids.
     */
    @PUT
    @Path("/{id}")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<Auction> updateAuction(@PathParam("id") Long id,
                                          UpdateAuctionRequest request) {
        return Result.ok(auctionService.updateAuction(id, request));
    }

    /**
     * PATCH /api/v1/auctions/{id}/status — state transition.
     * Allowed values: CANCELLED (owner cancels), SOLD (owner accepts highest bid).
     */
    @PATCH
    @Path("/{id}/status")
    @RoleAccess(UserRole.STUDENT)
    public Result<Void> updateStatus(@PathParam("id") Long id,
                                      AuctionStatusRequest request) {
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

    /** POST /api/v1/auctions/{id}/bids — submit a bid; bidder identity comes from JWT. */
    @POST
    @Path("/{id}/bids")
    @RoleAccess(UserRole.STUDENT)
    public Result<Bid> placeBid(@PathParam("id") Long id, PlaceBidRequest request) {
        Long bidderId = UserContext.get().userId();
        Bid bid = auctionService.placeBid(id, bidderId, request.getAmount());
        return Result.created(bid);
    }

    /** GET /api/v1/auctions/{id}/bids — all bids for an auction, newest first. */
    @GET
    @Path("/{id}/bids")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<List<Bid>> listBids(@PathParam("id") Long id) {
        return Result.ok(auctionService.listBids(id));
    }

    /**
     * POST /api/v1/auctions/{id}/orders — create an order for this auction.
     *
     * The request body is optional to support all three variants:
     *   - No body / empty body         → DIRECT-sale instant purchase.
     *   - {"bidId": 123}               → seller accepts a specific bid.
     *   - {"acceptHighest": true}       → seller accepts the current highest bid.
     *
     * @Consumes is widened to WILDCARD so Jersey does not reject requests with
     * no Content-Type or empty body; the body is read as raw String and parsed
     * manually so that a missing body maps cleanly to a null CreateOrderRequest.
     */
    @POST
    @Path("/{id}/orders")
    @RoleAccess(UserRole.STUDENT)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.WILDCARD})
    public Result<Order> createOrder(@PathParam("id") Long id, String bodyJson) {
        CreateOrderRequest request = null;
        if (bodyJson != null && !bodyJson.isBlank()) {
            try {
                request = objectMapper.readValue(bodyJson, CreateOrderRequest.class);
            } catch (JsonProcessingException e) {
                throw new ServiceException(HttpStatus.BAD_REQUEST,
                        "Invalid request body: " + e.getMessage());
            }
        }
        Order order;
        if (request != null && request.getBidId() != null) {
            order = auctionService.acceptBid(id, request.getBidId());
        } else if (request != null && Boolean.TRUE.equals(request.getAcceptHighest())) {
            order = auctionService.acceptCurrentHighestBid(id);
        } else {
            order = auctionService.buy(id);
        }
        return Result.created(order);
    }

    /**
     * DELETE /api/v1/auctions/{id} — remove an auction and all its bids.
     * ADMIN may delete any; STUDENT may only delete their own.
     */
    @DELETE
    @Path("/{id}")
    @RoleAccess({UserRole.ADMIN, UserRole.STUDENT})
    public Result<Void> deleteAuction(@PathParam("id") Long id) {
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
