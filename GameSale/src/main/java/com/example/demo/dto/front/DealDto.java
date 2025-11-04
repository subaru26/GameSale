package com.example.demo.dto.front;

import lombok.Data;

@Data
public class DealDto {
    private String title;
    private String shop;
    private double priceNew;
    private double priceOld;
    private int cut;
    private String url;
    private String image;

    // 過去最安値関連
    private double storeLow;      // 現在ストアでの最安値
    private double historyLow;    // 過去最安値
    private double historyLow1y;  // 過去1年間の最安値
    private double historyLow3m;  // 過去3か月の最安値
}
