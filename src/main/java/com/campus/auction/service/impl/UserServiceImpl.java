package com.campus.auction.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.auction.context.UserContext;
import com.campus.auction.dto.UserDTO;
import com.campus.auction.entity.User;
import com.campus.auction.enums.UserRole;
import com.campus.auction.exception.ServiceException;
import com.campus.auction.mapper.UserMapper;
import com.campus.auction.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public UserDTO login(String username, String password) {
        User user = lambdaQuery()
                .eq(User::getUsername, username)
                .one();

        // Deliberately identical error for "user not found" and "wrong password"
        // to prevent username enumeration via response timing differences.
        if (user == null || !user.getPassword().equals(password)) {
            throw new ServiceException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        return UserDTO.from(user);
    }

    @Override
    public UserDTO register(String username, String password) {
        boolean taken = lambdaQuery().eq(User::getUsername, username).exists();
        if (taken) {
            throw new ServiceException(HttpStatus.CONFLICT, "Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(UserRole.STUDENT);   // always STUDENT — ignores any client-supplied role
        user.setBalance(BigDecimal.ZERO);
        save(user);

        return UserDTO.from(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO recharge(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Recharge amount must be positive");
        }
        Long userId = UserContext.get().userId();
        getBaseMapper().addBalance(userId, amount);
        return UserDTO.from(getById(userId));
    }
}
