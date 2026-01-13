package com.example.demo.controller;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.MailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class RegisterController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailService mailService;

    private static final SecureRandom random = new SecureRandom();

    @GetMapping("/register")
    public String signupPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String account_name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String password_confirm,
            HttpSession session,
            Model model
    ) {
        if (!password.equals(password_confirm)) {
            model.addAttribute("error", "パスワードが一致していません。");
            model.addAttribute("account_name", account_name);
            model.addAttribute("email", email);
            return "register";
        }

        if (userRepository.findByEmail(email).isPresent()) {
            model.addAttribute("error", "このメールアドレスは既に登録されています。");
            model.addAttribute("account_name", account_name);
            return "register";
        }

        String code = String.format("%06d", random.nextInt(999999));

        session.setAttribute("reg_name", account_name);
        session.setAttribute("reg_email", email);
        session.setAttribute("reg_password", password);
        session.setAttribute("verify_code", code);
        session.setMaxInactiveInterval(10 * 60);

        mailService.sendVerificationMail(email, code);

        return "verify";
    }

    @PostMapping("/verify")
    public String verifyCode(
            @RequestParam String code,
            HttpSession session,
            HttpServletRequest request,
            Model model
    ) {
        String correct = (String) session.getAttribute("verify_code");

        if (correct == null || !code.equals(correct)) {
            model.addAttribute("error", "認証コードが違います。");
            return "verify";
        }

        User user = new User();
        user.setAccount_name((String) session.getAttribute("reg_name"));
        user.setEmail((String) session.getAttribute("reg_email"));
        user.setPassword((String) session.getAttribute("reg_password"));
        
        // 【重要】ここでTRUEを設定することで、SupabaseにTRUEが保存されます
        user.setDarkMode(true);

        userRepository.save(user);

        User savedUser = userRepository.findByEmail(user.getEmail()).orElse(null);
        
        if (savedUser != null) {
            Map<String, String> sessionUser = new HashMap<>();
            sessionUser.put("id", String.valueOf(savedUser.getId()));
            sessionUser.put("name", savedUser.getAccount_name());
            sessionUser.put("email", savedUser.getEmail());
            
            session.removeAttribute("reg_name");
            session.removeAttribute("reg_email");
            session.removeAttribute("reg_password");
            session.removeAttribute("verify_code");
            
            session.setAttribute("user", sessionUser);
            session.setMaxInactiveInterval(30 * 60);
            
            return "redirect:/deals";
        }

        session.invalidate();
        return "redirect:/login";
    }

    @PostMapping("/resend-code")
    public String resendCode(HttpSession session, Model model) {
        String email = (String) session.getAttribute("reg_email");
        if (email == null) {
            model.addAttribute("error", "メールアドレスが不明です。再度登録してください。");
            return "register";
        }

        String code = String.format("%06d", random.nextInt(999999));
        session.setAttribute("verify_code", code);
        mailService.sendVerificationMail(email, code);

        model.addAttribute("message", "認証コードを再送信しました。");
        return "verify";
    }
}