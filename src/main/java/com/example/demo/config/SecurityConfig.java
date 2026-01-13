package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRFを無効化（フォームPOST用）
            .csrf(csrf -> csrf.disable())

            // 認可設定
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/login/verify", "/login/resend-code",
                               "/register", "/verify", "/resend-code", 
                               "/css/**", "/js/**").permitAll()
                .requestMatchers("/deals", "/wishlist", "/settings", "/api/**").permitAll()
                .anyRequest().permitAll()
            )

            // Springの自動ログイン機能を完全オフ
            .formLogin(form -> form.disable())

            // ログアウト設定
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .permitAll()
            );

        return http.build();
    }
}