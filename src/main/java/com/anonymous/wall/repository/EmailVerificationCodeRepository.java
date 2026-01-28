package com.anonymous.wall.repository;

import com.anonymous.wall.entity.EmailVerificationCode;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface EmailVerificationCodeRepository extends CrudRepository<EmailVerificationCode, Long> {
    Optional<EmailVerificationCode> findByEmailAndCodeAndPurpose(String email, String code, String purpose);
    void deleteByEmail(String email);
}
