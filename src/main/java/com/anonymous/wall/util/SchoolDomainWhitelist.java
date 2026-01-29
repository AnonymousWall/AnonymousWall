package com.anonymous.wall.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Whitelist of approved school/university domains
 * This prevents registration with personal email domains like @gmail.com, @outlook.com, etc.
 *
 * Add legitimate school domains here. When deploying, load from database or config file.
 */
public class SchoolDomainWhitelist {

    /**
     * Approved US Universities & Schools
     */
    private static final Set<String> APPROVED_DOMAINS = new HashSet<>();

    static {
        // Ivy League
        APPROVED_DOMAINS.add("harvard.edu");
        APPROVED_DOMAINS.add("yale.edu");
        APPROVED_DOMAINS.add("princeton.edu");
        APPROVED_DOMAINS.add("upenn.edu");
        APPROVED_DOMAINS.add("dartmouth.edu");
        APPROVED_DOMAINS.add("brown.edu");
        APPROVED_DOMAINS.add("columbia.edu");
        APPROVED_DOMAINS.add("cornell.edu");

        // Top Tech Schools
        APPROVED_DOMAINS.add("mit.edu");
        APPROVED_DOMAINS.add("stanford.edu");
        APPROVED_DOMAINS.add("berkeley.edu");
        APPROVED_DOMAINS.add("caltech.edu");
        APPROVED_DOMAINS.add("cmu.edu");

        // Other Major Universities
        APPROVED_DOMAINS.add("nyu.edu");
        APPROVED_DOMAINS.add("northwestern.edu");
        APPROVED_DOMAINS.add("duke.edu");
        APPROVED_DOMAINS.add("chicago.edu");
        APPROVED_DOMAINS.add("jhu.edu");
        APPROVED_DOMAINS.add("penn.edu");

        // UK Universities
        APPROVED_DOMAINS.add("ox.ac.uk");
        APPROVED_DOMAINS.add("cam.ac.uk");
        APPROVED_DOMAINS.add("lse.ac.uk");
        APPROVED_DOMAINS.add("ucl.ac.uk");
        APPROVED_DOMAINS.add("ic.ac.uk");
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
        return APPROVED_DOMAINS.contains(domain.toLowerCase());
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
     * Get size of whitelist (for testing/monitoring)
     *
     * @return Number of approved domains
     */
    public static int size() {
        return APPROVED_DOMAINS.size();
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
