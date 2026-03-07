package com.anonymous.wall.repository;

import com.anonymous.wall.entity.RefreshToken;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.ORACLE)
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    void updateRevokedByUserId(UUID userId, boolean revoked);

    void deleteByExpiresAtBefore(OffsetDateTime expiresAt);
}
