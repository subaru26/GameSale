package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Wishlist;
import com.example.demo.repository.WishlistRepository;

@Service
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    // ユーザーのウィッシュリスト取得
    public List<Wishlist> getUserWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId);
    }

    // ウィッシュリストに追加
    public boolean addToWishlist(Wishlist wishlist) {
        // 既に存在する場合は追加しない
        if (wishlistRepository.exists(wishlist.getUserId(), wishlist.getGameId(), wishlist.getShop())) {
            return false;
        }
        return wishlistRepository.add(wishlist);
    }

    // ウィッシュリストから削除
    public boolean removeFromWishlist(Long userId, String gameId, String shop) {
        return wishlistRepository.delete(userId, gameId, shop);
    }

    // ウィッシュリストに存在するか確認
    public boolean isInWishlist(Long userId, String gameId) {
        // 互換用（shop指定なしの画面で使う場合）
        return wishlistRepository.existsAnyShop(userId, gameId);
    }
}