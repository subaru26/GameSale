package com.example.demo.dto.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiPriceResponse {
    private String id;
    private HistoryLow historyLow;
    private List<Deal> deals;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HistoryLow {
        private PriceInfo all;
        private PriceInfo y1;
        private PriceInfo m3;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceInfo {
        private Double amount;
        private Integer amountInt;
        private String currency;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Deal {
        private Shop shop;
        private PriceInfo price;
        private PriceInfo regular;
        private Integer cut;
        private String voucher;
        private PriceInfo storeLow;
        private String flag;
        @com.fasterxml.jackson.annotation.JsonIgnore
        private List<Object> drm; // DRM情報はオブジェクト配列だが、価格表示には不要のため無視
        private List<Platform> platforms;
        private String timestamp;
        private String expiry;
        private String url;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Shop {
        private Integer id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Platform {
        private Integer id;
        private String name;
    }
}
