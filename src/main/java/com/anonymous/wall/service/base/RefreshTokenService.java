package com.anonymous.wall.service.base;

import com.anonymous.wall.entity.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenService {
    void updateRevokedByUserId(UUID userId, boolean revoked);
    void save(RefreshToken refreshToken);
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);
    void update(RefreshToken refreshToken);
}
