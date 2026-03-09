package com.anonymous.wall.util;

import com.anonymous.wall.service.base.SchoolDomainService;

/**
 * Whitelist of approved school/university domains
 * This prevents registration with personal email domains like @gmail.com, @outlook.com, etc.
 *
 * Now uses database for school domains instead of hardcoding.
 */
public class SchoolDomainWhitelist {

    private static volatile SchoolDomainService schoolDomainService;

    /**
     * Initialize the school domain service (called by Micronaut on startup)
     */
    public static void initialize(SchoolDomainService service) {
        schoolDomainService = service;
    }

    /**
     * Check if a domain is in the whitelist
     *
     * @param domain The domain to check (without @)
     * @return true if domain is approved, false otherwise
     */
    public static boolean isApprovedDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }
        
        SchoolDomainService service = schoolDomainService;
        if (service == null) {
            // Service not available - this can happen during tests or early initialization
            return false;
        }
        
        try {
            return service.isDomainApproved(domain.toLowerCase());
        } catch (Exception e) {
            // If there's any error accessing the database, return false
            return false;
        }
    }

    /**
     * Check if a domain is a personal email domain (blocked)
     *
     * @param domain The domain to check
     * @return true if domain is personal/commercial, false otherwise
     */
    public static boolean isPersonalEmailDomain(String domain) {
        if (domain == null) {
            return false;
        }

        String lowerDomain = domain.toLowerCase();

        // Personal/commercial email providers
        return lowerDomain.equals("gmail.com")
            || lowerDomain.equals("outlook.com")
            || lowerDomain.equals("hotmail.com")
            || lowerDomain.equals("yahoo.com")
            || lowerDomain.equals("protonmail.com")
            || lowerDomain.equals("icloud.com")
            || lowerDomain.equals("aol.com")
            || lowerDomain.equals("mail.com")
            || lowerDomain.equals("zoho.com")
            || lowerDomain.equals("yandex.com")
            || lowerDomain.equals("tutanota.com")
            || lowerDomain.equals("mailgun.org")
            || lowerDomain.equals("10minutemail.com")
            || lowerDomain.equals("tempmail.com")
            || lowerDomain.equals("guerrillamail.com");
    }

    /**
     * Check if email domain is allowed for registration
     *
     * @param email The email address
     * @return true if allowed, false if personal domain or not whitelisted
     */
    public static boolean isEmailAllowed(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }

        String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();

        // Reject personal email domains
        if (isPersonalEmailDomain(domain)) {
            return false;
        }

        // Accept if in whitelist
        return isApprovedDomain(domain);
    }
}
