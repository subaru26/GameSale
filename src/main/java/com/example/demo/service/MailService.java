package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Value("${MAIL_FROM}")
    private String from;

    @Value("${MAIL_SUBJECT}")
    private String subject;

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // 新規登録用の認証メール
    public void sendVerificationMail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(
                "GameSale にご登録いただきありがとうございます。\n\n" +
                "以下の認証コードを入力してください：\n\n" +
                code + "\n\n" +
                "このコードは10分間有効です。"
        );
        
        mailSender.send(message);
    }
    
    // ログイン用の認証メール
    public void sendLoginVerificationMail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("GameSale - ログイン認証コード");
        message.setText(
                "GameSale へのログインを確認しています。\n\n" +
                "以下の認証コードを入力してログインを完了してください：\n\n" +
                code + "\n\n" +
                "このコードは10分間有効です。\n\n" +
                "※ ご自身でログインしていない場合は、このメールを無視してください。"
        );
        
        mailSender.send(message);
    }

    // ウィッシュリスト期限通知（24時間前）
    public void sendWishlistExpiryReminder(String to, String gameTitle, String shop, String url, String expiryIso) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("GameSale - セール終了間近のお知らせ");
        StringBuilder body = new StringBuilder();
        body.append("ウィッシュリストに登録したセールが、24時間以内に終了予定です。\n\n");
        body.append("タイトル: ").append(gameTitle).append("\n");
        if (shop != null && !shop.isBlank()) body.append("ストア: ").append(shop).append("\n");
        if (expiryIso != null && !expiryIso.isBlank()) body.append("終了予定: ").append(expiryIso).append("\n");
        if (url != null && !url.isBlank()) body.append("\nストアページ: ").append(url).append("\n");
        body.append("\n※この通知は設定画面からOFFにできます。\n");
        message.setText(body.toString());
        mailSender.send(message);
    }
}