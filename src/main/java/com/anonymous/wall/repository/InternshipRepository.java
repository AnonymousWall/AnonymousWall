package com.anonymous.wall.repository;

import com.anonymous.wall.entity.Internship;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface InternshipRepository extends CrudRepository<Internship, UUID> {

    Page<Internship> findByHiddenOrderByCreatedAtDesc(boolean hidden, Pageable pageable);

    Page<Internship> findByHiddenOrderByCreatedAtAsc(boolean hidden, Pageable pageable);

    // Wall-based queries (same pattern as Posts)
    Page<Internship> findByWallAndHiddenFalseOrderByCreatedAtDesc(String wall, Pageable pageable);

    Page<Internship> findByWallAndHiddenFalseOrderByCreatedAtAsc(String wall, Pageable pageable);

    Page<Internship> findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtDesc(String wall, String schoolDomain, Pageable pageable);

    Page<Internship> findByWallAndSchoolDomainAndHiddenFalseOrderByCreatedAtAsc(String wall, String schoolDomain, Pageable pageable);

    List<Internship> findByUserId(UUID userId);

    Page<Internship> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Internship> findByUserIdAndHiddenFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Internship> findByUserIdAndHiddenFalseOrderByCreatedAtAsc(UUID userId, Pageable pageable);

    Optional<Internship> findById(UUID id);

    long countByUserId(UUID userId);

    Internship update(Internship internship);

    void updateProfileNameByUserId(UUID userId, String profileName);
}
