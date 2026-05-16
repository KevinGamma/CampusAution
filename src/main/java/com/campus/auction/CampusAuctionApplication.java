package com.campus.auction;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.campus.auction.mapper")
public class CampusAuctionApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusAuctionApplication.class, args);
    }
}
