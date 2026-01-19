package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class SettingsController {

    @Autowired
    private UserService userService;
    
    @Value("${app.test-mode.enabled:false}")
    private boolean testModeEnabled;

    // 設定画面表示
    @GetMapping("/settings")
    public String settingsPage(HttpSession session, Model model) {
        Map<String, String> sessionUser = (Map<String, String>) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        Long userId = Long.parseLong(sessionUser.get("id"));
        User user = userService.findById(userId);

        model.addAttribute("username", user.getAccount_name());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("userid", userId);

        // 【修正】DBのdarkModeがfalseなら、画面には「ダークモードである(true)」と伝える
        // これにより、初期値falseの新規ユーザーはダークモードで表示されます
        model.addAttribute("darkMode", user == null || !user.isDarkMode());
        
        // テストモード状態を追加
        model.addAttribute("testModeEnabled", testModeEnabled);

        return "settings";
    }

    // ユーザー名変更
    @PostMapping("/api/settings/username")
    @ResponseBody
    public Map<String, Object> updateUsername(
            @RequestParam String newUsername,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        Map<String, String> sessionUser = (Map<String, String>) session.getAttribute("user");

        if (sessionUser == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return response;
        }

        if (newUsername == null || newUsername.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "ユーザー名を入力してください");
            return response;
        }

        Long userId = Long.parseLong(sessionUser.get("id"));
        boolean success = userService.updateAccountName(userId, newUsername.trim());

        if (success) {
            // セッション情報も更新
            sessionUser.put("name", newUsername.trim());
            session.setAttribute("user", sessionUser);

            response.put("success", true);
            response.put("message", "ユーザー名を変更しました");
        } else {
            response.put("success", false);
            response.put("message", "変更に失敗しました");
        }

        return response;
    }

    // パスワード変更
    @PostMapping("/api/settings/password")
    @ResponseBody
    public Map<String, Object> updatePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        Map<String, String> sessionUser = (Map<String, String>) session.getAttribute("user");

        if (sessionUser == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return response;
        }

        // 新しいパスワードと確認用が一致するか
        if (!newPassword.equals(confirmPassword)) {
            response.put("success", false);
            response.put("message", "新しいパスワードと確認用パスワードが一致しません");
            return response;
        }

        // パスワード強度チェック
        if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            response.put("success", false);
            response.put("message", "パスワードは8文字以上で、大文字・小文字・数字を含む必要があります");
            return response;
        }

        Long userId = Long.parseLong(sessionUser.get("id"));
        boolean success = userService.updatePassword(userId, currentPassword, newPassword);

        if (success) {
            response.put("success", true);
            response.put("message", "パスワードを変更しました");
        } else {
            response.put("success", false);
            response.put("message", "現在のパスワードが正しくありません");
        }

        return response;
    }

    // ダークモード設定変更
    @PostMapping("/api/settings/darkmode")
    @ResponseBody
    public Map<String, Object> updateDarkMode(
            @RequestParam boolean darkMode,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        Map<String, String> sessionUser = (Map<String, String>) session.getAttribute("user");

        if (sessionUser == null) {
            response.put("success", false);
            response.put("message", "ログインが必要です");
            return response;
        }

        Long userId = Long.parseLong(sessionUser.get("id"));
        boolean success = userService.updateDarkMode(userId, darkMode);

        if (success) {
            response.put("success", true);
            response.put("darkMode", darkMode);
            response.put("message", darkMode ? "ダークモードを有効にしました" : "ライトモードに変更しました");
        } else {
            response.put("success", false);
            response.put("message", "変更に失敗しました");
        }

        return response;
    }
}