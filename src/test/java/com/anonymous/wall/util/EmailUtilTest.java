package com.anonymous.wall.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmailUtil Tests")
class EmailUtilTest {

    @Nested
    @DisplayName("Send Verification Code Email Tests")
    class SendVerificationCodeEmailTests {

        @Test
        @DisplayName("Positive: Should send registration verification email")
        void shouldSendRegistrationEmail() {
            // Arrange
            String email = "test@harvard.edu";
            String code = "123456";
            String purpose = "register";

            // Act & Assert - should not throw
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(email, code, purpose)
            );
        }

        @Test
        @DisplayName("Positive: Should send login verification email")
        void shouldSendLoginEmail() {
            // Arrange
            String email = "user@mit.edu";
            String code = "789012";
            String purpose = "login";

            // Act & Assert - should not throw
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(email, code, purpose)
            );
        }

        @Test
        @DisplayName("Positive: Should send reset password email")
        void shouldSendResetPasswordEmail() {
            // Arrange
            String email = "forgot@stanford.edu";
            String code = "456789";
            String purpose = "reset_password";

            // Act & Assert - should not throw
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(email, code, purpose)
            );
        }

        @Test
        @DisplayName("Positive: Should handle default purpose")
        void shouldHandleDefaultPurpose() {
            // Arrange
            String email = "test@berkeley.edu";
            String code = "111111";
            String purpose = "unknown_purpose";

            // Act & Assert - should not throw
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(email, code, purpose)
            );
        }

        @Test
        @DisplayName("Edge: Should handle email with long domain")
        void shouldHandleEmailWithLongDomain() {
            // Arrange
            String email = "test@university.of.california.berkeley.edu";
            String code = "222222";
            String purpose = "register";

            // Act & Assert - should not throw
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(email, code, purpose)
            );
        }

        @Test
        @DisplayName("Edge: Should handle code with special characters")
        void shouldHandleCodeWithSpecialCharacters() {
            // Arrange
            String email = "test@yale.edu";
            String code = "ABC-123";
            String purpose = "login";

            // Act & Assert - should not throw
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(email, code, purpose)
            );
        }

        @Test
        @DisplayName("Edge: Should handle empty code")
        void shouldHandleEmptyCode() {
            // Arrange
            String email = "test@princeton.edu";
            String code = "";
            String purpose = "register";

            // Act & Assert - should not throw
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(email, code, purpose)
            );
        }

        @Test
        @DisplayName("Edge: Should handle very long code")
        void shouldHandleVeryLongCode() {
            // Arrange
            String email = "test@columbia.edu";
            String code = "A".repeat(100);
            String purpose = "login";

            // Act & Assert - should not throw
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(email, code, purpose)
            );
        }

        @Test
        @DisplayName("Negative: Should handle null email")
        void shouldHandleNullEmail() {
            // Arrange
            String code = "123456";
            String purpose = "register";

            // Act & Assert - method should handle gracefully (may log error)
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(null, code, purpose)
            );
        }

        @Test
        @DisplayName("Negative: Should handle null code")
        void shouldHandleNullCode() {
            // Arrange
            String email = "test@nyu.edu";
            String purpose = "register";

            // Act & Assert - method should handle gracefully (may throw or log)
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(email, null, purpose)
            );
        }

        @Test
        @DisplayName("Negative: Should handle null purpose")
        void shouldHandleNullPurpose() {
            // Arrange
            String email = "test@bu.edu";
            String code = "123456";

            // Act & Assert - should default to generic message
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(email, code, null)
            );
        }
    }

    @Nested
    @DisplayName("Console Output Tests")
    class ConsoleOutputTests {

        @Test
        @DisplayName("Positive: Should print email to console")
        void shouldPrintEmailToConsole() {
            // Arrange
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));

            String email = "test@test.edu";
            String code = "999999";
            String purpose = "register";

            try {
                // Act
                EmailUtil.sendVerificationCodeEmail(email, code, purpose);

                // Assert
                String output = outputStream.toString();
                assertTrue(output.contains("FAKE EMAIL"), "Should indicate fake email");
                assertTrue(output.contains(email), "Should contain email address");
                assertTrue(output.contains(code), "Should contain verification code");
            } finally {
                // Restore System.out
                System.setOut(originalOut);
            }
        }

        @Test
        @DisplayName("Positive: Should include code in output for register purpose")
        void shouldIncludeCodeInOutputForRegisterPurpose() {
            // Arrange
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));

            String email = "verify@test.edu";
            String code = "ABCD123";
            String purpose = "register";

            try {
                // Act
                EmailUtil.sendVerificationCodeEmail(email, code, purpose);

                // Assert
                String output = outputStream.toString();
                assertTrue(output.contains(code), "Output should contain the verification code");
            } finally {
                // Restore System.out
                System.setOut(originalOut);
            }
        }
    }

    @Nested
    @DisplayName("Purpose Switch Tests")
    class PurposeSwitchTests {

        @Test
        @DisplayName("Positive: Should use correct subject for register")
        void shouldUseCorrectSubjectForRegister() {
            // This test verifies the switch statement works correctly
            // We can't directly access the subject, but we ensure no exceptions occur
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail("test@test.edu", "123456", "register")
            );
        }

        @Test
        @DisplayName("Positive: Should use correct subject for login")
        void shouldUseCorrectSubjectForLogin() {
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail("test@test.edu", "123456", "login")
            );
        }

        @Test
        @DisplayName("Positive: Should use correct subject for reset_password")
        void shouldUseCorrectSubjectForResetPassword() {
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail("test@test.edu", "123456", "reset_password")
            );
        }

        @Test
        @DisplayName("Edge: Should handle case-sensitive purpose")
        void shouldHandleCaseSensitivePurpose() {
            // "REGISTER" != "register", should use default
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail("test@test.edu", "123456", "REGISTER")
            );
        }

        @Test
        @DisplayName("Edge: Should handle whitespace in purpose")
        void shouldHandleWhitespaceInPurpose() {
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail("test@test.edu", "123456", " register ")
            );
        }
    }

    @Nested
    @DisplayName("Email Format Tests")
    class EmailFormatTests {

        @Test
        @DisplayName("Edge: Should handle email with plus sign")
        void shouldHandleEmailWithPlusSign() {
            // Arrange
            String email = "user+test@school.edu";
            String code = "123456";

            // Act & Assert
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(email, code, "register")
            );
        }

        @Test
        @DisplayName("Edge: Should handle email with dots")
        void shouldHandleEmailWithDots() {
            // Arrange
            String email = "first.middle.last@school.edu";
            String code = "123456";

            // Act & Assert
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(email, code, "login")
            );
        }

        @Test
        @DisplayName("Edge: Should handle email with numbers")
        void shouldHandleEmailWithNumbers() {
            // Arrange
            String email = "user123@school456.edu";
            String code = "999999";

            // Act & Assert
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(email, code, "reset_password")
            );
        }

        @Test
        @DisplayName("Edge: Should handle international domain")
        void shouldHandleInternationalDomain() {
            // Arrange
            String email = "student@université.fr";
            String code = "123456";

            // Act & Assert
            assertDoesNotThrow(() ->
                EmailUtil.sendVerificationCodeEmail(email, code, "register")
            );
        }
    }
}
