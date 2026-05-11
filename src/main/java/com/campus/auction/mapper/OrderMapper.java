package com.campus.auction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.auction.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
