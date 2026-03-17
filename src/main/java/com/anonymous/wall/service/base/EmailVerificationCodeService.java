package com.anonymous.wall.service.base;

import com.anonymous.wall.entity.EmailVerificationCode;

import java.util.Optional;

public interface EmailVerificationCodeService {
    EmailVerificationCode save(EmailVerificationCode code);
    Optional<EmailVerificationCode> findByEmailAndCodeAndPurpose(String email, String code, String purpose);
    void deleteByEmail(String email);
}
