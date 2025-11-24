package com.example.demo.controller;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.MailService;

import jakarta.servlet.http.HttpSession;

@Controller
public class RegisterController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailService mailService;

    private static final SecureRandom random = new SecureRandom();

    // 新規登録画面
    @GetMapping("/register")
    public String signupPage() {
        return "register";
    }

    // 登録処理 → 認証コード送信
    @PostMapping("/register")
    public String register(
            @RequestParam String account_name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String password_confirm,
            HttpSession session,
            Model model
    ) {

        // パスワード一致チェック
        if (!password.equals(password_confirm)) {
            model.addAttribute("error", "パスワードが一致していません。");
            model.addAttribute("account_name", account_name);
            model.addAttribute("email", email);
            return "register";
        }

        // メール重複チェック
        if (userRepository.findByEmail(email).isPresent()) {
            model.addAttribute("error", "このメールアドレスは既に登録されています。");
            return "register";
        }

        // 認証コード生成（6桁）
        String code = String.format("%06d", random.nextInt(999999));

        // セッション保存
        session.setAttribute("reg_name", account_name);
        session.setAttribute("reg_email", email);
        session.setAttribute("reg_password", password);
        session.setAttribute("verify_code", code);
        session.setMaxInactiveInterval(10 * 60); // 10分

        // メール送信
        mailService.sendVerificationMail(email, code);

        return "verify";     // 認証画面へ
    }

    // 認証コード確認
    @PostMapping("/verify")
    public String verifyCode(
            @RequestParam String code,
            HttpSession session,
            Model model
    ) {

        String correct = (String) session.getAttribute("verify_code");

        if (!code.equals(correct)) {
            model.addAttribute("error", "認証コードが違います。");
            return "verify";
        }

        // 正しい → DB 登録
        User user = new User();
        user.setAccount_name((String) session.getAttribute("reg_name"));
        user.setEmail((String) session.getAttribute("reg_email"));
        user.setPassword((String) session.getAttribute("reg_password"));

        userRepository.save(user);

        // セッション削除
        session.invalidate();

        return "redirect:/login";
    }

    // 認証コード再送信
    @PostMapping("/resend-code")
    public String resendCode(HttpSession session, Model model) {
        String email = (String) session.getAttribute("reg_email");
        if (email == null) {
            model.addAttribute("error", "メールアドレスが不明です。再度登録してください。");
            return "register";
        }

        // 新しい認証コード生成
        String code = String.format("%06d", random.nextInt(999999));
        session.setAttribute("verify_code", code);
        session.setMaxInactiveInterval(10 * 60); // 10分

        // メール送信
        mailService.sendVerificationMail(email, code);

        model.addAttribute("message", "認証コードを再送信しました。メールを確認してください。");
        return "verify";
    }
}
