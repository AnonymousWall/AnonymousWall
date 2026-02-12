package com.anonymous.wall.controller;

import com.anonymous.wall.entity.EmailVerificationCode;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.*;
import com.anonymous.wall.repository.EmailVerificationCodeRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.JwtTokenService;
import com.anonymous.wall.util.PasswordUtil;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
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
 * Test suite for passwordSet field in UserDTO responses.
 * Verifies that the frontend can determine whether to show the set password window or not.
 */
@MicronautTest(transactional = false)
@DisplayName("UserDTO passwordSet Field Tests")
class UserDTOPasswordSetTests {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    EmailVerificationCodeRepository emailCodeRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/auth";

    private static final String UPDATE_PROFILE_NAME_PATH = "/api/v1/users/me/profile/name";

    @BeforeEach
    void setUp() {
        emailCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        emailCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("passwordSet Field in Registration Response")
    class RegistrationPasswordSetTests {

        @Test
        @DisplayName("Positive: Should return passwordSet=false after registration")
        void shouldReturnPasswordSetFalseAfterRegistration() {
            // Arrange
            String testEmail = "registration" + System.currentTimeMillis() + "@harvard.edu";
            String code = "123456";

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                testEmail, code, "register", OffsetDateTime.now().plusMinutes(15)
            );
            emailCodeRepository.save(verificationCode);

            RegisterEmailRequest request = new RegisterEmailRequest(testEmail, code);

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/register/email", request),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatus());
            Map<String, Object> body = response.body();
            assertNotNull(body);
            assertTrue(body.containsKey("user"));

            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) body.get("user");

            // ✅ Critical assertion: passwordSet should be in response and false
            assertTrue(userMap.containsKey("passwordSet"),
                "passwordSet field must be present in UserDTO response for frontend to determine if password setup window should be shown");
            assertEquals(false, userMap.get("passwordSet"),
                "passwordSet should be false for newly registered user");
        }

        @Test
        @DisplayName("Positive: Should have all required fields after registration")
        void shouldHaveAllRequiredFieldsAfterRegistration() {
            // Arrange
            String testEmail = "allfields" + System.currentTimeMillis() + "@harvard.edu";
            String code = "123456";

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                testEmail, code, "register", OffsetDateTime.now().plusMinutes(15)
            );
            emailCodeRepository.save(verificationCode);

            RegisterEmailRequest request = new RegisterEmailRequest(testEmail, code);

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/register/email", request),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatus());
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) response.body().get("user");

            // ✅ Verify all UserDTO fields are present
            assertTrue(userMap.containsKey("id"), "id field required");
            assertTrue(userMap.containsKey("email"), "email field required");
            assertTrue(userMap.containsKey("profileName"), "profileName field required");
            assertTrue(userMap.containsKey("isVerified"), "isVerified field required");
            assertTrue(userMap.containsKey("passwordSet"), "passwordSet field required for frontend");
            assertTrue(userMap.containsKey("createdAt"), "createdAt field required");

            // Verify field values
            assertNotNull(userMap.get("id"));
            assertEquals(testEmail, userMap.get("email"));
            assertEquals("Anonymous", userMap.get("profileName"));
            assertEquals(true, userMap.get("isVerified"));
            assertEquals(false, userMap.get("passwordSet"));
            assertNotNull(userMap.get("createdAt"));
        }
    }

    @Nested
    @DisplayName("passwordSet Field in Login Response")
    class LoginPasswordSetTests {

        @Test
        @DisplayName("Positive: Should return passwordSet=false after email code login (new user)")
        void shouldReturnPasswordSetFalseAfterEmailCodeLogin() {
            // Arrange - new user, no password set yet
            String testEmail = "emaillogin" + System.currentTimeMillis() + "@harvard.edu";
            String code = "123456";

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                testEmail, code, "login", OffsetDateTime.now().plusMinutes(15)
            );
            emailCodeRepository.save(verificationCode);

            LoginEmailRequest request = new LoginEmailRequest(testEmail, code);

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/login/email", request),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) response.body().get("user");

            // ✅ Critical: Should indicate password not set
            assertTrue(userMap.containsKey("passwordSet"));
            assertEquals(false, userMap.get("passwordSet"),
                "Frontend should show 'set password' window after email login");
        }

        @Test
        @DisplayName("Positive: Should return passwordSet=false after password login (when no password set yet)")
        void shouldHandleLoginWithoutPasswordSet() {
            // Arrange - user exists but never set password
            String testEmail = "nopasslogin" + System.currentTimeMillis() + "@harvard.edu";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setVerified(true);
            user.setPasswordSet(false);  // No password set
            userRepository.save(user);

            String code = "123456";
            EmailVerificationCode verificationCode = new EmailVerificationCode(
                testEmail, code, "login", OffsetDateTime.now().plusMinutes(15)
            );
            emailCodeRepository.save(verificationCode);

            LoginEmailRequest request = new LoginEmailRequest(testEmail, code);

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/login/email", request),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) response.body().get("user");

            // ✅ Should indicate password not set
            assertEquals(false, userMap.get("passwordSet"));
        }

        @Test
        @DisplayName("Positive: Should return passwordSet=true after password login (when password is set)")
        void shouldReturnPasswordSetTrueAfterPasswordLogin() {
            // Arrange - user with password already set
            String testEmail = "pwdlogin" + System.currentTimeMillis() + "@harvard.edu";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setVerified(true);
            String hashedPassword = PasswordUtil.hashPassword("Password123!");
            user.setPasswordHash(hashedPassword);
            user.setPasswordSet(true);  // Password is set
            userRepository.save(user);

            PasswordLoginRequest request = new PasswordLoginRequest(testEmail, "Password123!");

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/login/password", request),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) response.body().get("user");

            // ✅ Should indicate password is set
            assertTrue(userMap.containsKey("passwordSet"));
            assertEquals(true, userMap.get("passwordSet"),
                "Frontend should NOT show 'set password' window - password is already set");
        }
    }

    @Nested
    @DisplayName("passwordSet Field After Password Operations")
    class PasswordOperationPasswordSetTests {

        @Test
        @DisplayName("Positive: Should return passwordSet=true after setPassword")
        void shouldReturnPasswordSetTrueAfterSetPassword() {
            // Arrange - Register user first (passwordSet=false)
            String testEmail = "setpwd" + System.currentTimeMillis() + "@harvard.edu";
            String code = "123456";

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                testEmail, code, "register", OffsetDateTime.now().plusMinutes(15)
            );
            emailCodeRepository.save(verificationCode);

            RegisterEmailRequest registerRequest = new RegisterEmailRequest(testEmail, code);

            HttpResponse<Map> registerResponse = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/register/email", registerRequest),
                Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> registerBody = registerResponse.body();
            String token = (String) registerBody.get("accessToken");

            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) registerBody.get("user");
            String userId = (String) userMap.get("id");

            // Verify initially passwordSet is false
            assertEquals(false, userMap.get("passwordSet"));

            // Act - Set password
            SetPasswordRequest setPasswordRequest = new SetPasswordRequest("NewPassword123!");

            HttpResponse<Map> setPasswordResponse = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/password/set", setPasswordRequest)
                    .header("Authorization", "Bearer " + token),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, setPasswordResponse.getStatus());
            @SuppressWarnings("unchecked")
            Map<String, Object> passwordSetUserMap = setPasswordResponse.body();

            // ✅ Should now show passwordSet=true
            assertTrue(passwordSetUserMap.containsKey("passwordSet"),
                "passwordSet field must be returned after setPassword for frontend");
            assertEquals(true, passwordSetUserMap.get("passwordSet"),
                "Frontend should hide 'set password' window after password is set");
        }

        @Test
        @DisplayName("Positive: Should return passwordSet=true after changePassword")
        void shouldReturnPasswordSetTrueAfterChangePassword() {
            // Arrange - User with password already set
            String testEmail = "changepwd" + System.currentTimeMillis() + "@harvard.edu";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setVerified(true);
            String initialHash = PasswordUtil.hashPassword("InitialPassword123!");
            user.setPasswordHash(initialHash);
            user.setPasswordSet(true);
            UserEntity savedUser = userRepository.save(user);

            // Create JWT token for user
            String token = jwtTokenService.generateToken(savedUser);

            // Act - Change password
            ChangePasswordRequest changeRequest = new ChangePasswordRequest("InitialPassword123!", "NewPassword456!");

            HttpResponse<Map> changeResponse = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/password/change", changeRequest)
                    .header("Authorization", "Bearer " + token),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, changeResponse.getStatus());
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = changeResponse.body();

            // ✅ Should still show passwordSet=true
            assertTrue(responseBody.containsKey("passwordSet"));
            assertEquals(true, responseBody.get("passwordSet"),
                "passwordSet should remain true after password change");
        }

        @Test
        @DisplayName("Positive: Should return passwordSet=true after resetPassword")
        void shouldReturnPasswordSetTrueAfterResetPassword() {
            // Arrange - User with password, requests reset
            String testEmail = "resetpwd" + System.currentTimeMillis() + "@harvard.edu";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setVerified(true);
            String initialHash = PasswordUtil.hashPassword("OldPassword123!");
            user.setPasswordHash(initialHash);
            user.setPasswordSet(true);
            userRepository.save(user);

            // Request password reset
            String resetCode = "654321";
            EmailVerificationCode resetVerification = new EmailVerificationCode(
                testEmail, resetCode, "reset_password", OffsetDateTime.now().plusMinutes(15)
            );
            emailCodeRepository.save(resetVerification);

            // Act - Reset password
            ResetPasswordRequest resetRequest = new ResetPasswordRequest(testEmail, resetCode, "NewPassword789!");

            HttpResponse<Map> resetResponse = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/password/reset", resetRequest),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, resetResponse.getStatus());
            @SuppressWarnings("unchecked")
            Map<String, Object> resetBody = resetResponse.body();

            @SuppressWarnings("unchecked")
            Map<String, Object> userAfterReset = (Map<String, Object>) resetBody.get("user");

            // ✅ Should show passwordSet=true (password was just reset)
            assertTrue(userAfterReset.containsKey("passwordSet"));
            assertEquals(true, userAfterReset.get("passwordSet"),
                "passwordSet should be true after password reset");
        }
    }

    @Nested
    @DisplayName("passwordSet Field in Profile Updates")
    class ProfileUpdatePasswordSetTests {

        @Test
        @DisplayName("Positive: Should preserve passwordSet=false when updating profile")
        void shouldPreservePasswordSetFalseInProfileUpdate() {
            // Arrange - Register user (passwordSet=false)
            String testEmail = "profiletest" + System.currentTimeMillis() + "@harvard.edu";
            String code = "123456";

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                testEmail, code, "register", OffsetDateTime.now().plusMinutes(15)
            );
            emailCodeRepository.save(verificationCode);

            RegisterEmailRequest registerRequest = new RegisterEmailRequest(testEmail, code);

            HttpResponse<Map> registerResponse = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/register/email", registerRequest),
                Map.class
            );

            String token = (String) registerResponse.body().get("accessToken");

            // Act - Update profile name
            UpdateProfileNameRequest updateRequest = new UpdateProfileNameRequest("John Doe");

            HttpResponse<Map> updateResponse = client.toBlocking().exchange(
                HttpRequest.PATCH(UPDATE_PROFILE_NAME_PATH, updateRequest)
                    .header("Authorization", "Bearer " + token),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, updateResponse.getStatus());
            @SuppressWarnings("unchecked")
            Map<String, Object> updatedUser = updateResponse.body();

            // ✅ Should still show passwordSet=false (not changed by profile update)
            assertTrue(updatedUser.containsKey("passwordSet"));
            assertEquals(false, updatedUser.get("passwordSet"),
                "Profile update should not affect passwordSet flag");
            assertEquals("John Doe", updatedUser.get("profileName"));
        }

        @Test
        @DisplayName("Positive: Should preserve passwordSet=true when updating profile after setting password")
        void shouldPreservePasswordSetTrueInProfileUpdate() {
            // Arrange - Setup user with password
            String testEmail = "profilepwd" + System.currentTimeMillis() + "@harvard.edu";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setVerified(true);
            String hashedPassword = PasswordUtil.hashPassword("Password123!");
            user.setPasswordHash(hashedPassword);
            user.setPasswordSet(true);
            UserEntity savedUser = userRepository.save(user);

            String token = jwtTokenService.generateToken(savedUser);

            // Act - Update profile name
            UpdateProfileNameRequest updateRequest = new UpdateProfileNameRequest("Jane Doe");

            HttpResponse<Map> updateResponse = client.toBlocking().exchange(
                HttpRequest.PATCH(UPDATE_PROFILE_NAME_PATH, updateRequest)
                    .header("Authorization", "Bearer " + token),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, updateResponse.getStatus());
            @SuppressWarnings("unchecked")
            Map<String, Object> updatedUser = updateResponse.body();

            // ✅ Should still show passwordSet=true
            assertTrue(updatedUser.containsKey("passwordSet"));
            assertEquals(true, updatedUser.get("passwordSet"),
                "passwordSet should remain true after profile update");
            assertEquals("Jane Doe", updatedUser.get("profileName"));
        }
    }

    @Nested
    @DisplayName("Frontend Use Case: Conditional Password Setup Window")
    class FrontendScenarioTests {

        @Test
        @DisplayName("Scenario: Frontend uses passwordSet to show/hide password setup window after registration")
        void frontendScenarioRegistrationPasswordSetup() {
            // This test documents the frontend behavior that depends on passwordSet field

            // Arrange - User registration
            String testEmail = "scenario1" + System.currentTimeMillis() + "@harvard.edu";
            String code = "123456";

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                testEmail, code, "register", OffsetDateTime.now().plusMinutes(15)
            );
            emailCodeRepository.save(verificationCode);

            RegisterEmailRequest request = new RegisterEmailRequest(testEmail, code);

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/register/email", request),
                Map.class
            );

            // Act & Assert - Frontend logic:
            // if (userDTO.passwordSet == false) {
            //     showSetPasswordWindow();  // ← This should happen
            // } else {
            //     skipToMainApp();
            // }

            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) response.body().get("user");

            // ✅ Frontend should detect passwordSet=false and show window
            assertFalse((Boolean) userMap.get("passwordSet"),
                "Frontend depends on this field to show 'Create Password' dialog");
        }

        @Test
        @DisplayName("Scenario: Frontend uses passwordSet to allow/disable password login option")
        void frontendScenarioLoginMethodSelection() {
            // Arrange - User with password set
            String testEmail = "scenario2" + System.currentTimeMillis() + "@harvard.edu";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setVerified(true);
            String hashedPassword = PasswordUtil.hashPassword("Password123!");
            user.setPasswordHash(hashedPassword);
            user.setPasswordSet(true);
            UserEntity savedUser = userRepository.save(user);

            // Act - Frontend checks passwordSet before showing login options
            // By logging in with password, we can verify passwordSet is true in response
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(testEmail, "Password123!");
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/login/password", loginRequest),
                Map.class
            );

            // Frontend logic:
            // if (userDTO.passwordSet == true) {
            //     showPasswordLoginOption();  // ← This should be shown
            // } else {
            //     showOnlyEmailCodeLoginOption();
            // }

            // Assert - Verify field is available for frontend decision
            assertEquals(HttpStatus.OK, response.getStatus());
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) response.body().get("user");

            assertTrue(userMap.containsKey("passwordSet"),
                "Frontend needs this field to decide which login methods to offer");
            assertTrue((Boolean) userMap.get("passwordSet"),
                "User with password should have passwordSet=true");
        }
    }
}
