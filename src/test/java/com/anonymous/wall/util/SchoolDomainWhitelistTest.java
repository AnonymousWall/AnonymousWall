package com.anonymous.wall.util;

import com.anonymous.wall.service.SchoolDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SchoolDomainWhitelist Tests")
class SchoolDomainWhitelistTest {

    private SchoolDomainService mockService;

    @BeforeEach
    void setUp() {
        mockService = mock(SchoolDomainService.class);
        SchoolDomainWhitelist.initialize(mockService);
    }

    @Nested
    @DisplayName("Initialize Tests")
    class InitializeTests {

        @Test
        @DisplayName("Positive: Should initialize with service")
        void shouldInitializeWithService() {
            // Arrange
            SchoolDomainService service = mock(SchoolDomainService.class);

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> SchoolDomainWhitelist.initialize(service));
        }

        @Test
        @DisplayName("Edge: Should handle null service initialization")
        void shouldHandleNullServiceInitialization() {
            // Act & Assert
            assertDoesNotThrow(() -> SchoolDomainWhitelist.initialize(null));
        }
    }

    @Nested
    @DisplayName("IsApprovedDomain Tests")
    class IsApprovedDomainTests {

        @Test
        @DisplayName("Positive: Should return true for approved domain")
        void shouldReturnTrueForApprovedDomain() {
            // Arrange
            when(mockService.isDomainApproved("harvard.edu")).thenReturn(true);

            // Act
            boolean result = SchoolDomainWhitelist.isApprovedDomain("harvard.edu");

            // Assert
            assertTrue(result);
            verify(mockService).isDomainApproved("harvard.edu");
        }

        @Test
        @DisplayName("Negative: Should return false for non-approved domain")
        void shouldReturnFalseForNonApprovedDomain() {
            // Arrange
            when(mockService.isDomainApproved("unknown.edu")).thenReturn(false);

            // Act
            boolean result = SchoolDomainWhitelist.isApprovedDomain("unknown.edu");

            // Assert
            assertFalse(result);
            verify(mockService).isDomainApproved("unknown.edu");
        }

        @Test
        @DisplayName("Edge: Should handle null domain")
        void shouldHandleNullDomain() {
            // Act
            boolean result = SchoolDomainWhitelist.isApprovedDomain(null);

            // Assert
            assertFalse(result);
            verify(mockService, never()).isDomainApproved(any());
        }

        @Test
        @DisplayName("Edge: Should handle empty domain")
        void shouldHandleEmptyDomain() {
            // Act
            boolean result = SchoolDomainWhitelist.isApprovedDomain("");

            // Assert
            assertFalse(result);
            verify(mockService, never()).isDomainApproved(any());
        }

        @Test
        @DisplayName("Edge: Should handle whitespace domain")
        void shouldHandleWhitespaceDomain() {
            // Act
            boolean result = SchoolDomainWhitelist.isApprovedDomain("   ");

            // Assert
            assertFalse(result);
            verify(mockService, never()).isDomainApproved(any());
        }

        @Test
        @DisplayName("Positive: Should convert domain to lowercase")
        void shouldConvertDomainToLowercase() {
            // Arrange
            when(mockService.isDomainApproved("stanford.edu")).thenReturn(true);

            // Act
            boolean result = SchoolDomainWhitelist.isApprovedDomain("STANFORD.EDU");

            // Assert
            assertTrue(result);
            verify(mockService).isDomainApproved("stanford.edu");
        }

        @Test
        @DisplayName("Edge: Should handle exception from service")
        void shouldHandleExceptionFromService() {
            // Arrange
            when(mockService.isDomainApproved(anyString()))
                .thenThrow(new RuntimeException("Database error"));

            // Act
            boolean result = SchoolDomainWhitelist.isApprovedDomain("mit.edu");

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Negative: Should return false when service is not initialized")
        void shouldReturnFalseWhenServiceNotInitialized() {
            // Arrange - reset to null
            SchoolDomainWhitelist.initialize(null);

            // Act
            boolean result = SchoolDomainWhitelist.isApprovedDomain("yale.edu");

            // Assert
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("IsPersonalEmailDomain Tests")
    class IsPersonalEmailDomainTests {

        @Test
        @DisplayName("Positive: Should identify gmail.com as personal")
        void shouldIdentifyGmailAsPersonal() {
            assertTrue(SchoolDomainWhitelist.isPersonalEmailDomain("gmail.com"));
        }

        @Test
        @DisplayName("Positive: Should identify outlook.com as personal")
        void shouldIdentifyOutlookAsPersonal() {
            assertTrue(SchoolDomainWhitelist.isPersonalEmailDomain("outlook.com"));
        }

        @Test
        @DisplayName("Positive: Should identify hotmail.com as personal")
        void shouldIdentifyHotmailAsPersonal() {
            assertTrue(SchoolDomainWhitelist.isPersonalEmailDomain("hotmail.com"));
        }

        @Test
        @DisplayName("Positive: Should identify yahoo.com as personal")
        void shouldIdentifyYahooAsPersonal() {
            assertTrue(SchoolDomainWhitelist.isPersonalEmailDomain("yahoo.com"));
        }

        @Test
        @DisplayName("Positive: Should identify protonmail.com as personal")
        void shouldIdentifyProtonmailAsPersonal() {
            assertTrue(SchoolDomainWhitelist.isPersonalEmailDomain("protonmail.com"));
        }

        @Test
        @DisplayName("Positive: Should identify icloud.com as personal")
        void shouldIdentifyIcloudAsPersonal() {
            assertTrue(SchoolDomainWhitelist.isPersonalEmailDomain("icloud.com"));
        }

        @Test
        @DisplayName("Positive: Should identify temporary email services")
        void shouldIdentifyTemporaryEmailServices() {
            assertTrue(SchoolDomainWhitelist.isPersonalEmailDomain("10minutemail.com"));
            assertTrue(SchoolDomainWhitelist.isPersonalEmailDomain("tempmail.com"));
            assertTrue(SchoolDomainWhitelist.isPersonalEmailDomain("guerrillamail.com"));
        }

        @Test
        @DisplayName("Negative: Should return false for educational domain")
        void shouldReturnFalseForEducationalDomain() {
            assertFalse(SchoolDomainWhitelist.isPersonalEmailDomain("harvard.edu"));
            assertFalse(SchoolDomainWhitelist.isPersonalEmailDomain("mit.edu"));
        }

        @Test
        @DisplayName("Edge: Should be case-insensitive")
        void shouldBeCaseInsensitive() {
            assertTrue(SchoolDomainWhitelist.isPersonalEmailDomain("GMAIL.COM"));
            assertTrue(SchoolDomainWhitelist.isPersonalEmailDomain("GmAiL.CoM"));
            assertTrue(SchoolDomainWhitelist.isPersonalEmailDomain("Outlook.COM"));
        }

        @Test
        @DisplayName("Edge: Should handle null domain")
        void shouldHandleNullDomain() {
            assertFalse(SchoolDomainWhitelist.isPersonalEmailDomain(null));
        }

        @Test
        @DisplayName("Edge: Should handle empty domain")
        void shouldHandleEmptyDomain() {
            assertFalse(SchoolDomainWhitelist.isPersonalEmailDomain(""));
        }

        @Test
        @DisplayName("Negative: Should return false for subdomain of personal email")
        void shouldReturnFalseForSubdomainOfPersonalEmail() {
            // "mail.gmail.com" is not in the list, only "gmail.com"
            assertFalse(SchoolDomainWhitelist.isPersonalEmailDomain("mail.gmail.com"));
        }
    }

    @Nested
    @DisplayName("IsEmailAllowed Tests")
    class IsEmailAllowedTests {

        @Test
        @DisplayName("Positive: Should allow valid educational email")
        void shouldAllowValidEducationalEmail() {
            // Arrange
            when(mockService.isDomainApproved("harvard.edu")).thenReturn(true);

            // Act
            boolean result = SchoolDomainWhitelist.isEmailAllowed("student@harvard.edu");

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Negative: Should reject personal email (gmail)")
        void shouldRejectGmailEmail() {
            // Act
            boolean result = SchoolDomainWhitelist.isEmailAllowed("user@gmail.com");

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Negative: Should reject personal email (outlook)")
        void shouldRejectOutlookEmail() {
            // Act
            boolean result = SchoolDomainWhitelist.isEmailAllowed("user@outlook.com");

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Negative: Should reject email not in whitelist")
        void shouldRejectEmailNotInWhitelist() {
            // Arrange
            when(mockService.isDomainApproved("unknown.com")).thenReturn(false);

            // Act
            boolean result = SchoolDomainWhitelist.isEmailAllowed("user@unknown.com");

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Edge: Should handle null email")
        void shouldHandleNullEmail() {
            // Act
            boolean result = SchoolDomainWhitelist.isEmailAllowed(null);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Edge: Should handle email without @")
        void shouldHandleEmailWithoutAt() {
            // Act
            boolean result = SchoolDomainWhitelist.isEmailAllowed("notanemail");

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Edge: Should handle empty email")
        void shouldHandleEmptyEmail() {
            // Act
            boolean result = SchoolDomainWhitelist.isEmailAllowed("");

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Positive: Should extract domain correctly with multiple @")
        void shouldExtractDomainCorrectlyWithMultipleAt() {
            // Arrange - email with @ in local part (rare but valid)
            when(mockService.isDomainApproved("mit.edu")).thenReturn(true);

            // Act - uses lastIndexOf so gets the last @
            boolean result = SchoolDomainWhitelist.isEmailAllowed("user@test@mit.edu");

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Edge: Should convert domain to lowercase")
        void shouldConvertDomainToLowercase() {
            // Arrange
            when(mockService.isDomainApproved("stanford.edu")).thenReturn(true);

            // Act
            boolean result = SchoolDomainWhitelist.isEmailAllowed("student@STANFORD.EDU");

            // Assert
            assertTrue(result);
            verify(mockService).isDomainApproved("stanford.edu");
        }

        @Test
        @DisplayName("Positive: Should allow email with complex local part")
        void shouldAllowEmailWithComplexLocalPart() {
            // Arrange
            when(mockService.isDomainApproved("berkeley.edu")).thenReturn(true);

            // Act
            boolean result = SchoolDomainWhitelist.isEmailAllowed("first.last+tag@berkeley.edu");

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Edge: Should handle email with only @ at end")
        void shouldHandleEmailWithOnlyAtAtEnd() {
            // Act
            boolean result = SchoolDomainWhitelist.isEmailAllowed("user@");

            // Assert
            assertFalse(result); // Domain would be empty string
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Integration: Personal email should be rejected even if in service")
        void personalEmailShouldBeRejectedEvenIfInService() {
            // Arrange - even if service says gmail is approved, it should be rejected
            when(mockService.isDomainApproved("gmail.com")).thenReturn(true);

            // Act
            boolean result = SchoolDomainWhitelist.isEmailAllowed("user@gmail.com");

            // Assert
            assertFalse(result, "Personal email should be rejected before checking service");
            verify(mockService, never()).isDomainApproved(any());
        }

        @Test
        @DisplayName("Integration: Non-personal, approved domain should be allowed")
        void nonPersonalApprovedDomainShouldBeAllowed() {
            // Arrange
            when(mockService.isDomainApproved("princeton.edu")).thenReturn(true);

            // Act
            boolean result = SchoolDomainWhitelist.isEmailAllowed("student@princeton.edu");

            // Assert
            assertTrue(result);
            verify(mockService).isDomainApproved("princeton.edu");
        }

        @Test
        @DisplayName("Integration: Non-personal, non-approved domain should be rejected")
        void nonPersonalNonApprovedDomainShouldBeRejected() {
            // Arrange
            when(mockService.isDomainApproved("unknown.edu")).thenReturn(false);

            // Act
            boolean result = SchoolDomainWhitelist.isEmailAllowed("user@unknown.edu");

            // Assert
            assertFalse(result);
            verify(mockService).isDomainApproved("unknown.edu");
        }
    }
}
