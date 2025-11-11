package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;

@Controller
public class RegisterController {

    @Autowired
    private UserService userService;

    // 新規登録ページ表示
    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    // 登録処理
    @PostMapping("/register")
    public String registerUser(
            @RequestParam String account_name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String password_confirm,
            Model model) {

        // パスワード一致チェック
        if (!password.equals(password_confirm)) {
            model.addAttribute("error", "パスワードが一致しません。");
            return "register";
        }

        // 重複メールチェック
        if (userService.findByEmail(email) != null) {
            model.addAttribute("error", "このメールアドレスはすでに登録されています。");
            return "register";
        }

        // ユーザー作成して保存（SupabaseにINSERT）
        User newUser = new User();
        newUser.setAccount_name(account_name);
        newUser.setEmail(email);
        newUser.setPassword(password);

        userService.save(newUser);

        model.addAttribute("success", "登録が完了しました。ログインしてください。");
        return "redirect:/login";
    }
}
