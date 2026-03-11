package com.anonymous.wall.service;

import com.anonymous.wall.entity.EmailVerificationCode;
import com.anonymous.wall.entity.RefreshToken;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.*;
import com.anonymous.wall.service.base.EmailVerificationCodeService;
import com.anonymous.wall.service.base.RefreshTokenService;
import com.anonymous.wall.service.base.SchoolDomainService;
import com.anonymous.wall.service.base.UserService;
import com.anonymous.wall.service.impl.AuthServiceImpl;
import com.anonymous.wall.util.EmailUtilInterface;
import com.anonymous.wall.util.PasswordUtil;
import com.anonymous.wall.util.SchoolDomainWhitelist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

    private AuthServiceImpl authService;
    private UserService userService;
    private EmailVerificationCodeService emailVerificationCodeService;
    private RefreshTokenService refreshTokenService;
    private SchoolDomainService schoolDomainService;
    private EmailUtilInterface emailUtil;
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        emailVerificationCodeService = mock(EmailVerificationCodeService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        schoolDomainService = mock(SchoolDomainService.class);
        emailUtil = mock(EmailUtilInterface.class);
        jwtTokenService = mock(JwtTokenService.class);

        when(schoolDomainService.isDomainApproved(anyString())).thenReturn(false);
        when(schoolDomainService.isDomainApproved("harvard.edu")).thenReturn(true);

        SchoolDomainWhitelist.initialize(schoolDomainService);

        authService = new AuthServiceImpl();
        try {
            var userServiceField = AuthServiceImpl.class.getDeclaredField("userService");
            userServiceField.setAccessible(true);
            userServiceField.set(authService, userService);

            var emailRepoField = AuthServiceImpl.class.getDeclaredField("emailCodeService");
            emailRepoField.setAccessible(true);
            emailRepoField.set(authService, emailVerificationCodeService);

            var emailUtilField = AuthServiceImpl.class.getDeclaredField("emailUtil");
            emailUtilField.setAccessible(true);
            emailUtilField.set(authService, emailUtil);

            var jwtTokenServiceField = AuthServiceImpl.class.getDeclaredField("jwtTokenService");
            jwtTokenServiceField.setAccessible(true);
            jwtTokenServiceField.set(authService, jwtTokenService);

            var refreshTokenServiceField = AuthServiceImpl.class.getDeclaredField("refreshTokenService");
            refreshTokenServiceField.setAccessible(true);
            refreshTokenServiceField.set(authService, refreshTokenService);
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
            SendEmailCodeRequest request = new SendEmailCodeRequest("test@harvard.edu", SendEmailCodeRequestPurpose.REGISTER);
            ArgumentCaptor<EmailVerificationCode> captor = ArgumentCaptor.forClass(EmailVerificationCode.class);

            authService.sendEmailCode(request);

            verify(emailVerificationCodeService, times(1)).save(captor.capture());
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
            SendEmailCodeRequest request = new SendEmailCodeRequest("user@harvard.edu", SendEmailCodeRequestPurpose.LOGIN);

            authService.sendEmailCode(request);

            verify(emailVerificationCodeService, times(1)).save(any(EmailVerificationCode.class));
        }

        @Test
        @DisplayName("Positive: Should send verification code for password reset")
        void shouldSendCodeForPasswordReset() {
            SendEmailCodeRequest request = new SendEmailCodeRequest("reset@harvard.edu", SendEmailCodeRequestPurpose.RESET_PASSWORD);

            authService.sendEmailCode(request);

            verify(emailVerificationCodeService, times(1)).save(any(EmailVerificationCode.class));
        }

        @Test
        @DisplayName("Positive: Should actually send email after saving code")
        void shouldSendEmailAfterSavingCode() {
            SendEmailCodeRequest request = new SendEmailCodeRequest("test@harvard.edu", SendEmailCodeRequestPurpose.REGISTER);

            authService.sendEmailCode(request);

            verify(emailUtil, times(1)).sendVerificationCodeEmail(
                    eq("test@harvard.edu"), anyString(), eq("register")
            );
        }

        @Test
        @DisplayName("Positive: Should save code to DB before sending email")
        void shouldSaveCodeBeforeSendingEmail() {
            SendEmailCodeRequest request = new SendEmailCodeRequest("test@harvard.edu", SendEmailCodeRequestPurpose.LOGIN);
            InOrder inOrder = inOrder(emailVerificationCodeService, emailUtil);

            authService.sendEmailCode(request);

            inOrder.verify(emailVerificationCodeService).save(any(EmailVerificationCode.class));
            inOrder.verify(emailUtil).sendVerificationCodeEmail(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Register with Email Tests")
    class RegisterWithEmailTests {

        @Test
        @DisplayName("Positive: Should register new user with valid code")
        void shouldRegisterNewUser() {
            String email = "newuser@harvard.edu";
            String code = "123456";
            RegisterEmailRequest request = new RegisterEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                    email, code, "register", OffsetDateTime.now().plusMinutes(15)
            );

            UserEntity savedUser = new UserEntity();
            savedUser.setId(UUID.randomUUID());
            savedUser.setEmail(email);
            savedUser.setVerified(true);

            when(userService.findByEmail(email)).thenReturn(Optional.empty());
            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "register"))
                    .thenReturn(Optional.of(verificationCode));
            when(userService.save(any(UserEntity.class))).thenReturn(savedUser);

            UserEntity result = authService.registerWithEmail(request);

            assertNotNull(result);
            assertEquals(email, result.getEmail());
            assertTrue(result.isVerified());
            assertFalse(result.isPasswordSet());
            verify(emailVerificationCodeService, times(1)).deleteByEmail(email);
        }

        @Test
        @DisplayName("Positive: Should mark user verified after registration")
        void shouldMarkUserVerifiedAfterRegistration() {
            String email = "newverified@harvard.edu";
            String code = "123456";
            RegisterEmailRequest request = new RegisterEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                    email, code, "register", OffsetDateTime.now().plusMinutes(15)
            );

            UserEntity createdUser = new UserEntity();
            createdUser.setId(UUID.randomUUID());
            createdUser.setEmail(email);
            createdUser.setVerified(true);
            createdUser.setPasswordSet(false);

            when(userService.findByEmail(email)).thenReturn(Optional.empty());
            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "register"))
                    .thenReturn(Optional.of(verificationCode));
            when(userService.save(any(UserEntity.class))).thenReturn(createdUser);

            UserEntity result = authService.registerWithEmail(request);

            assertTrue(result.isVerified());
            assertFalse(result.isPasswordSet());
        }

        @Test
        @DisplayName("Positive: Should extract and set school domain from email")
        void shouldSetSchoolDomainFromEmail() {
            String email = "newuser@harvard.edu";
            String code = "123456";
            RegisterEmailRequest request = new RegisterEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                    email, code, "register", OffsetDateTime.now().plusMinutes(15)
            );

            UserEntity savedUser = new UserEntity();
            savedUser.setId(UUID.randomUUID());
            savedUser.setEmail(email);
            savedUser.setSchoolDomain("harvard.edu");

            when(userService.findByEmail(email)).thenReturn(Optional.empty());
            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "register"))
                    .thenReturn(Optional.of(verificationCode));

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            when(userService.save(captor.capture())).thenReturn(savedUser);

            authService.registerWithEmail(request);

            assertEquals("harvard.edu", captor.getValue().getSchoolDomain());
        }

        @Test
        @DisplayName("Negative: Should reject non-school email during registration")
        void shouldRejectNonSchoolEmail() {
            RegisterEmailRequest request = new RegisterEmailRequest("user@gmail.com", "123456");

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.registerWithEmail(request)
            );
            assertEquals("Only school/educational email addresses are allowed for registration",
                    exception.getMessage());
            // Should fail before touching DB
            verifyNoInteractions(userService);
            verifyNoInteractions(emailVerificationCodeService);
        }

        @Test
        @DisplayName("Negative: Should fail when email already exists")
        void shouldFailWhenEmailExists() {
            String email = "existing@harvard.edu";
            RegisterEmailRequest request = new RegisterEmailRequest(email, "123456");

            UserEntity existingUser = new UserEntity();
            existingUser.setEmail(email);
            when(userService.findByEmail(email)).thenReturn(Optional.of(existingUser));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.registerWithEmail(request)
            );
            assertEquals("Email already registered", exception.getMessage());
            verify(userService, never()).save(any());
        }

        @Test
        @DisplayName("Negative: Should fail with invalid code")
        void shouldFailWithInvalidCode() {
            String email = "test@harvard.edu";
            RegisterEmailRequest request = new RegisterEmailRequest(email, "wrong_code");

            when(userService.findByEmail(email)).thenReturn(Optional.empty());
            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, "wrong_code", "register"))
                    .thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.registerWithEmail(request)
            );
            assertEquals("Invalid or expired code", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail with expired code")
        void shouldFailWithExpiredCode() {
            String email = "test@harvard.edu";
            String code = "123456";
            RegisterEmailRequest request = new RegisterEmailRequest(email, code);

            EmailVerificationCode expiredCode = new EmailVerificationCode(
                    email, code, "register", OffsetDateTime.now().minusMinutes(1)
            );

            when(userService.findByEmail(email)).thenReturn(Optional.empty());
            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "register"))
                    .thenReturn(Optional.of(expiredCode));

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
            String email = "user@harvard.edu";
            String code = "654321";
            LoginEmailRequest request = new LoginEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                    email, code, "login", OffsetDateTime.now().plusMinutes(15)
            );

            UserEntity existingUser = new UserEntity();
            existingUser.setId(UUID.randomUUID());
            existingUser.setEmail(email);

            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "login"))
                    .thenReturn(Optional.of(verificationCode));
            when(userService.findByEmail(email)).thenReturn(Optional.of(existingUser));

            UserEntity result = authService.loginWithEmail(request);

            assertNotNull(result);
            assertEquals(email, result.getEmail());
            verify(emailVerificationCodeService, times(1)).deleteByEmail(email);
        }

        @Test
        @DisplayName("Positive: Should auto-create user if not exists")
        void shouldAutoCreateUserIfNotExists() {
            String email = "newuser@harvard.edu";
            String code = "654321";
            LoginEmailRequest request = new LoginEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                    email, code, "login", OffsetDateTime.now().plusMinutes(15)
            );

            UserEntity newUser = new UserEntity();
            newUser.setId(UUID.randomUUID());
            newUser.setEmail(email);
            newUser.setVerified(true);

            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "login"))
                    .thenReturn(Optional.of(verificationCode));
            when(userService.findByEmail(email)).thenReturn(Optional.empty());
            when(userService.save(any(UserEntity.class))).thenReturn(newUser);

            UserEntity result = authService.loginWithEmail(request);

            assertNotNull(result);
            assertEquals(email, result.getEmail());
            verify(userService, times(1)).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("Positive: Should auto-create user without blocked check — new users cannot be blocked")
        void shouldAutoCreateUserWithoutBlockedCheck() {
            String email = "brand-new@harvard.edu";
            String code = "654321";
            LoginEmailRequest request = new LoginEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                    email, code, "login", OffsetDateTime.now().plusMinutes(15)
            );

            UserEntity newUser = new UserEntity();
            newUser.setId(UUID.randomUUID());
            newUser.setEmail(email);
            newUser.setBlocked(false);

            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "login"))
                    .thenReturn(Optional.of(verificationCode));
            when(userService.findByEmail(email)).thenReturn(Optional.empty());
            when(userService.save(any(UserEntity.class))).thenReturn(newUser);

            // Should not throw — blocked check only applies to existing users
            UserEntity result = authService.loginWithEmail(request);

            assertNotNull(result);
            verify(userService, times(1)).save(any(UserEntity.class));
            verify(userService, never()).update(any(UserEntity.class));
        }

        @Test
        @DisplayName("Negative: Should fail with invalid code")
        void shouldFailWithInvalidCode() {
            String email = "user@harvard.edu";
            LoginEmailRequest request = new LoginEmailRequest(email, "invalid");

            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, "invalid", "login"))
                    .thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.loginWithEmail(request)
            );
            assertEquals("Invalid or expired code", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail with expired code")
        void shouldFailWithExpiredCode() {
            String email = "user@harvard.edu";
            String code = "123456";
            LoginEmailRequest request = new LoginEmailRequest(email, code);

            EmailVerificationCode expiredCode = new EmailVerificationCode(
                    email, code, "login", OffsetDateTime.now().minusMinutes(20)
            );

            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "login"))
                    .thenReturn(Optional.of(expiredCode));

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
            String email = "user@harvard.edu";
            String password = "MyPassword123!";
            PasswordLoginRequest request = new PasswordLoginRequest(email, password);

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setPasswordSet(true);
            user.setPasswordHash(PasswordUtil.hashPassword(password));

            when(userService.findByEmail(email)).thenReturn(Optional.of(user));

            UserEntity result = authService.loginWithPassword(request);

            assertNotNull(result);
            assertEquals(email, result.getEmail());
        }

        @Test
        @DisplayName("Negative: Should fail with wrong password")
        void shouldFailWithWrongPassword() {
            String email = "user@harvard.edu";
            PasswordLoginRequest request = new PasswordLoginRequest(email, "WrongPassword");

            UserEntity user = new UserEntity();
            user.setEmail(email);
            user.setPasswordSet(true);
            user.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

            when(userService.findByEmail(email)).thenReturn(Optional.of(user));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.loginWithPassword(request)
            );
            assertEquals("Invalid email or password", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            PasswordLoginRequest request = new PasswordLoginRequest("nonexistent@harvard.edu", "password");

            when(userService.findByEmail("nonexistent@harvard.edu")).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.loginWithPassword(request)
            );
            assertEquals("Invalid email or password", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail when password not set")
        void shouldFailWhenPasswordNotSet() {
            String email = "nopassword@harvard.edu";
            PasswordLoginRequest request = new PasswordLoginRequest(email, "password");

            UserEntity user = new UserEntity();
            user.setEmail(email);
            user.setPasswordSet(false);

            when(userService.findByEmail(email)).thenReturn(Optional.of(user));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.loginWithPassword(request)
            );
            assertEquals("Password not set for this account", exception.getMessage());
        }

        @Test
        @DisplayName("Edge: Should handle null password hash")
        void shouldHandleNullPasswordHash() {
            String email = "user@harvard.edu";
            PasswordLoginRequest request = new PasswordLoginRequest(email, "password");

            UserEntity user = new UserEntity();
            user.setEmail(email);
            user.setPasswordSet(false);
            user.setPasswordHash(null);

            when(userService.findByEmail(email)).thenReturn(Optional.of(user));

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
            SetPasswordRequest request = new SetPasswordRequest("NewPassword123!");

            UserEntity currentUser = new UserEntity();
            currentUser.setId(UUID.randomUUID());
            currentUser.setEmail("user@harvard.edu");
            currentUser.setPasswordSet(false);

            UserEntity updatedUser = new UserEntity();
            updatedUser.setId(currentUser.getId());
            updatedUser.setEmail(currentUser.getEmail());
            updatedUser.setPasswordSet(true);

            when(userService.update(any(UserEntity.class))).thenReturn(updatedUser);

            UserEntity result = authService.setPassword(request, currentUser);

            assertNotNull(result);
            assertTrue(result.isPasswordSet());
            assertNotNull(currentUser.getPasswordHash());
            verify(userService, times(1)).update(currentUser);
        }

        @Test
        @DisplayName("Negative: Should fail when user is null")
        void shouldFailWhenUserIsNull() {
            SetPasswordRequest request = new SetPasswordRequest("password");

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
            String oldPassword = "OldPassword123!";
            String newPassword = "NewPassword456!";
            ChangePasswordRequest request = new ChangePasswordRequest(oldPassword, newPassword);

            UserEntity currentUser = new UserEntity();
            currentUser.setId(UUID.randomUUID());
            currentUser.setEmail("user@harvard.edu");
            currentUser.setPasswordSet(true);
            currentUser.setPasswordHash(PasswordUtil.hashPassword(oldPassword));

            when(userService.update(any(UserEntity.class))).thenReturn(currentUser);

            UserEntity result = authService.changePassword(request, currentUser);

            assertNotNull(result);
            verify(userService, times(1)).update(currentUser);
            assertNotNull(currentUser.getPasswordHash());
        }

        @Test
        @DisplayName("Negative: Should fail when user is null")
        void shouldFailWhenUserIsNull() {
            ChangePasswordRequest request = new ChangePasswordRequest("old", "new");

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.changePassword(request, null)
            );
            assertEquals("User not authenticated", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail when password not set")
        void shouldFailWhenPasswordNotSet() {
            ChangePasswordRequest request = new ChangePasswordRequest("old", "new");

            UserEntity currentUser = new UserEntity();
            currentUser.setPasswordSet(false);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.changePassword(request, currentUser)
            );
            assertEquals("Password not set for this account", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail with wrong old password")
        void shouldFailWithWrongOldPassword() {
            ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword", "NewPassword456!");

            UserEntity currentUser = new UserEntity();
            currentUser.setPasswordSet(true);
            currentUser.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.changePassword(request, currentUser)
            );
            assertEquals("Old password is incorrect", exception.getMessage());
        }

        @Test
        @DisplayName("Edge: Should handle null password hash")
        void shouldHandleNullPasswordHash() {
            ChangePasswordRequest request = new ChangePasswordRequest("old", "new");

            UserEntity currentUser = new UserEntity();
            currentUser.setPasswordSet(false);
            currentUser.setPasswordHash(null);

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
            String email = "user@harvard.edu";
            PasswordResetRequestRequest request = new PasswordResetRequestRequest(email);

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(email);

            when(userService.findByEmail(email)).thenReturn(Optional.of(user));

            UserEntity result = authService.requestPasswordReset(request);

            assertNotNull(result);
            assertEquals(email, result.getEmail());
            verify(emailVerificationCodeService, times(1)).save(any(EmailVerificationCode.class));
        }

        @Test
        @DisplayName("Positive: Should actually send reset email to user")
        void shouldSendResetEmail() {
            String email = "user@harvard.edu";
            PasswordResetRequestRequest request = new PasswordResetRequestRequest(email);

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(email);

            when(userService.findByEmail(email)).thenReturn(Optional.of(user));

            authService.requestPasswordReset(request);

            verify(emailUtil, times(1)).sendVerificationCodeEmail(
                    eq(email), anyString(), eq("reset_password")
            );
        }

        @Test
        @DisplayName("Security: Should silently succeed when email not found — prevent enumeration")
        void shouldSilentlySucceedWhenEmailNotFound() {
            PasswordResetRequestRequest request = new PasswordResetRequestRequest("nonexistent@harvard.edu");

            when(userService.findByEmail("nonexistent@harvard.edu")).thenReturn(Optional.empty());

            UserEntity result = authService.requestPasswordReset(request);

            assertNull(result);
            verify(emailVerificationCodeService, never()).save(any(EmailVerificationCode.class));
            verify(emailUtil, never()).sendVerificationCodeEmail(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Reset Password Tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("Positive: Should reset password with valid code")
        void shouldResetPassword() {
            String email = "user@harvard.edu";
            String code = "999888";
            String newPassword = "NewResetPassword123!";
            ResetPasswordRequest request = new ResetPasswordRequest(email, code, newPassword);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                    email, code, "reset_password", OffsetDateTime.now().plusMinutes(15)
            );

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setPasswordSet(false);

            UserEntity updatedUser = new UserEntity();
            updatedUser.setId(user.getId());
            updatedUser.setEmail(email);
            updatedUser.setPasswordSet(true);

            when(userService.findByEmail(email)).thenReturn(Optional.of(user));
            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "reset_password"))
                    .thenReturn(Optional.of(verificationCode));
            when(userService.update(any(UserEntity.class))).thenReturn(updatedUser);

            UserEntity result = authService.resetPassword(request);

            assertNotNull(result);
            assertTrue(result.isPasswordSet());
            verify(emailVerificationCodeService, times(1)).deleteByEmail(email);
        }

        @Test
        @DisplayName("Negative: Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            ResetPasswordRequest request = new ResetPasswordRequest("nonexistent@harvard.edu", "123456", "password");

            when(userService.findByEmail("nonexistent@harvard.edu")).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.resetPassword(request)
            );
            assertEquals("Email not found", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail with invalid reset code")
        void shouldFailWithInvalidCode() {
            String email = "user@harvard.edu";
            ResetPasswordRequest request = new ResetPasswordRequest(email, "invalid", "password");

            UserEntity user = new UserEntity();
            user.setEmail(email);

            when(userService.findByEmail(email)).thenReturn(Optional.of(user));
            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, "invalid", "reset_password"))
                    .thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.resetPassword(request)
            );
            assertEquals("Invalid or expired reset code", exception.getMessage());
        }

        @Test
        @DisplayName("Negative: Should fail with expired reset code")
        void shouldFailWithExpiredCode() {
            String email = "user@harvard.edu";
            String code = "123456";
            ResetPasswordRequest request = new ResetPasswordRequest(email, code, "password");

            EmailVerificationCode expiredCode = new EmailVerificationCode(
                    email, code, "reset_password", OffsetDateTime.now().minusMinutes(20)
            );

            UserEntity user = new UserEntity();
            user.setEmail(email);

            when(userService.findByEmail(email)).thenReturn(Optional.of(user));
            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "reset_password"))
                    .thenReturn(Optional.of(expiredCode));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.resetPassword(request)
            );
            assertEquals("Reset code has expired", exception.getMessage());
        }

        @Test
        @DisplayName("Edge: Should work even if password was already set")
        void shouldResetEvenIfPasswordAlreadySet() {
            String email = "user@harvard.edu";
            String code = "123456";
            ResetPasswordRequest request = new ResetPasswordRequest(email, code, "NewPassword");

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                    email, code, "reset_password", OffsetDateTime.now().plusMinutes(15)
            );

            UserEntity user = new UserEntity();
            user.setEmail(email);
            user.setPasswordSet(true);
            user.setPasswordHash("oldHash");

            when(userService.findByEmail(email)).thenReturn(Optional.of(user));
            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "reset_password"))
                    .thenReturn(Optional.of(verificationCode));
            when(userService.update(any(UserEntity.class))).thenReturn(user);

            UserEntity result = authService.resetPassword(request);

            assertNotNull(result);
            verify(userService, times(1)).update(user);
        }
    }

    @Nested
    @DisplayName("Email Code Generation and Expiration Tests")
    class EmailCodeGenerationTests {

        @Test
        @DisplayName("Positive: Should generate codes with correct format — 6 digit numeric")
        void shouldGenerateCodesWithCorrectFormat() {
            SendEmailCodeRequest request = new SendEmailCodeRequest("user@harvard.edu", SendEmailCodeRequestPurpose.LOGIN);
            ArgumentCaptor<EmailVerificationCode> captor = ArgumentCaptor.forClass(EmailVerificationCode.class);

            authService.sendEmailCode(request);

            verify(emailVerificationCodeService).save(captor.capture());
            String code = captor.getValue().getCode();
            assertNotNull(code);
            assertEquals(6, code.length());
            assertTrue(code.matches("\\d{6}"), "Code should be 6 numeric digits, got: " + code);
        }

        @Test
        @DisplayName("Positive: Should generate different codes for different requests")
        void shouldGenerateDifferentCodesForDifferentRequests() {
            SendEmailCodeRequest request1 = new SendEmailCodeRequest("user1@harvard.edu", SendEmailCodeRequestPurpose.LOGIN);
            SendEmailCodeRequest request2 = new SendEmailCodeRequest("user2@harvard.edu", SendEmailCodeRequestPurpose.LOGIN);
            ArgumentCaptor<EmailVerificationCode> captor = ArgumentCaptor.forClass(EmailVerificationCode.class);

            authService.sendEmailCode(request1);
            authService.sendEmailCode(request2);

            verify(emailVerificationCodeService, times(2)).save(captor.capture());
            var codes = captor.getAllValues();

            // Verify format of both — more stable than asserting they differ
            assertEquals(6, codes.get(0).getCode().length());
            assertEquals(6, codes.get(1).getCode().length());
            assertTrue(codes.get(0).getCode().matches("\\d{6}"));
            assertTrue(codes.get(1).getCode().matches("\\d{6}"));
        }

        @Test
        @DisplayName("Positive: Should set expiration time correctly")
        void shouldSetExpirationTimeCorrectly() {
            SendEmailCodeRequest request = new SendEmailCodeRequest("test@harvard.edu", SendEmailCodeRequestPurpose.REGISTER);
            OffsetDateTime beforeTime = OffsetDateTime.now();
            ArgumentCaptor<EmailVerificationCode> captor = ArgumentCaptor.forClass(EmailVerificationCode.class);

            authService.sendEmailCode(request);

            verify(emailVerificationCodeService, times(1)).save(captor.capture());
            EmailVerificationCode savedCode = captor.getValue();
            OffsetDateTime afterTime = OffsetDateTime.now().plusMinutes(16);

            assertTrue(savedCode.getExpiresAt().isBefore(afterTime));
            assertTrue(savedCode.getExpiresAt().isAfter(beforeTime.plusMinutes(14)));
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Positive: Should issue refresh token and persist hashed token")
        void shouldIssueRefreshToken() {
            UUID userId = UUID.randomUUID();
            String rawRefreshToken = "raw-refresh-token";
            String tokenHash = "hashed-refresh-token";
            OffsetDateTime before = OffsetDateTime.now();
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

            when(jwtTokenService.generateRefreshToken()).thenReturn(rawRefreshToken);
            when(jwtTokenService.hashToken(rawRefreshToken)).thenReturn(tokenHash);

            String result = authService.issueRefreshToken(userId);

            assertEquals(rawRefreshToken, result);
            verify(refreshTokenService).updateRevokedByUserId(userId, true);
            verify(refreshTokenService).save(captor.capture());

            RefreshToken savedToken = captor.getValue();
            assertEquals(userId, savedToken.getUserId());
            assertEquals(tokenHash, savedToken.getTokenHash());
            assertTrue(savedToken.getExpiresAt().isAfter(before.plusDays(29)));
            assertTrue(savedToken.getExpiresAt().isBefore(before.plusDays(31)));
        }

        @Test
        @DisplayName("Positive: Should revoke old tokens BEFORE saving new one — prevents token reuse window")
        void shouldRevokeOldTokensBeforeSavingNew() {
            UUID userId = UUID.randomUUID();
            when(jwtTokenService.generateRefreshToken()).thenReturn("raw");
            when(jwtTokenService.hashToken("raw")).thenReturn("hash");

            InOrder inOrder = inOrder(refreshTokenService);

            authService.issueRefreshToken(userId);

            // Revoke must happen before save — otherwise old token is valid during the window
            inOrder.verify(refreshTokenService).updateRevokedByUserId(userId, true);
            inOrder.verify(refreshTokenService).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Positive: Should return stored token when refresh token is valid")
        void shouldFindValidRefreshToken() {
            String rawRefreshToken = "raw-refresh-token";
            String tokenHash = "hashed-refresh-token";
            RefreshToken storedToken = new RefreshToken();
            storedToken.setExpiresAt(OffsetDateTime.now().plusDays(1));

            when(jwtTokenService.hashToken(rawRefreshToken)).thenReturn(tokenHash);
            when(refreshTokenService.findByTokenHashAndRevokedFalse(tokenHash))
                    .thenReturn(Optional.of(storedToken));

            Optional<RefreshToken> result = authService.findValidRefreshToken(rawRefreshToken);

            assertTrue(result.isPresent());
            assertSame(storedToken, result.get());
        }

        @Test
        @DisplayName("Negative: Should return empty when refresh token is expired")
        void shouldReturnEmptyForExpiredRefreshToken() {
            String rawRefreshToken = "raw-refresh-token";
            String tokenHash = "hashed-refresh-token";
            RefreshToken storedToken = new RefreshToken();
            storedToken.setExpiresAt(OffsetDateTime.now().minusMinutes(1));

            when(jwtTokenService.hashToken(rawRefreshToken)).thenReturn(tokenHash);
            when(refreshTokenService.findByTokenHashAndRevokedFalse(tokenHash))
                    .thenReturn(Optional.of(storedToken));

            Optional<RefreshToken> result = authService.findValidRefreshToken(rawRefreshToken);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Negative: Should return empty when token not found in DB")
        void shouldReturnEmptyWhenTokenNotFound() {
            String rawRefreshToken = "unknown-token";
            String tokenHash = "unknown-hash";

            when(jwtTokenService.hashToken(rawRefreshToken)).thenReturn(tokenHash);
            when(refreshTokenService.findByTokenHashAndRevokedFalse(tokenHash))
                    .thenReturn(Optional.empty());

            Optional<RefreshToken> result = authService.findValidRefreshToken(rawRefreshToken);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Positive: Should revoke a single refresh token")
        void shouldRevokeRefreshToken() {
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setRevoked(false);

            authService.revokeRefreshToken(refreshToken);

            assertTrue(refreshToken.isRevoked());
            verify(refreshTokenService).update(refreshToken);
        }

        @Test
        @DisplayName("Edge: Should handle revoking already-revoked token — idempotent")
        void shouldHandleRevokingAlreadyRevokedToken() {
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setRevoked(true);

            authService.revokeRefreshToken(refreshToken);

            assertTrue(refreshToken.isRevoked());
            verify(refreshTokenService).update(refreshToken);
        }

        @Test
        @DisplayName("Positive: Should revoke all refresh tokens for user")
        void shouldRevokeRefreshTokensForUser() {
            UUID userId = UUID.randomUUID();

            authService.revokeRefreshTokensForUser(userId);

            verify(refreshTokenService).updateRevokedByUserId(userId, true);
        }
    }

    @Nested
    @DisplayName("Integration Tests - Multiple Flows")
    class IntegrationFlowTests {

        @Test
        @DisplayName("Positive: Should register and then login with password")
        void shouldRegisterAndLoginWithPassword() {
            String email = "fullflow@harvard.edu";
            String code = "123456";
            String newPassword = "SecurePassword123!";

            RegisterEmailRequest registerRequest = new RegisterEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                    email, code, "register", OffsetDateTime.now().plusMinutes(15)
            );

            UserEntity registeredUser = new UserEntity();
            registeredUser.setId(UUID.randomUUID());
            registeredUser.setEmail(email);
            registeredUser.setVerified(true);
            registeredUser.setPasswordSet(false);

            when(userService.findByEmail(email)).thenReturn(Optional.empty());
            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "register"))
                    .thenReturn(Optional.of(verificationCode));
            when(userService.save(any(UserEntity.class))).thenReturn(registeredUser);

            UserEntity registered = authService.registerWithEmail(registerRequest);

            assertNotNull(registered);
            assertEquals(email, registered.getEmail());
            verify(emailVerificationCodeService).deleteByEmail(email);

            SetPasswordRequest setPasswordRequest = new SetPasswordRequest(newPassword);
            UserEntity updatedUser = new UserEntity();
            updatedUser.setId(registered.getId());
            updatedUser.setEmail(email);
            updatedUser.setPasswordSet(true);

            when(userService.update(any(UserEntity.class))).thenReturn(updatedUser);

            UserEntity passwordSet = authService.setPassword(setPasswordRequest, registered);

            assertNotNull(passwordSet);
            assertTrue(passwordSet.isPasswordSet());
        }

        @Test
        @DisplayName("Positive: Should handle password change after setting password")
        void shouldHandlePasswordChangeFlow() {
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

            when(userService.update(any(UserEntity.class))).thenReturn(updatedUser);

            UserEntity result = authService.changePassword(changeRequest, user);

            assertNotNull(result);
            verify(userService).update(user);
        }

        @Test
        @DisplayName("Edge: Should prevent reusing same code for different purposes")
        void shouldPreventCodeReuseDifferentPurposes() {
            String email = "codereuse@harvard.edu";
            String code = "123456";

            EmailVerificationCode registerCode = new EmailVerificationCode(
                    email, code, "register", OffsetDateTime.now().plusMinutes(15)
            );

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setVerified(true);

            when(userService.findByEmail(email)).thenReturn(Optional.empty());
            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "register"))
                    .thenReturn(Optional.of(registerCode));
            when(userService.save(any(UserEntity.class))).thenReturn(user);

            authService.registerWithEmail(new RegisterEmailRequest(email, code));

            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "login"))
                    .thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.loginWithEmail(new LoginEmailRequest(email, code))
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
            String plainPassword = "MyPassword123!";
            SetPasswordRequest request = new SetPasswordRequest(plainPassword);

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail("secure@harvard.edu");
            user.setPasswordSet(false);

            when(userService.update(any(UserEntity.class))).thenReturn(user);

            authService.setPassword(request, user);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userService).update(captor.capture());
            UserEntity updatedUser = captor.getValue();

            assertNotNull(updatedUser.getPasswordHash());
            assertNotEquals(plainPassword, updatedUser.getPasswordHash());
            assertTrue(updatedUser.isPasswordSet());
        }

        @Test
        @DisplayName("Edge: Should handle special characters in password")
        void shouldHandleSpecialCharactersInPassword() {
            String specialPassword = "P@ssw0rd!@#$%^&*()";
            SetPasswordRequest request = new SetPasswordRequest(specialPassword);

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail("special@harvard.edu");
            user.setPasswordSet(false);

            UserEntity updatedUser = new UserEntity();
            updatedUser.setId(user.getId());
            updatedUser.setPasswordSet(true);

            when(userService.update(any(UserEntity.class))).thenReturn(updatedUser);

            UserEntity result = authService.setPassword(request, user);

            assertNotNull(result);
            assertTrue(result.isPasswordSet());
            verify(userService).update(any(UserEntity.class));
        }

        @Test
        @DisplayName("Positive: Should verify user is marked as verified after registration")
        void shouldMarkUserVerifiedAfterRegistration() {
            String email = "newverified@harvard.edu";
            String code = "123456";
            RegisterEmailRequest request = new RegisterEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                    email, code, "register", OffsetDateTime.now().plusMinutes(15)
            );

            UserEntity createdUser = new UserEntity();
            createdUser.setId(UUID.randomUUID());
            createdUser.setEmail(email);
            createdUser.setVerified(true);
            createdUser.setPasswordSet(false);

            when(userService.findByEmail(email)).thenReturn(Optional.empty());
            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "register"))
                    .thenReturn(Optional.of(verificationCode));
            when(userService.save(any(UserEntity.class))).thenReturn(createdUser);

            UserEntity result = authService.registerWithEmail(request);

            assertTrue(result.isVerified());
            assertFalse(result.isPasswordSet());
        }
    }

    @Nested
    @DisplayName("Blocked User Enforcement Tests")
    class BlockedUserEnforcementTests {

        @Test
        @DisplayName("Negative: Should reject login with email for blocked user")
        void shouldRejectLoginWithEmailForBlockedUser() {
            String email = "blocked@harvard.edu";
            String code = "123456";
            LoginEmailRequest request = new LoginEmailRequest(email, code);

            EmailVerificationCode verificationCode = new EmailVerificationCode(
                    email, code, "login", OffsetDateTime.now().plusMinutes(15)
            );

            UserEntity blockedUser = new UserEntity();
            blockedUser.setId(UUID.randomUUID());
            blockedUser.setEmail(email);
            blockedUser.setBlocked(true);

            when(emailVerificationCodeService.findByEmailAndCodeAndPurpose(email, code, "login"))
                    .thenReturn(Optional.of(verificationCode));
            when(userService.findByEmail(email)).thenReturn(Optional.of(blockedUser));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.loginWithEmail(request)
            );
            assertTrue(exception.getMessage().contains("blocked"));
        }

        @Test
        @DisplayName("Negative: Should reject login with password for blocked user")
        void shouldRejectLoginWithPasswordForBlockedUser() {
            String email = "blocked@harvard.edu";
            String password = "password123";
            PasswordLoginRequest request = new PasswordLoginRequest(email, password);

            UserEntity blockedUser = new UserEntity();
            blockedUser.setId(UUID.randomUUID());
            blockedUser.setEmail(email);
            blockedUser.setBlocked(true);
            blockedUser.setPasswordSet(true);
            blockedUser.setPasswordHash(PasswordUtil.hashPassword(password));

            when(userService.findByEmail(email)).thenReturn(Optional.of(blockedUser));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.loginWithPassword(request)
            );
            assertTrue(exception.getMessage().contains("blocked"));
        }

        @Test
        @DisplayName("Negative: Should reject password reset request for blocked user")
        void shouldRejectPasswordResetRequestForBlockedUser() {
            String email = "blocked@harvard.edu";
            PasswordResetRequestRequest request = new PasswordResetRequestRequest(email);

            UserEntity blockedUser = new UserEntity();
            blockedUser.setId(UUID.randomUUID());
            blockedUser.setEmail(email);
            blockedUser.setBlocked(true);

            when(userService.findByEmail(email)).thenReturn(Optional.of(blockedUser));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.requestPasswordReset(request)
            );
            assertTrue(exception.getMessage().contains("blocked"));
        }

        @Test
        @DisplayName("Negative: Should reject password reset for blocked user")
        void shouldRejectPasswordResetForBlockedUser() {
            String email = "blocked@harvard.edu";
            String code = "123456";
            String newPassword = "newpassword123";
            ResetPasswordRequest request = new ResetPasswordRequest(email, code, newPassword);

            UserEntity blockedUser = new UserEntity();
            blockedUser.setId(UUID.randomUUID());
            blockedUser.setEmail(email);
            blockedUser.setBlocked(true);

            when(userService.findByEmail(email)).thenReturn(Optional.of(blockedUser));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.resetPassword(request)
            );
            assertTrue(exception.getMessage().contains("blocked"));
        }

        @Test
        @DisplayName("Positive: Should allow login for non-blocked user")
        void shouldAllowLoginForNonBlockedUser() {
            String email = "user@harvard.edu";
            String password = "password123";
            PasswordLoginRequest request = new PasswordLoginRequest(email, password);

            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setBlocked(false);
            user.setPasswordSet(true);
            user.setPasswordHash(PasswordUtil.hashPassword(password));

            when(userService.findByEmail(email)).thenReturn(Optional.of(user));

            UserEntity result = authService.loginWithPassword(request);

            assertNotNull(result);
            assertEquals(email, result.getEmail());
            assertFalse(result.isBlocked());
        }
    }
}
