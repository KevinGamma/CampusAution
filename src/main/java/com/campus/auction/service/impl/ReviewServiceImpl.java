package com.campus.auction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.auction.context.UserContext;
import com.campus.auction.dto.CreateReviewRequest;
import com.campus.auction.dto.ReviewResponse;
import com.campus.auction.entity.Order;
import com.campus.auction.entity.Review;
import com.campus.auction.entity.User;
import com.campus.auction.exception.ServiceException;
import com.campus.auction.mapper.OrderMapper;
import com.campus.auction.mapper.ReviewMapper;
import com.campus.auction.mapper.UserMapper;
import com.campus.auction.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper  orderMapper;
    private final UserMapper   userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Review createReview(CreateReviewRequest request) {

        // ── 1. Validate rating range ──────────────────────────────────────────
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        // ── 2. Verify the order exists ────────────────────────────────────────
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new ServiceException(HttpStatus.NOT_FOUND, "Order not found");
        }

        // ── 3. Caller must be buyer or seller ─────────────────────────────────
        Long callerId = UserContext.get().userId();
        Long revieweeId;
        if (callerId.equals(order.getBuyerId())) {
            revieweeId = order.getSellerId();   // buyer reviews the seller
        } else if (callerId.equals(order.getSellerId())) {
            revieweeId = order.getBuyerId();    // seller reviews the buyer
        } else {
            throw new ServiceException(HttpStatus.FORBIDDEN,
                    "You were not a participant in this transaction");
        }

        // ── 4. Prevent duplicate review for (order, reviewer) ─────────────────
        boolean alreadyReviewed = reviewMapper.exists(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId,    request.getOrderId())
                .eq(Review::getReviewerId, callerId));
        if (alreadyReviewed) {
            throw new ServiceException(HttpStatus.CONFLICT,
                    "You have already submitted a review for this transaction");
        }

        // ── 5. Persist ────────────────────────────────────────────────────────
        Review review = new Review();
        review.setOrderId(request.getOrderId());
        review.setReviewerId(callerId);
        review.setRevieweeId(revieweeId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCreateTime(LocalDateTime.now());
        reviewMapper.insert(review);

        return review;
    }

    @Override
    public List<ReviewResponse> getReviewsByReviewee(Long revieweeId) {
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getRevieweeId, revieweeId)
                .orderByDesc(Review::getCreateTime));

        if (reviews.isEmpty()) return Collections.emptyList();

        // Batch-load reviewer profiles to avoid N+1 queries
        Set<Long> reviewerIds = reviews.stream()
                .map(Review::getReviewerId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(reviewerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return reviews.stream()
                .map(r -> {
                    User reviewer = userMap.get(r.getReviewerId());
                    return new ReviewResponse(
                            r.getId(),
                            r.getOrderId(),
                            r.getReviewerId(),
                            reviewer != null ? reviewer.getUsername()  : "未知用户",
                            reviewer != null ? reviewer.getAvatarUrl() : null,
                            r.getRevieweeId(),
                            r.getRating(),
                            r.getComment(),
                            r.getCreateTime()
                    );
                })
                .toList();
    }

    @Override
    public Double getAverageRating(Long revieweeId) {
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getRevieweeId, revieweeId)
                .select(Review::getRating));
        if (reviews.isEmpty()) return null;
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    @Override
    public long getReviewCount(Long revieweeId) {
        return reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getRevieweeId, revieweeId));
    }
}
