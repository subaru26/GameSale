package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.front.DealDto;
import com.example.demo.entity.Wishlist;
import com.example.demo.repository.WishlistRepository;

@Service
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;
    
    @Autowired
    private DealService dealService;

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

    public boolean isInWishlist(Long userId, String gameId) {
        // 互換用（shop指定なしの画面で使う場合）
        return wishlistRepository.existsAnyShop(userId, gameId);
    }

    // ウィッシュリストの価格情報を更新
    public boolean refreshWishlistItem(Long userId, String gameId, String shop) {
        // 1. 最新の価格情報を取得
        // shopパラメータを渡して、適切なストアの価格を取得する
        com.example.demo.dto.front.DealDto deal = dealService.fetchDeal(gameId, shop);
        
        // 該当ショップでの取り扱いがない、または取得できない場合
        if (deal == null) {
            return false;
        }

        // 2. Wishlistオブジェクトを作成して更新
        Wishlist wishlist = new Wishlist();
        wishlist.setUserId(userId);
        wishlist.setGameId(gameId);
        wishlist.setShop(shop); 
        
        // 取得した情報で上書き
        wishlist.setCurrentPrice(deal.getPriceNew());
        wishlist.setPriceOld(deal.getPriceOld());
        wishlist.setCut(deal.getCut());
        wishlist.setExpiry(deal.getExpiry());
        
        wishlist.setHistoryLow(deal.getHistoryLow());
        wishlist.setHistoryLow1y(deal.getHistoryLow1y());
        wishlist.setHistoryLow3m(deal.getHistoryLow3m());
        wishlist.setStoreLow(deal.getStoreLow());
        
        return wishlistRepository.update(wishlist);
    }

    // DTOから直接更新（API再取得なし）
    public boolean updateFromDto(Long userId, String gameId, String shop, DealDto deal) {
        if (deal == null) return false;

        Wishlist wishlist = new Wishlist();
        wishlist.setUserId(userId);
        wishlist.setGameId(gameId);
        wishlist.setShop(shop); 
        
        wishlist.setCurrentPrice(deal.getPriceNew());
        wishlist.setPriceOld(deal.getPriceOld());
        wishlist.setCut(deal.getCut());
        wishlist.setExpiry(deal.getExpiry());
        
        wishlist.setHistoryLow(deal.getHistoryLow());
        wishlist.setHistoryLow1y(deal.getHistoryLow1y());
        wishlist.setHistoryLow3m(deal.getHistoryLow3m());
        wishlist.setStoreLow(deal.getStoreLow());
        
        return wishlistRepository.update(wishlist);
    }
}