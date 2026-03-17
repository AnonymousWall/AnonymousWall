package com.anonymous.wall.service.impl;

import com.anonymous.wall.entity.RefreshToken;
import com.anonymous.wall.repository.RefreshTokenRepository;
import com.anonymous.wall.service.base.RefreshTokenService;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Optional;
import java.util.UUID;

@Singleton
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Inject
    private RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public void updateRevokedByUserId(UUID userId, boolean revoked) {
        refreshTokenRepository.updateRevokedByUserId(userId, revoked);
    }

    @Override
    @Transactional
    public void save(RefreshToken refreshToken) {
        refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash) {
        return refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash);
    }

    @Override
    @Transactional
    public void update(RefreshToken refreshToken) {
        refreshTokenRepository.update(refreshToken);
    }
}
