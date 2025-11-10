package com.example.demo.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.GameInfoService;

@RestController
public class GameInfoController {

    private final GameInfoService gameInfoService;

    public GameInfoController(GameInfoService gameInfoService) {
        this.gameInfoService = gameInfoService;
    }

    @GetMapping("/api/gameinfo")
    public Map<String, Object> getGameInfo(@RequestParam String id) {
        return gameInfoService.fetchGameInfo(id);
    }
}
