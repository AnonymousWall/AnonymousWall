package com.anonymous.wall.controller;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.mapper.UserMapper;
import com.anonymous.wall.model.*;
import com.anonymous.wall.service.AuthService;
import com.anonymous.wall.service.JwtTokenService;
import com.anonymous.wall.service.UserService;
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
    private AuthService authService;

    @Inject
    private UserService userService;

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
            Optional<UserEntity> userOpt = userService.findByEmail(request.getEmail());

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

            authService.sendEmailCode(request);
            log.info("POST /auth/email/send-code - Verification code sent successfully to email: {}", request.getEmail());
            return HttpResponse.ok(new MessageResponse("Verification code sent to email"));
        } catch (Exception e) {
            log.error("POST /auth/email/send-code - Error sending email code", e);
            return HttpResponse.serverError(error("Failed to send verification code"));
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

            UserEntity user = authService.registerWithEmail(request);
            String token = jwtTokenService.generateToken(user);

            log.info("POST /auth/register/email - User registered successfully, userId={}", user.getId());
            return HttpResponse.created(success(
                userMapper.toDTO(user),
                token
            ));
        } catch (IllegalArgumentException e) {
            log.warn("POST /auth/register/email - Registration failed: {}", e.getMessage());
            if (e.getMessage().contains("already registered")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.CONFLICT);
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /auth/register/email - Error registering user", e);
            return HttpResponse.serverError(error("Registration failed"));
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

            UserEntity user = authService.loginWithEmail(request);
            String token = jwtTokenService.generateToken(user);

            log.info("POST /auth/login/email - User logged in successfully, userId={}", user.getId());
            return HttpResponse.ok(success(
                userMapper.toDTO(user),
                token
            ));
        } catch (IllegalArgumentException e) {
            log.warn("POST /auth/login/email - Login failed: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /auth/login/email - Error logging in with email", e);
            return HttpResponse.serverError(error("Authentication failed"));
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

            UserEntity user = authService.loginWithPassword(request);
            String token = jwtTokenService.generateToken(user);

            log.info("POST /auth/login/password - User logged in successfully, userId={}", user.getId());
            return HttpResponse.ok(success(
                userMapper.toDTO(user),
                token
            ));
        } catch (IllegalArgumentException e) {
            log.warn("POST /auth/login/password - Login failed: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /auth/login/password - Error logging in with password", e);
            return HttpResponse.serverError(error("Authentication failed"));
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

            Optional<UserEntity> userOpt = userService.findById(userId);

            if (userOpt.isEmpty()) {
                log.warn("POST /auth/password/set - User not found: {}", userId);
                return HttpResponse.badRequest(error("User not found"));
            }

            UserEntity user = authService.setPassword(request, userOpt.get());
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
            return HttpResponse.serverError(error("Failed to set password"));
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

            Optional<UserEntity> userOpt = userService.findById(userId);

            if (userOpt.isEmpty()) {
                log.warn("POST /auth/password/change - User not found: {}", userId);
                return HttpResponse.badRequest(error("User not found"));
            }

            UserEntity user = authService.changePassword(request, userOpt.get());
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
            return HttpResponse.serverError(error("Password change failed"));
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

            authService.requestPasswordReset(request);

            log.info("POST /auth/password/reset-request - Password reset code sent successfully to email: {}", request.getEmail());
            return HttpResponse.ok(new MessageResponse("Password reset code sent to email"));
        } catch (IllegalArgumentException e) {
            log.warn("POST /auth/password/reset-request - Invalid request: {}", e.getMessage());
            return HttpResponse.notFound(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /auth/password/reset-request - Error requesting password reset", e);
            return HttpResponse.serverError(error("Failed to process request"));
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

            UserEntity user = authService.resetPassword(request);
            String token = jwtTokenService.generateToken(user);

            log.info("POST /auth/password/reset - Password reset successfully, userId={}", user.getId());
            return HttpResponse.ok(success(
                userMapper.toDTO(user),
                token
            ));
        } catch (IllegalArgumentException e) {
            log.warn("POST /auth/password/reset - Invalid request: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /auth/password/reset - Error resetting password", e);
            return HttpResponse.serverError(error("Password reset failed"));
        }
    }

    // -------- Helper Methods --------

    private ErrorResponse error(String message) {
        return new ErrorResponse(message);
    }

    private AuthSuccessResponse success(UserDTO user, String token) {
        return new AuthSuccessResponse(token, user);
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
        private UserDTO user;

        public AuthSuccessResponse() {}
        public AuthSuccessResponse(String accessToken, UserDTO user) {
            this.accessToken = accessToken;
            this.user = user;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public UserDTO getUser() {
            return user;
        }

        public void setUser(UserDTO user) {
            this.user = user;
        }
    }
}