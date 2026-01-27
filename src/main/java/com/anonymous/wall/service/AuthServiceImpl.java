package com.anonymous.wall.service;

import com.anonymous.wall.model.*;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.util.PasswordUtil;

import jakarta.inject.Singleton;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Inject
    private UserRepository userRepository;

    private final Map<String, String> emailCodeCache = new HashMap<>();

    @Override
    public void sendEmailCode(SendEmailCodeRequest request) {
        String code = String.format("%06d", (int)(Math.random() * 1_000_000));
        code = "123456";
        emailCodeCache.put(request.getEmail(), code);
        System.out.println("Email code sent: " + code + " to " + request.getEmail());
    }

    @Override
    public UserEntity verifyEmailCode(VerifyEmailCodeRequest request) {
        String cached = emailCodeCache.get(request.getEmail());
        if (cached == null || !cached.equals(request.getCode())) {
            throw new RuntimeException("Invalid code");
        }

        Optional<UserEntity> userOpt = userRepository.findByEmail(request.getEmail());

        UserEntity user;
        if (userOpt.isEmpty()) {
            user = new UserEntity();
            user.setEmail(request.getEmail());
            user.setVerified(true);
            user.setPasswordSet(false);
            user.setCreatedAt(ZonedDateTime.now());
            userRepository.save(user);
        } else {
            user = userOpt.get();
            user.setVerified(true);
            userRepository.update(user);
        }

        emailCodeCache.remove(request.getEmail());
        return user;
    }

    @Override
    public void setPassword(SetPasswordRequest request, UserEntity currentUser) {
        if (currentUser == null) throw new RuntimeException("User not authenticated");

        if (currentUser.isPasswordSet()) {
            // Password already set, old password is required
            if (request.getOldPassword() == null) {
                throw new RuntimeException("Old password is required to change password");
            }
            if (!PasswordUtil.checkPassword(request.getOldPassword(), currentUser.getPasswordHash())) {
                throw new RuntimeException("Old password is incorrect");
            }
        }

        currentUser.setPasswordHash(PasswordUtil.hashPassword(request.getPassword()));
        currentUser.setPasswordSet(true);
        userRepository.update(currentUser);
    }

    @Override
    public UserEntity loginWithPassword(PasswordLoginRequest request) {
        Optional<UserEntity> user = userRepository.findByEmail(request.getEmail());
        if (user.isEmpty() || !PasswordUtil.checkPassword(request.getPassword(), user.get().getPasswordHash()))
            throw new RuntimeException("Invalid email or password");
        return user.get();
    }
}