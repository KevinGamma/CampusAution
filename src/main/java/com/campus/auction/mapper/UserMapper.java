package com.campus.auction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.auction.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE users SET balance = balance - #{amount} WHERE id = #{id}")
    int deductBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    @Update("UPDATE users SET balance = balance + #{amount} WHERE id = #{id}")
    int addBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
