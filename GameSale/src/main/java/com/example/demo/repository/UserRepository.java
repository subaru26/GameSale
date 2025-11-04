package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import com.example.demo.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class UserRepository {

    @Value("${SUPABASE_URL}")
    private String supabaseUrl;

    @Value("${SUPABASE_KEY}")
    private String supabaseKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Optional<User> findByEmail(String email) {
        // ✅ /rest/v1 はここで付ける（プロパティには含めない）
        String url = supabaseUrl + "/rest/v1/users?email=eq." + email;

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseKey);
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            ObjectMapper mapper = new ObjectMapper();
            List<User> users = mapper.readValue(response.getBody(), new TypeReference<List<User>>() {});
            if (!users.isEmpty()) {
                return Optional.of(users.get(0));
            }
        } catch (Exception e) {
            System.err.println("❌ Supabaseからユーザー取得時にエラー発生: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }
}
