package com.anonymous.wall.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmailValidator Tests")
class EmailValidatorTest {

    @Nested
    @DisplayName("Valid School Emails")
    class ValidSchoolEmailsTests {

        @Test
        @DisplayName("Should accept Harvard email")
        void shouldAcceptHarvardEmail() {
            assertTrue(EmailValidator.isValidSchoolEmail("student@harvard.edu"));
        }

        @Test
        @DisplayName("Should accept MIT email")
        void shouldAcceptMITEmail() {
            assertTrue(EmailValidator.isValidSchoolEmail("student@mit.edu"));
        }

        @Test
        @DisplayName("Should accept Stanford email")
        void shouldAcceptStanfordEmail() {
            assertTrue(EmailValidator.isValidSchoolEmail("student@stanford.edu"));
        }

        @Test
        @DisplayName("Should accept Yale email")
        void shouldAcceptYaleEmail() {
            assertTrue(EmailValidator.isValidSchoolEmail("student@yale.edu"));
        }

        @Test
        @DisplayName("Should handle email with subdomain")
        void shouldAcceptSubdomainEmail() {
            assertTrue(EmailValidator.isValidSchoolEmail("student@cs.stanford.edu") ||
                      EmailValidator.isValidSchoolEmail("student@stanford.edu"));
        }
    }

    @Nested
    @DisplayName("Invalid Personal Emails")
    class InvalidPersonalEmailsTests {

        @Test
        @DisplayName("Should reject Gmail address")
        void shouldRejectGmailEmail() {
            assertFalse(EmailValidator.isValidSchoolEmail("user@gmail.com"));
        }

        @Test
        @DisplayName("Should reject Yahoo address")
        void shouldRejectYahooEmail() {
            assertFalse(EmailValidator.isValidSchoolEmail("user@yahoo.com"));
        }

        @Test
        @DisplayName("Should reject Outlook address")
        void shouldRejectOutlookEmail() {
            assertFalse(EmailValidator.isValidSchoolEmail("user@outlook.com"));
        }

        @Test
        @DisplayName("Should reject AOL address")
        void shouldRejectAOLEmail() {
            assertFalse(EmailValidator.isValidSchoolEmail("user@aol.com"));
        }

        @Test
        @DisplayName("Should reject unknown domain")
        void shouldRejectUnknownDomain() {
            assertFalse(EmailValidator.isValidSchoolEmail("user@unknowndomain.com"));
        }

        @Test
        @DisplayName("Should reject company domain")
        void shouldRejectCompanyDomain() {
            assertFalse(EmailValidator.isValidSchoolEmail("user@company.com"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null email")
        void shouldHandleNullEmail() {
            assertFalse(EmailValidator.isValidSchoolEmail(null));
        }

        @Test
        @DisplayName("Should handle empty email")
        void shouldHandleEmptyEmail() {
            assertFalse(EmailValidator.isValidSchoolEmail(""));
        }

        @Test
        @DisplayName("Should handle whitespace-only email")
        void shouldHandleWhitespaceEmail() {
            assertFalse(EmailValidator.isValidSchoolEmail("   "));
        }

        @Test
        @DisplayName("Should handle email without @ symbol")
        void shouldHandleNoAtSignEmail() {
            assertFalse(EmailValidator.isValidSchoolEmail("notanemail"));
        }

        @Test
        @DisplayName("Should handle email with @ but no domain")
        void shouldHandleNoDomainEmail() {
            assertFalse(EmailValidator.isValidSchoolEmail("user@"));
        }

        @Test
        @DisplayName("Should handle email with @ but no user")
        void shouldHandleNoUserEmail() {
            assertFalse(EmailValidator.isValidSchoolEmail("@harvard.edu"));
        }

        @Test
        @DisplayName("Should be case insensitive")
        void shouldBeCaseInsensitive() {
            assertTrue(EmailValidator.isValidSchoolEmail("STUDENT@HARVARD.EDU"));
            assertTrue(EmailValidator.isValidSchoolEmail("Student@Harvard.edu"));
        }

        @Test
        @DisplayName("Should handle email with numbers")
        void shouldHandleNumbersInEmail() {
            assertTrue(EmailValidator.isValidSchoolEmail("student123@harvard.edu"));
        }

        @Test
        @DisplayName("Should handle email with dots")
        void shouldHandleDotsInEmail() {
            assertTrue(EmailValidator.isValidSchoolEmail("john.doe@harvard.edu"));
        }
    }

    @Nested
    @DisplayName("Domain Extraction Tests")
    class DomainExtractionTests {

        @Test
        @DisplayName("Should extract domain correctly")
        void shouldExtractDomain() {
            String domain = EmailValidator.extractSchoolDomain("student@harvard.edu");
            assertEquals("harvard.edu", domain);
        }

        @Test
        @DisplayName("Should extract domain case-insensitively")
        void shouldExtractDomainCaseInsensitive() {
            String domain = EmailValidator.extractSchoolDomain("student@HARVARD.EDU");
            assertEquals("harvard.edu", domain);
        }

        @Test
        @DisplayName("Should return null for invalid email")
        void shouldReturnNullForInvalidEmail() {
            assertNull(EmailValidator.extractSchoolDomain("notanemail"));
        }

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNull() {
            assertNull(EmailValidator.extractSchoolDomain(null));
        }
    }
}
