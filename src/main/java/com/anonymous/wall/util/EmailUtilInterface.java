package com.anonymous.wall.util;

public interface EmailUtilInterface {
    void sendVerificationCodeEmail(String email, String code, String purpose);
}
