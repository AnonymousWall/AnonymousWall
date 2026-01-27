package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.*;

public interface AuthService {
    void sendEmailCode(SendEmailCodeRequest request);
    UserEntity verifyEmailCode(VerifyEmailCodeRequest request);
    void setPassword(SetPasswordRequest request, UserEntity currentUser);
    UserEntity loginWithPassword(PasswordLoginRequest request);
}
