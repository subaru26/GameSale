package com.example.demo.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GameInfoService {

    private static final String API_KEY = "1c17fd70c2b436ce327b048ce319119b8bdcd4e2";
    private static final String BASE_URL = "https://api.isthereanydeal.com/games/info/v2";

    public Map<String, Object> fetchGameInfo(String id) {
        RestTemplate restTemplate = new RestTemplate();
        String url = BASE_URL + "?key=" + API_KEY + "&id=" + id;

        System.out.println("[GameInfoService] Fetching from: " + url);

        return restTemplate.getForObject(url, Map.class);
    }
}
