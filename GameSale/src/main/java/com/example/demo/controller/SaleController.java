package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/deals")
public class SaleController {

    private final RestTemplate restTemplate = new RestTemplate();

    private final String API_KEY = "1c17fd70c2b436ce327b048ce319119b8bdcd4e2";

    @GetMapping("/steam")
    public String getSteamDeals() {
        String url = "https://api.isthereanydeal.com/deals/v2?key=" + API_KEY + "&shops=61&country=jp";

        return restTemplate.getForObject(url, String.class);
    }
}
