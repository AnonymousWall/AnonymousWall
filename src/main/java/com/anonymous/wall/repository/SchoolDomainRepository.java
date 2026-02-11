package com.anonymous.wall.repository;

import com.anonymous.wall.entity.SchoolDomain;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface SchoolDomainRepository extends CrudRepository<SchoolDomain, UUID> {
    /**
     * Find domain by exact match
     */
    Optional<SchoolDomain> findByDomain(String domain);
    
    /**
     * Check if a domain exists
     */
    boolean existsByDomain(String domain);
}
