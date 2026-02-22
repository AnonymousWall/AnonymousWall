package com.anonymous.wall.util;

import io.micronaut.context.annotation.Requires;
import io.micronaut.email.Email;
import io.micronaut.email.EmailSender;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Requires(env = "prod")
public class ProdEmailUtil implements EmailUtilInterface {
    private static final Logger log = LoggerFactory.getLogger(ProdEmailUtil.class);

    @Inject
    private EmailSender<?, ?> emailSender;

    @Override
    public void sendVerificationCodeEmail(String email, String code, String purpose) {
        String subject = switch (purpose) {
            case "register" -> "Echo Talk - Verify Your Email";
            case "login" -> "Echo Talk - Login Code";
            case "reset_password" -> "Echo Talk - Reset Your Password";
            default -> "Echo Talk - Verification Code";
        };

        String body = switch (purpose) {
            case "register" -> String.format(
                    "Welcome to Echo Talk!\n\nYour verification code is: %s\n\nThis code expires in 15 minutes.\nIf you didn't request this, you can ignore this email.", code);
            case "login" -> String.format(
                    "Your Echo Talk login code is: %s\n\nThis code expires in 15 minutes.\nIf you didn't request this, you can ignore this email.", code);
            case "reset_password" -> String.format(
                    "Your password reset code is: %s\n\nThis code expires in 15 minutes.\nIf you didn't request this, you can ignore this email.", code);
            default -> String.format("Your verification code is: %s", code);
        };

        try {
            emailSender.send(Email.builder()
                    .to(email)           // no .from() needed, in prod properties
                    .subject(subject)
                    .body(body));
            log.info("Email sent to: {} for purpose: {}", email, purpose);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", email, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}