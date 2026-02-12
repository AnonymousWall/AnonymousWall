package com.anonymous.wall.service;

import com.anonymous.wall.model.*;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.entity.EmailVerificationCode;
import com.anonymous.wall.repository.EmailVerificationCodeRepository;
import com.anonymous.wall.util.PasswordUtil;
import com.anonymous.wall.util.CodeGenerator;
import com.anonymous.wall.util.EmailUtil;
import com.anonymous.wall.util.EmailValidator;

import io.micronaut.transaction.annotation.Transactional;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Singleton;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Optional;

@Singleton
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int CODE_EXPIRATION_MINUTES = 15;

    @Inject
    private UserService userService;

    @Inject
    private EmailVerificationCodeRepository emailCodeRepository;

    /**
     * Send verification code to email
     */
    @Override
    @Retryable(attempts = "3", delay = "1000ms")
    public void sendEmailCode(SendEmailCodeRequest request) {
        log.debug("Generating verification code for email: {}, purpose: {}", request.getEmail(), request.getPurpose());

        String code = CodeGenerator.generateCode();
        String purpose = request.getPurpose().toString().toLowerCase();

        // Store code in database
        EmailVerificationCode emailCode = new EmailVerificationCode(
            request.getEmail(),
            code,
            purpose,
            OffsetDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES)
        );
        emailCodeRepository.save(emailCode);
        log.debug("Verification code stored in database, email: {}, purpose: {}", request.getEmail(), purpose);

        // Send email (fake for local testing)
        EmailUtil.sendVerificationCodeEmail(request.getEmail(), code, purpose);
        log.info("Verification code sent to email: {}, purpose: {}", request.getEmail(), purpose);
    }

    /**
     * Register new user with email and verification code
     */
    @Override
    @Transactional
    @Retryable(attempts = "3", delay = "500ms")
    public UserEntity registerWithEmail(RegisterEmailRequest request) {
        log.debug("Attempting to register user with email: {}", request.getEmail());

        // Validate school email
        if (!EmailValidator.isValidSchoolEmail(request.getEmail())) {
            log.warn("Registration failed - invalid school email: {}", request.getEmail());
            throw new IllegalArgumentException("Only school/educational email addresses are allowed for registration");
        }

        // Check if email already exists
        if (userService.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed - email already registered: {}", request.getEmail());
            throw new IllegalArgumentException("Email already registered");
        }

        // Verify the code
        log.debug("Verifying registration code for email: {}", request.getEmail());
        Optional<EmailVerificationCode> codeRecord = emailCodeRepository
            .findByEmailAndCodeAndPurpose(request.getEmail(), request.getCode(), "register");

        if (codeRecord.isEmpty()) {
            log.warn("Registration failed - invalid or missing verification code for email: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid or expired code");
        }

        EmailVerificationCode code = codeRecord.get();
        if (code.getExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("Registration failed - verification code expired for email: {}", request.getEmail());
            throw new IllegalArgumentException("Code has expired");
        }

        // Create new user
        log.debug("Creating new user account for email: {}", request.getEmail());
        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setSchoolDomain(EmailValidator.extractSchoolDomain(request.getEmail()));
        user.setVerified(true);
        user.setPasswordSet(false);
        user.setCreatedAt(OffsetDateTime.now());

        UserEntity savedUser = userService.save(user);
        log.debug("User account created, userId: {}, schoolDomain: {}", savedUser.getId(), savedUser.getSchoolDomain());

        // Clean up used code
        emailCodeRepository.deleteByEmail(request.getEmail());
        log.debug("Verification code cleaned up for email: {}", request.getEmail());

        log.info("User registered successfully: email={}, userId={}", request.getEmail(), savedUser.getId());
        return savedUser;
    }

    /**
     * Login user with email and verification code (password-less login)
     */
    @Override
    @Transactional
    @Retryable(attempts = "3", delay = "500ms")
    public UserEntity loginWithEmail(LoginEmailRequest request) {
        log.debug("Attempting email-based login for: {}", request.getEmail());

        // Verify the code
        log.debug("Verifying login code for email: {}", request.getEmail());
        Optional<EmailVerificationCode> codeRecord = emailCodeRepository
            .findByEmailAndCodeAndPurpose(request.getEmail(), request.getCode(), "login");

        if (codeRecord.isEmpty()) {
            log.warn("Email login failed - invalid or missing verification code for email: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid or expired code");
        }

        EmailVerificationCode code = codeRecord.get();
        if (code.getExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("Email login failed - verification code expired for email: {}", request.getEmail());
            throw new IllegalArgumentException("Code has expired");
        }

        // Find or create user
        log.debug("Looking up user account for email: {}", request.getEmail());
        Optional<UserEntity> userOpt = userService.findByEmail(request.getEmail());
        UserEntity user;

        if (userOpt.isEmpty()) {
            // Auto-create user if not exists
            log.debug("User not found, auto-creating account for email: {}", request.getEmail());
            user = new UserEntity();
            user.setEmail(request.getEmail());
            user.setVerified(true);
            user.setPasswordSet(false);
            user.setCreatedAt(OffsetDateTime.now());
            user = userService.save(user);
            log.debug("Auto-created user account, userId: {}", user.getId());
        } else {
            user = userOpt.get();
            log.debug("User account found, userId: {}", user.getId());
        }

        // Clean up used code
        emailCodeRepository.deleteByEmail(request.getEmail());
        log.debug("Verification code cleaned up for email: {}", request.getEmail());

        log.info("User logged in with email code successfully: email={}, userId={}", request.getEmail(), user.getId());
        return user;
    }

    /**
     * Login user with email and password
     */
    @Override
    public UserEntity loginWithPassword(PasswordLoginRequest request) {
        log.debug("Attempting password-based login for: {}", request.getEmail());

        Optional<UserEntity> userOpt = userService.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            log.warn("Password login failed - user not found for email: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid email or password");
        }

        UserEntity user = userOpt.get();

        if (!user.isPasswordSet() || user.getPasswordHash() == null) {
            log.warn("Password login failed - password not set for user: {}", request.getEmail());
            throw new IllegalArgumentException("Password not set for this account");
        }

        if (!PasswordUtil.checkPassword(request.getPassword(), user.getPasswordHash())) {
            log.warn("Password login failed - incorrect password for user: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid email or password");
        }

        log.info("User logged in with password successfully: email={}, userId={}", request.getEmail(), user.getId());
        return user;
    }

    /**
     * Set password for first time (user must be authenticated)
     */
    @Override
    public UserEntity setPassword(SetPasswordRequest request, UserEntity currentUser) {
        if (currentUser == null) {
            log.warn("Set password failed - user not authenticated");
            throw new IllegalArgumentException("User not authenticated");
        }

        log.debug("Setting password for first time, userId: {}", currentUser.getId());

        String hashedPassword = PasswordUtil.hashPassword(request.getPassword());
        currentUser.setPasswordHash(hashedPassword);
        currentUser.setPasswordSet(true);

        UserEntity updated = userService.update(currentUser);
        log.info("Password set successfully for user: email={}, userId={}", currentUser.getEmail(), currentUser.getId());
        return updated;
    }

    /**
     * Change password (user must be authenticated and provide old password)
     */
    @Override
    public UserEntity changePassword(ChangePasswordRequest request, UserEntity currentUser) {
        if (currentUser == null) {
            log.warn("Change password failed - user not authenticated");
            throw new IllegalArgumentException("User not authenticated");
        }

        log.debug("Attempting to change password, userId: {}", currentUser.getId());

        if (!currentUser.isPasswordSet() || currentUser.getPasswordHash() == null) {
            log.warn("Change password failed - password not previously set for user: {}", currentUser.getId());
            throw new IllegalArgumentException("Password not set for this account");
        }

        if (!PasswordUtil.checkPassword(request.getOldPassword(), currentUser.getPasswordHash())) {
            log.warn("Change password failed - old password incorrect for user: {}", currentUser.getId());
            throw new IllegalArgumentException("Old password is incorrect");
        }

        String hashedPassword = PasswordUtil.hashPassword(request.getNewPassword());
        currentUser.setPasswordHash(hashedPassword);

        UserEntity updated = userService.update(currentUser);
        log.info("Password changed successfully for user: email={}, userId={}", currentUser.getEmail(), currentUser.getId());
        return updated;
    }

    /**
     * Request password reset (forgot password flow)
     */
    @Override
    public UserEntity requestPasswordReset(PasswordResetRequestRequest request) {
        log.debug("Processing password reset request for email: {}", request.getEmail());

        Optional<UserEntity> userOpt = userService.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            // Don't reveal if email exists
            log.warn("Password reset request - email not found: {}", request.getEmail());
            throw new IllegalArgumentException("Email not found");
        }

        // Send reset code
        log.debug("Generating password reset code for email: {}", request.getEmail());
        String code = CodeGenerator.generateCode();
        EmailVerificationCode resetCode = new EmailVerificationCode(
            request.getEmail(),
            code,
            "reset_password",
            OffsetDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES)
        );
        emailCodeRepository.save(resetCode);
        log.debug("Password reset code saved in database for email: {}", request.getEmail());

        EmailUtil.sendVerificationCodeEmail(request.getEmail(), code, "reset_password");
        log.info("Password reset code sent to email: {}", request.getEmail());

        return userOpt.get();
    }

    /**
     * Reset password with verification code (forgot password flow)
     */
    @Override
    @Transactional
    @Retryable(attempts = "3", delay = "500ms")
    public UserEntity resetPassword(ResetPasswordRequest request) {
        log.debug("Processing password reset for email: {}", request.getEmail());

        Optional<UserEntity> userOpt = userService.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            log.warn("Password reset failed - user not found for email: {}", request.getEmail());
            throw new IllegalArgumentException("Email not found");
        }

        // Verify the code
        log.debug("Verifying password reset code for email: {}", request.getEmail());
        Optional<EmailVerificationCode> codeRecord = emailCodeRepository
            .findByEmailAndCodeAndPurpose(request.getEmail(), request.getCode(), "reset_password");

        if (codeRecord.isEmpty()) {
            log.warn("Password reset failed - invalid or missing reset code for email: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid or expired reset code");
        }

        EmailVerificationCode code = codeRecord.get();
        if (code.getExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("Password reset failed - reset code expired for email: {}", request.getEmail());
            throw new IllegalArgumentException("Reset code has expired");
        }

        // Update password
        log.debug("Updating password for user email: {}", request.getEmail());
        UserEntity user = userOpt.get();
        String hashedPassword = PasswordUtil.hashPassword(request.getNewPassword());
        user.setPasswordHash(hashedPassword);
        user.setPasswordSet(true);

        UserEntity updated = userService.update(user);
        log.debug("Password updated in database for email: {}", request.getEmail());

        // Clean up used code
        emailCodeRepository.deleteByEmail(request.getEmail());
        log.debug("Reset code cleaned up for email: {}", request.getEmail());

        log.info("Password reset successfully for user: email={}, userId={}", request.getEmail(), updated.getId());
        return updated;
    }
}