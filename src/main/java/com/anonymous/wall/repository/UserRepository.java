package com.anonymous.wall.repository;

import com.anonymous.wall.entity.UserEntity;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface UserRepository extends CrudRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
}
