package com.campus.auction.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.auction.entity.Auction;
import com.campus.auction.enums.AuctionStatus;
import com.campus.auction.enums.SaleType;
import com.campus.auction.mapper.AuctionMapper;
import com.campus.auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionTask {

    private static final ZoneId CST = ZoneId.of("Asia/Shanghai");

    private final AuctionMapper  auctionMapper;
    private final AuctionService auctionService;

    @Scheduled(cron = "0 * * * * ?")
    public void settleExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now(CST);
        List<Auction> expired = auctionMapper.selectList(
                new LambdaQueryWrapper<Auction>()
                        .eq(Auction::getStatus, AuctionStatus.ACTIVE)
                        .eq(Auction::getSaleType, SaleType.AUCTION)
                        .le(Auction::getEndTime, now));

        for (Auction auction : expired) {
            try {
                auctionService.settleExpiredAuction(auction.getId());
                log.info("Auto-settled auction {}", auction.getId());
            } catch (Exception e) {
                log.error("Failed to settle auction {}: {}", auction.getId(), e.getMessage(), e);
            }
        }
    }
}
