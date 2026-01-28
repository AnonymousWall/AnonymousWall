package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.*;

public interface AuthService {
    void sendEmailCode(SendEmailCodeRequest request);
    UserEntity registerWithEmail(RegisterEmailRequest request);
    UserEntity loginWithEmail(LoginEmailRequest request);
    UserEntity loginWithPassword(PasswordLoginRequest request);
    UserEntity setPassword(SetPasswordRequest request, UserEntity currentUser);
    UserEntity changePassword(ChangePasswordRequest request, UserEntity currentUser);
    UserEntity requestPasswordReset(PasswordResetRequestRequest request);
    UserEntity resetPassword(ResetPasswordRequest request);
}
