package com.anonymous.wall.service.base;

import com.anonymous.wall.entity.RefreshToken;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.*;

import java.util.Optional;
import java.util.UUID;

public interface AuthService {
    void sendEmailCode(SendEmailCodeRequest request);
    UserEntity registerWithEmail(RegisterEmailRequest request);
    UserEntity loginWithEmail(LoginEmailRequest request);
    UserEntity loginWithPassword(PasswordLoginRequest request);
    UserEntity setPassword(SetPasswordRequest request, UserEntity currentUser);
    UserEntity changePassword(ChangePasswordRequest request, UserEntity currentUser);
    UserEntity requestPasswordReset(PasswordResetRequestRequest request);
    UserEntity resetPassword(ResetPasswordRequest request);
    String issueRefreshToken(UUID userId);
    Optional<RefreshToken> findValidRefreshToken(String rawRefreshToken);
    void revokeRefreshToken(RefreshToken refreshToken);
    void revokeRefreshTokensForUser(UUID userId);
}
