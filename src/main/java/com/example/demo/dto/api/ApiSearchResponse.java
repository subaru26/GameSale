package com.example.demo.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiSearchResponse {
    private String id;
    private String slug;
    private String title;
    private String type;
    private Boolean mature;
    private Assets assets;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Assets {
        // assetsオブジェクトは空の場合もあるため、フィールドは定義しない
    }
}
