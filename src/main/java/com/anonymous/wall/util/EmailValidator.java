package com.anonymous.wall.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for validating school/educational email addresses
 */
public class EmailValidator {

    // Allowed school email domains/patterns
    // Add more domains or patterns as needed
    private static final Set<String> ALLOWED_DOMAINS = new HashSet<>(Arrays.asList(
        ".edu",                    // All .edu domains
        ".ac.uk",                  // UK academic institutions
        ".ac.jp",                  // Japanese academic institutions
        ".edu.au",                 // Australian educational institutions
        ".ac.nz"                   // New Zealand academic institutions
    ));

    // Specific whitelisted institutional domains
    private static final Set<String> WHITELISTED_DOMAINS = new HashSet<>(Arrays.asList(
        // Add specific school domains here if needed
        // Example: "harvard.edu", "mit.edu", "stanford.edu"
        // Test domains
        "test.com",
        "example.com"
    ));

    /**
     * Validates if an email is a valid school/educational email
     *
     * @param email The email address to validate
     * @return true if the email is from an allowed school domain, false otherwise
     */
    public static boolean isValidSchoolEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        email = email.toLowerCase().trim();

        // Check if it's in the whitelisted domains
        for (String domain : WHITELISTED_DOMAINS) {
            if (email.endsWith("@" + domain)) {
                return true;
            }
        }

        // Check if it matches allowed domain patterns
        for (String pattern : ALLOWED_DOMAINS) {
            if (email.endsWith(pattern)) {
                return true;
            }
        }

        return false;
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

    /**
     * Add a custom allowed domain to the whitelist
     *
     * @param domain The domain to add (without @), e.g., "myuniversity.edu"
     */
    public static void addWhitelistedDomain(String domain) {
        if (domain != null && !domain.isEmpty()) {
            WHITELISTED_DOMAINS.add(domain.toLowerCase());
        }
    }
}
