package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.api.ApiDealResponse;
import com.example.demo.dto.front.DealDto;

@Service
public class DealService {

    private static final String API_KEY = "1c17fd70c2b436ce327b048ce319119b8bdcd4e2";
    private static final String BASE_URL = "https://api.isthereanydeal.com/deals/v2?key=" + API_KEY + "&shops=61&country=jp&limit=40";

    public List<DealDto> fetchDeals() {
        RestTemplate restTemplate = new RestTemplate();
        ApiDealResponse apiResponse = restTemplate.getForObject(BASE_URL, ApiDealResponse.class);

        System.out.println("===== API raw response =====");
        System.out.println(apiResponse);

        List<DealDto> deals = new ArrayList<>();

        if (apiResponse != null && apiResponse.getList() != null) {
            System.out.println("データ件数: " + apiResponse.getList().size());

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
                    dto.setImage(item.getAssets().getBanner300() != null ? item.getAssets().getBanner300()
                            : item.getAssets().getBanner145());
                }

                deals.add(dto);
            }
        } else {
            System.out.println("⚠️ APIレスポンスが空または形式が異なります");
        }

        return deals;
    }
}
