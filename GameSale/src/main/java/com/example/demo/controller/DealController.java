package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.dto.front.DealDto;
import com.example.demo.service.DealService;

@Controller
public class DealController {

    @Autowired
    private DealService dealService;

    // HTML表示
    @GetMapping("/deals")
    public String dealsPage(Model model) {
        return "deals"; // templates/deals.html
    }

    // JSON API（JavaScriptが呼び出す用）
    @GetMapping("/api/deals")
    @ResponseBody
    public List<DealDto> getDeals() {
        return dealService.fetchDeals();
    }
}
