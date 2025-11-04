package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;

    // ログイン画面表示
    @GetMapping("/login")
    public String loginPage(HttpServletRequest request) {
        // 古いセッションを破棄して新しいセッションを生成
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        request.getSession(true); // 新しいセッション生成

        return "login";
    }

    // ログイン処理
    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpServletRequest request,
                        Model model) {

        User user = userService.authenticate(email, password);

        if (user != null) {
            // セッションにユーザー情報を格納
            HttpSession session = request.getSession(true);
            Map<String, String> sessionUser = new HashMap<>();
            sessionUser.put("id", String.valueOf(user.getId()));
            sessionUser.put("name", user.getAccount_name());
            sessionUser.put("email", user.getEmail());

            session.setAttribute("user", sessionUser);

            // セッションの有効期限を設定（30分）
            session.setMaxInactiveInterval(30 * 60);

            return "redirect:/deals";
        } else {
            model.addAttribute("error", "メールアドレスまたはパスワードが間違っています。");
            return "login";
        }
    }

    // ログアウト処理
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}
