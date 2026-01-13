package com.example.demo.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Wishlist {
    private Long id;
    private Long userId;
    private String gameId;
    private String gameTitle;
    private String gameImage;
    private Double currentPrice;
    private String shop;
    private String url;  // ストアURLを追加
    private LocalDateTime addedAt;
}