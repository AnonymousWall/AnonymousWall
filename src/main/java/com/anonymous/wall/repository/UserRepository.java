package com.anonymous.wall.repository;

import com.anonymous.wall.entity.UserEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface UserRepository extends CrudRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    
    // Admin queries
    Page<UserEntity> findAll(Pageable pageable);
    
    List<UserEntity> findByRole(String role);
    
    @Query("UPDATE users SET account_status = :status WHERE id = :userId")
    void updateAccountStatus(UUID userId, String status);
}
