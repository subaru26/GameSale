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
}