package com.example.demo.dto.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiDealResponse {
    private int nextOffset;
    private boolean hasMore;
    private List<ListItem> list; // ← 「list」キーに合わせる

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListItem {
        private String id;
        private String slug;
        private String title;
        private Deal deal;
        private Assets assets;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Deal {
        private Shop shop;
        private Price price;
        private Price regular;
        private int cut;
        private String url;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Shop {
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Price {
        private double amount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Assets {
        private String banner145;
        private String banner300;
        private String banner400;
        private String banner600;
    }
}