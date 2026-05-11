package com.campus.auction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.auction.entity.Auction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuctionMapper extends BaseMapper<Auction> {
}
