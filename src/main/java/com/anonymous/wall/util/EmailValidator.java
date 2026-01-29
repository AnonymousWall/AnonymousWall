package com.anonymous.wall.util;

/**
 * Utility class for validating school/educational email addresses
 * Uses SchoolDomainWhitelist to ensure only legitimate school domains are allowed
 */
public class EmailValidator {

    /**
     * Validates if an email is a valid school/educational email
     *
     * @param email The email address to validate
     * @return true if the email is from an approved school domain, false otherwise
     */
    public static boolean isValidSchoolEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        email = email.toLowerCase().trim();

        // Check if email is from a personal domain (reject immediately)
        if (isPersonalEmail(email)) {
            return false;
        }

        // Check if domain is in school whitelist
        String domain = extractSchoolDomain(email);
        if (domain == null) {
            return false;
        }

        return SchoolDomainWhitelist.isEmailAllowed(email);
    }

    /**
     * Check if email is from a personal provider
     *
     * @param email The email address
     * @return true if personal email domain, false otherwise
     */
    private static boolean isPersonalEmail(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }

        String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();
        return SchoolDomainWhitelist.isPersonalEmailDomain(domain);
    }

    /**
     * Extract school domain from email
     *
     * @param email The email address
     * @return The domain part of the email (after @), or null if invalid
     */
    public static String extractSchoolDomain(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        int atIndex = email.lastIndexOf('@');
        if (atIndex > 0 && atIndex < email.length() - 1) {
            return email.substring(atIndex + 1).toLowerCase();
        }

        return null;
    }
}
