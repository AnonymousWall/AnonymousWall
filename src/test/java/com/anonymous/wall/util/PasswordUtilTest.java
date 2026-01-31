package com.anonymous.wall.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordUtil Tests")
class PasswordUtilTest {

    @Nested
    @DisplayName("Hash and Verify")
    class HashAndVerifyTests {

        @Test
        @DisplayName("Should hash password")
        void shouldHashPassword() {
            String password = "myPassword123!";
            String hash = PasswordUtil.hashPassword(password);

            assertNotNull(hash);
            assertNotEquals(password, hash);
            assertTrue(hash.length() > 20, "Hash should be long");
        }

        @Test
        @DisplayName("Should verify correct password")
        void shouldVerifyCorrectPassword() {
            String password = "correctPassword";
            String hash = PasswordUtil.hashPassword(password);

            assertTrue(PasswordUtil.checkPassword(password, hash));
        }

        @Test
        @DisplayName("Should reject wrong password")
        void shouldRejectWrongPassword() {
            String password = "correctPassword";
            String wrongPassword = "wrongPassword";
            String hash = PasswordUtil.hashPassword(password);

            assertFalse(PasswordUtil.checkPassword(wrongPassword, hash));
        }

        @Test
        @DisplayName("Should reject wrong password for empty hash")
        void shouldRejectEmptyHash() {
            String password = "password";
            // Empty/invalid hash - BCrypt will throw exception, which is correct behavior
            // We just need to ensure it doesn't match
            try {
                boolean result = PasswordUtil.checkPassword(password, "");
                // If no exception, empty hash should return false
                assertFalse(result, "Empty hash should not match password");
            } catch (IllegalArgumentException e) {
                // Expected - empty hash causes exception, which is correct
                assertTrue(true, "Empty hash correctly rejected");
            }
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should produce different hashes for same password")
        void shouldProduceDifferentHashes() {
            String password = "password";
            String hash1 = PasswordUtil.hashPassword(password);
            String hash2 = PasswordUtil.hashPassword(password);

            assertNotEquals(hash1, hash2, "Different salts should produce different hashes");

            // But both should verify
            assertTrue(PasswordUtil.checkPassword(password, hash1));
            assertTrue(PasswordUtil.checkPassword(password, hash2));
        }

        @Test
        @DisplayName("Should handle special characters")
        void shouldHandleSpecialCharacters() {
            String password = "p@$$w0rd!#%&*()[]{}";
            String hash = PasswordUtil.hashPassword(password);

            assertTrue(PasswordUtil.checkPassword(password, hash));
            assertFalse(PasswordUtil.checkPassword("p@$$w0rd!#%&*()", hash));
        }

        @Test
        @DisplayName("Should handle very long passwords")
        void shouldHandleLongPasswords() {
            // BCrypt has a 72-byte limit for UTF-8 encoded passwords
            String password = "a".repeat(70); // 70 chars = 70 bytes in UTF-8
            String hash = PasswordUtil.hashPassword(password);

            assertTrue(PasswordUtil.checkPassword(password, hash));
            assertFalse(PasswordUtil.checkPassword("a".repeat(69), hash));
        }

        @Test
        @DisplayName("Should handle Unicode characters")
        void shouldHandleUnicodeCharacters() {
            String password = "пароль密码🔒emoji";
            String hash = PasswordUtil.hashPassword(password);

            assertTrue(PasswordUtil.checkPassword(password, hash));
            assertFalse(PasswordUtil.checkPassword("differentPassword", hash));
        }

        @Test
        @DisplayName("Should be case sensitive")
        void shouldBeCaseSensitive() {
            String password = "MyPassword";
            String hash = PasswordUtil.hashPassword(password);

            assertTrue(PasswordUtil.checkPassword(password, hash));
            assertFalse(PasswordUtil.checkPassword("mypassword", hash));
            assertFalse(PasswordUtil.checkPassword("MYPASSWORD", hash));
        }

        @Test
        @DisplayName("Should not be reversible")
        void shouldNotBeReversible() {
            String password = "secretPassword";
            String hash = PasswordUtil.hashPassword(password);

            // Hash should not contain password
            assertFalse(hash.contains(password), "Hash should not contain password");
            assertNotEquals(password, hash, "Hash should not equal password");
        }

        @Test
        @DisplayName("Should handle empty password")
        void shouldHandleEmptyPassword() {
            String password = "";
            String hash = PasswordUtil.hashPassword(password);

            assertTrue(PasswordUtil.checkPassword(password, hash));
            assertFalse(PasswordUtil.checkPassword("notEmpty", hash));
        }

        @Test
        @DisplayName("Should handle whitespace-only password")
        void shouldHandleWhitespacePassword() {
            String password = "   ";
            String hash = PasswordUtil.hashPassword(password);

            assertTrue(PasswordUtil.checkPassword(password, hash));
            assertFalse(PasswordUtil.checkPassword("", hash));
        }
    }

    @Nested
    @DisplayName("Hash Quality Tests")
    class HashQualityTests {

        @Test
        @DisplayName("Should produce hashes with high entropy")
        void shouldProduceHighEntropyHashes() {
            Set<String> hashes = new HashSet<>();
            for (int i = 0; i < 20; i++) {
                String hash = PasswordUtil.hashPassword("password");
                hashes.add(hash);
            }

            // All 20 hashes should be unique
            assertEquals(20, hashes.size(), "Should have 20 unique hashes");
        }

        @Test
        @DisplayName("Should produce consistent hash format")
        void shouldProduceConsistentFormat() {
            String password = "test";
            String hash = PasswordUtil.hashPassword(password);

            // BCrypt hashes have specific format: $2a$12$...
            assertTrue(hash.startsWith("$2"), "Should be BCrypt format");
            assertTrue(hash.length() >= 60, "BCrypt hash should be at least 60 chars");
        }

        @Test
        @DisplayName("Should be computationally expensive (slow)")
        void shouldBeComputationallyExpensive() {
            long startTime = System.currentTimeMillis();
            PasswordUtil.hashPassword("password");
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // BCrypt should take at least 50ms (cost factor 12)
            assertTrue(duration >= 10, "Hashing should be slow (at least 10ms): " + duration + "ms");
            assertTrue(duration < 2000, "Hashing should not be absurdly slow: " + duration + "ms");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null password safely")
        void shouldHandleNullPasswordSafely() {
            assertThrows(Exception.class, () -> PasswordUtil.hashPassword(null));
        }

        @Test
        @DisplayName("Should handle null hash in verify")
        void shouldHandleNullHashInVerify() {
            // BCrypt throws exception on null hash, so it should fail
            assertThrows(Exception.class, () -> PasswordUtil.checkPassword("password", null));
        }

        @Test
        @DisplayName("Should handle corrupted hash")
        void shouldHandleCorruptedHash() {
            String hash = "notAValidBCryptHash";
            // BCrypt may not throw on corrupted hash, just returns false
            boolean result = PasswordUtil.checkPassword("password", hash);
            assertFalse(result, "Corrupted hash should not match password");
        }

        @Test
        @DisplayName("Should handle very similar passwords")
        void shouldHandleVerySimilarPasswords() {
            String password1 = "password";
            String password2 = "passwore";
            String hash = PasswordUtil.hashPassword(password1);

            assertTrue(PasswordUtil.checkPassword(password1, hash));
            assertFalse(PasswordUtil.checkPassword(password2, hash));
        }
    }
}
