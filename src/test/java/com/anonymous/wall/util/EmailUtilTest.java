package com.anonymous.wall.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmailUtil Tests")
class EmailUtilTest {

    private EmailUtilInterface emailUtil;

    @BeforeEach
    void setUp() {
        emailUtil = new EmailUtil();
    }

    @Nested
    @DisplayName("Positive Cases - Valid Email Sending")
    class PositiveCases {

        @Test
        @DisplayName("Should send registration verification email")
        void shouldSendRegistrationEmail() {
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                emailUtil.sendVerificationCodeEmail("test@harvard.edu", "123456", "register");
                
                String output = outContent.toString();
                assertTrue(output.contains("test@harvard.edu"), "Should contain recipient email");
                assertTrue(output.contains("123456"), "Should contain verification code");
                assertTrue(output.contains("FAKE EMAIL SENT"), "Should indicate fake email for testing");
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        @DisplayName("Should send login verification email")
        void shouldSendLoginEmail() {
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                emailUtil.sendVerificationCodeEmail("user@mit.edu", "654321", "login");
                
                String output = outContent.toString();
                assertTrue(output.contains("user@mit.edu"), "Should contain recipient email");
                assertTrue(output.contains("654321"), "Should contain login code");
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        @DisplayName("Should send password reset email")
        void shouldSendPasswordResetEmail() {
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                emailUtil.sendVerificationCodeEmail("student@stanford.edu", "789012", "reset_password");
                
                String output = outContent.toString();
                assertTrue(output.contains("student@stanford.edu"), "Should contain recipient email");
                assertTrue(output.contains("789012"), "Should contain reset code");
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        @DisplayName("Should send email with default purpose")
        void shouldSendEmailWithDefaultPurpose() {
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                emailUtil.sendVerificationCodeEmail("test@yale.edu", "111111", "unknown_purpose");
                
                String output = outContent.toString();
                assertTrue(output.contains("test@yale.edu"), "Should contain recipient email");
                assertTrue(output.contains("111111"), "Should contain verification code");
            } finally {
                System.setOut(originalOut);
            }
        }
    }

    @Nested
    @DisplayName("Negative Cases - Invalid Inputs")
    class NegativeCases {

        @Test
        @DisplayName("Should handle null email gracefully")
        void shouldHandleNullEmail() {
            assertDoesNotThrow(() -> {
                emailUtil.sendVerificationCodeEmail(null, "123456", "register");
            }, "Should not throw exception for null email");
        }

        @Test
        @DisplayName("Should handle null code gracefully")
        void shouldHandleNullCode() {
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                emailUtil.sendVerificationCodeEmail("test@test.edu", null, "register");
                
                String output = outContent.toString();
                assertTrue(output.contains("test@test.edu"), "Should still contain email");
                assertTrue(output.contains("null"), "Should show null code");
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        @DisplayName("Should handle null purpose gracefully")
        void shouldHandleNullPurpose() {
            assertDoesNotThrow(() -> {
                emailUtil.sendVerificationCodeEmail("test@test.edu", "123456", null);
            }, "Should handle null purpose with default case");
        }

        @Test
        @DisplayName("Should handle empty email")
        void shouldHandleEmptyEmail() {
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                emailUtil.sendVerificationCodeEmail("", "123456", "register");
                
                String output = outContent.toString();
                assertTrue(output.contains("FAKE EMAIL SENT"), "Should still attempt to send");
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        @DisplayName("Should handle empty code")
        void shouldHandleEmptyCode() {
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                emailUtil.sendVerificationCodeEmail("test@test.edu", "", "register");
                
                String output = outContent.toString();
                assertTrue(output.contains("test@test.edu"), "Should contain email");
            } finally {
                System.setOut(originalOut);
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases - Boundary Conditions")
    class EdgeCases {

        @Test
        @DisplayName("Should handle very long email address")
        void shouldHandleVeryLongEmail() {
            String longEmail = "a".repeat(200) + "@university.edu";
            
            assertDoesNotThrow(() -> {
                emailUtil.sendVerificationCodeEmail(longEmail, "123456", "register");
            }, "Should handle very long email");
        }

        @Test
        @DisplayName("Should handle very long verification code")
        void shouldHandleVeryLongCode() {
            String longCode = "1".repeat(1000);
            
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                emailUtil.sendVerificationCodeEmail("test@test.edu", longCode, "register");
                
                String output = outContent.toString();
                assertTrue(output.contains(longCode), "Should contain the long code");
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        @DisplayName("Should handle special characters in email")
        void shouldHandleSpecialCharactersInEmail() {
            String emailWithSpecialChars = "test+filter@uni-versity.co.uk";
            
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                emailUtil.sendVerificationCodeEmail(emailWithSpecialChars, "123456", "register");
                
                String output = outContent.toString();
                assertTrue(output.contains(emailWithSpecialChars), "Should handle special characters");
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        @DisplayName("Should handle special characters in code")
        void shouldHandleSpecialCharactersInCode() {
            String codeWithSpecialChars = "ABC-123!@#";
            
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                emailUtil.sendVerificationCodeEmail("test@test.edu", codeWithSpecialChars, "register");
                
                String output = outContent.toString();
                assertTrue(output.contains(codeWithSpecialChars), "Should handle special characters in code");
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        @DisplayName("Should handle Unicode characters")
        void shouldHandleUnicodeCharacters() {
            String unicodeEmail = "用户@大学.edu";
            String unicodeCode = "验证码123";
            
            assertDoesNotThrow(() -> {
                emailUtil.sendVerificationCodeEmail(unicodeEmail, unicodeCode, "register");
            }, "Should handle Unicode characters");
        }

        @Test
        @DisplayName("Should handle all purpose types correctly")
        void shouldHandleAllPurposeTypes() {
            String[] purposes = {"register", "login", "reset_password", "unknown"};
            
            for (String purpose : purposes) {
                ByteArrayOutputStream outContent = new ByteArrayOutputStream();
                PrintStream originalOut = System.out;
                System.setOut(new PrintStream(outContent));

                try {
                    emailUtil.sendVerificationCodeEmail("test@test.edu", "123456", purpose);
                    
                    String output = outContent.toString();
                    assertTrue(output.contains("test@test.edu"), 
                        "Should send email for purpose: " + purpose);
                    assertTrue(output.contains("123456"), 
                        "Should include code for purpose: " + purpose);
                } finally {
                    System.setOut(originalOut);
                }
            }
        }

        @Test
        @DisplayName("Should handle empty purpose string")
        void shouldHandleEmptyPurpose() {
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                emailUtil.sendVerificationCodeEmail("test@test.edu", "123456", "");
                
                String output = outContent.toString();
                assertTrue(output.contains("test@test.edu"), "Should handle empty purpose");
            } finally {
                System.setOut(originalOut);
            }
        }
    }

    @Nested
    @DisplayName("Format and Output Tests")
    class FormatTests {

        @Test
        @DisplayName("Should format console output correctly")
        void shouldFormatConsoleOutput() {
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outContent));

            try {
                emailUtil.sendVerificationCodeEmail("test@test.edu", "123456", "register");
                
                String output = outContent.toString();
                assertTrue(output.contains("╔"), "Should contain box drawing characters");
                assertTrue(output.contains("║"), "Should contain vertical box lines");
                assertTrue(output.contains("╚"), "Should contain bottom box corner");
                assertTrue(output.contains("To:"), "Should contain 'To:' label");
                assertTrue(output.contains("Subject:"), "Should contain 'Subject:' label");
                assertTrue(output.contains("Code:"), "Should contain 'Code:' label");
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        @DisplayName("Should not throw exceptions during email sending")
        void shouldNotThrowExceptions() {
            assertDoesNotThrow(() -> {
                emailUtil.sendVerificationCodeEmail("test@test.edu", "123456", "register");
                emailUtil.sendVerificationCodeEmail("test@test.edu", "123456", "login");
                emailUtil.sendVerificationCodeEmail("test@test.edu", "123456", "reset_password");
                emailUtil.sendVerificationCodeEmail("test@test.edu", "123456", "other");
            }, "Should never throw exceptions");
        }
    }
}
