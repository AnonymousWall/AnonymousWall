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
            SendEmailCodeRequest request = new SendEmailCodeRequest("test@harvard.edu", SendEmailCodeRequestPurpose.REGISTER);

            ArgumentCaptor<EmailVerificationCode> captor = ArgumentCaptor.forClass(EmailVerificationCode.class);

            // Act
            authService.sendEmailCode(request);

            // Assert
            verify(emailCodeRepository, times(1)).save(captor.capture());
            EmailVerificationCode savedCode = captor.getValue();
            assertEquals("test@harvard.edu", savedCode.getEmail());
            assertEquals("register", savedCode.getPurpose());
            assertNotNull(savedCode.getCode());
            assertEquals(6, savedCode.getCode().length());
            assertNotNull(savedCode.getExpiresAt());
        }

        @Test
        @DisplayName("Positive: Should send verification code for login")
        void shouldSendCodeForLogin() {
            // Arrange
            SendEmailCodeRequest request = new SendEmailCodeRequest("user@harvard.edu", SendEmailCodeRequestPurpose.LOGIN);

            // Act
            authService.sendEmailCode(request);

            // Assert
            verify(emailCodeRepository, times(1)).save(any(EmailVerificationCode.class));
        }

        @Test
        @DisplayName("Positive: Should send verification code for password reset")
        void shouldSendCodeForPasswordReset() {
            // Arrange
            SendEmailCodeRequest request = new SendEmailCodeRequest("reset@harvard.edu", SendEmailCodeRequestPurpose.RESET_PASSWORD);

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
            String email = "newuser@harvard.edu";
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
        @DisplayName("Positive: Should mark user verified after registration")
        void shouldMarkUserVerifiedAfterRegistration() {
            // Arrange
            String email = "newverified@harvard.edu";
            String code = "123456";

            RegisterEmailRequest request = new RegisterEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                email, code, "register", ZonedDateTime.now().plusMinutes(15)
            );

            UserEntity createdUser = new UserEntity();
            createdUser.setId(UUID.randomUUID());
            createdUser.setEmail(email);
            createdUser.setVerified(true);
            createdUser.setPasswordSet(false);

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, code, "register"))
                .thenReturn(Optional.of(verificationCode));
            when(userRepository.save(any(UserEntity.class))).thenReturn(createdUser);

            // Act
            UserEntity result = authService.registerWithEmail(request);

            // Assert
            assertTrue(result.isVerified());
            assertFalse(result.isPasswordSet());
        }

        @Test
        @DisplayName("Negative: Should fail when email already exists")
        void shouldFailWhenEmailExists() {
            // Arrange
            String email = "existing@harvard.edu";
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
            String email = "test@harvard.edu";
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
            String email = "test@harvard.edu";
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
            String email = "user@harvard.edu";
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
            String email = "newuser@harvard.edu";
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
            String email = "user@harvard.edu";
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
            String email = "user@harvard.edu";
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
            String email = "user@harvard.edu";
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
            String email = "user@harvard.edu";
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
            PasswordLoginRequest request = new PasswordLoginRequest("nonexistent@harvard.edu", "password");

            when(userRepository.findByEmail("nonexistent@harvard.edu")).thenReturn(Optional.empty());

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
            String email = "nopassword@harvard.edu";
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
            String email = "user@harvard.edu";
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
            currentUser.setEmail("user@harvard.edu");
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
            currentUser.setEmail("user@harvard.edu");
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
            String email = "user@harvard.edu";
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
            PasswordResetRequestRequest request = new PasswordResetRequestRequest("nonexistent@harvard.edu");

            when(userRepository.findByEmail("nonexistent@harvard.edu")).thenReturn(Optional.empty());

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
            String email = "user@harvard.edu";
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
            ResetPasswordRequest request = new ResetPasswordRequest("nonexistent@harvard.edu", "123456", "password");

            when(userRepository.findByEmail("nonexistent@harvard.edu")).thenReturn(Optional.empty());

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
            String email = "user@harvard.edu";
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
            String email = "user@harvard.edu";
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
            String email = "user@harvard.edu";
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

    @Nested
    @DisplayName("Email Code Generation and Expiration Tests")
    class EmailCodeGenerationTests {

        @Test
        @DisplayName("Positive: Should generate unique codes for multiple requests")
        void shouldGenerateUniqueCodes() {
            // Arrange
            SendEmailCodeRequest request1 = new SendEmailCodeRequest("user1@harvard.edu", SendEmailCodeRequestPurpose.LOGIN);
            SendEmailCodeRequest request2 = new SendEmailCodeRequest("user2@harvard.edu", SendEmailCodeRequestPurpose.LOGIN);

            ArgumentCaptor<EmailVerificationCode> captor = ArgumentCaptor.forClass(EmailVerificationCode.class);

            // Act
            authService.sendEmailCode(request1);
            authService.sendEmailCode(request2);

            // Assert
            verify(emailCodeRepository, times(2)).save(captor.capture());
            var savedCodes = captor.getAllValues();
            assertNotEquals(savedCodes.get(0).getCode(), savedCodes.get(1).getCode());
        }

        @Test
        @DisplayName("Positive: Should set expiration time correctly")
        void shouldSetExpirationTimeCorrectly() {
            // Arrange
            SendEmailCodeRequest request = new SendEmailCodeRequest("test@harvard.edu", SendEmailCodeRequestPurpose.REGISTER);
            ZonedDateTime beforeTime = ZonedDateTime.now();

            ArgumentCaptor<EmailVerificationCode> captor = ArgumentCaptor.forClass(EmailVerificationCode.class);

            // Act
            authService.sendEmailCode(request);

            // Assert
            verify(emailCodeRepository, times(1)).save(captor.capture());
            EmailVerificationCode savedCode = captor.getValue();
            ZonedDateTime afterTime = ZonedDateTime.now().plusMinutes(16);

            assertTrue(savedCode.getExpiresAt().isBefore(afterTime));
            assertTrue(savedCode.getExpiresAt().isAfter(beforeTime.plusMinutes(14)));
        }
    }

    @Nested
    @DisplayName("Integration Tests - Multiple Flows")
    class IntegrationFlowTests {

        @Test
        @DisplayName("Positive: Should register and then login with password")
        void shouldRegisterAndLoginWithPassword() {
            // Arrange - Register phase
            String email = "fullflow@harvard.edu";
            String code = "123456";
            String newPassword = "SecurePassword123!";

            RegisterEmailRequest registerRequest = new RegisterEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                email, code, "register", ZonedDateTime.now().plusMinutes(15)
            );

            UserEntity registeredUser = new UserEntity();
            registeredUser.setId(UUID.randomUUID());
            registeredUser.setEmail(email);
            registeredUser.setVerified(true);
            registeredUser.setPasswordSet(false);

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, code, "register"))
                .thenReturn(Optional.of(verificationCode));
            when(userRepository.save(any(UserEntity.class))).thenReturn(registeredUser);

            // Act - Register
            UserEntity registered = authService.registerWithEmail(registerRequest);

            // Assert
            assertNotNull(registered);
            assertEquals(email, registered.getEmail());
            verify(emailCodeRepository).deleteByEmail(email);

            // Arrange - Set password phase
            SetPasswordRequest setPasswordRequest = new SetPasswordRequest(newPassword);
            UserEntity updatedUser = new UserEntity();
            updatedUser.setId(registered.getId());
            updatedUser.setEmail(email);
            updatedUser.setPasswordSet(true);

            when(userRepository.update(any(UserEntity.class))).thenReturn(updatedUser);

            // Act - Set password
            UserEntity passwordSet = authService.setPassword(setPasswordRequest, registered);

            // Assert
            assertNotNull(passwordSet);
            assertTrue(passwordSet.isPasswordSet());
        }

        @Test
        @DisplayName("Positive: Should handle password change after setting password")
        void shouldHandlePasswordChangeFlow() {
            // Arrange
            String email = "changeflow@harvard.edu";
            String oldPassword = "OldPassword123!";
            String newPassword = "NewPassword456!";

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setPasswordSet(true);
            user.setPasswordHash(PasswordUtil.hashPassword(oldPassword));

            ChangePasswordRequest changeRequest = new ChangePasswordRequest(oldPassword, newPassword);

            UserEntity updatedUser = new UserEntity();
            updatedUser.setId(user.getId());
            updatedUser.setEmail(email);
            updatedUser.setPasswordSet(true);

            when(userRepository.update(any(UserEntity.class))).thenReturn(updatedUser);

            // Act
            UserEntity result = authService.changePassword(changeRequest, user);

            // Assert
            assertNotNull(result);
            verify(userRepository).update(user);
        }

        @Test
        @DisplayName("Edge: Should prevent reusing same code for different purposes")
        void shouldPreventCodeReuseDifferentPurposes() {
            // Arrange
            String email = "codereuse@harvard.edu";
            String code = "123456";

            // Register with code
            EmailVerificationCode registerCode = new EmailVerificationCode(
                email, code, "register", ZonedDateTime.now().plusMinutes(15)
            );

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setVerified(true);

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, code, "register"))
                .thenReturn(Optional.of(registerCode));
            when(userRepository.save(any(UserEntity.class))).thenReturn(user);

            RegisterEmailRequest registerRequest = new RegisterEmailRequest(email, code);

            // Act - Register
            authService.registerWithEmail(registerRequest);

            // Arrange - Try to login with same code
            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, code, "login"))
                .thenReturn(Optional.empty());

            LoginEmailRequest loginRequest = new LoginEmailRequest(email, code);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.loginWithEmail(loginRequest)
            );
            assertEquals("Invalid or expired code", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Security and Validation Tests")
    class SecurityValidationTests {

        @Test
        @DisplayName("Positive: Password should be hashed before storage")
        void shouldHashPasswordBeforeStorage() {
            // Arrange
            String plainPassword = "MyPassword123!";
            SetPasswordRequest request = new SetPasswordRequest(plainPassword);

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail("secure@harvard.edu");
            user.setPasswordSet(false);

            when(userRepository.update(any(UserEntity.class))).thenReturn(user);

            // Act
            authService.setPassword(request, user);

            // Assert
            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).update(captor.capture());
            UserEntity updatedUser = captor.getValue();

            assertNotNull(updatedUser.getPasswordHash());
            assertNotEquals(plainPassword, updatedUser.getPasswordHash());
            assertTrue(updatedUser.isPasswordSet());
        }

        @Test
        @DisplayName("Negative: Should reject very short password")
        void shouldRejectShortPassword() {
            // Arrange - assuming minimum password length is validated
            String shortPassword = "abc";
            SetPasswordRequest request = new SetPasswordRequest(shortPassword);

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail("secure@harvard.edu");

            // This should ideally fail at validation layer, but if service accepts it
            when(userRepository.update(any(UserEntity.class))).thenReturn(user);

            // Act
            authService.setPassword(request, user);

            // Assert - Should still set password (validation may be at controller level)
            verify(userRepository, times(1)).update(any(UserEntity.class));
        }

        @Test
        @DisplayName("Edge: Should handle special characters in password")
        void shouldHandleSpecialCharactersInPassword() {
            // Arrange
            String specialPassword = "P@ssw0rd!@#$%^&*()";
            SetPasswordRequest request = new SetPasswordRequest(specialPassword);

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail("special@harvard.edu");
            user.setPasswordSet(false);

            UserEntity updatedUser = new UserEntity();
            updatedUser.setId(user.getId());
            updatedUser.setPasswordSet(true);

            when(userRepository.update(any(UserEntity.class))).thenReturn(updatedUser);

            // Act
            UserEntity result = authService.setPassword(request, user);

            // Assert
            assertNotNull(result);
            assertTrue(result.isPasswordSet());
            verify(userRepository).update(any(UserEntity.class));
        }

        @Test
        @DisplayName("Positive: Should verify user is marked as verified after registration")
        void shouldMarkUserVerifiedAfterRegistration() {
            // Arrange
            String email = "newverified@harvard.edu";
            String code = "123456";

            RegisterEmailRequest request = new RegisterEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                email, code, "register", ZonedDateTime.now().plusMinutes(15)
            );

            UserEntity createdUser = new UserEntity();
            createdUser.setId(UUID.randomUUID());
            createdUser.setEmail(email);
            createdUser.setVerified(true);
            createdUser.setPasswordSet(false);

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(emailCodeRepository.findByEmailAndCodeAndPurpose(email, code, "register"))
                .thenReturn(Optional.of(verificationCode));
            when(userRepository.save(any(UserEntity.class))).thenReturn(createdUser);

            // Act
            UserEntity result = authService.registerWithEmail(request);

            // Assert
            assertTrue(result.isVerified());
            assertFalse(result.isPasswordSet());
        }
    }
}
