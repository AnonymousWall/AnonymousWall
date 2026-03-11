package com.anonymous.wall.service;

import com.anonymous.wall.entity.RefreshToken;
import com.anonymous.wall.repository.RefreshTokenRepository;
import com.anonymous.wall.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("RefreshTokenServiceImpl Unit Tests")
class RefreshTokenServiceImplTest {

    private RefreshTokenServiceImpl service;
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        service = new RefreshTokenServiceImpl();
        try {
            var field = RefreshTokenServiceImpl.class.getDeclaredField("refreshTokenRepository");
            field.setAccessible(true);
            field.set(service, refreshTokenRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private RefreshToken buildToken(UUID userId, String tokenHash) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(tokenHash);
        token.setRevoked(false);
        return token;
    }

    // ─── updateRevokedByUserId ─────────────────────────────────────────────────

    @Nested
    @DisplayName("updateRevokedByUserId()")
    class UpdateRevokedByUserIdTests {

        @Test
        @DisplayName("Should delegate to repository with correct userId and revoked=true")
        void shouldDelegateRevokeTrue() {
            UUID userId = UUID.randomUUID();

            service.updateRevokedByUserId(userId, true);

            verify(refreshTokenRepository).updateRevokedByUserId(userId, true);
            verifyNoMoreInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Should delegate to repository with correct userId and revoked=false")
        void shouldDelegateRevokeFalse() {
            UUID userId = UUID.randomUUID();

            service.updateRevokedByUserId(userId, false);

            verify(refreshTokenRepository).updateRevokedByUserId(userId, false);
            verifyNoMoreInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Should not confuse userId and revoked — parameter order is preserved")
        void shouldNotConfuseParameters() {
            UUID userId = UUID.randomUUID();

            service.updateRevokedByUserId(userId, true);

            // Verify exact call — wrong arg order would be caught here
            verify(refreshTokenRepository).updateRevokedByUserId(userId, true);
            verify(refreshTokenRepository, never()).updateRevokedByUserId(userId, false);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            RuntimeException dbError = new RuntimeException("DB error");
            doThrow(dbError).when(refreshTokenRepository).updateRevokedByUserId(any(), anyBoolean());

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.updateRevokedByUserId(UUID.randomUUID(), true));
            assertSame(dbError, thrown);
        }
    }

    // ─── save ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("Should delegate to repository with the exact token passed in")
        void shouldDelegateToRepository() {
            RefreshToken token = buildToken(UUID.randomUUID(), "hash123");

            service.save(token);

            verify(refreshTokenRepository).save(token);
            verifyNoMoreInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Should pass token to repository unchanged — no mutation in service layer")
        void shouldPassTokenUnchanged() {
            UUID userId = UUID.randomUUID();
            RefreshToken token = buildToken(userId, "hash_abc");

            service.save(token);

            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            RefreshToken token = buildToken(UUID.randomUUID(), "hash123");
            RuntimeException dbError = new RuntimeException("DB error");
            doThrow(dbError).when(refreshTokenRepository).save(token);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.save(token));
            assertSame(dbError, thrown);
        }
    }

    // ─── findByTokenHashAndRevokedFalse ────────────────────────────────────────

    @Nested
    @DisplayName("findByTokenHashAndRevokedFalse()")
    class FindByTokenHashAndRevokedFalseTests {

        @Test
        @DisplayName("Should return present Optional when active token found")
        void shouldReturnPresentOptional() {
            RefreshToken token = buildToken(UUID.randomUUID(), "active_hash");
            when(refreshTokenRepository.findByTokenHashAndRevokedFalse("active_hash"))
                    .thenReturn(Optional.of(token));

            Optional<RefreshToken> result = service.findByTokenHashAndRevokedFalse("active_hash");

            assertTrue(result.isPresent());
            assertSame(token, result.get());
        }

        @Test
        @DisplayName("Should return empty Optional when no matching token found")
        void shouldReturnEmptyOptional() {
            when(refreshTokenRepository.findByTokenHashAndRevokedFalse("unknown_hash"))
                    .thenReturn(Optional.empty());

            Optional<RefreshToken> result = service.findByTokenHashAndRevokedFalse("unknown_hash");

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should return empty Optional when token exists but is revoked")
        void shouldReturnEmptyWhenRevoked() {
            // Repository method already filters revoked=true — so it returns empty
            when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any()))
                    .thenReturn(Optional.empty());

            Optional<RefreshToken> result = service.findByTokenHashAndRevokedFalse("revoked_hash");

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should pass tokenHash to repository unchanged")
        void shouldPassTokenHashUnchanged() {
            String hash = "exact_hash_value";
            when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any()))
                    .thenReturn(Optional.empty());

            service.findByTokenHashAndRevokedFalse(hash);

            verify(refreshTokenRepository).findByTokenHashAndRevokedFalse(hash);
            verifyNoMoreInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            RuntimeException dbError = new RuntimeException("DB error");
            when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any()))
                    .thenThrow(dbError);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.findByTokenHashAndRevokedFalse("some_hash"));
            assertSame(dbError, thrown);
        }
    }

    // ─── update ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("Should delegate to repository with the exact token passed in")
        void shouldDelegateToRepository() {
            RefreshToken token = buildToken(UUID.randomUUID(), "hash123");

            service.update(token);

            verify(refreshTokenRepository).update(token);
            verifyNoMoreInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Should pass token to repository unchanged — no mutation in service layer")
        void shouldPassTokenUnchanged() {
            RefreshToken token = buildToken(UUID.randomUUID(), "hash_xyz");
            token.setRevoked(true);

            service.update(token);

            verify(refreshTokenRepository).update(token);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            RefreshToken token = buildToken(UUID.randomUUID(), "hash123");
            RuntimeException dbError = new RuntimeException("DB error");
            doThrow(dbError).when(refreshTokenRepository).update(token);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.update(token));
            assertSame(dbError, thrown);
        }
    }
}
