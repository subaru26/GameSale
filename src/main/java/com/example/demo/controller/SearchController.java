package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.front.DealDto;
import com.example.demo.service.SearchService;

@RestController
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/api/search")
    public List<DealDto> searchGames(@RequestParam String q) {
        return searchService.searchGames(q);
    }
}
