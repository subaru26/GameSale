package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.entity.User;
import com.example.demo.entity.Wishlist;
import com.example.demo.service.UserService;
import com.example.demo.service.WishlistService;

import jakarta.servlet.http.HttpSession;

@Controller
public class WishlistController {

    private static final Logger logger = LoggerFactory.getLogger(WishlistController.class);

    @Autowired
    private WishlistService wishlistService;
    
    @Autowired
    private UserService userService;

    // ウィッシュリスト画面表示
    @GetMapping("/wishlist")
    public String wishlistPage(HttpSession session, Model model) {
        Map<String, String> user = (Map<String, String>) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Long userId = Long.parseLong(user.get("id"));
        User fullUser = userService.findById(userId);
        boolean darkMode = fullUser == null || !fullUser.isDarkMode();
        
        List<Wishlist> wishlistItems = wishlistService.getUserWishlist(userId);
        
        logger.info("Displaying wishlist for user {}: {} items, darkMode={}", userId, wishlistItems.size(), darkMode);

        model.addAttribute("username", user.get("name"));
        model.addAttribute("userid", user.get("id"));
        model.addAttribute("darkMode", darkMode);
        model.addAttribute("wishlistItems", wishlistItems);

        return "wishlist";
    }

    // ウィッシュリストに追加(API)
    @PostMapping("/api/wishlist")
    @ResponseBody
    public Map<String, Object> addToWishlist(@RequestBody Map<String, Object> requestData, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        Map<String, String> user = (Map<String, String>) session.getAttribute("user");
        if (user == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return response;
        }

        try {
            Long userId = Long.parseLong(user.get("id"));

            // 入力検証
            if (requestData.get("gameId") == null || requestData.get("gameId").toString().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "ゲームIDが不正です");
                return response;
            }
            
            if (requestData.get("gameTitle") == null || requestData.get("gameTitle").toString().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "ゲームタイトルが不正です");
                return response;
            }

            Wishlist wishlist = new Wishlist();
            wishlist.setUserId(userId);
            wishlist.setGameId(requestData.get("gameId").toString());
            wishlist.setGameTitle(requestData.get("gameTitle").toString());
            wishlist.setGameImage(requestData.get("gameImage") != null ? requestData.get("gameImage").toString() : null);
            
            // 価格情報
            wishlist.setCurrentPrice(getDoubleValue(requestData, "currentPrice"));
            wishlist.setPriceOld(getDoubleValue(requestData, "priceOld"));
            wishlist.setCut(getIntegerValue(requestData, "cut"));
            
            wishlist.setShop(requestData.get("shop") != null ? requestData.get("shop").toString() : null);
            wishlist.setUrl(requestData.get("url") != null ? requestData.get("url").toString() : null);
            
            // 過去最安値情報を追加
            wishlist.setHistoryLow(getDoubleValue(requestData, "historyLow"));
            wishlist.setHistoryLow1y(getDoubleValue(requestData, "historyLow1y"));
            wishlist.setHistoryLow3m(getDoubleValue(requestData, "historyLow3m"));
            wishlist.setStoreLow(getDoubleValue(requestData, "storeLow"));
            
            // セール終了日時
            wishlist.setExpiry(requestData.get("expiry") != null ? requestData.get("expiry").toString() : null);

            boolean success = wishlistService.addToWishlist(wishlist);

            if (success) {
                response.put("success", true);
                response.put("message", "ウィッシュリストに追加しました");
                logger.info("Added to wishlist: userId={}, gameId={}, title={}", 
                    userId, wishlist.getGameId(), wishlist.getGameTitle());
            } else {
                response.put("success", false);
                response.put("message", "既にウィッシュリストに存在します");
                logger.info("Item already in wishlist: userId={}, gameId={}", userId, wishlist.getGameId());
            }

        } catch (NumberFormatException e) {
            logger.error("Invalid number format in wishlist data", e);
            response.put("success", false);
            response.put("message", "データ形式が不正です");
        } catch (Exception e) {
            logger.error("Error adding to wishlist", e);
            response.put("success", false);
            response.put("message", "エラーが発生しました");
        }

        return response;
    }

    // ウィッシュリストから削除(API)
    @DeleteMapping("/api/wishlist")
    @ResponseBody
    public Map<String, Object> removeFromWishlist(@RequestParam String gameId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        Map<String, String> user = (Map<String, String>) session.getAttribute("user");
        if (user == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return response;
        }

        try {
            Long userId = Long.parseLong(user.get("id"));
            boolean success = wishlistService.removeFromWishlist(userId, gameId);

            response.put("success", success);
            response.put("message", success ? "削除しました" : "削除に失敗しました");
            
            if (success) {
                logger.info("Removed from wishlist: userId={}, gameId={}", userId, gameId);
            }
        } catch (Exception e) {
            logger.error("Error removing from wishlist", e);
            response.put("success", false);
            response.put("message", "エラーが発生しました");
        }

        return response;
    }

    // ウィッシュリスト一覧取得(API)
    @GetMapping("/api/wishlist")
    @ResponseBody
    public Map<String, Object> getWishlist(HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        Map<String, String> user = (Map<String, String>) session.getAttribute("user");
        if (user == null) {
            response.put("success", false);
            response.put("items", List.of());
            return response;
        }

        try {
            Long userId = Long.parseLong(user.get("id"));
            List<Wishlist> items = wishlistService.getUserWishlist(userId);

            response.put("success", true);
            response.put("items", items);
        } catch (Exception e) {
            logger.error("Error getting wishlist", e);
            response.put("success", false);
            response.put("items", List.of());
        }

        return response;
    }
    
    // ヘルパーメソッド: Doubleの値を安全に取得
    private Double getDoubleValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse double value for key {}: {}", key, value);
            return null;
        }
    }
    
    // ヘルパーメソッド: Integerの値を安全に取得
    private Integer getIntegerValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse integer value for key {}: {}", key, value);
            return null;
        }
    }
}