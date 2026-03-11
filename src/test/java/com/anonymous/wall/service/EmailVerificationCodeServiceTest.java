package com.anonymous.wall.service;

import com.anonymous.wall.entity.EmailVerificationCode;
import com.anonymous.wall.repository.EmailVerificationCodeRepository;
import com.anonymous.wall.service.impl.EmailVerificationCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("EmailVerificationCodeService Unit Tests")
class EmailVerificationCodeServiceTest {

    private EmailVerificationCodeServiceImpl service;
    private EmailVerificationCodeRepository emailCodeRepository;

    @BeforeEach
    void setUp() {
        emailCodeRepository = mock(EmailVerificationCodeRepository.class);
        service = new EmailVerificationCodeServiceImpl();

        try {
            var field = EmailVerificationCodeServiceImpl.class.getDeclaredField("emailCodeRepository");
            field.setAccessible(true);
            field.set(service, emailCodeRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private EmailVerificationCode buildCode(String email, String code, String purpose) {
        return new EmailVerificationCode(email, code, purpose, OffsetDateTime.now().plusMinutes(15));
    }

    // ─── save ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("Should delegate to repository and return saved entity")
        void shouldDelegateAndReturnSavedEntity() {
            EmailVerificationCode input = buildCode("user@harvard.edu", "123456", "register");
            EmailVerificationCode saved = buildCode("user@harvard.edu", "123456", "register");
            when(emailCodeRepository.save(input)).thenReturn(saved);

            EmailVerificationCode result = service.save(input);

            assertSame(saved, result);
            verify(emailCodeRepository).save(input);
        }

        @Test
        @DisplayName("Should pass entity to repository unchanged")
        void shouldPassEntityUnchanged() {
            EmailVerificationCode input = buildCode("user@harvard.edu", "654321", "login");
            when(emailCodeRepository.save(any())).thenReturn(input);

            service.save(input);

            verify(emailCodeRepository).save(input);
            verifyNoMoreInteractions(emailCodeRepository);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            EmailVerificationCode input = buildCode("user@harvard.edu", "123456", "register");
            RuntimeException dbError = new RuntimeException("DB error");
            when(emailCodeRepository.save(input)).thenThrow(dbError);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.save(input));
            assertSame(dbError, thrown);
        }
    }

    // ─── findByEmailAndCodeAndPurpose ──────────────────────────────────────────

    @Nested
    @DisplayName("findByEmailAndCodeAndPurpose()")
    class FindByEmailAndCodeAndPurpose {

        @Test
        @DisplayName("Should return present Optional when code exists")
        void shouldReturnPresentOptionalWhenFound() {
            EmailVerificationCode stored = buildCode("user@harvard.edu", "123456", "register");
            when(emailCodeRepository.findByEmailAndCodeAndPurpose("user@harvard.edu", "123456", "register"))
                    .thenReturn(Optional.of(stored));

            Optional<EmailVerificationCode> result =
                    service.findByEmailAndCodeAndPurpose("user@harvard.edu", "123456", "register");

            assertTrue(result.isPresent());
            assertSame(stored, result.get());
        }

        @Test
        @DisplayName("Should return empty Optional when code does not exist")
        void shouldReturnEmptyOptionalWhenNotFound() {
            when(emailCodeRepository.findByEmailAndCodeAndPurpose("user@harvard.edu", "wrong", "register"))
                    .thenReturn(Optional.empty());

            Optional<EmailVerificationCode> result =
                    service.findByEmailAndCodeAndPurpose("user@harvard.edu", "wrong", "register");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should pass all three parameters to repository unchanged")
        void shouldPassAllParametersToRepository() {
            when(emailCodeRepository.findByEmailAndCodeAndPurpose(any(), any(), any()))
                    .thenReturn(Optional.empty());

            service.findByEmailAndCodeAndPurpose("user@harvard.edu", "123456", "reset_password");

            verify(emailCodeRepository).findByEmailAndCodeAndPurpose(
                    "user@harvard.edu", "123456", "reset_password");
            verifyNoMoreInteractions(emailCodeRepository);
        }

        @Test
        @DisplayName("Should not find login code when searching for register purpose")
        void shouldNotMixUpPurposes() {
            when(emailCodeRepository.findByEmailAndCodeAndPurpose("user@harvard.edu", "123456", "register"))
                    .thenReturn(Optional.empty());

            Optional<EmailVerificationCode> result =
                    service.findByEmailAndCodeAndPurpose("user@harvard.edu", "123456", "register");

            assertTrue(result.isEmpty());
            verify(emailCodeRepository).findByEmailAndCodeAndPurpose(
                    "user@harvard.edu", "123456", "register");
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            RuntimeException dbError = new RuntimeException("DB error");
            when(emailCodeRepository.findByEmailAndCodeAndPurpose(any(), any(), any()))
                    .thenThrow(dbError);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.findByEmailAndCodeAndPurpose("user@harvard.edu", "123456", "login"));
            assertSame(dbError, thrown);
        }
    }

    // ─── deleteByEmail ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteByEmail()")
    class DeleteByEmail {

        @Test
        @DisplayName("Should delegate delete to repository")
        void shouldDelegateDeleteToRepository() {
            doNothing().when(emailCodeRepository).deleteByEmail("user@harvard.edu");

            assertDoesNotThrow(() -> service.deleteByEmail("user@harvard.edu"));

            verify(emailCodeRepository).deleteByEmail("user@harvard.edu");
            verifyNoMoreInteractions(emailCodeRepository);
        }

        @Test
        @DisplayName("Should pass email to repository unchanged")
        void shouldPassEmailUnchanged() {
            String email = "specific@harvard.edu";

            service.deleteByEmail(email);

            verify(emailCodeRepository).deleteByEmail(email);
        }

        @Test
        @DisplayName("Should not throw when no codes exist for email — repository handles gracefully")
        void shouldNotThrowWhenNoCodesExist() {
            doNothing().when(emailCodeRepository).deleteByEmail("nonexistent@harvard.edu");

            assertDoesNotThrow(() -> service.deleteByEmail("nonexistent@harvard.edu"));
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            RuntimeException dbError = new RuntimeException("DB error");
            doThrow(dbError).when(emailCodeRepository).deleteByEmail("user@harvard.edu");

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.deleteByEmail("user@harvard.edu"));
            assertSame(dbError, thrown);
        }
    }
}
