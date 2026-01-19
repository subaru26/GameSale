package com.example.demo.repository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.SearchHistory;

@Repository
public class SearchHistoryRepository {

    @Value("${SUPABASE_URL}")
    private String supabaseUrl;

    @Value("${SUPABASE_KEY}")
    private String supabaseKey;

    private final HttpClient client = HttpClient.newHttpClient();

    // ユーザーの検索履歴を取得（最新順、最大20件）
    public List<SearchHistory> findByUserId(Long userId) {
        try {
            URI uri = new URI(supabaseUrl + "/rest/v1/search_history?user_id=eq." + userId 
                    + "&select=*&order=searched_at.desc&limit=20");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray array = new JSONArray(response.body());

            List<SearchHistory> historyList = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                SearchHistory history = new SearchHistory();
                history.setId(obj.getLong("id"));
                history.setUserId(obj.getLong("user_id"));
                history.setQuery(obj.getString("query"));
                
                String searchedAtStr = obj.optString("searched_at", null);
                if (searchedAtStr != null && !searchedAtStr.isEmpty()) {
                    // UTC時刻をパースしてJSTに変換
                    ZonedDateTime utcTime = ZonedDateTime.parse(searchedAtStr, 
                        DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")));
                    ZonedDateTime jstTime = utcTime.withZoneSameInstant(ZoneId.of("Asia/Tokyo"));
                    history.setSearchedAt(jstTime.toLocalDateTime());
                }
                
                historyList.add(history);
            }

            return historyList;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // 検索履歴に追加
    public boolean add(SearchHistory searchHistory) {
        try {
            // 既に同じクエリが最近検索されている場合は、既存のレコードを更新（重複を避ける）
            // まず、同じクエリの最新のレコードを確認
            List<SearchHistory> existing = findByUserIdAndQuery(searchHistory.getUserId(), searchHistory.getQuery());
            if (!existing.isEmpty()) {
                // 既存のレコードの検索日時を更新
                SearchHistory existingHistory = existing.get(0);
                return updateSearchedAt(existingHistory.getId());
            }
            
            // 新規追加
            URI uri = new URI(supabaseUrl + "/rest/v1/search_history");
            JSONObject obj = new JSONObject();
            obj.put("user_id", searchHistory.getUserId());
            obj.put("query", searchHistory.getQuery());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .POST(HttpRequest.BodyPublishers.ofString(obj.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 201;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 検索履歴から削除
    public boolean delete(Long userId, String query) {
        try {
            URI uri = new URI(supabaseUrl + "/rest/v1/search_history?user_id=eq." + userId + "&query=eq." 
                    + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 204;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ユーザーIDとクエリで検索履歴を取得
    private List<SearchHistory> findByUserIdAndQuery(Long userId, String query) {
        try {
            URI uri = new URI(supabaseUrl + "/rest/v1/search_history?user_id=eq." + userId 
                    + "&query=eq." + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)
                    + "&select=*&order=searched_at.desc&limit=1");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray array = new JSONArray(response.body());

            List<SearchHistory> historyList = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                SearchHistory history = new SearchHistory();
                history.setId(obj.getLong("id"));
                history.setUserId(obj.getLong("user_id"));
                history.setQuery(obj.getString("query"));
                
                String searchedAtStr = obj.optString("searched_at", null);
                if (searchedAtStr != null && !searchedAtStr.isEmpty()) {
                    ZonedDateTime utcTime = ZonedDateTime.parse(searchedAtStr, 
                        DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")));
                    ZonedDateTime jstTime = utcTime.withZoneSameInstant(ZoneId.of("Asia/Tokyo"));
                    history.setSearchedAt(jstTime.toLocalDateTime());
                }
                
                historyList.add(history);
            }

            return historyList;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // 検索日時を更新
    private boolean updateSearchedAt(Long id) {
        try {
            URI uri = new URI(supabaseUrl + "/rest/v1/search_history?id=eq." + id);
            // searched_atは自動更新される想定（Supabaseの設定次第）
            // もし自動更新されない場合は、ここで明示的に更新する
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 204;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
