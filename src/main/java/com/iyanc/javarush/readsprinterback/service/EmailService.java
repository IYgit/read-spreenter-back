package com.iyanc.javarush.readsprinterback.service;

import com.iyanc.javarush.readsprinterback.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(User user) {
        String link = baseUrl + "/api/auth/verify?token=" + user.getVerificationToken();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject("Підтвердіть вашу email-адресу — Read Sprinter");
        message.setText(
                "Привіт, " + user.getUsername() + "!\n\n" +
                "Для підтвердження email перейдіть за посиланням:\n" +
                link + "\n\n" +
                "Посилання дійсне протягом 24 годин.\n\n" +
                "Read Sprinter"
        );

        try {
            mailSender.send(message);
            log.info("Verification email sent to {}", user.getEmail());
        } catch (MailException e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
            throw e;
        }
    }
}

