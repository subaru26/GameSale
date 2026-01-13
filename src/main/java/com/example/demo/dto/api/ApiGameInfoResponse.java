package com.example.demo.dto.api;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiGameInfoResponse {
    private String id;
    private String slug;
    private String title;
    private String type;
    private boolean mature;
    private Assets assets;
    private boolean earlyAccess;
    private boolean achievements;
    private boolean tradingCards;
    private String appid;
    private List<String> tags;
    private String releaseDate;
    private List<Publisher> publishers;
    private Stats stats;
    private Map<String, String> urls;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Assets {
        private String banner145;
        private String banner300;
        private String banner400;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Publisher {
        private int id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Stats {
        private int rank;
        private int waitlisted;
        private int collected;
    }
}
