package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.api.ApiDealResponse;
import com.example.demo.dto.front.DealDto;

@Service
public class DealService {

    private static final String API_KEY = "1c17fd70c2b436ce327b048ce319119b8bdcd4e2";
    private static final String BASE_URL = "https://api.isthereanydeal.com/deals/v2";

    private static final Map<String, String> SHOP_IDS = Map.of(
        "steam", "61",
        "epic", "16",
        "ea", "52",
        "microsoft", "48",
        "fanatical", "6"
    );

    public List<DealDto> fetchDeals(List<String> selectedStores, int offset, int limit) {
        String shopsParam = selectedStores.stream()
                .map(String::toLowerCase)
                .map(SHOP_IDS::get)
                .filter(Objects::nonNull)
                .reduce((a, b) -> a + "," + b)
                .orElse("61"); // デフォルトSteam

        String url = BASE_URL + "?key=" + API_KEY
                + "&shops=" + shopsParam
                + "&country=jp"
                + "&offset=" + offset
                + "&limit=" + limit;

        RestTemplate restTemplate = new RestTemplate();
        ApiDealResponse apiResponse = restTemplate.getForObject(url, ApiDealResponse.class);

        List<DealDto> deals = new ArrayList<>();

        if (apiResponse != null && apiResponse.getList() != null) {
            for (ApiDealResponse.ListItem item : apiResponse.getList()) {
                DealDto dto = new DealDto();
                dto.setTitle(item.getTitle());

                if (item.getDeal() != null) {
                    dto.setShop(item.getDeal().getShop() != null ? item.getDeal().getShop().getName() : "不明");
                    dto.setPriceNew(item.getDeal().getPrice() != null ? item.getDeal().getPrice().getAmount() : 0);
                    dto.setPriceOld(item.getDeal().getRegular() != null ? item.getDeal().getRegular().getAmount() : 0);
                    dto.setCut(item.getDeal().getCut());
                    dto.setUrl(item.getDeal().getUrl());
                }

                if (item.getAssets() != null) {
                    dto.setImage(item.getAssets().getBanner300() != null
                            ? item.getAssets().getBanner300()
                            : item.getAssets().getBanner145());
                }

                deals.add(dto);
            }
        }

        return deals;
    }
}
