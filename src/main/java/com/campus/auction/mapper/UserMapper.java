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

    /**
     * Atomically deducts {@code amount} from the user's balance only when the
     * current balance is sufficient ({@code balance >= amount}).  Returns the
     * number of rows updated: 1 on success, 0 when the balance would go
     * negative (no write occurs).  This single-statement approach eliminates
     * the TOCTOU race that a separate SELECT + UPDATE pattern introduces.
     */
    @Update("UPDATE users SET balance = balance - #{amount} WHERE id = #{id} AND balance >= #{amount}")
    int deductBalanceIfSufficient(@Param("id") Long id, @Param("amount") BigDecimal amount);

    @Update("UPDATE users SET balance = balance + #{amount} WHERE id = #{id}")
    int addBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
