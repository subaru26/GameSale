package com.example.demo.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.api.ApiPriceResponse;
import com.example.demo.dto.api.ApiSearchResponse;
import com.example.demo.dto.api.DeepLResponse;
import com.example.demo.dto.front.DealDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class SearchService {

    private static final String ITAD_API_KEY = "1c17fd70c2b436ce327b048ce319119b8bdcd4e2";
    private static final String DEEPL_API_KEY = "d69817ca-9139-4eb9-a332-695e3e468cbd:fx";
    private static final String ITAD_SEARCH_URL = "https://api.isthereanydeal.com/games/search/v1";
    private static final String ITAD_PRICE_URL = "https://api.isthereanydeal.com/games/prices/v3";
    private static final String DEEPL_API_URL = "https://api-free.deepl.com/v2/translate";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // 日本語文字を判定するパターン（ひらがな、カタカナ、漢字）
    private static final Pattern JAPANESE_PATTERN = Pattern.compile(
        "[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF]"
    );

    /**
     * 検索クエリが日本語かどうかを判定
     */
    private boolean isJapanese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return JAPANESE_PATTERN.matcher(text).find();
    }

    /**
     * DeepL APIを使用して日本語を英語に翻訳
     */
    private String translateToEnglish(String japaneseText) {
        try {
            String url = DEEPL_API_URL + "?auth_key=" + DEEPL_API_KEY
                    + "&text=" + java.net.URLEncoder.encode(japaneseText, StandardCharsets.UTF_8)
                    + "&source_lang=JA&target_lang=EN";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                DeepLResponse deepLResponse = objectMapper.readValue(response.body(), DeepLResponse.class);
                if (deepLResponse.getTranslations() != null && !deepLResponse.getTranslations().isEmpty()) {
                    return deepLResponse.getTranslations().get(0).getText();
                }
            }
            
            System.out.println("[SearchService] DeepL translation failed: " + response.statusCode());
            return japaneseText; // 翻訳失敗時は元のテキストを返す
        } catch (Exception e) {
            System.err.println("[SearchService] Error translating: " + e.getMessage());
            return japaneseText;
        }
    }


    /**
     * ITAD APIでゲームIDから価格情報を取得
     */
    private List<ApiPriceResponse> fetchPrices(List<String> gameIds) {
        List<ApiPriceResponse> priceResponses = new ArrayList<>();
        
        if (gameIds.isEmpty()) {
            return priceResponses;
        }

        try {
            // JSON配列を作成
            String jsonBody = objectMapper.writeValueAsString(gameIds);
            
            String url = ITAD_PRICE_URL + "?key=" + ITAD_API_KEY + "&country=JP";

            System.out.println("[SearchService] Fetching prices: " + url);
            System.out.println("[SearchService] Request body: " + jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // レスポンスは配列なので、配列としてパース
                List<ApiPriceResponse> responses = objectMapper.readValue(
                    response.body(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ApiPriceResponse.class)
                );
                priceResponses.addAll(responses);
            } else {
                System.out.println("[SearchService] Price fetch failed: " + response.statusCode());
                System.out.println("[SearchService] Response: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("[SearchService] Error fetching prices: " + e.getMessage());
            e.printStackTrace();
        }
        
        return priceResponses;
    }

    /**
     * 検索を実行してDealDtoのリストを返す
     */
    public List<DealDto> searchGames(String query) {
        List<DealDto> results = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String searchQuery = query.trim();
        
        // 日本語の場合は翻訳
        if (isJapanese(searchQuery)) {
            System.out.println("[SearchService] Japanese detected, translating...");
            searchQuery = translateToEnglish(searchQuery);
            System.out.println("[SearchService] Translated to: " + searchQuery);
        }

        // ゲームIDを検索（タイトル情報も含む）
        List<ApiSearchResponse> searchResults = searchGameResults(searchQuery);
        
        if (searchResults.isEmpty()) {
            System.out.println("[SearchService] No games found for: " + searchQuery);
            return results;
        }

        System.out.println("[SearchService] Found " + searchResults.size() + " game(s)");

        // ゲームIDのリストを作成
        List<String> gameIds = new ArrayList<>();
        for (ApiSearchResponse result : searchResults) {
            if (result.getId() != null) {
                gameIds.add(result.getId());
            }
        }

        // 価格情報を取得
        List<ApiPriceResponse> priceResponses = fetchPrices(gameIds);
        
        // IDとタイトル、タイプのマップを作成
        java.util.Map<String, String> idToTitleMap = new java.util.HashMap<>();
        java.util.Map<String, String> idToTypeMap = new java.util.HashMap<>();
        for (ApiSearchResponse result : searchResults) {
            if (result.getId() != null && result.getTitle() != null) {
                idToTitleMap.put(result.getId(), result.getTitle());
                if (result.getType() != null) {
                    idToTypeMap.put(result.getId(), result.getType());
                }
            }
        }
        
        // DealDtoに変換（各ゲームごとに最安値を計算）
        for (ApiPriceResponse priceResponse : priceResponses) {
            String gameTitle = idToTitleMap.getOrDefault(priceResponse.getId(), "Unknown Game");
            
            if (priceResponse.getDeals() == null || priceResponse.getDeals().isEmpty()) {
                continue;
            }

            // 最安値のストアを探す
            ApiPriceResponse.Deal cheapestDeal = null;
            Double cheapestPrice = Double.MAX_VALUE;
            
            for (ApiPriceResponse.Deal deal : priceResponse.getDeals()) {
                if (deal.getPrice() != null && deal.getPrice().getAmount() != null) {
                    double price = deal.getPrice().getAmount();
                    if (price < cheapestPrice) {
                        cheapestPrice = price;
                        cheapestDeal = deal;
                    }
                }
            }
            
            // 最安値が見つからない場合はスキップ
            if (cheapestDeal == null) {
                continue;
            }
            
            // 最安値のストア情報をメイン表示に設定
            DealDto dto = new DealDto();
            dto.setGameID(priceResponse.getId());
            dto.setTitle(gameTitle);
            
            if (cheapestDeal.getShop() != null) {
                dto.setShop(cheapestDeal.getShop().getName());
            }
            
            if (cheapestDeal.getPrice() != null) {
                dto.setPriceNew(cheapestDeal.getPrice().getAmount());
            }
            
            if (cheapestDeal.getRegular() != null) {
                dto.setPriceOld(cheapestDeal.getRegular().getAmount());
            }
            
            dto.setCut(cheapestDeal.getCut());
            dto.setUrl(cheapestDeal.getUrl());
            dto.setExpiry(cheapestDeal.getExpiry());
            
            if (cheapestDeal.getStoreLow() != null) {
                dto.setStoreLow(cheapestDeal.getStoreLow().getAmount());
            }
            
            if (priceResponse.getHistoryLow() != null && priceResponse.getHistoryLow().getAll() != null) {
                dto.setHistoryLow(priceResponse.getHistoryLow().getAll().getAmount());
            }
            
            if (priceResponse.getHistoryLow() != null && priceResponse.getHistoryLow().getY1() != null) {
                dto.setHistoryLow1y(priceResponse.getHistoryLow().getY1().getAmount());
            }
            
            if (priceResponse.getHistoryLow() != null && priceResponse.getHistoryLow().getM3() != null) {
                dto.setHistoryLow3m(priceResponse.getHistoryLow().getM3().getAmount());
            }
            
            // 他のストアの情報を収集
            List<DealDto.OtherDealInfo> otherDeals = new ArrayList<>();
            for (ApiPriceResponse.Deal deal : priceResponse.getDeals()) {
                // 最安値のストアは除外
                if (deal == cheapestDeal) {
                    continue;
                }
                
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
            
            // 画像情報を取得（GameInfoServiceを使用）
            try {
                String imageUrl = fetchGameImage(priceResponse.getId());
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    dto.setImage(imageUrl);
                }
            } catch (Exception e) {
                System.err.println("[SearchService] Error fetching image for " + priceResponse.getId() + ": " + e.getMessage());
            }
            
            results.add(dto);
        }
        
        // 検索文字列との類似度でソート（DLCなどは下に配置）
        final String finalSearchQuery = searchQuery.toLowerCase();
        results.sort(Comparator
            // 1. まずtypeが"game"かどうかで分ける（"game"を優先、DLCなどは後ろに）
            .comparing((DealDto dto) -> {
                String type = idToTypeMap.getOrDefault(dto.getGameID(), "");
                return "game".equals(type) ? 0 : 1;
            })
            // 2. 検索文字列との類似度でソート
            .thenComparing((DealDto dto) -> {
                String title = dto.getTitle() != null ? dto.getTitle().toLowerCase() : "";
                // タイトルが検索文字列で始まるもの（最優先）
                if (title.startsWith(finalSearchQuery)) {
                    return 0;
                }
                // タイトルに検索文字列が完全に含まれるもの
                if (title.contains(finalSearchQuery)) {
                    return 1;
                }
                // レーベンシュタイン距離でソート
                return 2 + calculateLevenshteinDistance(title, finalSearchQuery);
            })
            // 3. タイトルでアルファベット順
            .thenComparing(DealDto::getTitle, Comparator.nullsLast(Comparator.naturalOrder()))
        );
        
        return results;
    }

    /**
     * ゲームIDから画像URLを取得
     */
    private String fetchGameImage(String gameId) {
        try {
            String url = "https://api.isthereanydeal.com/games/info/v2?key=" + ITAD_API_KEY + "&id=" + gameId;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                JsonNode assets = jsonNode.get("assets");
                if (assets != null) {
                    JsonNode banner300 = assets.get("banner300");
                    if (banner300 != null && !banner300.isNull()) {
                        return banner300.asText();
                    }
                    JsonNode banner145 = assets.get("banner145");
                    if (banner145 != null && !banner145.isNull()) {
                        return banner145.asText();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SearchService] Error fetching game image: " + e.getMessage());
        }
        return null;
    }

    /**
     * ITAD APIでゲーム名から検索結果（IDとタイトル）を取得
     */
    private List<ApiSearchResponse> searchGameResults(String gameTitle) {
        List<ApiSearchResponse> results = new ArrayList<>();
        
        try {
            String url = ITAD_SEARCH_URL + "?key=" + ITAD_API_KEY
                    + "&title=" + java.net.URLEncoder.encode(gameTitle, StandardCharsets.UTF_8);

            System.out.println("[SearchService] Searching games: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // レスポンスは配列形式で直接返される
                List<ApiSearchResponse> searchResults = objectMapper.readValue(
                    response.body(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ApiSearchResponse.class)
                );
                if (searchResults != null) {
                    results.addAll(searchResults);
                }
            } else {
                System.out.println("[SearchService] Search failed: " + response.statusCode());
                System.out.println("[SearchService] Response: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("[SearchService] Error searching games: " + e.getMessage());
            e.printStackTrace();
        }
        
        return results;
    }

    /**
     * レーベンシュタイン距離を計算（文字列の類似度）
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return Integer.MAX_VALUE;
        }
        
        int len1 = s1.length();
        int len2 = s2.length();
        
        if (len1 == 0) return len2;
        if (len2 == 0) return len1;
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[len1][len2];
    }
}
