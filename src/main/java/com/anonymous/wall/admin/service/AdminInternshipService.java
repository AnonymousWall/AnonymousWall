package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.Internship;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.UUID;

public interface AdminInternshipService {
    Page<Internship> getAllInternships(Pageable pageable, UUID userId, Boolean hidden, String sortBy, String sortOrder);
    Internship getInternshipById(UUID id);
    void hideInternship(UUID id);
    void unhideInternship(UUID id);
    Page<Internship> getInternshipsByUserId(UUID userId, Pageable pageable);
}
