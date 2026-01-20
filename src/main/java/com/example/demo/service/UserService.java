package com.example.demo.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // ログイン認証
    public User authenticate(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    // 登録処理（SupabaseのusersテーブルにINSERTされる）
    public void save(User user) {
        userRepository.save(user);
    }

    // メールでユーザーを検索
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    // IDでユーザーを検索
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // ユーザー名を更新
    public boolean updateAccountName(Long userId, String newAccountName) {
        return userRepository.updateAccountName(userId, newAccountName);
    }

    // パスワードを更新（現在のパスワード確認付き）
    public boolean updatePassword(Long userId, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // 現在のパスワードが正しいか確認
            if (user.getPassword().equals(currentPassword)) {
                return userRepository.updatePassword(userId, newPassword);
            }
        }
        return false;
    }

    // ダークモード設定を更新
    public boolean updateDarkMode(Long userId, boolean darkMode) {
        return userRepository.updateDarkMode(userId, darkMode);
    }

    public boolean updateWishlistNotifyEnabled(Long userId, boolean enabled) {
        return userRepository.updateWishlistNotifyEnabled(userId, enabled);
    }
}