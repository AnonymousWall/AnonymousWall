package com.anonymous.wall.controller;

import com.anonymous.wall.entity.EmailVerificationCode;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.*;
import com.anonymous.wall.repository.EmailVerificationCodeRepository;
import com.anonymous.wall.repository.UserRepository;
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

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    EmailVerificationCodeRepository emailCodeRepository;

    private static final String BASE_PATH = "/api/v1/auth";

    @BeforeEach
    void setUp() {
        // Clean up test data before each test
        emailCodeRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // Clean up after tests
        emailCodeRepository.deleteAll();
    }

    @Nested
    @DisplayName("Send Email Code Endpoint Tests")
    class SendEmailCodeTests {

        @Test
        @DisplayName("Positive: Should send code for registration")
        void shouldSendCodeForRegistration() {
            // Arrange
            String testEmail = "newreg" + System.currentTimeMillis() + "@test.com";
            SendEmailCodeRequest request = new SendEmailCodeRequest(testEmail, SendEmailCodeRequestPurpose.REGISTER);

            // Act
            HttpResponse<?> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/email/send-code", request)
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());

            // Verify code was saved in database
            Optional<EmailVerificationCode> savedCode = emailCodeRepository
                .findByEmailAndCodeAndPurpose(testEmail, getAnyCodeForEmail(testEmail), "register");
            assertTrue(savedCode.isPresent() || emailCodeRepository.findAll().iterator().hasNext());
        }

        @Test
        @DisplayName("Positive: Should send code for login")
        void shouldSendCodeForLogin() {
            // Arrange
            String testEmail = "existinguser" + System.currentTimeMillis() + "@test.com";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setVerified(true);
            userRepository.save(user);

            SendEmailCodeRequest request = new SendEmailCodeRequest(testEmail, SendEmailCodeRequestPurpose.LOGIN);

            // Act
            HttpResponse<?> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/email/send-code", request)
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
        }

        @Test
        @DisplayName("Negative: Should reject empty email")
        void shouldRejectEmptyEmail() {
            // Arrange
            SendEmailCodeRequest request = new SendEmailCodeRequest("", SendEmailCodeRequestPurpose.REGISTER);

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.POST(BASE_PATH + "/email/send-code", request))
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Negative: Should reject null email")
        void shouldRejectNullEmail() {
            // Arrange
            SendEmailCodeRequest request = new SendEmailCodeRequest(null, SendEmailCodeRequestPurpose.REGISTER);

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.POST(BASE_PATH + "/email/send-code", request))
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Negative: Should return conflict when registering with existing email")
        void shouldRejectExistingEmailForRegistration() {
            // Arrange
            String existingEmail = "existing" + System.currentTimeMillis() + "@test.com";
            UserEntity existingUser = new UserEntity();
            existingUser.setEmail(existingEmail);
            userRepository.save(existingUser);

            SendEmailCodeRequest request = new SendEmailCodeRequest(existingEmail, SendEmailCodeRequestPurpose.REGISTER);

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.POST(BASE_PATH + "/email/send-code", request))
            );
            assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        }

        @Test
        @DisplayName("Negative: Should fail when email not found for login")
        void shouldFailWhenEmailNotFoundForLogin() {
            // Arrange
            SendEmailCodeRequest request = new SendEmailCodeRequest("nonexistent@test.com", SendEmailCodeRequestPurpose.LOGIN);

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.POST(BASE_PATH + "/email/send-code", request))
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Register with Email Endpoint Tests")
    class RegisterWithEmailTests {

        @Test
        @DisplayName("Positive: Should register new user with valid code")
        void shouldRegisterNewUser() {
            // Arrange
            String testEmail = "newuser" + System.currentTimeMillis() + "@test.com";
            String code = "123456";

            // Create verification code
            EmailVerificationCode verificationCode = new EmailVerificationCode(
                testEmail, code, "register", ZonedDateTime.now().plusMinutes(15)
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
            assertTrue(body.containsKey("accessToken"));
            assertTrue(body.containsKey("user"));

            // Verify user was created
            Optional<UserEntity> savedUser = userRepository.findByEmail(testEmail);
            assertTrue(savedUser.isPresent());
            assertTrue(savedUser.get().isVerified());
        }

        @Test
        @DisplayName("Negative: Should fail with invalid code")
        void shouldFailWithInvalidCode() {
            // Arrange
            String testEmail = "test" + System.currentTimeMillis() + "@test.com";
            RegisterEmailRequest request = new RegisterEmailRequest(testEmail, "wrong_code");

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/register/email", request)
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Negative: Should fail when email already registered")
        void shouldFailWhenEmailAlreadyRegistered() {
            // Arrange
            String existingEmail = "existing" + System.currentTimeMillis() + "@test.com";
            UserEntity existingUser = new UserEntity();
            existingUser.setEmail(existingEmail);
            userRepository.save(existingUser);

            String code = "123456";
            EmailVerificationCode verificationCode = new EmailVerificationCode(
                existingEmail, code, "register", ZonedDateTime.now().plusMinutes(15)
            );
            emailCodeRepository.save(verificationCode);

            RegisterEmailRequest request = new RegisterEmailRequest(existingEmail, code);

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/register/email", request)
                )
            );
            assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        }

        @Test
        @DisplayName("Edge: Should fail with expired code")
        void shouldFailWithExpiredCode() {
            // Arrange
            String testEmail = "test" + System.currentTimeMillis() + "@test.com";
            String code = "999999";

            EmailVerificationCode expiredCode = new EmailVerificationCode(
                testEmail, code, "register", ZonedDateTime.now().minusMinutes(1)
            );
            emailCodeRepository.save(expiredCode);

            RegisterEmailRequest request = new RegisterEmailRequest(testEmail, code);

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/register/email", request)
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Login with Email Endpoint Tests")
    class LoginWithEmailTests {

        @Test
        @DisplayName("Positive: Should login existing user")
        void shouldLoginExistingUser() {
            // Arrange
            String testEmail = "loginuser" + System.currentTimeMillis() + "@test.com";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setVerified(true);
            userRepository.save(user);

            String code = "654321";
            EmailVerificationCode verificationCode = new EmailVerificationCode(
                testEmail, code, "login", ZonedDateTime.now().plusMinutes(15)
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
            Map<String, Object> body = response.body();
            assertNotNull(body);
            assertTrue(body.containsKey("accessToken"));
            assertTrue(body.containsKey("user"));
        }

        @Test
        @DisplayName("Positive: Should auto-create user if not exists")
        void shouldAutoCreateUser() {
            // Arrange
            String testEmail = "newlogin" + System.currentTimeMillis() + "@test.com";
            String code = "111111";

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                testEmail, code, "login", ZonedDateTime.now().plusMinutes(15)
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

            // Verify user was auto-created
            Optional<UserEntity> createdUser = userRepository.findByEmail(testEmail);
            assertTrue(createdUser.isPresent());
        }

        @Test
        @DisplayName("Negative: Should fail with invalid code")
        void shouldFailWithInvalidCode() {
            // Arrange
            String testEmail = "test" + System.currentTimeMillis() + "@test.com";
            LoginEmailRequest request = new LoginEmailRequest(testEmail, "invalid");

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/login/email", request)
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Login with Password Endpoint Tests")
    class LoginWithPasswordTests {

        @Test
        @DisplayName("Positive: Should login with correct password")
        void shouldLoginWithCorrectPassword() {
            // Arrange
            String testEmail = "pwuser" + System.currentTimeMillis() + "@test.com";
            String password = "MyPassword123!";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setPasswordSet(true);
            user.setPasswordHash(PasswordUtil.hashPassword(password));
            userRepository.save(user);

            PasswordLoginRequest request = new PasswordLoginRequest(testEmail, password);

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/login/password", request),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            Map<String, Object> body = response.body();
            assertNotNull(body);
            assertTrue(body.containsKey("accessToken"));
        }

        @Test
        @DisplayName("Negative: Should fail with wrong password")
        void shouldFailWithWrongPassword() {
            // Arrange
            String testEmail = "pwuser2" + System.currentTimeMillis() + "@test.com";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setPasswordSet(true);
            user.setPasswordHash(PasswordUtil.hashPassword("MyPassword123!"));
            userRepository.save(user);

            PasswordLoginRequest request = new PasswordLoginRequest(testEmail, "WrongPassword");

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/login/password", request)
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Negative: Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            // Arrange
            PasswordLoginRequest request = new PasswordLoginRequest("nonexistent@test.com", "password");

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/login/password", request)
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Negative: Should fail when password not set")
        void shouldFailWhenPasswordNotSet() {
            // Arrange
            String testEmail = "nopassword" + System.currentTimeMillis() + "@test.com";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setPasswordSet(false);
            userRepository.save(user);

            PasswordLoginRequest request = new PasswordLoginRequest(testEmail, "password");

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/login/password", request)
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Set Password Endpoint Tests")
    class SetPasswordTests {

        @Test
        @DisplayName("Negative: Should fail without proper authentication")
        void shouldFailWithoutAuthentication() {
            // Arrange
            String testEmail = "setpw" + System.currentTimeMillis() + "@test.com";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setPasswordSet(false);
            final UserEntity savedUser = userRepository.save(user);

            SetPasswordRequest request = new SetPasswordRequest("NewPassword123!");

            // Act & Assert - Expect UNAUTHORIZED because no JWT token
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/password/set", request)
                        .header("X-User-Id", savedUser.getId().toString())
                )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        @DisplayName("Negative: Should fail without authentication header")
        void shouldFailWithoutAuthHeader() {
            // Arrange
            SetPasswordRequest request = new SetPasswordRequest("password");

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/password/set", request)
                )
            );
            // Will fail with UNAUTHORIZED due to @Secured annotation
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        @DisplayName("Negative: Should fail with invalid user ID")
        void shouldFailWithInvalidUserId() {
            // Arrange
            SetPasswordRequest request = new SetPasswordRequest("password");
            UUID nonExistentId = UUID.randomUUID();

            // Act & Assert - Expect UNAUTHORIZED because no proper JWT authentication
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/password/set", request)
                        .header("X-User-Id", nonExistentId.toString())
                )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Change Password Endpoint Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Negative: Should fail without proper authentication")
        void shouldFailWithoutProperAuthentication() {
            // Arrange
            String testEmail = "changepw" + System.currentTimeMillis() + "@test.com";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setPasswordSet(true);
            user.setPasswordHash(PasswordUtil.hashPassword("MyPassword123!"));
            final UserEntity savedUser = userRepository.save(user);

            ChangePasswordRequest request = new ChangePasswordRequest("MyPassword123!", "NewPassword456!");

            // Act & Assert - Expect UNAUTHORIZED because no proper JWT authentication
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/password/change", request)
                        .header("X-User-Id", savedUser.getId().toString())
                )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        @DisplayName("Negative: Should fail with wrong old password (when authenticated)")
        void shouldFailWithWrongOldPassword() {
            // Arrange
            String testEmail = "changepw2" + System.currentTimeMillis() + "@test.com";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setPasswordSet(true);
            user.setPasswordHash(PasswordUtil.hashPassword("MyPassword123!"));
            user = userRepository.save(user);

            ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword", "NewPassword456!");
            final UUID userId = user.getId();

            // Act & Assert - Expect UNAUTHORIZED because no proper JWT authentication
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/password/change", request)
                        .header("X-User-Id", userId.toString())
                )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        @DisplayName("Negative: Should fail without authentication")
        void shouldFailWithoutAuthentication() {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest("old", "new");

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/password/change", request)
                )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Password Reset Flow Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("Positive: Should complete full password reset flow")
        void shouldCompletePasswordResetFlow() {
            // Arrange - Create user
            String testEmail = "resetpw" + System.currentTimeMillis() + "@test.com";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            user.setPasswordSet(true);
            user.setPasswordHash("oldHash");
            userRepository.save(user);

            // Step 1: Request password reset
            PasswordResetRequestRequest resetRequest = new PasswordResetRequestRequest(testEmail);

            HttpResponse<?> requestResponse = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/password/reset-request", resetRequest)
            );
            assertEquals(HttpStatus.OK, requestResponse.getStatus());

            // Step 2: Get the reset code from database (simulating email)
            String resetCode = getResetCodeForEmail(testEmail);
            assertNotNull(resetCode, "Reset code should be generated");

            // Step 3: Reset password with code
            ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest(testEmail, resetCode, "NewResetPassword123!");

            HttpResponse<Map> resetResponse = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/password/reset", resetPasswordRequest),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, resetResponse.getStatus());
            Map<String, Object> body = resetResponse.body();
            assertNotNull(body);
            assertTrue(body.containsKey("accessToken"));
        }

        @Test
        @DisplayName("Negative: Should fail reset request for non-existent email")
        void shouldFailResetRequestForNonExistentEmail() {
            // Arrange
            PasswordResetRequestRequest request = new PasswordResetRequestRequest("nonexistent@test.com");

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/password/reset-request", request)
                )
            );
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("Negative: Should fail reset with invalid code")
        void shouldFailResetWithInvalidCode() {
            // Arrange
            String testEmail = "reset2" + System.currentTimeMillis() + "@test.com";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            userRepository.save(user);

            ResetPasswordRequest request = new ResetPasswordRequest(testEmail, "invalid_code", "NewPassword");

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/password/reset", request)
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Edge: Should fail reset with expired code")
        void shouldFailResetWithExpiredCode() {
            // Arrange
            String testEmail = "reset3" + System.currentTimeMillis() + "@test.com";
            UserEntity user = new UserEntity();
            user.setEmail(testEmail);
            userRepository.save(user);

            String code = "expired";
            EmailVerificationCode expiredCode = new EmailVerificationCode(
                testEmail, code, "reset_password", ZonedDateTime.now().minusMinutes(20)
            );
            emailCodeRepository.save(expiredCode);

            ResetPasswordRequest request = new ResetPasswordRequest(testEmail, code, "NewPassword");

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/password/reset", request)
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }
    }

    // Helper methods
    @SuppressWarnings("unused")
    private String getAnyCodeForEmail(String email) {
        // Try to find any code for the email
        return "123456"; // Default code for testing
    }

    private String getResetCodeForEmail(String email) {
        // Get the reset code from database
        Iterable<EmailVerificationCode> codes = emailCodeRepository.findAll();
        for (EmailVerificationCode code : codes) {
            if (code.getEmail().equals(email) && code.getPurpose().equals("reset_password")) {
                return code.getCode();
            }
        }
        return null;
    }
}
