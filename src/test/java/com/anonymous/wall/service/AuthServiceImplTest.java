package com.anonymous.wall.service;

import com.anonymous.wall.entity.EmailVerificationCode;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.*;
import com.anonymous.wall.repository.EmailVerificationCodeRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

    private AuthServiceImpl authService;
    private UserRepository userRepository;
    private EmailVerificationCodeRepository emailCodeRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        emailCodeRepository = mock(EmailVerificationCodeRepository.class);
        authService = new AuthServiceImpl();
        // Use reflection to inject mocks
        try {
            var userRepoField = AuthServiceImpl.class.getDeclaredField("userRepository");
            userRepoField.setAccessible(true);
            userRepoField.set(authService, userRepository);

            var emailRepoField = AuthServiceImpl.class.getDeclaredField("emailCodeRepository");
            emailRepoField.setAccessible(true);
            emailRepoField.set(authService, emailCodeRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Send Email Code Tests")
    class SendEmailCodeTests {

        @Test
        @DisplayName("Positive: Should send verification code for registration")
        void shouldSendCodeForRegistration() {
            // Arrange
            SendEmailCodeRequest request = new SendEmailCodeRequest("test@example.com", SendEmailCodeRequestPurpose.REGISTER);

            ArgumentCaptor<EmailVerificationCode> captor = ArgumentCaptor.forClass(EmailVerificationCode.class);

            // Act
            authService.sendEmailCode(request);

            // Assert
            verify(emailCodeRepository, times(1)).save(captor.capture());
            EmailVerificationCode savedCode = captor.getValue();
            assertEquals("test@example.com", savedCode.getEmail());
            assertEquals("register", savedCode.getPurpose());
            assertNotNull(savedCode.getCode());
            assertEquals(6, savedCode.getCode().length());
            assertNotNull(savedCode.getExpiresAt());
        }

        @Test
        @DisplayName("Positive: Should send verification code for login")
        void shouldSendCodeForLogin() {
            // Arrange
            SendEmailCodeRequest request = new SendEmailCodeRequest("user@test.com", SendEmailCodeRequestPurpose.LOGIN);

            // Act
            authService.sendEmailCode(request);

            // Assert
            verify(emailCodeRepository, times(1)).save(any(EmailVerificationCode.class));
        }

        @Test
        @DisplayName("Positive: Should send verification code for password reset")
        void shouldSendCodeForPasswordReset() {
            // Arrange
            SendEmailCodeRequest request = new SendEmailCodeRequest("reset@test.com", SendEmailCodeRequestPurpose.RESET_PASSWORD);

            // Act
            authService.sendEmailCode(request);

            // Assert
            verify(emailCodeRepository, times(1)).save(any(EmailVerificationCode.class));
        }
    }

    @Nested
    @DisplayName("Register with Email Tests")
    class RegisterWithEmailTests {

        @Test
        @DisplayName("Positive: Should register new user with valid code")
        void shouldRegisterNewUser() {
            // Arrange
            String email = "newuser@test.com";
            String code = "123456";

            RegisterEmailRequest request = new RegisterEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                email, code, "register", ZonedDateTime.now().plusMinutes(15)
            );

            UserEntity savedUser = new UserEntity();
            savedUser.setId(UUID.randomUUID());
            savedUser.setEmail(email);
            savedUser.setVerified(true);

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, code, "register"))
                .thenReturn(Optional.of(verificationCode));
            when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

            // Act
            UserEntity result = authService.registerWithEmail(request);

            // Assert
            assertNotNull(result);
            assertEquals(email, result.getEmail());
            assertTrue(result.isVerified());
            assertFalse(result.isPasswordSet());
            verify(emailCodeRepository, times(1)).deleteByEmail(email);
        }

        @Test
        @DisplayName("Negative: Should fail when email already exists")
        void shouldFailWhenEmailExists() {
            // Arrange
            String email = "existing@test.com";
            RegisterEmailRequest request = new RegisterEmailRequest(email, "123456");

            UserEntity existingUser = new UserEntity();
            existingUser.setEmail(email);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerWithEmail(request)
            );
            assertEquals("Email already registered", exception.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Negative: Should fail with invalid code")
        void shouldFailWithInvalidCode() {
            // Arrange
            String email = "test@example.com";
            RegisterEmailRequest request = new RegisterEmailRequest(email, "wrong_code");

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, "wrong_code", "register"))
                .thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerWithEmail(request)
            );
            assertEquals("Invalid or expired code", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail with expired code")
        void shouldFailWithExpiredCode() {
            // Arrange
            String email = "test@example.com";
            String code = "123456";
            RegisterEmailRequest request = new RegisterEmailRequest(email, code);

            EmailVerificationCode expiredCode = new EmailVerificationCode(
                email, code, "register", ZonedDateTime.now().minusMinutes(1)
            );

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, code, "register"))
                .thenReturn(Optional.of(expiredCode));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerWithEmail(request)
            );
            assertEquals("Code has expired", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Login with Email Tests")
    class LoginWithEmailTests {

        @Test
        @DisplayName("Positive: Should login existing user with valid code")
        void shouldLoginExistingUser() {
            // Arrange
            String email = "user@test.com";
            String code = "654321";

            LoginEmailRequest request = new LoginEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                email, code, "login", ZonedDateTime.now().plusMinutes(15)
            );

            UserEntity existingUser = new UserEntity();
            existingUser.setId(UUID.randomUUID());
            existingUser.setEmail(email);

            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, code, "login"))
                .thenReturn(Optional.of(verificationCode));
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

            // Act
            UserEntity result = authService.loginWithEmail(request);

            // Assert
            assertNotNull(result);
            assertEquals(email, result.getEmail());
            verify(emailCodeRepository, times(1)).deleteByEmail(email);
        }

        @Test
        @DisplayName("Positive: Should auto-create user if not exists")
        void shouldAutoCreateUserIfNotExists() {
            // Arrange
            String email = "newuser@test.com";
            String code = "654321";

            LoginEmailRequest request = new LoginEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                email, code, "login", ZonedDateTime.now().plusMinutes(15)
            );

            UserEntity newUser = new UserEntity();
            newUser.setId(UUID.randomUUID());
            newUser.setEmail(email);
            newUser.setVerified(true);

            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, code, "login"))
                .thenReturn(Optional.of(verificationCode));
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(userRepository.save(any(UserEntity.class))).thenReturn(newUser);

            // Act
            UserEntity result = authService.loginWithEmail(request);

            // Assert
            assertNotNull(result);
            assertEquals(email, result.getEmail());
            verify(userRepository, times(1)).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("Negative: Should fail with invalid code")
        void shouldFailWithInvalidCode() {
            // Arrange
            String email = "user@test.com";
            LoginEmailRequest request = new LoginEmailRequest(email, "invalid");

            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, "invalid", "login"))
                .thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.loginWithEmail(request)
            );
            assertEquals("Invalid or expired code", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail with expired code")
        void shouldFailWithExpiredCode() {
            // Arrange
            String email = "user@test.com";
            String code = "123456";
            LoginEmailRequest request = new LoginEmailRequest(email, code);

            EmailVerificationCode expiredCode = new EmailVerificationCode(
                email, code, "login", ZonedDateTime.now().minusMinutes(20)
            );

            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, code, "login"))
                .thenReturn(Optional.of(expiredCode));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.loginWithEmail(request)
            );
            assertEquals("Code has expired", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Login with Password Tests")
    class LoginWithPasswordTests {

        @Test
        @DisplayName("Positive: Should login with correct password")
        void shouldLoginWithCorrectPassword() {
            // Arrange
            String email = "user@test.com";
            String password = "MyPassword123!";

            PasswordLoginRequest request = new PasswordLoginRequest(email, password);

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setPasswordSet(true);
            // Generate proper hash for the password
            user.setPasswordHash(PasswordUtil.hashPassword(password));

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // Act
            UserEntity result = authService.loginWithPassword(request);

            // Assert
            assertNotNull(result);
            assertEquals(email, result.getEmail());
        }

        @Test
        @DisplayName("Negative: Should fail with wrong password")
        void shouldFailWithWrongPassword() {
            // Arrange
            String email = "user@test.com";
            PasswordLoginRequest request = new PasswordLoginRequest(email, "WrongPassword");

            UserEntity user = new UserEntity();
            user.setEmail(email);
            user.setPasswordSet(true);
            user.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.loginWithPassword(request)
            );
            assertEquals("Invalid email or password", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            // Arrange
            PasswordLoginRequest request = new PasswordLoginRequest("nonexistent@test.com", "password");

            when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.loginWithPassword(request)
            );
            assertEquals("Invalid email or password", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail when password not set")
        void shouldFailWhenPasswordNotSet() {
            // Arrange
            String email = "nopassword@test.com";
            PasswordLoginRequest request = new PasswordLoginRequest(email, "password");

            UserEntity user = new UserEntity();
            user.setEmail(email);
            user.setPasswordSet(false);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.loginWithPassword(request)
            );
            assertEquals("Password not set for this account", exception.getMessage());
        }

        @Test
        @DisplayName("Edge: Should handle null password hash")
        void shouldHandleNullPasswordHash() {
            // Arrange
            String email = "user@test.com";
            PasswordLoginRequest request = new PasswordLoginRequest(email, "password");

            UserEntity user = new UserEntity();
            user.setEmail(email);
            user.setPasswordSet(false);
            user.setPasswordHash(null);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.loginWithPassword(request)
            );
            assertEquals("Password not set for this account", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Set Password Tests")
    class SetPasswordTests {

        @Test
        @DisplayName("Positive: Should set password for first time")
        void shouldSetPassword() {
            // Arrange
            SetPasswordRequest request = new SetPasswordRequest("NewPassword123!");

            UserEntity currentUser = new UserEntity();
            currentUser.setId(UUID.randomUUID());
            currentUser.setEmail("user@test.com");
            currentUser.setPasswordSet(false);

            UserEntity updatedUser = new UserEntity();
            updatedUser.setId(currentUser.getId());
            updatedUser.setEmail(currentUser.getEmail());
            updatedUser.setPasswordSet(true);

            when(userRepository.update(any(UserEntity.class))).thenReturn(updatedUser);

            // Act
            UserEntity result = authService.setPassword(request, currentUser);

            // Assert
            assertNotNull(result);
            assertTrue(result.isPasswordSet());
            assertNotNull(currentUser.getPasswordHash());
            verify(userRepository, times(1)).update(currentUser);
        }

        @Test
        @DisplayName("Negative: Should fail when user is null")
        void shouldFailWhenUserIsNull() {
            // Arrange
            SetPasswordRequest request = new SetPasswordRequest("password");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.setPassword(request, null)
            );
            assertEquals("User not authenticated", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Change Password Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Positive: Should change password with correct old password")
        void shouldChangePassword() {
            // Arrange
            String oldPassword = "OldPassword123!";
            String newPassword = "NewPassword456!";

            ChangePasswordRequest request = new ChangePasswordRequest(oldPassword, newPassword);

            UserEntity currentUser = new UserEntity();
            currentUser.setId(UUID.randomUUID());
            currentUser.setEmail("user@test.com");
            currentUser.setPasswordSet(true);
            // Generate proper hash for old password
            currentUser.setPasswordHash(PasswordUtil.hashPassword(oldPassword));

            when(userRepository.update(any(UserEntity.class))).thenReturn(currentUser);

            // Act
            UserEntity result = authService.changePassword(request, currentUser);

            // Assert
            assertNotNull(result);
            verify(userRepository, times(1)).update(currentUser);
            // Password hash should exist
            assertNotNull(currentUser.getPasswordHash());
        }

        @Test
        @DisplayName("Negative: Should fail when user is null")
        void shouldFailWhenUserIsNull() {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest("old", "new");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.changePassword(request, null)
            );
            assertEquals("User not authenticated", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail when password not set")
        void shouldFailWhenPasswordNotSet() {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest("old", "new");

            UserEntity currentUser = new UserEntity();
            currentUser.setPasswordSet(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.changePassword(request, currentUser)
            );
            assertEquals("Password not set for this account", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail with wrong old password")
        void shouldFailWithWrongOldPassword() {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword", "NewPassword456!");

            UserEntity currentUser = new UserEntity();
            currentUser.setPasswordSet(true);
            currentUser.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.changePassword(request, currentUser)
            );
            assertEquals("Old password is incorrect", exception.getMessage());
        }

        @Test
        @DisplayName("Edge: Should handle null password hash")
        void shouldHandleNullPasswordHash() {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest("old", "new");

            UserEntity currentUser = new UserEntity();
            currentUser.setPasswordSet(false);
            currentUser.setPasswordHash(null);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.changePassword(request, currentUser)
            );
            assertEquals("Password not set for this account", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Request Password Reset Tests")
    class RequestPasswordResetTests {

        @Test
        @DisplayName("Positive: Should send reset code for existing user")
        void shouldSendResetCode() {
            // Arrange
            String email = "user@test.com";
            PasswordResetRequestRequest request = new PasswordResetRequestRequest(email);

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(email);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // Act
            UserEntity result = authService.requestPasswordReset(request);

            // Assert
            assertNotNull(result);
            assertEquals(email, result.getEmail());
            verify(emailCodeRepository, times(1)).save(any(EmailVerificationCode.class));
        }

        @Test
        @DisplayName("Negative: Should fail when email not found")
        void shouldFailWhenEmailNotFound() {
            // Arrange
            PasswordResetRequestRequest request = new PasswordResetRequestRequest("nonexistent@test.com");

            when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.requestPasswordReset(request)
            );
            assertEquals("Email not found", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Reset Password Tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("Positive: Should reset password with valid code")
        void shouldResetPassword() {
            // Arrange
            String email = "user@test.com";
            String code = "999888";
            String newPassword = "NewResetPassword123!";

            ResetPasswordRequest request = new ResetPasswordRequest(email, code, newPassword);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                email, code, "reset_password", ZonedDateTime.now().plusMinutes(15)
            );

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setPasswordSet(false);

            UserEntity updatedUser = new UserEntity();
            updatedUser.setId(user.getId());
            updatedUser.setEmail(email);
            updatedUser.setPasswordSet(true);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, code, "reset_password"))
                .thenReturn(Optional.of(verificationCode));
            when(userRepository.update(any(UserEntity.class))).thenReturn(updatedUser);

            // Act
            UserEntity result = authService.resetPassword(request);

            // Assert
            assertNotNull(result);
            assertTrue(result.isPasswordSet());
            verify(emailCodeRepository, times(1)).deleteByEmail(email);
        }

        @Test
        @DisplayName("Negative: Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            // Arrange
            ResetPasswordRequest request = new ResetPasswordRequest("nonexistent@test.com", "123456", "password");

            when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resetPassword(request)
            );
            assertEquals("Email not found", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail with invalid reset code")
        void shouldFailWithInvalidCode() {
            // Arrange
            String email = "user@test.com";
            ResetPasswordRequest request = new ResetPasswordRequest(email, "invalid", "password");

            UserEntity user = new UserEntity();
            user.setEmail(email);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, "invalid", "reset_password"))
                .thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resetPassword(request)
            );
            assertEquals("Invalid or expired reset code", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail with expired reset code")
        void shouldFailWithExpiredCode() {
            // Arrange
            String email = "user@test.com";
            String code = "123456";
            ResetPasswordRequest request = new ResetPasswordRequest(email, code, "password");

            EmailVerificationCode expiredCode = new EmailVerificationCode(
                email, code, "reset_password", ZonedDateTime.now().minusMinutes(20)
            );

            UserEntity user = new UserEntity();
            user.setEmail(email);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, code, "reset_password"))
                .thenReturn(Optional.of(expiredCode));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resetPassword(request)
            );
            assertEquals("Reset code has expired", exception.getMessage());
        }

        @Test
        @DisplayName("Edge: Should work even if password was already set")
        void shouldResetEvenIfPasswordAlreadySet() {
            // Arrange
            String email = "user@test.com";
            String code = "123456";
            ResetPasswordRequest request = new ResetPasswordRequest(email, code, "NewPassword");

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                email, code, "reset_password", ZonedDateTime.now().plusMinutes(15)
            );

            UserEntity user = new UserEntity();
            user.setEmail(email);
            user.setPasswordSet(true);
            user.setPasswordHash("oldHash");

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, code, "reset_password"))
                .thenReturn(Optional.of(verificationCode));
            when(userRepository.update(any(UserEntity.class))).thenReturn(user);

            // Act
            UserEntity result = authService.resetPassword(request);

            // Assert
            assertNotNull(result);
            verify(userRepository, times(1)).update(user);
        }
    }
}
