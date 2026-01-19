package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.SearchHistoryService;

import jakarta.servlet.http.HttpSession;

@RestController
public class SearchHistoryController {

    @Autowired
    private SearchHistoryService searchHistoryService;

    // 検索履歴取得（API）
    @GetMapping("/api/search-history")
    public Map<String, Object> getSearchHistory(HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        Map<String, String> user = (Map<String, String>) session.getAttribute("user");
        if (user == null) {
            response.put("success", false);
            response.put("history", List.of());
            return response;
        }

        Long userId = Long.parseLong(user.get("id"));
        List<String> history = searchHistoryService.getUserSearchHistory(userId);

        response.put("success", true);
        response.put("history", history);

        return response;
    }

    // 検索履歴に追加（API）
    @PostMapping("/api/search-history")
    public Map<String, Object> addSearchHistory(@RequestBody Map<String, String> requestData, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        Map<String, String> user = (Map<String, String>) session.getAttribute("user");
        if (user == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return response;
        }

        Long userId = Long.parseLong(user.get("id"));
        String query = requestData.get("query");

        boolean success = searchHistoryService.addSearchHistory(userId, query);

        response.put("success", success);
        response.put("message", success ? "検索履歴に追加しました" : "追加に失敗しました");

        return response;
    }

    // 検索履歴から削除（API）
    @DeleteMapping("/api/search-history")
    public Map<String, Object> deleteSearchHistory(@RequestParam String query, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        Map<String, String> user = (Map<String, String>) session.getAttribute("user");
        if (user == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return response;
        }

        Long userId = Long.parseLong(user.get("id"));
        boolean success = searchHistoryService.deleteSearchHistory(userId, query);

        response.put("success", success);
        response.put("message", success ? "削除しました" : "削除に失敗しました");

        return response;
    }
}
