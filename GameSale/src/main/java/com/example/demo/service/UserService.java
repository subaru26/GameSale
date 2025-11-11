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
}
