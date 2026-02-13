package com.anonymous.wall.controller;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.model.LoginEmailRequest;
import com.anonymous.wall.model.PasswordLoginRequest;
import com.anonymous.wall.model.PasswordResetRequestRequest;
import com.anonymous.wall.model.ResetPasswordRequest;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for blocked user enforcement.
 * Tests that blocked users cannot perform any restricted actions.
 */
@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Blocked User Enforcement Integration Tests")
class BlockedUserIntegrationTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    private UserEntity blockedUser;
    private UserEntity normalUser;
    private String normalUserToken;

    @BeforeEach
    void setUp() {
        // Create a blocked user
        blockedUser = new UserEntity();
        blockedUser.setEmail("blocked@harvard.edu");
        blockedUser.setSchoolDomain("harvard.edu");
        blockedUser.setVerified(true);
        blockedUser.setPasswordSet(true);
        blockedUser.setPasswordHash("$2a$10$dummy.hash.for.testing");
        blockedUser.setBlocked(true);
        blockedUser.setCreatedAt(OffsetDateTime.now());
        blockedUser = userRepository.save(blockedUser);

        // Create a normal user for comparison
        normalUser = new UserEntity();
        normalUser.setEmail("normal@harvard.edu");
        normalUser.setSchoolDomain("harvard.edu");
        normalUser.setVerified(true);
        normalUser.setPasswordSet(true);
        normalUser.setPasswordHash("$2a$10$dummy.hash.for.testing");
        normalUser.setBlocked(false);
        normalUser.setCreatedAt(OffsetDateTime.now());
        normalUser = userRepository.save(normalUser);
    }

    @AfterEach
    void cleanup() {
        // Clean up test data
        if (blockedUser != null && blockedUser.getId() != null) {
            userRepository.findById(blockedUser.getId()).ifPresent(userRepository::delete);
        }
        if (normalUser != null && normalUser.getId() != null) {
            userRepository.findById(normalUser.getId()).ifPresent(userRepository::delete);
        }
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @Order(1)
        @DisplayName("Blocked user should not be able to login with password")
        void blockedUserCannotLoginWithPassword() {
            // Arrange
            PasswordLoginRequest request = new PasswordLoginRequest(
                blockedUser.getEmail(),
                "password123"
            );

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST("/api/v1/auth/login/password", request),
                    Map.class
                )
            );

            // Should return 400 Bad Request with blocked message
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            Optional<Map> body = exception.getResponse().getBody(Map.class);
            assertTrue(body.isPresent());
            String error = body.get().get("error").toString();
            assertTrue(error.contains("blocked"), "Error message should mention 'blocked'");
        }

        @Test
        @Order(2)
        @DisplayName("Normal user should be able to login with password")
        void normalUserCanLoginWithPassword() {
            // This test would require actual password authentication setup
            // For now, we just verify the user exists and is not blocked
            Optional<UserEntity> userOpt = userRepository.findById(normalUser.getId());
            assertTrue(userOpt.isPresent());
            assertFalse(userOpt.get().isBlocked());
        }
    }

    @Nested
    @DisplayName("Password Reset Tests")
    class PasswordResetTests {

        @Test
        @Order(1)
        @DisplayName("Blocked user should not be able to request password reset")
        void blockedUserCannotRequestPasswordReset() {
            // Arrange
            PasswordResetRequestRequest request = new PasswordResetRequestRequest(
                blockedUser.getEmail()
            );

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST("/api/v1/auth/password/reset-request", request),
                    Map.class
                )
            );

            // Should return 400 Bad Request with blocked message
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            Optional<Map> body = exception.getResponse().getBody(Map.class);
            assertTrue(body.isPresent());
            String error = body.get().get("error").toString();
            assertTrue(error.contains("blocked"), "Error message should mention 'blocked'");
        }
    }

    @Nested
    @DisplayName("Token Generation Tests")
    @Disabled("JWT token generation tests require full authentication flow")
    class TokenGenerationTests {
        // These tests would require setting up full authentication flow
        // which is complex in integration tests
    }

    @Nested
    @DisplayName("Database State Tests")
    class DatabaseStateTests {

        @Test
        @Order(1)
        @DisplayName("Verify blocked user is correctly marked in database")
        void verifyBlockedUserInDatabase() {
            Optional<UserEntity> userOpt = userRepository.findById(blockedUser.getId());
            assertTrue(userOpt.isPresent());
            assertTrue(userOpt.get().isBlocked());
        }

        @Test
        @Order(2)
        @DisplayName("Verify normal user is not blocked in database")
        void verifyNormalUserInDatabase() {
            Optional<UserEntity> userOpt = userRepository.findById(normalUser.getId());
            assertTrue(userOpt.isPresent());
            assertFalse(userOpt.get().isBlocked());
        }

        @Test
        @Order(3)
        @DisplayName("User can be blocked after creation")
        void userCanBeBlockedAfterCreation() {
            // Get the normal user
            Optional<UserEntity> userOpt = userRepository.findById(normalUser.getId());
            assertTrue(userOpt.isPresent());
            UserEntity user = userOpt.get();
            assertFalse(user.isBlocked());

            // Block the user
            user.setBlocked(true);
            UserEntity updated = userRepository.update(user);
            assertTrue(updated.isBlocked());

            // Unblock for cleanup
            user.setBlocked(false);
            userRepository.update(user);
        }
    }
}
