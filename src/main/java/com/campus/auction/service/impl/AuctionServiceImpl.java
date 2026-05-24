package com.campus.auction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuctionServiceImpl extends ServiceImpl<AuctionMapper, Auction> implements AuctionService {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.05");
    private static final ZoneId     CST      = ZoneId.of("Asia/Shanghai");

    private final BidMapper   bidMapper;
    private final OrderMapper orderMapper;
    private final UserMapper  userMapper;

    @Override
    public Page<Auction> listActive(AuctionFilter f) {
        LambdaQueryWrapper<Auction> q = buildActiveQuery(f);

        // Count total matching records first (no LIMIT clause yet)
        long total = count(q);

        // Build a fresh wrapper with LIMIT/OFFSET appended for the data query.
        // Using int parameters is safe from SQL injection here.
        LambdaQueryWrapper<Auction> pagedQ = buildActiveQuery(f);
        int offset = (f.getPage() - 1) * f.getSize();
        pagedQ.last("LIMIT " + f.getSize() + " OFFSET " + offset);

        List<Auction> records = total > 0 ? list(pagedQ) : List.of();

        Page<Auction> result = new Page<>(f.getPage(), f.getSize(), total);
        result.setRecords(records);
        return result;
    }

    private LambdaQueryWrapper<Auction> buildActiveQuery(AuctionFilter f) {
        SaleType type = (f.getSaleType() != null && !f.getSaleType().isBlank())
                ? SaleType.valueOf(f.getSaleType().toUpperCase()) : null;

        LambdaQueryWrapper<Auction> q = new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, AuctionStatus.ACTIVE)
                .gt(Auction::getEndTime, LocalDateTime.now(CST))
                .eq(f.getCreatorId() != null, Auction::getCreatorId, f.getCreatorId())
                .eq(f.getCategory() != null && !f.getCategory().isBlank(), Auction::getCategory, f.getCategory())
                .eq(type != null, Auction::getSaleType, type)
                .ge(f.getMinPrice() != null, Auction::getCurrentPrice, f.getMinPrice())
                .le(f.getMaxPrice() != null, Auction::getCurrentPrice, f.getMaxPrice())
                .ge(f.getStartDate() != null, Auction::getEndTime,
                        f.getStartDate() != null ? f.getStartDate().atStartOfDay() : null)
                .le(f.getEndDate() != null, Auction::getEndTime,
                        f.getEndDate() != null ? f.getEndDate().plusDays(1).atStartOfDay() : null);

        if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
            String kw = f.getKeyword().trim();
            q.and(w -> w.like(Auction::getTitle, kw).or().like(Auction::getDescription, kw));
        }

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

        return q;
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
        if (LocalDateTime.now(CST).isAfter(auction.getEndTime())) {
            throw new ServiceException(HttpStatus.FORBIDDEN,
                    "Auction has already ended (end_time: " + auction.getEndTime() + ")");
        }

        // 409: bid must strictly beat the current price
        if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
            throw new ServiceException(HttpStatus.CONFLICT,
                    "Bid amount " + amount + " must be higher than current price " + auction.getCurrentPrice());
        }

        // 400: bidder must have sufficient balance to cover the bid
        User bidder = userMapper.selectById(bidderId);
        if (bidder == null || bidder.getBalance().compareTo(amount) < 0) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "出价金额不能超过您的账户余额");
        }

        // --- all checks passed; writes are atomic within this transaction ---

        Bid bid = new Bid();
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setAmount(amount);
        bid.setTimestamp(LocalDateTime.now(CST));
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
                && (request.getEndTime() == null || !request.getEndTime().isAfter(LocalDateTime.now(CST)))) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "截止时间必须是未来的时间");
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
        // Persist comma-separated image URLs when provided
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            auction.setImageUrls(String.join(",", request.getImageUrls()));
        }

        // Direct-sale items don't expire — use a far-future sentinel if no endTime is provided.
        if (saleType == SaleType.DIRECT && auction.getEndTime() == null) {
            auction.setEndTime(LocalDateTime.now(CST).plusYears(10));
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

        BigDecimal price  = bid.getAmount();
        Long       buyerId = bid.getBidderId();

        // Atomic check-and-deduct: the WHERE clause enforces balance >= price so
        // two concurrent accept-bid calls cannot both succeed for the same bidder.
        int deducted = userMapper.deductBalanceIfSufficient(buyerId, price);
        if (deducted == 0) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        // Credit seller (platform retains 5% fee implicitly)
        BigDecimal sellerReceives = price.multiply(BigDecimal.ONE.subtract(FEE_RATE))
                .setScale(2, RoundingMode.HALF_UP);
        userMapper.addBalance(auction.getCreatorId(), sellerReceives);

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
        order.setCreatedAt(LocalDateTime.now(CST));
        orderMapper.insert(order);

        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order acceptCurrentHighestBid(Long auctionId) {
        Auction auction = getAuctionById(auctionId);

        Long callerId = UserContext.get().userId();
        if (!auction.getCreatorId().equals(callerId)) {
            throw new ServiceException(HttpStatus.FORBIDDEN, "Only the auction owner can accept a bid");
        }
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "Auction is not active (status: " + auction.getStatus() + ")");
        }
        if (auction.getSaleType() != SaleType.AUCTION) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "Direct sale items cannot use accept-highest-bid");
        }

        Bid highest = bidMapper.selectHighestBid(auctionId);
        if (highest == null) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "当前没有出价，无法成交");
        }

        return acceptBid(auctionId, highest.getId());
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
                            "已有用户出价，无法修改价格与截止时间");
                }
            } else {
                if (request.getStartPrice() != null) {
                    auction.setStartPrice(request.getStartPrice());
                    auction.setCurrentPrice(request.getStartPrice());
                }
                if (request.getEndTime() != null) {
                    if (!request.getEndTime().isAfter(LocalDateTime.now(CST))) {
                        throw new ServiceException(HttpStatus.BAD_REQUEST, "截止时间必须是未来的时间");
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

        if (request.getImageUrls() != null) {
            auction.setImageUrls(request.getImageUrls().isEmpty()
                    ? null
                    : String.join(",", request.getImageUrls()));
        }

        updateById(auction);
        return auction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleExpiredAuction(Long auctionId) {
        // Re-fetch with row lock to guard against concurrent scheduler runs
        Auction auction = lambdaQuery()
                .eq(Auction::getId, auctionId)
                .last("FOR UPDATE")
                .one();
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVE) return;

        long bidCount = bidMapper.selectCount(
                new LambdaQueryWrapper<Bid>().eq(Bid::getAuctionId, auctionId));

        if (bidCount > 0) {
            Bid highest = bidMapper.selectHighestBid(auctionId);
            if (highest == null) {
                cancelAuctionInternal(auction);
                return;
            }

            int deducted = userMapper.deductBalanceIfSufficient(highest.getBidderId(), highest.getAmount());
            if (deducted == 0) {
                // Winning bidder has insufficient balance — cancel rather than hanging indefinitely
                cancelAuctionInternal(auction);
                return;
            }

            BigDecimal sellerReceives = highest.getAmount()
                    .multiply(BigDecimal.ONE.subtract(FEE_RATE))
                    .setScale(2, RoundingMode.HALF_UP);
            userMapper.addBalance(auction.getCreatorId(), sellerReceives);

            auction.setStatus(AuctionStatus.SOLD);
            auction.setCurrentPrice(highest.getAmount());
            updateById(auction);

            Order order = new Order();
            order.setAuctionId(auctionId);
            order.setBidId(highest.getId());
            order.setBuyerId(highest.getBidderId());
            order.setSellerId(auction.getCreatorId());
            order.setAmount(highest.getAmount());
            order.setCreatedAt(LocalDateTime.now(CST));
            orderMapper.insert(order);
        } else {
            cancelAuctionInternal(auction);
        }
    }

    private void cancelAuctionInternal(Auction auction) {
        auction.setStatus(AuctionStatus.CANCELLED);
        updateById(auction);
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
                .map(o -> new PurchasedItem(
                        o.getId(),
                        o.getSellerId(),
                        auctionMap.get(o.getAuctionId()),
                        o.getAmount(),
                        o.getCreatedAt()))
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

        BigDecimal price = auction.getCurrentPrice();

        // Atomic check-and-deduct: the WHERE clause enforces balance >= price so
        // two concurrent buy calls for the same item cannot both drain the buyer.
        int deducted = userMapper.deductBalanceIfSufficient(buyerId, price);
        if (deducted == 0) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        // Credit seller (platform retains 5% fee implicitly)
        BigDecimal sellerReceives = price.multiply(BigDecimal.ONE.subtract(FEE_RATE))
                .setScale(2, RoundingMode.HALF_UP);
        userMapper.addBalance(auction.getCreatorId(), sellerReceives);

        // Step 4: Mark auction SOLD and record order
        auction.setStatus(AuctionStatus.SOLD);
        updateById(auction);

        Order order = new Order();
        order.setAuctionId(auctionId);
        order.setBidId(null);
        order.setBuyerId(buyerId);
        order.setSellerId(auction.getCreatorId());
        order.setAmount(price);
        order.setCreatedAt(LocalDateTime.now(CST));
        orderMapper.insert(order);

        return order;
    }
}
