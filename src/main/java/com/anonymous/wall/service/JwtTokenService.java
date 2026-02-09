package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.jwt.generator.JwtTokenGenerator;
import io.micronaut.security.token.jwt.validator.JwtTokenValidator;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service to generate JWT tokens for authenticated users
 */
@Singleton
public class JwtTokenService {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    @Inject
    private JwtTokenGenerator tokenGenerator;

    @Inject
    private JwtTokenValidator<Authentication> tokenValidator;

    /**
     * Generate JWT token for a user
     * Token expires in 24 hours
     */
    public String generateToken(UserEntity user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and user ID cannot be null");
        }

        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("email", user.getEmail());
            claims.put("verified", user.isVerified());
            claims.put("passwordSet", user.isPasswordSet());
            
            // Add schoolDomain to JWT claims to avoid redundant database lookups
            if (user.getSchoolDomain() != null) {
                claims.put("schoolDomain", user.getSchoolDomain());
            }

            // Convert 24 hours to seconds (86400 seconds)
            Integer expirationSeconds = 86400;

            Optional<String> token = tokenGenerator.generateToken(
                Authentication.build(user.getId().toString(), claims),
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
     * Token expires in 24 hours
     */
    public String generateToken(UserEntity user, Map<String, Object> customClaims) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and user ID cannot be null");
        }

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

            // Convert 24 hours to seconds (86400 seconds)
            Integer expirationSeconds = 86400;

            Optional<String> token = tokenGenerator.generateToken(
                Authentication.build(user.getId().toString(), claims),
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
     * Extract user ID from JWT token
     * @param token JWT token string
     * @return User ID or null if invalid
     */
    public UUID extractUserIdFromToken(String token) {
        try {
            // Validate token and extract authentication
            Optional<Authentication> authOpt = tokenValidator.validateToken(token);
            
            if (authOpt.isPresent()) {
                Authentication auth = authOpt.get();
                String userIdStr = auth.getName();
                
                if (userIdStr != null && !userIdStr.isEmpty()) {
                    return UUID.fromString(userIdStr);
                }
            }
            
            log.warn("Failed to extract user ID from token");
            return null;
        } catch (Exception e) {
            log.error("Error extracting user ID from token: {}", e.getMessage());
            return null;
        }
    }
}
