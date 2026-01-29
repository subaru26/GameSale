package com.example.demo.repository;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Wishlist;

@Repository
public class WishlistRepository {

    private static final Logger logger = LoggerFactory.getLogger(WishlistRepository.class);

    @Value("${SUPABASE_URL}")
    private String supabaseUrl;

    @Value("${SUPABASE_KEY}")
    private String supabaseKey;

    private static final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    // ユーザーのウィッシュリストを取得
    public List<Wishlist> findByUserId(Long userId) {
        try {
            String encodedUserId = URLEncoder.encode(String.valueOf(userId), StandardCharsets.UTF_8);
            URI uri = new URI(supabaseUrl + "/rest/v1/wishlist?user_id=eq." + encodedUserId + "&select=*&order=added_at.desc");
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                logger.error("Failed to fetch wishlist: HTTP {}", response.statusCode());
                return new ArrayList<>();
            }
            
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
                item.setPriceOld(obj.optDouble("price_old", 0.0));
                item.setCut(obj.optInt("cut", 0));
                item.setShop(obj.optString("shop", null));
                item.setUrl(obj.optString("url", null));
                
                // 過去最安値情報
                item.setHistoryLow(obj.has("history_low") && !obj.isNull("history_low") 
                    ? obj.getDouble("history_low") : null);
                item.setHistoryLow1y(obj.has("history_low_1y") && !obj.isNull("history_low_1y") 
                    ? obj.getDouble("history_low_1y") : null);
                item.setHistoryLow3m(obj.has("history_low_3m") && !obj.isNull("history_low_3m") 
                    ? obj.getDouble("history_low_3m") : null);
                item.setStoreLow(obj.has("store_low") && !obj.isNull("store_low") 
                    ? obj.getDouble("store_low") : null);
                
                // セール終了日時
                item.setExpiry(obj.optString("expiry", null));

                // 通知送信済み（24h前）
                item.setNotify24hSentAt(obj.optString("notify_24h_sent_at", null));
                
                // 追加日時
                String addedAtStr = obj.optString("added_at", null);
                if (addedAtStr != null && !addedAtStr.isEmpty()) {
                    try {
                        ZonedDateTime utcTime = ZonedDateTime.parse(addedAtStr, 
                            DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")));
                        ZonedDateTime jstTime = utcTime.withZoneSameInstant(ZoneId.of("Asia/Tokyo"));
                        item.setAddedAt(jstTime.toLocalDateTime());
                    } catch (Exception e) {
                        logger.warn("Failed to parse added_at date: {}", addedAtStr, e);
                    }
                }
                
                wishlistItems.add(item);
            }

            logger.info("Successfully fetched {} wishlist items for user {}", wishlistItems.size(), userId);
            return wishlistItems;

        } catch (Exception e) {
            logger.error("Error finding wishlist by userId: {}", userId, e);
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
            obj.put("price_old", wishlist.getPriceOld());
            obj.put("cut", wishlist.getCut());
            obj.put("shop", wishlist.getShop());
            obj.put("url", wishlist.getUrl());
            
            // 過去最安値情報を追加
            if (wishlist.getHistoryLow() != null) {
                obj.put("history_low", wishlist.getHistoryLow());
            }
            if (wishlist.getHistoryLow1y() != null) {
                obj.put("history_low_1y", wishlist.getHistoryLow1y());
            }
            if (wishlist.getHistoryLow3m() != null) {
                obj.put("history_low_3m", wishlist.getHistoryLow3m());
            }
            if (wishlist.getStoreLow() != null) {
                obj.put("store_low", wishlist.getStoreLow());
            }
            
            // セール終了日時
            if (wishlist.getExpiry() != null) {
                obj.put("expiry", wishlist.getExpiry());
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .POST(HttpRequest.BodyPublishers.ofString(obj.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            boolean success = response.statusCode() == 201;
            if (success) {
                logger.info("Successfully added item to wishlist: {}", wishlist.getGameTitle());
            } else {
                logger.error("Failed to add to wishlist: HTTP {}", response.statusCode());
            }
            
            
            return success;

        } catch (Exception e) {
            logger.error("Error adding to wishlist: {}", wishlist.getGameTitle(), e);
            return false;
        }
    }

    // ウィッシュリストを更新
    public boolean update(Wishlist wishlist) {
        try {
            URI uri = new URI(supabaseUrl + "/rest/v1/wishlist?user_id=eq." + wishlist.getUserId() 
                + "&game_id=eq." + wishlist.getGameId() 
                + "&shop=eq." + URLEncoder.encode(wishlist.getShop() == null ? "" : wishlist.getShop(), StandardCharsets.UTF_8));
                
            JSONObject obj = new JSONObject();
            
            // 価格情報更新
            obj.put("current_price", wishlist.getCurrentPrice());
            obj.put("price_old", wishlist.getPriceOld());
            obj.put("cut", wishlist.getCut());
            
            // 過去最安値情報を更新
            if (wishlist.getHistoryLow() != null) obj.put("history_low", wishlist.getHistoryLow());
            if (wishlist.getHistoryLow1y() != null) obj.put("history_low_1y", wishlist.getHistoryLow1y());
            if (wishlist.getHistoryLow3m() != null) obj.put("history_low_3m", wishlist.getHistoryLow3m());
            if (wishlist.getStoreLow() != null) obj.put("store_low", wishlist.getStoreLow());
            
            // セール終了日時
            if (wishlist.getExpiry() != null) {
                obj.put("expiry", wishlist.getExpiry());
            } else {
                obj.put("expiry", JSONObject.NULL);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(obj.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            boolean success = response.statusCode() == 204; // No Content
            if (success) {
                logger.info("Successfully updated wishlist item: userId={}, gameId={}", wishlist.getUserId(), wishlist.getGameId());
            } else {
                logger.error("Failed to update wishlist: HTTP {}", response.statusCode());
            }
            
            return success;

        } catch (Exception e) {
            logger.error("Error updating wishlist: userId={}, gameId={}", wishlist.getUserId(), wishlist.getGameId(), e);
            return false;
        }
    }

    // ウィッシュリストから削除
    public boolean delete(Long userId, String gameId, String shop) {
        try {
            String encodedUserId = URLEncoder.encode(String.valueOf(userId), StandardCharsets.UTF_8);
            String encodedGameId = URLEncoder.encode(gameId, StandardCharsets.UTF_8);
            String encodedShop = URLEncoder.encode(shop, StandardCharsets.UTF_8);
            
            URI uri = new URI(supabaseUrl + "/rest/v1/wishlist?user_id=eq." + encodedUserId + "&game_id=eq." + encodedGameId + "&shop=eq." + encodedShop);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            boolean success = response.statusCode() == 204;
            if (success) {
                logger.info("Successfully deleted from wishlist: userId={}, gameId={}", userId, gameId);
            } else {
                logger.error("Failed to delete from wishlist: HTTP {}", response.statusCode());
            }
            
            return success;

        } catch (Exception e) {
            logger.error("Error deleting from wishlist: userId={}, gameId={}", userId, gameId, e);
            return false;
        }
    }

    // 既にウィッシュリストに存在するか確認
    public boolean exists(Long userId, String gameId, String shop) {
        try {
            String encodedUserId = URLEncoder.encode(String.valueOf(userId), StandardCharsets.UTF_8);
            String encodedGameId = URLEncoder.encode(gameId, StandardCharsets.UTF_8);
            String encodedShop = URLEncoder.encode(shop == null ? "" : shop, StandardCharsets.UTF_8);
            
            URI uri = new URI(supabaseUrl + "/rest/v1/wishlist?user_id=eq." + encodedUserId + "&game_id=eq." + encodedGameId + "&shop=eq." + encodedShop + "&select=id");
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                logger.error("Failed to check wishlist existence: HTTP {}", response.statusCode());
                return false;
            }
            
            JSONArray array = new JSONArray(response.body());
            return array.length() > 0;

        } catch (Exception e) {
            logger.error("Error checking wishlist existence: userId={}, gameId={}, shop={}", userId, gameId, shop, e);
            return false;
        }
    }

    // 互換: gameIdが存在するか（shop問わず）
    public boolean existsAnyShop(Long userId, String gameId) {
        try {
            String encodedUserId = URLEncoder.encode(String.valueOf(userId), StandardCharsets.UTF_8);
            String encodedGameId = URLEncoder.encode(gameId, StandardCharsets.UTF_8);

            URI uri = new URI(supabaseUrl + "/rest/v1/wishlist?user_id=eq." + encodedUserId + "&game_id=eq." + encodedGameId + "&select=id");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return false;
            return new JSONArray(response.body()).length() > 0;
        } catch (Exception e) {
            logger.error("Error checking wishlist existence (any shop): userId={}, gameId={}", userId, gameId, e);
            return false;
        }
    }

    // 24時間前通知対象を取得（notify_24h_sent_atがnullのもの）
    public List<Wishlist> findExpiringBetween(Instant fromInclusive, Instant toExclusive) {
        try {
            String from = URLEncoder.encode(fromInclusive.toString(), StandardCharsets.UTF_8);
            String to = URLEncoder.encode(toExclusive.toString(), StandardCharsets.UTF_8);

            // SupabaseはISO文字列比較でフィルタできる前提（timestamptz推奨）
            URI uri = new URI(
                supabaseUrl + "/rest/v1/wishlist"
                + "?expiry=gte." + from
                + "&expiry=lt." + to
                + "&notify_24h_sent_at=is.null"
                + "&select=*"
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.error("Failed to fetch expiring wishlist: HTTP {}", response.statusCode());
                return new ArrayList<>();
            }

            JSONArray array = new JSONArray(response.body());
            List<Wishlist> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Wishlist item = new Wishlist();
                item.setId(obj.getLong("id"));
                item.setUserId(obj.getLong("user_id"));
                item.setGameId(obj.getString("game_id"));
                item.setGameTitle(obj.getString("game_title"));
                item.setGameImage(obj.optString("game_image", null));
                item.setCurrentPrice(obj.optDouble("current_price", 0.0));
                item.setPriceOld(obj.optDouble("price_old", 0.0));
                item.setCut(obj.optInt("cut", 0));
                item.setShop(obj.optString("shop", null));
                item.setUrl(obj.optString("url", null));
                item.setExpiry(obj.optString("expiry", null));
                item.setNotify24hSentAt(obj.optString("notify_24h_sent_at", null));
                list.add(item);
            }

            return list;
        } catch (Exception e) {
            logger.error("Error finding expiring wishlist", e);
            return new ArrayList<>();
        }
    }

    public boolean markNotify24hSent(Long wishlistId, Instant sentAt) {
        try {
            URI uri = new URI(supabaseUrl + "/rest/v1/wishlist?id=eq." + wishlistId);
            JSONObject obj = new JSONObject();
            obj.put("notify_24h_sent_at", sentAt.toString());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(obj.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 204;
        } catch (Exception e) {
            logger.error("Error marking notify_24h_sent_at: wishlistId={}", wishlistId, e);
            return false;
        }
    }
}