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
     * List internship postings with pagination
     *
     * @param pageable pagination parameters
     * @return page of internships
     */
    Page<Internship> listInternships(Pageable pageable);
}
