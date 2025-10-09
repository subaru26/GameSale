package com.example.demo.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.dto.front.DealDto;
import com.example.demo.service.DealService;

@Controller
public class DealController {

    @Autowired
    private DealService dealService;

    // ========================================
    // HTML表示用
    // /deals にアクセスしたときに templates/deals.html を返す
    // ========================================
    @GetMapping("/deals")
    public String dealsPage(Model model) {
        return "deals";
    }//けしちゃだめ

    // ========================================
    // JSON API（JavaScript用）
    // ストア選択、ページ番号（offset/limit）を受け取る
    // ========================================
    @GetMapping("/api/deals")
    @ResponseBody
    public List<DealDto> getDeals(
            @RequestParam(defaultValue = "steam") String stores,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "48") int limit) {

        List<String> selectedStores = Arrays.asList(stores.split(","));
        return dealService.fetchDeals(selectedStores, offset, limit);
    }
}
