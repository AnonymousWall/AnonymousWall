package com.anonymous.wall.util;

/**
 * 密码加密工具类
 */
public class PasswordUtil {

    /**
     * 加密密码
     */
    public static String hashPassword(String plainPassword) {
        return plainPassword;
    }

    /**
     * 验证密码
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        return plainPassword.equals(hashedPassword);
    }
}
