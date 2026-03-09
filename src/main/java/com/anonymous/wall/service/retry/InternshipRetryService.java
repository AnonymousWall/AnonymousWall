package com.anonymous.wall.service.retry;

import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.model.CreateInternshipRequest;
import com.anonymous.wall.service.base.InternshipService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Singleton;

import java.util.UUID;

/**
 * Internship retry wrapper.
 */
@Singleton
public class InternshipRetryService {

    private final InternshipService internshipService;

    public InternshipRetryService(InternshipService internshipService) {
        this.internshipService = internshipService;
    }

    @Retryable(attempts = "3", delay = "500ms")
    public Internship createInternship(CreateInternshipRequest request, UUID userId) {
        return internshipService.createInternship(request, userId);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public Page<Internship> listInternships(Pageable pageable, String sortBy) {
        return internshipService.listInternships(pageable, sortBy);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public Page<Internship> getInternshipsByWall(String wall, Pageable pageable, UUID userId, String schoolDomain, String sortBy) {
        return internshipService.getInternshipsByWall(wall, pageable, userId, schoolDomain, sortBy);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public Internship getInternship(UUID internshipId) {
        return internshipService.getInternship(internshipId);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public Internship getInternship(UUID internshipId, UUID userId) {
        return internshipService.getInternship(internshipId, userId);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public void hideInternship(UUID internshipId, UUID userId) {
        internshipService.hideInternship(internshipId, userId);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public void unhideInternship(UUID internshipId, UUID userId) {
        internshipService.unhideInternship(internshipId, userId);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public Page<Internship> getUserOwnInternships(UUID userId, Pageable pageable, String sortBy) {
        return internshipService.getUserOwnInternships(userId, pageable, sortBy);
    }
}
