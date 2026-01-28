package com.anonymous.wall.service;

import com.anonymous.wall.model.*;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.entity.EmailVerificationCode;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.repository.EmailVerificationCodeRepository;
import com.anonymous.wall.util.PasswordUtil;
import com.anonymous.wall.util.CodeGenerator;
import com.anonymous.wall.util.EmailUtil;

import jakarta.inject.Singleton;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Optional;

@Singleton
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int CODE_EXPIRATION_MINUTES = 15;

    @Inject
    private UserRepository userRepository;

    @Inject
    private EmailVerificationCodeRepository emailCodeRepository;

    /**
     * Send verification code to email
     */
    @Override
    public void sendEmailCode(SendEmailCodeRequest request) {
        String code = CodeGenerator.generateCode();
        String purpose = request.getPurpose().toString().toLowerCase();

        // Store code in database
        EmailVerificationCode emailCode = new EmailVerificationCode(
            request.getEmail(),
            code,
            purpose,
            ZonedDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES)
        );
        emailCodeRepository.save(emailCode);

        // Send email (fake for local testing)
        EmailUtil.sendVerificationCodeEmail(request.getEmail(), code, purpose);
        log.info("Verification code sent to email: {}", request.getEmail());
    }

    /**
     * Register new user with email and verification code
     */
    @Override
    public UserEntity registerWithEmail(RegisterEmailRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Verify the code
        Optional<EmailVerificationCode> codeRecord = emailCodeRepository
            .findByEmailAndCodeAndPurpose(request.getEmail(), request.getCode(), "register");

        if (codeRecord.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired code");
        }

        EmailVerificationCode code = codeRecord.get();
        if (code.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new IllegalArgumentException("Code has expired");
        }

        // Create new user
        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setVerified(true);
        user.setPasswordSet(false);
        user.setCreatedAt(ZonedDateTime.now());

        UserEntity savedUser = userRepository.save(user);

        // Clean up used code
        emailCodeRepository.deleteByEmail(request.getEmail());

        log.info("User registered: {}", request.getEmail());
        return savedUser;
    }

    /**
     * Login user with email and verification code (password-less login)
     */
    @Override
    public UserEntity loginWithEmail(LoginEmailRequest request) {
        // Verify the code
        Optional<EmailVerificationCode> codeRecord = emailCodeRepository
            .findByEmailAndCodeAndPurpose(request.getEmail(), request.getCode(), "login");

        if (codeRecord.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired code");
        }

        EmailVerificationCode code = codeRecord.get();
        if (code.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new IllegalArgumentException("Code has expired");
        }

        // Find or create user
        Optional<UserEntity> userOpt = userRepository.findByEmail(request.getEmail());
        UserEntity user;

        if (userOpt.isEmpty()) {
            // Auto-create user if not exists
            user = new UserEntity();
            user.setEmail(request.getEmail());
            user.setVerified(true);
            user.setPasswordSet(false);
            user.setCreatedAt(ZonedDateTime.now());
            user = userRepository.save(user);
        } else {
            user = userOpt.get();
        }

        // Clean up used code
        emailCodeRepository.deleteByEmail(request.getEmail());

        log.info("User logged in with email code: {}", request.getEmail());
        return user;
    }

    /**
     * Login user with email and password
     */
    @Override
    public UserEntity loginWithPassword(PasswordLoginRequest request) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        UserEntity user = userOpt.get();

        if (!user.isPasswordSet() || user.getPasswordHash() == null) {
            throw new IllegalArgumentException("Password not set for this account");
        }

        if (!PasswordUtil.checkPassword(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        log.info("User logged in with password: {}", request.getEmail());
        return user;
    }

    /**
     * Set password for first time (user must be authenticated)
     */
    @Override
    public UserEntity setPassword(SetPasswordRequest request, UserEntity currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("User not authenticated");
        }

        String hashedPassword = PasswordUtil.hashPassword(request.getPassword());
        currentUser.setPasswordHash(hashedPassword);
        currentUser.setPasswordSet(true);

        UserEntity updated = userRepository.update(currentUser);
        log.info("Password set for user: {}", currentUser.getEmail());
        return updated;
    }

    /**
     * Change password (user must be authenticated and provide old password)
     */
    @Override
    public UserEntity changePassword(ChangePasswordRequest request, UserEntity currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("User not authenticated");
        }

        if (!currentUser.isPasswordSet() || currentUser.getPasswordHash() == null) {
            throw new IllegalArgumentException("Password not set for this account");
        }

        if (!PasswordUtil.checkPassword(request.getOldPassword(), currentUser.getPasswordHash())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        String hashedPassword = PasswordUtil.hashPassword(request.getNewPassword());
        currentUser.setPasswordHash(hashedPassword);

        UserEntity updated = userRepository.update(currentUser);
        log.info("Password changed for user: {}", currentUser.getEmail());
        return updated;
    }

    /**
     * Request password reset (forgot password flow)
     */
    @Override
    public UserEntity requestPasswordReset(PasswordResetRequestRequest request) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            // Don't reveal if email exists
            throw new IllegalArgumentException("Email not found");
        }

        // Send reset code
        String code = CodeGenerator.generateCode();
        EmailVerificationCode resetCode = new EmailVerificationCode(
            request.getEmail(),
            code,
            "reset_password",
            ZonedDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES)
        );
        emailCodeRepository.save(resetCode);

        EmailUtil.sendVerificationCodeEmail(request.getEmail(), code, "reset_password");
        log.info("Password reset code sent to: {}", request.getEmail());

        return userOpt.get();
    }

    /**
     * Reset password with verification code (forgot password flow)
     */
    @Override
    public UserEntity resetPassword(ResetPasswordRequest request) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Email not found");
        }

        // Verify the code
        Optional<EmailVerificationCode> codeRecord = emailCodeRepository
            .findByEmailAndCodeAndPurpose(request.getEmail(), request.getCode(), "reset_password");

        if (codeRecord.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired reset code");
        }

        EmailVerificationCode code = codeRecord.get();
        if (code.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new IllegalArgumentException("Reset code has expired");
        }

        // Update password
        UserEntity user = userOpt.get();
        String hashedPassword = PasswordUtil.hashPassword(request.getNewPassword());
        user.setPasswordHash(hashedPassword);
        user.setPasswordSet(true);

        UserEntity updated = userRepository.update(user);

        // Clean up used code
        emailCodeRepository.deleteByEmail(request.getEmail());

        log.info("Password reset for user: {}", request.getEmail());
        return updated;
    }
}