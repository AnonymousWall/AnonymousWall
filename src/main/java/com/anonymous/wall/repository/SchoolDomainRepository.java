package com.anonymous.wall.repository;

import com.anonymous.wall.entity.SchoolDomain;
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
public interface SchoolDomainRepository extends CrudRepository<SchoolDomain, UUID> {
    /**
     * Find domain by exact match
     */
    Optional<SchoolDomain> findByDomain(String domain);
    
    /**
     * Find all domains for a school
     */
    List<SchoolDomain> findBySchoolId(UUID schoolId);
    
    /**
     * Find all school domains with pagination
     */
    Page<SchoolDomain> findAll(Pageable pageable);
    
    /**
     * Check if a domain exists
     */
    boolean existsByDomain(String domain);
}
