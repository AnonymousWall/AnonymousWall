package com.anonymous.wall.controller;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.mapper.UserMapper;
import com.anonymous.wall.model.*;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.AuthService;
import com.anonymous.wall.service.JwtTokenService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
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
    private UserRepository userRepository;

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
            // Validate email
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return HttpResponse.badRequest(error("Invalid email"));
            }

            // Check email exists for login/reset
            Optional<UserEntity> userOpt = userRepository.findByEmail(request.getEmail());

            if (request.getPurpose() == SendEmailCodeRequestPurpose.REGISTER) {
                if (userOpt.isPresent()) {
                    return HttpResponse.status(io.micronaut.http.HttpStatus.CONFLICT);
                }
            } else if (request.getPurpose() == SendEmailCodeRequestPurpose.LOGIN ||
                       request.getPurpose() == SendEmailCodeRequestPurpose.RESET_PASSWORD) {
                if (userOpt.isEmpty()) {
                    return HttpResponse.badRequest(error("Email not found"));
                }
            }

            authService.sendEmailCode(request);
            return HttpResponse.ok(new MessageResponse("Verification code sent to email"));
        } catch (Exception e) {
            log.error("Error sending email code", e);
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
            UserEntity user = authService.registerWithEmail(request);
            String token = jwtTokenService.generateToken(user);
            return HttpResponse.created(success(
                userMapper.toDTO(user),
                token
            ));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("already registered")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.CONFLICT);
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error registering user", e);
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
            UserEntity user = authService.loginWithEmail(request);
            String token = jwtTokenService.generateToken(user);
            return HttpResponse.ok(success(
                userMapper.toDTO(user),
                token
            ));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error logging in with email", e);
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
            UserEntity user = authService.loginWithPassword(request);
            String token = jwtTokenService.generateToken(user);
            return HttpResponse.ok(success(
                userMapper.toDTO(user),
                token
            ));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error logging in with password", e);
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
                                           @Header("X-User-Id") String userIdHeader) {
        try {
            UUID userId = UUID.fromString(userIdHeader);
            Optional<UserEntity> userOpt = userRepository.findById(userId);

            if (userOpt.isEmpty()) {
                return HttpResponse.badRequest(error("User not found"));
            }

            UserEntity user = authService.setPassword(request, userOpt.get());
            return HttpResponse.ok(userMapper.toDTO(user));
        } catch (Exception e) {
            log.error("Error setting password", e);
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
                                              @Header("X-User-Id") String userIdHeader) {
        try {
            UUID userId = UUID.fromString(userIdHeader);
            Optional<UserEntity> userOpt = userRepository.findById(userId);

            if (userOpt.isEmpty()) {
                return HttpResponse.badRequest(error("User not found"));
            }

            UserEntity user = authService.changePassword(request, userOpt.get());
            return HttpResponse.ok(userMapper.toDTO(user));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error changing password", e);
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
            authService.requestPasswordReset(request);
            return HttpResponse.ok(new MessageResponse("Password reset code sent to email"));
        } catch (IllegalArgumentException e) {
            return HttpResponse.notFound(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error requesting password reset", e);
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
            UserEntity user = authService.resetPassword(request);
            String token = jwtTokenService.generateToken(user);
            return HttpResponse.ok(success(
                userMapper.toDTO(user),
                token
            ));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error resetting password", e);
            return HttpResponse.badRequest(error("Password reset failed"));
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