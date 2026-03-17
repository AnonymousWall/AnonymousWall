package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import io.micronaut.context.annotation.Value;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.jwt.generator.JwtTokenGenerator;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to generate JWT tokens for authenticated users
 */
@Singleton
public class JwtTokenService {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);
    private static final int REFRESH_TOKEN_BYTES = 32;

    @Value("${micronaut.security.token.jwt.generator.access-token.expiration:900}")
    private int accessTokenExpiration;

    @Inject
    private JwtTokenGenerator tokenGenerator;

    private void validateUserForTokenGeneration(UserEntity user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and user ID cannot be null");
        }
        if (user.isBlocked()) {
            log.warn("Token generation denied for blocked user: userId={}, email={}", user.getId(), user.getEmail());
            throw new IllegalArgumentException("Access denied. Your account has been blocked.");
        }
    }

    /**
     * Generate JWT token for a user
     * Token expires in 15 minutes
     */
    public String generateToken(UserEntity user) {

        validateUserForTokenGeneration(user);

        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("email", user.getEmail());
            claims.put("verified", user.isVerified());
            claims.put("passwordSet", user.isPasswordSet());
            
            // Add schoolDomain to JWT claims to avoid redundant database lookups
            if (user.getSchoolDomain() != null) {
                claims.put("schoolDomain", user.getSchoolDomain());
            }

            // Create roles list from user role
            List<String> roles = Collections.singletonList(user.getRole());

            // Convert 15 minutes to seconds (900 seconds)
            int expirationSeconds = accessTokenExpiration;

            Optional<String> token = tokenGenerator.generateToken(
                Authentication.build(user.getId().toString(), roles, claims),
                expirationSeconds
            );

            if (token.isPresent()) {
                log.info("JWT token generated for user: {}", user.getEmail());
                return token.get();
            } else {
                throw new RuntimeException("Failed to generate JWT token");
            }
        } catch (Exception e) {
            log.error("Error generating JWT token", e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    /**
     * Generate JWT token for a user with custom claims
     * Token expires in 15 minutes
     */
    public String generateToken(UserEntity user, Map<String, Object> customClaims) {

        validateUserForTokenGeneration(user);

        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("email", user.getEmail());
            claims.put("verified", user.isVerified());
            claims.put("passwordSet", user.isPasswordSet());
            
            // Add schoolDomain to JWT claims to avoid redundant database lookups
            if (user.getSchoolDomain() != null) {
                claims.put("schoolDomain", user.getSchoolDomain());
            }

            // Add custom claims
            if (customClaims != null) {
                claims.putAll(customClaims);
            }

            // Create roles list from user role
            List<String> roles = Collections.singletonList(user.getRole());

            // Convert 15 minutes to seconds (900 seconds)
            int expirationSeconds = accessTokenExpiration;

            Optional<String> token = tokenGenerator.generateToken(
                Authentication.build(user.getId().toString(), roles, claims),
                expirationSeconds
            );

            if (token.isPresent()) {
                log.info("JWT token generated for user: {}", user.getEmail());
                return token.get();
            } else {
                throw new RuntimeException("Failed to generate JWT token");
            }
        } catch (Exception e) {
            log.error("Error generating JWT token with custom claims", e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    /**
     * Generate a cryptographically random refresh token.
     * Returns the raw token — store only the hash.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 hash a token for safe storage.
     * Never store raw refresh tokens in the database.
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
