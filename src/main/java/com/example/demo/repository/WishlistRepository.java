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

import com.example.demo.entity.Wishlist;

@Repository
public class WishlistRepository {

    @Value("${SUPABASE_URL}")
    private String supabaseUrl;

    @Value("${SUPABASE_KEY}")
    private String supabaseKey;

    private final HttpClient client = HttpClient.newHttpClient();

    // ユーザーのウィッシュリストを取得
    public List<Wishlist> findByUserId(Long userId) {
        try {
            URI uri = new URI(supabaseUrl + "/rest/v1/wishlist?user_id=eq." + userId + "&select=*&order=added_at.desc");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray array = new JSONArray(response.body());

            List<Wishlist> wishlistItems = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Wishlist item = new Wishlist();
                item.setId(obj.getLong("id"));
                item.setUserId(obj.getLong("user_id"));
                item.setGameId(obj.getString("game_id"));
                item.setGameTitle(obj.getString("game_title"));
                item.setGameImage(obj.optString("game_image", null));
                item.setCurrentPrice(obj.optDouble("current_price", 0.0));
                item.setShop(obj.optString("shop", null));
                item.setUrl(obj.optString("url", null));  // URL追加;
                
                String addedAtStr = obj.optString("added_at", null);
                if (addedAtStr != null && !addedAtStr.isEmpty()) {
                    // UTC時刻をパースしてJSTに変換
                    ZonedDateTime utcTime = ZonedDateTime.parse(addedAtStr, 
                        DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")));
                    ZonedDateTime jstTime = utcTime.withZoneSameInstant(ZoneId.of("Asia/Tokyo"));
                    item.setAddedAt(jstTime.toLocalDateTime());
                }
                
                wishlistItems.add(item);
            }

            return wishlistItems;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // ウィッシュリストに追加
    public boolean add(Wishlist wishlist) {
        try {
            URI uri = new URI(supabaseUrl + "/rest/v1/wishlist");
            JSONObject obj = new JSONObject();
            obj.put("user_id", wishlist.getUserId());
            obj.put("game_id", wishlist.getGameId());
            obj.put("game_title", wishlist.getGameTitle());
            obj.put("game_image", wishlist.getGameImage());
            obj.put("current_price", wishlist.getCurrentPrice());
            obj.put("shop", wishlist.getShop());
            obj.put("url", wishlist.getUrl());  // URL追加

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

    // ウィッシュリストから削除
    public boolean delete(Long userId, String gameId) {
        try {
            URI uri = new URI(supabaseUrl + "/rest/v1/wishlist?user_id=eq." + userId + "&game_id=eq." + gameId);
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

    // 既にウィッシュリストに存在するか確認
    public boolean exists(Long userId, String gameId) {
        try {
            URI uri = new URI(supabaseUrl + "/rest/v1/wishlist?user_id=eq." + userId + "&game_id=eq." + gameId + "&select=id");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray array = new JSONArray(response.body());
            return array.length() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}