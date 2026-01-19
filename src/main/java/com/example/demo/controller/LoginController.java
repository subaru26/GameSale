package com.example.demo.controller;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.User;
import com.example.demo.service.MailService;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private MailService mailService;
    
    @Value("${app.test-mode.enabled:false}")
    private boolean testModeEnabled;
    
    private static final SecureRandom random = new SecureRandom();

    // ログイン画面表示
    @GetMapping("/login")
    public String loginPage(HttpServletRequest request, Model model) {
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        request.getSession(true);
        model.addAttribute("testModeEnabled", testModeEnabled);
        return "login";
    }

    // ログイン処理（メールアドレスとパスワード確認 → 認証コード送信）
    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpServletRequest request,
                        Model model) {

        User user = userService.authenticate(email, password);

        if (user != null) {
            // テストモードが有効な場合、メール認証をスキップして直接ログイン
            if (testModeEnabled) {
                HttpSession session = request.getSession(true);
                Map<String, String> sessionUser = new HashMap<>();
                sessionUser.put("id", String.valueOf(user.getId()));
                sessionUser.put("name", user.getAccount_name());
                sessionUser.put("email", user.getEmail());
                
                session.setAttribute("user", sessionUser);
                session.setMaxInactiveInterval(30 * 60);
                
                return "redirect:/deals";
            }
            
            // 通常モード：認証コード送信
            String code = String.format("%06d", random.nextInt(999999));
            
            HttpSession session = request.getSession(true);
            session.setAttribute("login_user_id", String.valueOf(user.getId()));
            session.setAttribute("login_user_name", user.getAccount_name());
            session.setAttribute("login_user_email", user.getEmail());
            session.setAttribute("login_verify_code", code);
            session.setMaxInactiveInterval(10 * 60);
            
            // ログイン用メール送信
            mailService.sendLoginVerificationMail(email, code);
            
            model.addAttribute("email", email);
            return "login_verify";
        } else {
            model.addAttribute("error", "メールアドレスまたはパスワードが間違っています。");
            return "login";
        }
    }

    // ログイン認証コード確認
    @PostMapping("/login/verify")
    public String verifyLoginCode(@RequestParam String code,
                                   HttpServletRequest request,
                                   Model model) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            model.addAttribute("error", "セッションが切れました。再度ログインしてください。");
            return "login";
        }

        String correctCode = (String) session.getAttribute("login_verify_code");
        
        if (correctCode == null) {
            model.addAttribute("error", "認証コードの有効期限が切れました。");
            return "login";
        }

        if (!code.equals(correctCode)) {
            model.addAttribute("error", "認証コードが違います。");
            String email = (String) session.getAttribute("login_user_email");
            model.addAttribute("email", email);
            return "login_verify";
        }

        // 認証成功
        Map<String, String> sessionUser = new HashMap<>();
        sessionUser.put("id", (String) session.getAttribute("login_user_id"));
        sessionUser.put("name", (String) session.getAttribute("login_user_name"));
        sessionUser.put("email", (String) session.getAttribute("login_user_email"));

        session.removeAttribute("login_user_id");
        session.removeAttribute("login_user_name");
        session.removeAttribute("login_user_email");
        session.removeAttribute("login_verify_code");

        session.setAttribute("user", sessionUser);
        session.setMaxInactiveInterval(30 * 60);

        return "redirect:/deals";
    }

    // ログイン認証コード再送信
    @PostMapping("/login/resend-code")
    public String resendLoginCode(HttpSession session, Model model) {
        String email = (String) session.getAttribute("login_user_email");
        
        if (email == null) {
            model.addAttribute("error", "メールアドレスが不明です。再度ログインしてください。");
            return "login";
        }

        String code = String.format("%06d", random.nextInt(999999));
        session.setAttribute("login_verify_code", code);
        session.setMaxInactiveInterval(10 * 60);

        // ログイン用メール送信
        mailService.sendLoginVerificationMail(email, code);

        model.addAttribute("message", "認証コードを再送信しました。メールを確認してください。");
        model.addAttribute("email", email);
        return "login_verify";
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