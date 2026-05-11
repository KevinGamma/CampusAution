package com.campus.auction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.auction.context.UserContext;
import com.campus.auction.dto.AuctionFilter;
import com.campus.auction.dto.CreateAuctionRequest;
import com.campus.auction.dto.MyBidSummary;
import com.campus.auction.dto.PurchasedItem;
import com.campus.auction.dto.UpdateAuctionRequest;
import com.campus.auction.entity.Auction;
import com.campus.auction.entity.Bid;
import com.campus.auction.entity.Order;
import com.campus.auction.entity.User;
import com.campus.auction.enums.AuctionStatus;
import com.campus.auction.enums.SaleType;
import com.campus.auction.enums.UserRole;
import com.campus.auction.exception.ServiceException;
import com.campus.auction.mapper.AuctionMapper;
import com.campus.auction.mapper.BidMapper;
import com.campus.auction.mapper.OrderMapper;
import com.campus.auction.mapper.UserMapper;
import com.campus.auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuctionServiceImpl extends ServiceImpl<AuctionMapper, Auction> implements AuctionService {

    private final BidMapper   bidMapper;
    private final OrderMapper orderMapper;
    private final UserMapper  userMapper;

    @Override
    public List<Auction> listActive(AuctionFilter f) {
        SaleType type = (f.getSaleType() != null && !f.getSaleType().isBlank())
                ? SaleType.valueOf(f.getSaleType().toUpperCase()) : null;

        LambdaQueryWrapper<Auction> q = new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, AuctionStatus.ACTIVE)
                .gt(Auction::getEndTime, LocalDateTime.now())
                .eq(f.getCategory() != null && !f.getCategory().isBlank(), Auction::getCategory, f.getCategory())
                .eq(type != null, Auction::getSaleType, type)
                .ge(f.getMinPrice() != null, Auction::getCurrentPrice, f.getMinPrice())
                .le(f.getMaxPrice() != null, Auction::getCurrentPrice, f.getMaxPrice())
                .ge(f.getStartDate() != null, Auction::getEndTime,
                        f.getStartDate() != null ? f.getStartDate().atStartOfDay() : null)
                .le(f.getEndDate() != null, Auction::getEndTime,
                        f.getEndDate() != null ? f.getEndDate().plusDays(1).atStartOfDay() : null);

        // Full-text keyword search across title and description
        if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
            String kw = f.getKeyword().trim();
            q.and(w -> w.like(Auction::getTitle, kw).or().like(Auction::getDescription, kw));
        }

        // Dynamic sort — default: newest first (id DESC)
        boolean asc = "ASC".equalsIgnoreCase(f.getOrder());
        if ("currentPrice".equals(f.getSortBy())) {
            if (asc) q.orderByAsc(Auction::getCurrentPrice);
            else     q.orderByDesc(Auction::getCurrentPrice);
        } else if ("endTime".equals(f.getSortBy())) {
            if (asc) q.orderByAsc(Auction::getEndTime);
            else     q.orderByDesc(Auction::getEndTime);
        } else {
            q.orderByDesc(Auction::getId);
        }

        return list(q);
    }

    @Override
    public List<Auction> listAll() {
        return list(); // IService.list() with no wrapper → SELECT * FROM auctions
    }

    @Override
    public Auction getAuctionById(Long id) {
        Auction auction = getById(id);
        if (auction == null) {
            throw new ServiceException(HttpStatus.NOT_FOUND, "Auction not found: " + id);
        }
        return auction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Bid placeBid(Long auctionId, Long bidderId, BigDecimal amount) {
        // Lock the auction row for the duration of this transaction to prevent
        // concurrent bids from racing past the price check simultaneously.
        Auction auction = lambdaQuery()
                .eq(Auction::getId, auctionId)
                .last("FOR UPDATE")
                .one();

        if (auction == null) {
            throw new ServiceException(HttpStatus.NOT_FOUND, "Auction not found: " + auctionId);
        }

        // 403: status must be ACTIVE (FINISHED / SOLD / CANCELLED are all forbidden)
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ServiceException(HttpStatus.FORBIDDEN,
                    "Auction is not open for bidding (status: " + auction.getStatus() + ")");
        }

        // 403: creators may not bid on their own auctions
        if (bidderId.equals(auction.getCreatorId())) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "You cannot bid on your own auction");
        }

        // 403: wall-clock time must still be within the auction window
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new ServiceException(HttpStatus.FORBIDDEN,
                    "Auction has already ended (end_time: " + auction.getEndTime() + ")");
        }

        // 409: bid must strictly beat the current price
        if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
            throw new ServiceException(HttpStatus.CONFLICT,
                    "Bid amount " + amount + " must be higher than current price " + auction.getCurrentPrice());
        }

        // --- all checks passed; writes are atomic within this transaction ---

        Bid bid = new Bid();
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setAmount(amount);
        bid.setTimestamp(LocalDateTime.now());
        bidMapper.insert(bid);

        auction.setCurrentPrice(amount);
        updateById(auction);

        return bid;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAuction(Long auctionId) {
        Auction auction = getAuctionById(auctionId); // throws 404 if absent

        UserContext.CurrentUser caller = UserContext.get();

        // STUDENT: ownership check — only the creator may delete their own auction
        if (caller.role() == UserRole.STUDENT
                && !auction.getCreatorId().equals(caller.userId())) {
            throw new ServiceException(HttpStatus.FORBIDDEN,
                    "Students can only delete their own auctions");
        }

        // ADMIN: skips ownership check — can remove any auction for moderation

        // Delete associated bids first to satisfy the FK constraint
        // (bids.auction_id → auctions.id has no ON DELETE CASCADE)
        bidMapper.delete(new LambdaQueryWrapper<Bid>()
                .eq(Bid::getAuctionId, auctionId));

        removeById(auctionId);
    }

    @Override
    public Auction createAuction(CreateAuctionRequest request) {
        SaleType st = request.getSaleType() != null ? request.getSaleType() : SaleType.AUCTION;
        // Auction items must have an explicit future end time; DIRECT items default to 10 years out.
        if (st == SaleType.AUCTION
                && (request.getEndTime() == null || !request.getEndTime().isAfter(LocalDateTime.now()))) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "End time must be a future date");
        }

        Auction auction = new Auction();
        auction.setTitle(request.getTitle());
        auction.setDescription(request.getDescription());
        auction.setStartPrice(request.getStartPrice());
        auction.setCurrentPrice(request.getStartPrice());
        auction.setEndTime(request.getEndTime());
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setCreatorId(UserContext.get().userId());
        auction.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        auction.setCategory(request.getCategory() != null ? request.getCategory() : "Others");
        SaleType saleType = request.getSaleType() != null ? request.getSaleType() : SaleType.AUCTION;
        auction.setSaleType(saleType);

        // Direct-sale items don't expire — use a far-future sentinel if no endTime is provided.
        if (saleType == SaleType.DIRECT && auction.getEndTime() == null) {
            auction.setEndTime(LocalDateTime.now().plusYears(10));
        }

        save(auction);
        return auction;
    }

    @Override
    public int countBids(Long auctionId) {
        return Math.toIntExact(bidMapper.selectCount(
                new LambdaQueryWrapper<Bid>().eq(Bid::getAuctionId, auctionId)));
    }

    @Override
    public List<Auction> listMyItems() {
        Long userId = UserContext.get().userId();
        return lambdaQuery().eq(Auction::getCreatorId, userId).list();
    }

    @Override
    public List<MyBidSummary> listMyBids() {
        Long userId = UserContext.get().userId();

        List<Bid> myBids = bidMapper.selectList(
                new LambdaQueryWrapper<Bid>().eq(Bid::getBidderId, userId));
        if (myBids.isEmpty()) return List.of();

        // Highest bid amount this user placed per auction
        Map<Long, BigDecimal> maxByAuction = myBids.stream().collect(
                Collectors.groupingBy(
                        Bid::getAuctionId,
                        Collectors.mapping(Bid::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::max))));

        List<Auction> auctions = listByIds(maxByAuction.keySet());
        return auctions.stream()
                .map(a -> new MyBidSummary(a, maxByAuction.get(a.getId())))
                .toList();
    }

    @Override
    public List<Bid> listBids(Long auctionId) {
        getAuctionById(auctionId); // throws 404 if the auction doesn't exist
        return bidMapper.selectList(
                new LambdaQueryWrapper<Bid>()
                        .eq(Bid::getAuctionId, auctionId)
                        .orderByDesc(Bid::getTimestamp));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order acceptBid(Long auctionId, Long bidId) {
        Auction auction = getAuctionById(auctionId);

        Long callerId = UserContext.get().userId();
        if (!auction.getCreatorId().equals(callerId)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "Only the auction owner can accept a bid");
        }
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "Auction is not active (status: " + auction.getStatus() + ")");
        }

        Bid bid = bidMapper.selectById(bidId);
        if (bid == null || !bid.getAuctionId().equals(auctionId)) {
            throw new ServiceException(HttpStatus.NOT_FOUND, "Bid not found on this auction");
        }

        // Step 1: Check buyer (bidder) has sufficient balance
        BigDecimal price = bid.getAmount();
        Long buyerId = bid.getBidderId();
        User buyer = userMapper.selectById(buyerId);
        if (buyer == null || buyer.getBalance().compareTo(price) < 0) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        // Step 2: Deduct from buyer (atomic SQL arithmetic)
        userMapper.deductBalance(buyerId, price);

        // Step 3: Credit seller
        userMapper.addBalance(auction.getCreatorId(), price);

        // Step 4: Close auction and persist order
        auction.setStatus(AuctionStatus.SOLD);
        auction.setCurrentPrice(price);
        updateById(auction);

        Order order = new Order();
        order.setAuctionId(auctionId);
        order.setBidId(bidId);
        order.setBuyerId(buyerId);
        order.setSellerId(auction.getCreatorId());
        order.setAmount(price);
        order.setCreatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Auction updateAuction(Long auctionId, UpdateAuctionRequest request) {
        Auction auction = getAuctionById(auctionId);

        Long callerId = UserContext.get().userId();
        if (!auction.getCreatorId().equals(callerId)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "You can only edit your own listings");
        }
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Only active listings can be edited");
        }

        // Always-editable fields — skip when caller sent null
        if (request.getTitle()       != null) auction.setTitle(request.getTitle());
        if (request.getDescription() != null) auction.setDescription(request.getDescription());
        if (request.getCategory()    != null) auction.setCategory(request.getCategory());
        if (request.getQuantity()    != null) auction.setQuantity(request.getQuantity());

        if (auction.getSaleType() == SaleType.AUCTION) {
            long bidCount = bidMapper.selectCount(
                    new LambdaQueryWrapper<Bid>().eq(Bid::getAuctionId, auctionId));
            if (bidCount > 0) {
                if (request.getStartPrice() != null || request.getEndTime() != null) {
                    throw new ServiceException(HttpStatus.BAD_REQUEST,
                            "Cannot change price or end time after bids have been placed");
                }
            } else {
                if (request.getStartPrice() != null) {
                    auction.setStartPrice(request.getStartPrice());
                    auction.setCurrentPrice(request.getStartPrice());
                }
                if (request.getEndTime() != null) {
                    if (!request.getEndTime().isAfter(LocalDateTime.now())) {
                        throw new ServiceException(HttpStatus.BAD_REQUEST, "End time must be a future date");
                    }
                    auction.setEndTime(request.getEndTime());
                }
            }
        } else {
            // DIRECT sale — price is always editable
            if (request.getStartPrice() != null) {
                auction.setStartPrice(request.getStartPrice());
                auction.setCurrentPrice(request.getStartPrice());
            }
        }

        updateById(auction);
        return auction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAuction(Long auctionId) {
        Auction auction = getAuctionById(auctionId);

        Long callerId = UserContext.get().userId();
        if (!auction.getCreatorId().equals(callerId)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "You can only cancel your own listings");
        }
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Only active listings can be cancelled");
        }

        auction.setStatus(AuctionStatus.CANCELLED);
        updateById(auction);
    }

    @Override
    public List<PurchasedItem> listPurchased() {
        Long userId = UserContext.get().userId();

        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getBuyerId, userId)
                        .orderByDesc(Order::getCreatedAt));
        if (orders.isEmpty()) return List.of();

        Set<Long> auctionIds = orders.stream().map(Order::getAuctionId).collect(Collectors.toSet());
        Map<Long, Auction> auctionMap = listByIds(auctionIds).stream()
                .collect(Collectors.toMap(Auction::getId, a -> a));

        return orders.stream()
                .filter(o -> auctionMap.containsKey(o.getAuctionId()))
                .map(o -> new PurchasedItem(auctionMap.get(o.getAuctionId()), o.getAmount(), o.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order buy(Long auctionId) {
        Auction auction = getAuctionById(auctionId);

        if (auction.getSaleType() != SaleType.DIRECT) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "This item is listed as an auction — use bidding to participate");
        }
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "This item is no longer available (status: " + auction.getStatus() + ")");
        }

        Long buyerId = UserContext.get().userId();
        if (auction.getCreatorId().equals(buyerId)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "You cannot purchase your own listing");
        }

        // Step 1: Check buyer has sufficient balance
        BigDecimal price = auction.getCurrentPrice();
        User buyer = userMapper.selectById(buyerId);
        if (buyer == null || buyer.getBalance().compareTo(price) < 0) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        // Step 2: Deduct from buyer (atomic SQL arithmetic, no lost-update risk)
        userMapper.deductBalance(buyerId, price);

        // Step 3: Credit seller
        userMapper.addBalance(auction.getCreatorId(), price);

        // Step 4: Mark auction SOLD and record order
        auction.setStatus(AuctionStatus.SOLD);
        updateById(auction);

        Order order = new Order();
        order.setAuctionId(auctionId);
        order.setBidId(null);
        order.setBuyerId(buyerId);
        order.setSellerId(auction.getCreatorId());
        order.setAmount(price);
        order.setCreatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        return order;
    }
}
