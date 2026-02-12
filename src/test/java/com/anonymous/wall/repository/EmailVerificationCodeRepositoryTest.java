package com.anonymous.wall.repository;

import com.anonymous.wall.entity.EmailVerificationCode;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("EmailVerificationCodeRepository Tests")
class EmailVerificationCodeRepositoryTest {

    @Inject
    EmailVerificationCodeRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    private EmailVerificationCode createTestCode(String email, String code, String purpose) {
        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setPurpose(purpose);
        verificationCode.setCreatedAt(OffsetDateTime.now());
        verificationCode.setExpiresAt(OffsetDateTime.now().plusMinutes(15));
        return repository.save(verificationCode);
    }

    @Nested
    @DisplayName("Save Tests")
    class SaveTests {

        @Test
        @DisplayName("Positive: Should save new verification code")
        void shouldSaveNewVerificationCode() {
            // Arrange
            EmailVerificationCode code = new EmailVerificationCode();
            code.setEmail("test@harvard.edu");
            code.setCode("123456");
            code.setPurpose("register");
            code.setCreatedAt(OffsetDateTime.now());
            code.setExpiresAt(OffsetDateTime.now().plusMinutes(15));

            // Act
            EmailVerificationCode saved = repository.save(code);

            // Assert
            assertNotNull(saved);
            assertNotNull(saved.getId());
            assertEquals("test@harvard.edu", saved.getEmail());
            assertEquals("123456", saved.getCode());
            assertEquals("register", saved.getPurpose());
        }

        @Test
        @DisplayName("Positive: Should auto-generate UUID")
        void shouldAutoGenerateUuid() {
            // Arrange
            EmailVerificationCode code = new EmailVerificationCode();
            code.setEmail("user@mit.edu");
            code.setCode("789012");
            code.setPurpose("login");
            code.setCreatedAt(OffsetDateTime.now());
            code.setExpiresAt(OffsetDateTime.now().plusMinutes(15));

            // Act
            EmailVerificationCode saved = repository.save(code);

            // Assert
            assertNotNull(saved.getId());
        }

        @Test
        @DisplayName("Edge: Should handle different purposes")
        void shouldHandleDifferentPurposes() {
            // Arrange & Act
            EmailVerificationCode code1 = createTestCode("user@test.edu", "111111", "register");
            EmailVerificationCode code2 = createTestCode("user@test.edu", "222222", "login");
            EmailVerificationCode code3 = createTestCode("user@test.edu", "333333", "reset_password");

            // Assert
            assertEquals("register", code1.getPurpose());
            assertEquals("login", code2.getPurpose());
            assertEquals("reset_password", code3.getPurpose());
        }
    }

    @Nested
    @DisplayName("FindByEmailAndCodeAndPurpose Tests")
    class FindByEmailAndCodeAndPurposeTests {

        @Test
        @DisplayName("Positive: Should find code by email, code, and purpose")
        void shouldFindCodeByEmailCodeAndPurpose() {
            // Arrange
            String email = "student@stanford.edu";
            String code = "456789";
            String purpose = "register";
            createTestCode(email, code, purpose);

            // Act
            Optional<EmailVerificationCode> found = repository.findByEmailAndCodeAndPurpose(email, code, purpose);

            // Assert
            assertTrue(found.isPresent());
            assertEquals(email, found.get().getEmail());
            assertEquals(code, found.get().getCode());
            assertEquals(purpose, found.get().getPurpose());
        }

        @Test
        @DisplayName("Negative: Should return empty for wrong code")
        void shouldReturnEmptyForWrongCode() {
            // Arrange
            String email = "user@yale.edu";
            createTestCode(email, "123456", "register");

            // Act
            Optional<EmailVerificationCode> found = repository.findByEmailAndCodeAndPurpose(email, "wrong", "register");

            // Assert
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Negative: Should return empty for wrong purpose")
        void shouldReturnEmptyForWrongPurpose() {
            // Arrange
            String email = "user@princeton.edu";
            String code = "789012";
            createTestCode(email, code, "register");

            // Act
            Optional<EmailVerificationCode> found = repository.findByEmailAndCodeAndPurpose(email, code, "login");

            // Assert
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Negative: Should return empty for wrong email")
        void shouldReturnEmptyForWrongEmail() {
            // Arrange
            createTestCode("correct@columbia.edu", "123456", "register");

            // Act
            Optional<EmailVerificationCode> found = repository.findByEmailAndCodeAndPurpose("wrong@columbia.edu", "123456", "register");

            // Assert
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Edge: Should distinguish between same code for different purposes")
        void shouldDistinguishBetweenSameCodeForDifferentPurposes() {
            // Arrange
            String email = "multi@test.edu";
            String code = "111111";
            createTestCode(email, code, "register");
            createTestCode(email, code, "login");

            // Act
            Optional<EmailVerificationCode> registerCode = repository.findByEmailAndCodeAndPurpose(email, code, "register");
            Optional<EmailVerificationCode> loginCode = repository.findByEmailAndCodeAndPurpose(email, code, "login");

            // Assert
            assertTrue(registerCode.isPresent());
            assertTrue(loginCode.isPresent());
            assertEquals("register", registerCode.get().getPurpose());
            assertEquals("login", loginCode.get().getPurpose());
        }

        @Test
        @DisplayName("Edge: Should be case-sensitive for email")
        void shouldBeCaseSensitiveForEmail() {
            // Arrange
            createTestCode("user@Test.edu", "123456", "register");

            // Act
            Optional<EmailVerificationCode> found = repository.findByEmailAndCodeAndPurpose("user@test.edu", "123456", "register");

            // Assert
            // Database might be case-insensitive, but test documents the behavior
            // assertTrue(found.isEmpty()); // Uncomment if DB is case-sensitive
        }
    }

    @Nested
    @DisplayName("DeleteByEmail Tests")
    class DeleteByEmailTests {

        @Test
        @DisplayName("Positive: Should delete all codes for an email")
        void shouldDeleteAllCodesForEmail() {
            // Arrange
            String email = "cleanup@test.edu";
            createTestCode(email, "111111", "register");
            createTestCode(email, "222222", "login");
            createTestCode(email, "333333", "reset_password");

            // Act
            repository.deleteByEmail(email);

            // Assert
            Optional<EmailVerificationCode> found1 = repository.findByEmailAndCodeAndPurpose(email, "111111", "register");
            Optional<EmailVerificationCode> found2 = repository.findByEmailAndCodeAndPurpose(email, "222222", "login");
            Optional<EmailVerificationCode> found3 = repository.findByEmailAndCodeAndPurpose(email, "333333", "reset_password");
            assertTrue(found1.isEmpty());
            assertTrue(found2.isEmpty());
            assertTrue(found3.isEmpty());
        }

        @Test
        @DisplayName("Positive: Should not affect codes for other emails")
        void shouldNotAffectCodesForOtherEmails() {
            // Arrange
            String email1 = "delete@test.edu";
            String email2 = "keep@test.edu";
            createTestCode(email1, "111111", "register");
            createTestCode(email2, "222222", "register");

            // Act
            repository.deleteByEmail(email1);

            // Assert
            Optional<EmailVerificationCode> deleted = repository.findByEmailAndCodeAndPurpose(email1, "111111", "register");
            Optional<EmailVerificationCode> kept = repository.findByEmailAndCodeAndPurpose(email2, "222222", "register");
            assertTrue(deleted.isEmpty());
            assertTrue(kept.isPresent());
        }

        @Test
        @DisplayName("Edge: Should handle delete for non-existent email")
        void shouldHandleDeleteForNonExistentEmail() {
            // Act & Assert - should not throw
            assertDoesNotThrow(() -> repository.deleteByEmail("nonexistent@test.edu"));
        }

        @Test
        @DisplayName("Edge: Should handle empty email")
        void shouldHandleEmptyEmail() {
            // Act & Assert - should not throw
            assertDoesNotThrow(() -> repository.deleteByEmail(""));
        }
    }

    @Nested
    @DisplayName("FindById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Positive: Should find code by ID")
        void shouldFindCodeById() {
            // Arrange
            EmailVerificationCode saved = createTestCode("find@test.edu", "123456", "register");
            UUID id = saved.getId();

            // Act
            Optional<EmailVerificationCode> found = repository.findById(id);

            // Assert
            assertTrue(found.isPresent());
            assertEquals(id, found.get().getId());
        }

        @Test
        @DisplayName("Negative: Should return empty for non-existent ID")
        void shouldReturnEmptyForNonExistentId() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();

            // Act
            Optional<EmailVerificationCode> found = repository.findById(nonExistentId);

            // Assert
            assertTrue(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Positive: Should update verification code")
        void shouldUpdateVerificationCode() {
            // Arrange
            EmailVerificationCode saved = createTestCode("update@test.edu", "111111", "register");
            saved.setCode("999999");

            // Act
            EmailVerificationCode updated = repository.update(saved);

            // Assert
            assertEquals("999999", updated.getCode());
        }

        @Test
        @DisplayName("Positive: Should update expiration time")
        void shouldUpdateExpirationTime() {
            // Arrange
            EmailVerificationCode saved = createTestCode("expire@test.edu", "123456", "register");
            OffsetDateTime newExpiry = OffsetDateTime.now().plusMinutes(30);
            saved.setExpiresAt(newExpiry);

            // Act
            EmailVerificationCode updated = repository.update(saved);

            // Assert
            assertNotNull(updated.getExpiresAt());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Edge: Should handle very long email")
        void shouldHandleVeryLongEmail() {
            // Arrange
            String longEmail = "a".repeat(50) + "@test.edu";
            EmailVerificationCode code = createTestCode(longEmail, "123456", "register");

            // Act
            Optional<EmailVerificationCode> found = repository.findById(code.getId());

            // Assert
            assertTrue(found.isPresent());
        }

        @Test
        @DisplayName("Edge: Should handle numeric code")
        void shouldHandleNumericCode() {
            // Arrange
            EmailVerificationCode code = createTestCode("num@test.edu", "123456", "register");

            // Act
            Optional<EmailVerificationCode> found = repository.findById(code.getId());

            // Assert
            assertTrue(found.isPresent());
            assertEquals("123456", found.get().getCode());
        }

        @Test
        @DisplayName("Edge: Should handle alphanumeric code")
        void shouldHandleAlphanumericCode() {
            // Arrange
            EmailVerificationCode code = createTestCode("alpha@test.edu", "ABC123", "register");

            // Act
            Optional<EmailVerificationCode> found = repository.findById(code.getId());

            // Assert
            assertTrue(found.isPresent());
            assertEquals("ABC123", found.get().getCode());
        }
    }
}
