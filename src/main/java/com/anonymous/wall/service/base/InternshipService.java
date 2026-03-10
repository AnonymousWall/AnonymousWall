package com.anonymous.wall.service.base;

import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.model.CreateInternshipRequest;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface InternshipService {

    Internship createInternship(CreateInternshipRequest request, UUID userId);

    Page<Internship> listInternships(Pageable pageable, String sortBy);

    Page<Internship> getInternshipsByWall(String wall, Pageable pageable, UUID userId, String schoolDomain, String sortBy);

    Internship getInternship(UUID internshipId);

    Internship getInternship(UUID internshipId, UUID userId);

    void hideInternship(UUID internshipId, UUID userId);

    void unhideInternship(UUID internshipId, UUID userId);

    Page<Internship> getUserOwnInternships(UUID userId, Pageable pageable, String sortBy);

    void updateProfileNameByUserId(UUID userId, String profileName);

    Optional<Internship> findById(UUID internshipId);

    void update(Internship internship);
}
