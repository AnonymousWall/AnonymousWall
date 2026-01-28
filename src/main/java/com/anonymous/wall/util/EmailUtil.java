package com.anonymous.wall.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Email utility - for local testing, prints to console instead of sending real emails
 */
public class EmailUtil {
    private static final Logger log = LoggerFactory.getLogger(EmailUtil.class);

    /**
     * Send verification code email (fake - logs to console)
     * In production, replace with actual email service (SendGrid, AWS SES, etc.)
     */
    public static void sendVerificationCodeEmail(String email, String code, String purpose) {
        String subject = switch (purpose) {
            case "register" -> "Campus Wall - Verify Your Email";
            case "login" -> "Campus Wall - Login Code";
            case "reset_password" -> "Campus Wall - Reset Your Password";
            default -> "Campus Wall - Verification Code";
        };

        String body = switch (purpose) {
            case "register" -> String.format(
                "Welcome to Campus Wall!\n\n" +
                "Your verification code is: %s\n\n" +
                "This code expires in 15 minutes.\n" +
                "If you didn't request this, you can ignore this email.",
                code
            );
            case "login" -> String.format(
                "Your Campus Wall login code is: %s\n\n" +
                "This code expires in 15 minutes.\n" +
                "If you didn't request this, you can ignore this email.",
                code
            );
            case "reset_password" -> String.format(
                "Password Reset Request\n\n" +
                "Your password reset code is: %s\n\n" +
                "This code expires in 15 minutes.\n" +
                "If you didn't request this, you can ignore this email.",
                code
            );
            default -> String.format("Your verification code is: %s", code);
        };

        // FOR LOCAL TESTING: Print to console and logs
        printToConsole(email, subject, code);
        log.info("[FAKE EMAIL] To: {} | Subject: {} | Code: {}", email, subject, code);
    }

    /**
     * Print email details to console for local testing
     */
    private static void printToConsole(String email, String subject, String code) {
        System.out.println("\n" +
            "╔══════════════════════════════════════════════════════════╗\n" +
            "║               FAKE EMAIL SENT (LOCAL TESTING)            ║\n" +
            "╠══════════════════════════════════════════════════════════╣\n" +
            "║ To:     " + email + "\n" +
            "║ Subject: " + subject + "\n" +
            "║ Code:    " + code + "\n" +
            "╚══════════════════════════════════════════════════════════╝\n");
    }
}
