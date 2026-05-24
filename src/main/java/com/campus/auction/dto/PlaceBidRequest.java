package com.campus.auction.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlaceBidRequest {

    private BigDecimal amount;
}
