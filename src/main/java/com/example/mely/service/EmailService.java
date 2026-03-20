package com.example.mely.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String to, String otp){
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("Mely <melybank777@gmail.com>");
            message.setTo(to);
            message.setSubject("Mely | Verify Your Account");
            message.setText("Salam Alaikum! Your verification code for Mely is: "+ otp + "\n\nThis code will expire in 10 minutes.");
            mailSender.send(message);
        } catch (Exception e){
            System.out.println("Failed to send email: " + e.getMessage());
        }
    }
}
