package com.example.demo.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GameInfoService {

    @Value("${ITAD_API_KEY}")
    private String apiKey;

    @Value("${ITAD_INFO_URL}")
    private String baseUrl;

    public Map<String, Object> fetchGameInfo(String id) {
        RestTemplate restTemplate = new RestTemplate();
        String url = baseUrl + "?key=" + apiKey + "&id=" + id;

        System.out.println("[GameInfoService] Fetching from: " + url);

        return restTemplate.getForObject(url, Map.class);
    }
}
