package com.anonymous.wall.service.impl;

import com.anonymous.wall.model.*;
import com.anonymous.wall.entity.RefreshToken;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.entity.EmailVerificationCode;
import com.anonymous.wall.service.JwtTokenService;
import com.anonymous.wall.service.base.AuthService;
import com.anonymous.wall.service.base.EmailVerificationCodeService;
import com.anonymous.wall.service.base.RefreshTokenService;
import com.anonymous.wall.service.base.UserService;
import com.anonymous.wall.util.PasswordUtil;
import com.anonymous.wall.util.CodeGenerator;
import com.anonymous.wall.util.EmailUtilInterface;
import com.anonymous.wall.util.EmailValidator;

import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int CODE_EXPIRATION_MINUTES = 15;

    @Inject
    private UserService userService;

    @Inject
    private EmailUtilInterface emailUtil;

    @Inject
    private EmailVerificationCodeService emailCodeService;

    @Inject
    private JwtTokenService jwtTokenService;

    @Inject
    private RefreshTokenService refreshTokenService;

    /**
     * Send verification code to email
     */
    @Override
    @Transactional
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
        emailCodeService.save(emailCode);
        log.debug("Verification code stored in database, email: {}, purpose: {}", request.getEmail(), purpose);

        // Send email (fake for local testing)
        emailUtil.sendVerificationCodeEmail(request.getEmail(), code, purpose);
        log.debug("Verification code sent to email: {}, purpose: {}", request.getEmail(), purpose);
    }

    /**
     * Register new user with email and verification code
     */
    @Override
    @Transactional
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
        Optional<EmailVerificationCode> codeRecord = emailCodeService
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
        emailCodeService.deleteByEmail(request.getEmail());
        log.debug("Verification code cleaned up for email: {}", request.getEmail());

        log.debug("User registered successfully: email={}, userId={}", request.getEmail(), savedUser.getId());
        return savedUser;
    }

    /**
     * Login user with email and verification code (password-less login)
     */
    @Override
    @Transactional
    public UserEntity loginWithEmail(LoginEmailRequest request) {
        log.debug("Attempting email-based login for: {}", request.getEmail());

        // Verify the code
        log.debug("Verifying login code for email: {}", request.getEmail());
        Optional<EmailVerificationCode> codeRecord = emailCodeService
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
            // Validate school email before auto-creating — same check as registerWithEmail()
            if (!EmailValidator.isValidSchoolEmail(request.getEmail())) {
                log.warn("Email login auto-create blocked - invalid school email: {}", request.getEmail());
                throw new IllegalArgumentException("Only school/educational email addresses are allowed");
            }

            // Auto-create user if not exists
            log.debug("User not found, auto-creating account for email: {}", request.getEmail());
            user = new UserEntity();
            user.setEmail(request.getEmail());
            user.setSchoolDomain(EmailValidator.extractSchoolDomain(request.getEmail()));
            user.setVerified(true);
            user.setPasswordSet(false);
            user.setCreatedAt(OffsetDateTime.now());
            user = userService.save(user);
            log.debug("Auto-created user account, userId: {}", user.getId());
        } else {
            user = userOpt.get();
            log.debug("User account found, userId: {}", user.getId());
            
            // Check if user is blocked
            if (user.isBlocked()) {
                log.warn("Email login failed - user account is blocked: email={}, userId={}", request.getEmail(), user.getId());
                throw new IllegalArgumentException("Access denied. Your account has been blocked.");
            }
        }

        // Clean up used code
        emailCodeService.deleteByEmail(request.getEmail());
        log.debug("Verification code cleaned up for email: {}", request.getEmail());

        log.debug("User logged in with email code successfully: email={}, userId={}", request.getEmail(), user.getId());
        return user;
    }

    /**
     * Login user with email and password
     */
    @Override
    @Transactional(readOnly = true)
    public UserEntity loginWithPassword(PasswordLoginRequest request) {
        log.debug("Attempting password-based login for: {}", request.getEmail());

        Optional<UserEntity> userOpt = userService.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            log.warn("Password login failed - user not found for email: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid email or password");
        }

        UserEntity user = userOpt.get();

        // Check if user is blocked
        if (user.isBlocked()) {
            log.warn("Password login failed - user account is blocked: email={}, userId={}", request.getEmail(), user.getId());
            throw new IllegalArgumentException("Access denied. Your account has been blocked.");
        }

        if (!user.isPasswordSet() || user.getPasswordHash() == null) {
            log.warn("Password login failed - password not set for user: {}", request.getEmail());
            throw new IllegalArgumentException("Password not set for this account");
        }

        if (!PasswordUtil.checkPassword(request.getPassword(), user.getPasswordHash())) {
            log.warn("Password login failed - incorrect password for user: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid email or password");
        }

        log.debug("User logged in with password successfully: email={}, userId={}", request.getEmail(), user.getId());
        return user;
    }

    /**
     * Set password for first time (user must be authenticated)
     */
    @Override
    @Transactional
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
        log.debug("Password set successfully for user: email={}, userId={}", currentUser.getEmail(), currentUser.getId());
        return updated;
    }

    /**
     * Change password (user must be authenticated and provide old password)
     */
    @Override
    @Transactional
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
        log.debug("Password changed successfully for user: email={}, userId={}", currentUser.getEmail(), currentUser.getId());
        return updated;
    }

    /**
     * Request password reset (forgot password flow)
     */
    @Override
    @Transactional
    public UserEntity requestPasswordReset(PasswordResetRequestRequest request) {
        log.debug("Processing password reset request for email: {}", request.getEmail());

        Optional<UserEntity> userOpt = userService.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            // Silently succeed to avoid email enumeration attacks
            log.warn("Password reset request - email not found, silently ignoring: {}", request.getEmail());
            return null;
        }

        UserEntity user = userOpt.get();
        
        // Check if user is blocked
        if (user.isBlocked()) {
            log.warn("Password reset request denied - user account is blocked: email={}, userId={}", request.getEmail(), user.getId());
            throw new IllegalArgumentException("Access denied. Your account has been blocked.");
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
        emailCodeService.save(resetCode);
        log.debug("Password reset code saved in database for email: {}", request.getEmail());

        emailUtil.sendVerificationCodeEmail(request.getEmail(), code, "reset_password");
        log.debug("Password reset code sent to email: {}", request.getEmail());

        return user;
    }

    /**
     * Reset password with verification code (forgot password flow)
     */
    @Override
    @Transactional
    public UserEntity resetPassword(ResetPasswordRequest request) {
        log.debug("Processing password reset for email: {}", request.getEmail());

        Optional<UserEntity> userOpt = userService.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            log.warn("Password reset failed - user not found for email: {}", request.getEmail());
            throw new IllegalArgumentException("Email not found");
        }

        UserEntity user = userOpt.get();
        
        // Check if user is blocked
        if (user.isBlocked()) {
            log.warn("Password reset denied - user account is blocked: email={}, userId={}", request.getEmail(), user.getId());
            throw new IllegalArgumentException("Access denied. Your account has been blocked.");
        }

        // Verify the code
        log.debug("Verifying password reset code for email: {}", request.getEmail());
        Optional<EmailVerificationCode> codeRecord = emailCodeService
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
        String hashedPassword = PasswordUtil.hashPassword(request.getNewPassword());
        user.setPasswordHash(hashedPassword);
        user.setPasswordSet(true);

        UserEntity updated = userService.update(user);
        log.debug("Password updated in database for email: {}", request.getEmail());

        // Clean up used code
        emailCodeService.deleteByEmail(request.getEmail());
        log.debug("Reset code cleaned up for email: {}", request.getEmail());

        log.debug("Password reset successfully for user: email={}, userId={}", request.getEmail(), updated.getId());
        return updated;
    }

    @Override
    @Transactional
    public String issueRefreshToken(UUID userId) {
        refreshTokenService.updateRevokedByUserId(userId, true);

        String rawRefreshToken = jwtTokenService.generateRefreshToken();
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUserId(userId);
        refreshTokenEntity.setTokenHash(jwtTokenService.hashToken(rawRefreshToken));
        refreshTokenEntity.setExpiresAt(OffsetDateTime.now().plusDays(30));
        refreshTokenService.save(refreshTokenEntity);

        return rawRefreshToken;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findValidRefreshToken(String rawRefreshToken) {
        String tokenHash = jwtTokenService.hashToken(rawRefreshToken);
        Optional<RefreshToken> stored = refreshTokenService.findByTokenHashAndRevokedFalse(tokenHash);

        if (stored.isEmpty() || stored.get().getExpiresAt().isBefore(OffsetDateTime.now())) {
            return Optional.empty();
        }

        return stored;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshTokenService.update(refreshToken);
    }

    @Override
    @Transactional
    public void revokeRefreshTokensForUser(UUID userId) {
        refreshTokenService.updateRevokedByUserId(userId, true);
    }
}
