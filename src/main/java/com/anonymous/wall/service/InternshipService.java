package com.anonymous.wall.service;

import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.model.CreateInternshipRequest;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.UUID;

public interface InternshipService {

    /**
     * Create a new internship posting
     *
     * @param request the internship creation request
     * @param userId  the user creating the internship
     * @return the created internship
     */
    Internship createInternship(CreateInternshipRequest request, UUID userId);

    /**
     * List internship postings with pagination and sorting
     *
     * @param pageable pagination parameters
     * @param sortBy   sort option (newest, oldest)
     * @return page of internships
     */
    Page<Internship> listInternships(Pageable pageable, String sortBy);

    /**
     * Get a specific internship posting by ID
     *
     * @param internshipId the internship ID
     * @return the internship posting
     */
    Internship getInternship(UUID internshipId);

    /**
     * Hide an internship posting
     *
     * @param internshipId the internship ID
     * @param userId       the user attempting to hide
     */
    void hideInternship(UUID internshipId, UUID userId);

    /**
     * Unhide an internship posting
     *
     * @param internshipId the internship ID
     * @param userId       the user attempting to unhide
     */
    void unhideInternship(UUID internshipId, UUID userId);
}
