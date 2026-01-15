package com.example.demo.dto.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeepLResponse {
    private List<Translation> translations;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Translation {
        private String detected_source_language;
        private String text;
    }
}
