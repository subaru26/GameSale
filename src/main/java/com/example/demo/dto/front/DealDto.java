package com.example.demo.dto.front;

import lombok.Data;

@Data
public class DealDto {
	private String gameID;
    private String title;
    private String shop;
    private Double priceNew;
    private Double priceOld;
    private Integer cut;
    private String url;
    private String image;

    // 過去最安値関連
    private Double storeLow;      // 現在ストアでの最安値
    private Double historyLow;    // 過去最安値
    private Double historyLow1y;  // 過去1年間の最安値
    private Double historyLow3m;  // 過去3か月の最安値
}
