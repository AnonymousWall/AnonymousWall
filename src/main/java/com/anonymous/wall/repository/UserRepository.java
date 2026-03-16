package com.anonymous.wall.repository;

import com.anonymous.wall.entity.UserEntity;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.ORACLE)
public interface UserRepository extends CrudRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    
    /**
     * Find all users with pagination
     */
    Page<UserEntity> findAll(Pageable pageable);
    
    // ===== Sorting by basic fields =====
    
    /**
     * Find all users sorted by creation time (newest first)
     */
    Page<UserEntity> findAllOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Find all users sorted by creation time (oldest first)
     */
    Page<UserEntity> findAllOrderByCreatedAtAsc(Pageable pageable);
    
    /**
     * Find all users sorted by school domain (ascending)
     */
    Page<UserEntity> findAllOrderBySchoolDomainAsc(Pageable pageable);
    
    /**
     * Find all users sorted by school domain (descending)
     */
    Page<UserEntity> findAllOrderBySchoolDomainDesc(Pageable pageable);
    
    /**
     * Find all users sorted by report count (most reports first)
     */
    Page<UserEntity> findAllOrderByReportCountDesc(Pageable pageable);
    
    /**
     * Find all users sorted by report count (least reports first)
     */
    Page<UserEntity> findAllOrderByReportCountAsc(Pageable pageable);
    
    // ===== Filtering by blocked status =====
    
    /**
     * Find blocked users with pagination
     */
    Page<UserEntity> findByBlocked(boolean blocked, Pageable pageable);
    
    /**
     * Find blocked users sorted by creation time (newest first)
     */
    Page<UserEntity> findByBlockedOrderByCreatedAtDesc(boolean blocked, Pageable pageable);
    
    /**
     * Find blocked users sorted by creation time (oldest first)
     */
    Page<UserEntity> findByBlockedOrderByCreatedAtAsc(boolean blocked, Pageable pageable);
}
