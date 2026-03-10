package com.anonymous.wall.service.impl;

import com.anonymous.wall.entity.EmailVerificationCode;
import com.anonymous.wall.repository.EmailVerificationCodeRepository;
import com.anonymous.wall.service.base.EmailVerificationCodeService;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
public class EmailVerificationCodeServiceImpl implements EmailVerificationCodeService {
    @Inject
    private EmailVerificationCodeRepository emailCodeRepository;

    @Override
    @Transactional
    public EmailVerificationCode save(EmailVerificationCode code) {
        return emailCodeRepository.save(code);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmailVerificationCode> findByEmailAndCodeAndPurpose(String email, String code, String purpose) {
        return emailCodeRepository.findByEmailAndCodeAndPurpose(email, code, purpose);
    }

    @Override
    @Transactional
    public void deleteByEmail(String email) {
        emailCodeRepository.deleteByEmail(email);
    }
}
