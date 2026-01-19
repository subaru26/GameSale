package com.example.demo.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchHistory {
    private Long id;
    private Long userId;
    private String query;
    private LocalDateTime searchedAt;
}
