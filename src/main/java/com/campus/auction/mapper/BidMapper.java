package com.campus.auction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.auction.entity.Bid;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BidMapper extends BaseMapper<Bid> {

    @Select("SELECT * FROM bids WHERE auction_id = #{auctionId} ORDER BY amount DESC LIMIT 1")
    Bid selectHighestBid(@Param("auctionId") Long auctionId);
}
