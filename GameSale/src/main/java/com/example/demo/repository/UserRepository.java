package com.example.demo.repository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.User;

@Repository
public class UserRepository {

    @Value("${SUPABASE_URL}")
    private String supabaseUrl;

    @Value("${SUPABASE_KEY}")
    private String supabaseKey;

    private final HttpClient client = HttpClient.newHttpClient();

    // メールアドレスでユーザーを検索
    public Optional<User> findByEmail(String email) {
        try {
            URI uri = new URI(supabaseUrl + "/rest/v1/users?email=eq." + email + "&select=*");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray array = new JSONArray(response.body());

            if (array.isEmpty()) {
                return Optional.empty();
            }

            JSONObject obj = array.getJSONObject(0);
            User user = new User();
            user.setId(obj.getLong("id"));
            user.setAccount_name(obj.optString("account_name"));
            user.setEmail(obj.optString("email"));
            user.setPassword(obj.optString("password"));
            return Optional.of(user);

        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    // 新規登録
    public void save(User user) {
        try {
            URI uri = new URI(supabaseUrl + "/rest/v1/users");
            JSONObject obj = new JSONObject();
            obj.put("account_name", user.getAccount_name());
            obj.put("email", user.getEmail());
            obj.put("password", user.getPassword());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(obj.toString()))
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
