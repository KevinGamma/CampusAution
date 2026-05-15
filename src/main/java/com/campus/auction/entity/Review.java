package com.campus.auction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reviews")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** The completed order this review is for. */
    private Long orderId;

    /** The user who wrote the review. */
    private Long reviewerId;

    /** The user being reviewed. */
    private Long revieweeId;

    /** Star rating 1 (poor) – 5 (excellent). */
    private Integer rating;

    /** Optional free-text comment. */
    private String comment;

    private LocalDateTime createTime;
}
