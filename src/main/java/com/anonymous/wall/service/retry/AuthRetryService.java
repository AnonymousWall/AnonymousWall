package com.anonymous.wall.service.retry;

import com.anonymous.wall.entity.RefreshToken;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.*;
import com.anonymous.wall.service.base.AuthService;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Singleton;

import java.util.Optional;
import java.util.UUID;

/**
 * Auth retry wrapper.
 *
 * Contract:
 * - Owns retry semantics for auth flows.
 * - Delegates to {@link AuthService} for actual work so service methods stay clean.
 */
@Singleton
public class AuthRetryService {

    private final AuthService authService;

    public AuthRetryService(AuthService authService) {
        this.authService = authService;
    }

    @Retryable(attempts = "3", delay = "1000ms")
    public void sendEmailCode(SendEmailCodeRequest request) {
        authService.sendEmailCode(request);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public UserEntity registerWithEmail(RegisterEmailRequest request) {
        return authService.registerWithEmail(request);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public UserEntity loginWithEmail(LoginEmailRequest request) {
        return authService.loginWithEmail(request);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public UserEntity loginWithPassword(PasswordLoginRequest request) {
        return authService.loginWithPassword(request);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public UserEntity setPassword(SetPasswordRequest request, UserEntity currentUser) {
        return authService.setPassword(request, currentUser);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public UserEntity changePassword(ChangePasswordRequest request, UserEntity currentUser) {
        return authService.changePassword(request, currentUser);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public UserEntity requestPasswordReset(PasswordResetRequestRequest request) {
        return authService.requestPasswordReset(request);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public UserEntity resetPassword(ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public String issueRefreshToken(UUID userId) {
        return authService.issueRefreshToken(userId);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public Optional<RefreshToken> findValidRefreshToken(String rawRefreshToken) {
        return authService.findValidRefreshToken(rawRefreshToken);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public void revokeRefreshToken(RefreshToken refreshToken) {
        authService.revokeRefreshToken(refreshToken);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public void revokeRefreshTokensForUser(UUID userId) {
        authService.revokeRefreshTokensForUser(userId);
    }
}
