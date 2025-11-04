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

    /**
     * Supabase上のユーザー認証を行う
     * 
     * @param email    入力されたメールアドレス
     * @param password 入力されたパスワード（平文）
     * @return 一致したユーザー、またはnull
     */
    public User authenticate(String email, String password) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // パスワード照合
                if (user.getPassword() != null && user.getPassword().equals(password)) {
                    return user;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 認証中にエラーが発生: " + e.getMessage());
            e.printStackTrace();
        }

        return null; // 認証失敗時はnullを返す
    }
}
