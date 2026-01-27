package com.anonymous.wall.controller;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.mapper.UserMapper;
import com.anonymous.wall.model.*;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.AuthService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
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

    @Post("/email/code")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Void> sendEmailCode(@Body SendEmailCodeRequest request) {
        Optional<UserEntity> currentUser = userRepository.findByEmail(request.getEmail());
        if (request.getPurpose() == SendEmailCodeRequestPurpose.REGISTER) {
            if (currentUser.isPresent()) {
                return HttpResponse.badRequest();
            }
            authService.sendEmailCode(request);
            return HttpResponse.ok();
        } else if (request.getPurpose() == SendEmailCodeRequestPurpose.LOGIN || request.getPurpose() == SendEmailCodeRequestPurpose.RESET_PASSWORD) {
            if (currentUser.isPresent()) {
                authService.sendEmailCode(request);
                return HttpResponse.ok();
            }
            return HttpResponse.badRequest();
        }
        return HttpResponse.badRequest();
    }

    @Post("/email/verify")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<UserDTO> verifyEmailCode(@Body VerifyEmailCodeRequest request) {
        try {
            UserEntity user = authService.verifyEmailCode(request);
            return HttpResponse.ok(userMapper.toDTO(user));
        } catch (Exception e) {
            log.error("e: ", e);
            return HttpResponse.badRequest();
        }
    }

    @Post("/password")
//    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Void> setPassword(@Body SetPasswordRequest request, @Header("X-User-Id") String userIdHeader) {
        if (userIdHeader == null) {
            return HttpResponse.unauthorized();
        }
        try {
            UUID userId = UUID.fromString(userIdHeader);
            Optional<UserEntity> currentUser = userRepository.findById(userId);
            if (currentUser.isEmpty()) {
                return HttpResponse.badRequest();
            }
            authService.setPassword(request, currentUser.get());
            return HttpResponse.ok();
        } catch (Exception e) {
            return HttpResponse.badRequest();
        }
    }

    @Post("/login/password")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<UserDTO> loginWithPassword(@Body PasswordLoginRequest request) {
        try {
            UserEntity user = authService.loginWithPassword(request);
            return HttpResponse.ok(userMapper.toDTO(user));
        } catch (Exception e) {
            return HttpResponse.unauthorized();
        }
    }
}