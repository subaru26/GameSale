package com.example.demo.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.api.ApiDealResponse;
import com.example.demo.dto.api.ApiPriceResponse;
import com.example.demo.dto.front.DealDto;

@Service
public class DealService {

    @Value("${ITAD_API_KEY}")
    private String apiKey;

    @Value("${ITAD_DEALS_URL}")
    private String baseUrl;

    @Value("${ITAD_PRICE_URL}")
    private String priceUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final Map<String, String> SHOP_IDS = Map.of(
        "steam", "61",
        "epic", "16",
        "ea", "52",
        "microsoft", "48",
        "fanatical", "6"
    );

    public List<DealDto> fetchDeals(List<String> selectedStores, int offset, int limit, String sort) {

        String shopsParam = selectedStores.stream()
                .map(String::toLowerCase)
                .map(SHOP_IDS::get)
                .filter(Objects::nonNull)
                .reduce((a, b) -> a + "," + b)
                .orElse("61"); // デフォルトSteam

        String url = baseUrl + "?key=" + apiKey
                + "&shops=" + shopsParam
                + "&country=jp"
                + "&offset=" + offset
                + "&limit=" + limit;

        // 🔧 通常価格順（regular）の場合は sort を付けない
        if (sort != null 
                && !sort.isEmpty() 
                && !sort.equals("default")
                && !sort.equals("regular")) {
            url += "&sort=" + sort;
        }

        System.out.println("[API] Fetching URL: " + url);

        RestTemplate restTemplate = new RestTemplate();
        ApiDealResponse apiResponse = restTemplate.getForObject(url, ApiDealResponse.class);

        List<DealDto> deals = new ArrayList<>();

        if (apiResponse != null && apiResponse.getList() != null) {
            for (ApiDealResponse.ListItem item : apiResponse.getList()) {
                DealDto dto = new DealDto();

                dto.setGameID(item.getId());
                dto.setTitle(item.getTitle());

                if (item.getDeal() != null) {
                    dto.setShop(item.getDeal().getShop() != null ? item.getDeal().getShop().getName() : "不明");
                    dto.setPriceNew(item.getDeal().getPrice() != null ? item.getDeal().getPrice().getAmount() : 0);
                    dto.setPriceOld(item.getDeal().getRegular() != null ? item.getDeal().getRegular().getAmount() : 0);
                    dto.setCut(item.getDeal().getCut());
                    dto.setUrl(item.getDeal().getUrl());

                    dto.setStoreLow(item.getDeal().getStoreLow() != null ? item.getDeal().getStoreLow().getAmount() : null);
                    dto.setHistoryLow(item.getDeal().getHistoryLow() != null ? item.getDeal().getHistoryLow().getAmount() : null);
                    dto.setHistoryLow1y(item.getDeal().getHistoryLow_1y() != null ? item.getDeal().getHistoryLow_1y().getAmount() : null);
                    dto.setHistoryLow3m(item.getDeal().getHistoryLow_3m() != null ? item.getDeal().getHistoryLow_3m().getAmount() : null);
                    dto.setExpiry(item.getDeal().getExpiry());
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
    public DealDto fetchDeal(String gameId, String shopName) {
        // ショップIDの特定
        String shopIdStr = "61"; // デフォルトSteam
        if (shopName != null) {
            shopIdStr = SHOP_IDS.getOrDefault(shopName.toLowerCase(), "61");
        }
        int targetShopId = Integer.parseInt(shopIdStr);
        
        // v3 API (POST) を使用
        try {
            String url = priceUrl + "?key=" + apiKey + "&country=jp";
            // JSON配列を作成: ["gameId"]
            String jsonBody = objectMapper.writeValueAsString(List.of(gameId));

            System.out.println("[API] Fetching Price URL: " + url);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // レスポンスは配列
                List<ApiPriceResponse> prices = objectMapper.readValue(
                    response.body(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ApiPriceResponse.class)
                );

                if (prices != null && !prices.isEmpty()) {
                    ApiPriceResponse item = prices.get(0);
                    
                    // 指定ショップのDealを探す
                    if (item.getDeals() != null) {
                        for (ApiPriceResponse.Deal deal : item.getDeals()) {
                            if (deal.getShop() != null && deal.getShop().getId() != null 
                                    && deal.getShop().getId() == targetShopId) {
                                
                                DealDto dto = new DealDto();
                                dto.setGameID(item.getId());
                                // TitleはPrice APIには含まれないため設定しない（Wishlist更新用なら不要）
                                dto.setTitle("Unknown"); 

                                dto.setShop(deal.getShop().getName());
                                dto.setPriceNew(deal.getPrice() != null && deal.getPrice().getAmount() != null ? deal.getPrice().getAmount() : 0.0);
                                dto.setPriceOld(deal.getRegular() != null && deal.getRegular().getAmount() != null ? deal.getRegular().getAmount() : 0.0);
                                dto.setCut(deal.getCut() != null ? deal.getCut() : 0);
                                dto.setUrl(deal.getUrl());

                                if (deal.getStoreLow() != null) {
                                    dto.setStoreLow(deal.getStoreLow().getAmount());
                                }
                                
                                // 履歴情報のセット
                                if (item.getHistoryLow() != null) {
                                    if (item.getHistoryLow().getAll() != null) {
                                        dto.setHistoryLow(item.getHistoryLow().getAll().getAmount());
                                    }
                                    if (item.getHistoryLow().getY1() != null) {
                                        dto.setHistoryLow1y(item.getHistoryLow().getY1().getAmount());
                                    }
                                    if (item.getHistoryLow().getM3() != null) {
                                        dto.setHistoryLow3m(item.getHistoryLow().getM3().getAmount());
                                    }
                                }
                                dto.setExpiry(deal.getExpiry());

                                return dto;
                            }
                        }
                    }
                }
            } else {
                System.out.println("[API] Price fetch failed: " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    public DealDto getDealWithOthers(String gameId) {
        try {
            String url = priceUrl + "?key=" + apiKey + "&country=jp";
            // JSON配列を作成: ["gameId"]
            String jsonBody = objectMapper.writeValueAsString(List.of(gameId));

            System.out.println("[API] Fetching Price URL (Lookup): " + url);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<ApiPriceResponse> prices = objectMapper.readValue(
                    response.body(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ApiPriceResponse.class)
                );

                if (prices != null && !prices.isEmpty()) {
                    ApiPriceResponse item = prices.get(0);
                    
                    if (item.getDeals() == null || item.getDeals().isEmpty()) {
                        return null;
                    }
                    
                    // 最安値を探す
                    ApiPriceResponse.Deal cheapestDeal = null;
                    Double cheapestPrice = Double.MAX_VALUE;
                    
                    for (ApiPriceResponse.Deal deal : item.getDeals()) {
                        if (deal.getPrice() != null && deal.getPrice().getAmount() != null) {
                            double price = deal.getPrice().getAmount();
                            if (price < cheapestPrice) {
                                cheapestPrice = price;
                                cheapestDeal = deal;
                            }
                        }
                    }
                    
                    if (cheapestDeal == null) return null;
                    
                    // Main DTO作成
                    DealDto dto = new DealDto();
                    dto.setGameID(item.getId());
                    dto.setTitle("Unknown"); // Controller/Frontend側で補完、またはGameInfoAPIで取得する必要があるが、Wishlist画面ならTitleはDOMから取れる

                    dto.setShop(cheapestDeal.getShop() != null ? cheapestDeal.getShop().getName() : "不明");
                    dto.setPriceNew(cheapestDeal.getPrice() != null ? cheapestDeal.getPrice().getAmount() : 0.0);
                    dto.setPriceOld(cheapestDeal.getRegular() != null ? cheapestDeal.getRegular().getAmount() : 0.0);
                    dto.setCut(cheapestDeal.getCut() != null ? cheapestDeal.getCut() : 0);
                    dto.setUrl(cheapestDeal.getUrl());
                    dto.setExpiry(cheapestDeal.getExpiry());

                    if (cheapestDeal.getStoreLow() != null) {
                        dto.setStoreLow(cheapestDeal.getStoreLow().getAmount());
                    }
                    
                    // 履歴情報のセット
                    if (item.getHistoryLow() != null) {
                        if (item.getHistoryLow().getAll() != null) {
                            dto.setHistoryLow(item.getHistoryLow().getAll().getAmount());
                        }
                        if (item.getHistoryLow().getY1() != null) {
                            dto.setHistoryLow1y(item.getHistoryLow().getY1().getAmount());
                        }
                        if (item.getHistoryLow().getM3() != null) {
                            dto.setHistoryLow3m(item.getHistoryLow().getM3().getAmount());
                        }
                    }

                    // 他のストアの情報を収集
                    List<DealDto.OtherDealInfo> otherDeals = new ArrayList<>();
                    for (ApiPriceResponse.Deal deal : item.getDeals()) {
                        if (deal == cheapestDeal) continue;
                        
                        DealDto.OtherDealInfo otherDeal = new DealDto.OtherDealInfo();
                        if (deal.getShop() != null) {
                            otherDeal.setShop(deal.getShop().getName());
                        }
                        if (deal.getPrice() != null) {
                            otherDeal.setPriceNew(deal.getPrice().getAmount());
                        }
                        if (deal.getRegular() != null) {
                            otherDeal.setPriceOld(deal.getRegular().getAmount());
                        }
                        otherDeal.setCut(deal.getCut());
                        otherDeal.setUrl(deal.getUrl());
                        otherDeal.setExpiry(deal.getExpiry());
                        
                        otherDeals.add(otherDeal);
                    }
                    dto.setOtherDeals(otherDeals);
                    
                    return dto;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
