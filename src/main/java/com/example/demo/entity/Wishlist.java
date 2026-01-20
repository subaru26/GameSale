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
    private Double priceOld;        // 通常価格を追加
    private Integer cut;            // 割引率を追加
    private String shop;
    private String url;
    
    // 過去最安値情報を追加
    private Double historyLow;      // 過去最安値
    private Double historyLow1y;    // 過去1年間の最安値
    private Double historyLow3m;    // 過去3か月の最安値
    private Double storeLow;        // 現在ストアでの最安値
    
    // セール終了日時を追加
    private String expiry;
    
    private LocalDateTime addedAt;
}