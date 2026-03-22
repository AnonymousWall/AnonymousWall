package com.anonymous.wall.repository;

import com.anonymous.wall.entity.EmailVerificationCode;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.ORACLE)
public interface EmailVerificationCodeRepository extends CrudRepository<EmailVerificationCode, UUID> {
    Optional<EmailVerificationCode> findByEmailAndCodeAndPurpose(String email, String code, String purpose);
    void deleteByEmail(String email);
    long deleteByExpiresAtBefore(OffsetDateTime expiresAt);
}
