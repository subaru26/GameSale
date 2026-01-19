package com.example.demo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.entity.SearchHistory;
import com.example.demo.repository.SearchHistoryRepository;

@Service
public class SearchHistoryService {

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    // ユーザーの検索履歴取得（重複を除去してクエリのみ返す）
    public List<String> getUserSearchHistory(Long userId) {
        List<SearchHistory> historyList = searchHistoryRepository.findByUserId(userId);
        // クエリのみを抽出し、重複を除去（最新のもののみ保持）
        return historyList.stream()
                .map(SearchHistory::getQuery)
                .distinct()
                .limit(10) // 最大10件
                .collect(Collectors.toList());
    }

    // 検索履歴に追加
    public boolean addSearchHistory(Long userId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        
        SearchHistory searchHistory = new SearchHistory();
        searchHistory.setUserId(userId);
        searchHistory.setQuery(query.trim());
        
        return searchHistoryRepository.add(searchHistory);
    }

    // 検索履歴から削除
    public boolean deleteSearchHistory(Long userId, String query) {
        return searchHistoryRepository.delete(userId, query);
    }
}
