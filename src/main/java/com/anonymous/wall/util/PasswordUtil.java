package com.anonymous.wall.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Password encryption utility class
 */
public class PasswordUtil {

    /**
     * Hash a password using BCrypt
     */
    public static String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }

    /**
     * Check if a password matches its hash
     */
    public static boolean checkPassword(String password, String hash) {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
    }
}
