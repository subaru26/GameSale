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
}
