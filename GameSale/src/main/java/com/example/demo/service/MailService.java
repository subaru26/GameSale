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
        System.out.println("FROM=" + from);
        System.out.println("SUBJECT=" + subject);

        mailSender.send(message);
    }
    
}
