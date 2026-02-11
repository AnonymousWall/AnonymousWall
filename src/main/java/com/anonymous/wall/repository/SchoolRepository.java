package com.anonymous.wall.repository;

import com.anonymous.wall.entity.School;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface SchoolRepository extends CrudRepository<School, UUID> {
    /**
     * Find school by name
     */
    Optional<School> findByName(String name);
    
    /**
     * Find all schools with pagination
     */
    Page<School> findAll(Pageable pageable);
}
