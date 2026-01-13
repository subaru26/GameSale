package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        
        // ダークモード設定を取得
        User fullUser = userService.findById(userId);
        boolean darkMode = fullUser == null || !fullUser.isDarkMode();
        
        List<Wishlist> wishlistItems = wishlistService.getUserWishlist(userId);

        model.addAttribute("username", user.get("name"));
        model.addAttribute("userid", user.get("id"));
        model.addAttribute("darkMode", darkMode);
        model.addAttribute("wishlistItems", wishlistItems);

        return "wishlist";
    }

    // ウィッシュリストに追加（API）
    @PostMapping("/api/wishlist")
    @ResponseBody
    public Map<String, Object> addToWishlist(@RequestBody Map<String, String> requestData, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        Map<String, String> user = (Map<String, String>) session.getAttribute("user");
        if (user == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return response;
        }

        Long userId = Long.parseLong(user.get("id"));

        Wishlist wishlist = new Wishlist();
        wishlist.setUserId(userId);
        wishlist.setGameId(requestData.get("gameId"));
        wishlist.setGameTitle(requestData.get("gameTitle"));
        wishlist.setGameImage(requestData.get("gameImage"));
        wishlist.setCurrentPrice(Double.parseDouble(requestData.getOrDefault("currentPrice", "0")));
        wishlist.setShop(requestData.get("shop"));
        wishlist.setUrl(requestData.get("url"));

        boolean success = wishlistService.addToWishlist(wishlist);

        if (success) {
            response.put("success", true);
            response.put("message", "ウィッシュリストに追加しました");
        } else {
            response.put("success", false);
            response.put("message", "既にウィッシュリストに存在します");
        }

        return response;
    }

    // ウィッシュリストから削除（API）
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

        Long userId = Long.parseLong(user.get("id"));
        boolean success = wishlistService.removeFromWishlist(userId, gameId);

        response.put("success", success);
        response.put("message", success ? "削除しました" : "削除に失敗しました");

        return response;
    }

    // ウィッシュリスト一覧取得（API）
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

        Long userId = Long.parseLong(user.get("id"));
        List<Wishlist> items = wishlistService.getUserWishlist(userId);

        response.put("success", true);
        response.put("items", items);

        return response;
    }
}