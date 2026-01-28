package com.anonymous.wall.util;

import java.security.SecureRandom;

/**
 * Generate verification codes
 */
public class CodeGenerator {
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate a 6-digit verification code
     */
    public static String generateCode() {
        int code = random.nextInt(1000000);
        return String.format("%06d", code);
    }
}
