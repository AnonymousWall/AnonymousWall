package com.anonymous.wall.controller;

import com.anonymous.wall.entity.RefreshToken;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.mapper.UserMapper;
import com.anonymous.wall.model.*;
import com.anonymous.wall.service.retry.AuthRetryService;
import com.anonymous.wall.service.JwtTokenService;
import com.anonymous.wall.service.retry.UserRetryService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

@Controller("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Inject
    private AuthRetryService authRetryService;

    @Inject
    private UserRetryService userRetryService;

    @Inject
    private UserMapper userMapper;

    @Inject
    private JwtTokenService jwtTokenService;

    /**
     * POST /auth/email/send-code
     * Send verification code to email
     */
    @Post("/email/send-code")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Object> sendEmailCode(@Body SendEmailCodeRequest request) {
        try {
            log.info("POST /auth/email/send-code - Sending verification code, email={}, purpose={}", request.getEmail(), request.getPurpose());

            // Validate email
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                log.warn("POST /auth/email/send-code - Invalid email provided");
                return HttpResponse.badRequest(error("Invalid email"));
            }

            // Check email exists for login/reset
            Optional<UserEntity> userOpt = userRetryService.findByEmail(request.getEmail());

            if (request.getPurpose() == SendEmailCodeRequestPurpose.REGISTER) {
                if (userOpt.isPresent()) {
                    log.warn("POST /auth/email/send-code - Email already registered: {}", request.getEmail());
                    return HttpResponse.status(io.micronaut.http.HttpStatus.CONFLICT);
                }
            } else if (request.getPurpose() == SendEmailCodeRequestPurpose.LOGIN ||
                       request.getPurpose() == SendEmailCodeRequestPurpose.RESET_PASSWORD) {
                if (userOpt.isEmpty()) {
                    log.warn("POST /auth/email/send-code - Email not found: {}", request.getEmail());
                    return HttpResponse.badRequest(error("Email not found"));
                }
            }

            authRetryService.sendEmailCode(request);
            log.info("POST /auth/email/send-code - Verification code sent successfully to email: {}", request.getEmail());
            return HttpResponse.ok(new MessageResponse("Verification code sent to email"));
        } catch (Exception e) {
            log.error("POST /auth/email/send-code - Error sending email code", e);
            return HttpResponse.badRequest(error(e.getMessage()));
        }
    }

    /**
     * POST /auth/register/email
     * Register new account with email and verification code
     */
    @Post("/register/email")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Object> registerWithEmail(@Body RegisterEmailRequest request) {
        try {
            log.info("POST /auth/register/email - Registering new user with email: {}", request.getEmail());

            UserEntity user = authRetryService.registerWithEmail(request);
            String accessToken = jwtTokenService.generateToken(user);
            String rawRefreshToken = authRetryService.issueRefreshToken(user.getId());

            log.info("POST /auth/register/email - User registered successfully, userId={}", user.getId());
            return HttpResponse.created(success(
                userMapper.toDTO(user),
                accessToken,
                rawRefreshToken
            ));
        } catch (IllegalArgumentException e) {
            log.warn("POST /auth/register/email - Registration failed: {}", e.getMessage());
            if (e.getMessage().contains("already registered")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.CONFLICT);
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /auth/register/email - Error registering user", e);
            return HttpResponse.badRequest(error("Registration failed"));
        }
    }

    /**
     * POST /auth/login/email
     * Login with email and verification code (password-less)
     */
    @Post("/login/email")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Object> loginWithEmail(@Body LoginEmailRequest request) {
        try {
            log.info("POST /auth/login/email - Login attempt with email: {}", request.getEmail());

            UserEntity user = authRetryService.loginWithEmail(request);
            String accessToken = jwtTokenService.generateToken(user);
            String rawRefreshToken = authRetryService.issueRefreshToken(user.getId());

            log.info("POST /auth/login/email - User logged in successfully, userId={}", user.getId());
            return HttpResponse.ok(success(
                userMapper.toDTO(user),
                accessToken,
                rawRefreshToken
            ));
        } catch (IllegalArgumentException e) {
            log.warn("POST /auth/login/email - Login failed: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /auth/login/email - Error logging in with email", e);
            return HttpResponse.badRequest(error("Authentication failed"));
        }
    }

    /**
     * POST /auth/login/password
     * Login with email and password
     */
    @Post("/login/password")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Object> loginWithPassword(@Body PasswordLoginRequest request) {
        try {
            log.info("POST /auth/login/password - Login attempt with email: {}", request.getEmail());

            UserEntity user = authRetryService.loginWithPassword(request);
            String accessToken = jwtTokenService.generateToken(user);
            String rawRefreshToken = authRetryService.issueRefreshToken(user.getId());

            log.info("POST /auth/login/password - User logged in successfully, userId={}", user.getId());
            return HttpResponse.ok(success(
                userMapper.toDTO(user),
                accessToken,
                rawRefreshToken
            ));
        } catch (IllegalArgumentException e) {
            log.warn("POST /auth/login/password - Login failed: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /auth/login/password - Error logging in with password", e);
            return HttpResponse.badRequest(error("Authentication failed"));
        }
    }

    /**
     * POST /auth/password/set
     * Set password for the first time (requires authentication)
     */
    @Post("/password/set")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> setPassword(@Body SetPasswordRequest request,
                                           io.micronaut.http.HttpRequest<?> httpRequest) {
        try {
            // Extract user ID from JWT Principal (secure source of truth)
            Optional<java.security.Principal> principalOpt = httpRequest.getUserPrincipal();
            if (principalOpt.isEmpty()) {
                log.warn("POST /auth/password/set - User not authenticated");
                return HttpResponse.badRequest(error("User not authenticated"));
            }

            UUID userId = UUID.fromString(principalOpt.get().getName());
            log.info("POST /auth/password/set - Setting password for user: {}", userId);

            Optional<UserEntity> userOpt = userRetryService.findById(userId);

            if (userOpt.isEmpty()) {
                log.warn("POST /auth/password/set - User not found: {}", userId);
                return HttpResponse.badRequest(error("User not found"));
            }

            UserEntity user = authRetryService.setPassword(request, userOpt.get());
            if (user == null) {
                log.warn("POST /auth/password/set - Failed to set password for user: {}", userId);
                return HttpResponse.badRequest(error("Failed to set password"));
            }

            log.info("POST /auth/password/set - Password set successfully for user: {}", userId);
            return HttpResponse.ok(userMapper.toDTO(user));
        } catch (IllegalArgumentException e) {
            log.warn("POST /auth/password/set - Invalid request: {}", e.getMessage());
            return HttpResponse.badRequest(error("Invalid request: " + e.getMessage()));
        } catch (Exception e) {
            log.error("POST /auth/password/set - Error setting password", e);
            return HttpResponse.badRequest(error(e.getMessage()));
        }
    }

    /**
     * POST /auth/password/change
     * Change password (requires authentication and old password)
     */
    @Post("/password/change")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> changePassword(@Body ChangePasswordRequest request,
                                              io.micronaut.http.HttpRequest<?> httpRequest) {
        try {
            // Extract user ID from JWT Principal (secure source of truth)
            Optional<java.security.Principal> principalOpt = httpRequest.getUserPrincipal();
            if (principalOpt.isEmpty()) {
                log.warn("POST /auth/password/change - User not authenticated");
                return HttpResponse.badRequest(error("User not authenticated"));
            }

            UUID userId = UUID.fromString(principalOpt.get().getName());
            log.info("POST /auth/password/change - Changing password for user: {}", userId);

            Optional<UserEntity> userOpt = userRetryService.findById(userId);

            if (userOpt.isEmpty()) {
                log.warn("POST /auth/password/change - User not found: {}", userId);
                return HttpResponse.badRequest(error("User not found"));
            }

            UserEntity user = authRetryService.changePassword(request, userOpt.get());
            if (user == null) {
                log.warn("POST /auth/password/change - Failed to change password for user: {}", userId);
                return HttpResponse.badRequest(error("Failed to change password"));
            }

            log.info("POST /auth/password/change - Password changed successfully for user: {}", userId);
            return HttpResponse.ok(userMapper.toDTO(user));
        } catch (IllegalArgumentException e) {
            log.warn("POST /auth/password/change - Invalid request: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /auth/password/change - Error changing password", e);
            return HttpResponse.badRequest(error("Password change failed"));
        }
    }

    /**
     * POST /auth/password/reset-request
     * Request password reset (forgot password)
     */
    @Post("/password/reset-request")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Object> resetPasswordRequest(@Body PasswordResetRequestRequest request) {
        try {
            log.info("POST /auth/password/reset-request - Password reset request for email: {}", request.getEmail());

            authRetryService.requestPasswordReset(request);

            log.info("POST /auth/password/reset-request - Password reset code sent successfully to email: {}", request.getEmail());
            return HttpResponse.ok(new MessageResponse("Password reset code sent to email"));
        } catch (IllegalArgumentException e) {
            log.warn("POST /auth/password/reset-request - Invalid request: {}", e.getMessage());
            return HttpResponse.notFound(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /auth/password/reset-request - Error requesting password reset", e);
            return HttpResponse.badRequest(error("Failed to process request"));
        }
    }

    /**
     * POST /auth/password/reset
     * Reset password with verification code
     */
    @Post("/password/reset")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Object> resetPassword(@Body ResetPasswordRequest request) {
        try {
            log.info("POST /auth/password/reset - Resetting password");

            UserEntity user = authRetryService.resetPassword(request);
            String accessToken = jwtTokenService.generateToken(user);
            String rawRefreshToken = authRetryService.issueRefreshToken(user.getId());

            log.info("POST /auth/password/reset - Password reset successfully, userId={}", user.getId());
            return HttpResponse.ok(success(
                userMapper.toDTO(user),
                accessToken,
                rawRefreshToken
            ));
        } catch (IllegalArgumentException e) {
            log.warn("POST /auth/password/reset - Invalid request: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /auth/password/reset - Error resetting password", e);
            return HttpResponse.badRequest(error("Password reset failed"));
        }
    }

    // -------- Helper Methods --------

    private ErrorResponse error(String message) {
        return new ErrorResponse(message);
    }

    private AuthSuccessResponse success(UserDTO user, String accessToken, String refreshToken) {
        return new AuthSuccessResponse(accessToken, refreshToken, user);
    }

    /**
     * POST /auth/refresh
     * Exchange a refresh token for a new token pair (token rotation)
     */
    @Post("/refresh")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<?> refresh(@Body RefreshRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            return HttpResponse.badRequest(error("Refresh token is required"));
        }

        Optional<RefreshToken> stored = authRetryService.findValidRefreshToken(request.getRefreshToken());
        if (stored.isEmpty()) {
            return HttpResponse.unauthorized();
        }

        RefreshToken existing = stored.get();
        Optional<UserEntity> userOpt = userRetryService.findById(existing.getUserId());
        if (userOpt.isEmpty()) {
            return HttpResponse.unauthorized();
        }

        UserEntity user = userOpt.get();
        if (user.isBlocked()) {
            return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN)
                    .body(error("Access denied. Your account has been blocked."));
        }

        // Rotate — revoke old token, issue new pair
        authRetryService.revokeRefreshToken(existing);

        String newAccessToken = jwtTokenService.generateToken(user);
        String newRawRefreshToken = authRetryService.issueRefreshToken(user.getId());

        log.info("POST /auth/refresh - Refresh token rotated for userId={}", user.getId());

        return HttpResponse.ok(new AuthSuccessResponse(newAccessToken, newRawRefreshToken, null));
    }

    /**
     * POST /auth/logout
     * Revoke all refresh tokens for the authenticated user
     */
    @Post("/logout")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<?> logout(io.micronaut.http.HttpRequest<?> httpRequest) {
        Optional<java.security.Principal> principalOpt = httpRequest.getUserPrincipal();
        if (principalOpt.isEmpty()) {
            return HttpResponse.unauthorized();
        }

        UUID userId = UUID.fromString(principalOpt.get().getName());
        authRetryService.revokeRefreshTokensForUser(userId);

        log.info("POST /auth/logout - Refresh tokens revoked for userId={}", userId);
        return HttpResponse.ok(new MessageResponse("Logged out successfully"));
    }

    // -------- Response DTOs --------

    @Serdeable
    public static class ErrorResponse {
        private String error;

        public ErrorResponse() {}
        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    @Serdeable
    public static class MessageResponse {
        private String message;

        public MessageResponse() {}
        public MessageResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @Serdeable
    public static class AuthSuccessResponse {
        private String accessToken;
        private String refreshToken;
        private UserDTO user;

        public AuthSuccessResponse() {}
        public AuthSuccessResponse(String accessToken, String refreshToken, UserDTO user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public UserDTO getUser() {
            return user;
        }

        public void setUser(UserDTO user) {
            this.user = user;
        }
    }
}
