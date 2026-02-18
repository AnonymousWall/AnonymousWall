package com.anonymous.wall.service;

import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateInternshipRequest;
import com.anonymous.wall.repository.InternshipRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class InternshipServiceImpl implements InternshipService {

    private static final Logger log = LoggerFactory.getLogger(InternshipServiceImpl.class);

    @Inject
    private InternshipRepository internshipRepository;

    @Inject
    private UserRepository userRepository;

    @Override
    @Transactional
    public Internship createInternship(CreateInternshipRequest request, UUID userId) {
        log.info("Creating internship for user {}", userId);

        // Validate user exists
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        // Validate required fields
        if (request.getCompany() == null || request.getCompany().trim().isEmpty()) {
            throw new IllegalArgumentException("Company is required");
        }

        if (request.getRole() == null || request.getRole().trim().isEmpty()) {
            throw new IllegalArgumentException("Role is required");
        }

        String trimmedCompany = request.getCompany().trim();
        String trimmedRole = request.getRole().trim();

        if (trimmedCompany.length() > 255) {
            throw new IllegalArgumentException("Company name cannot exceed 255 characters");
        }

        if (trimmedRole.length() > 255) {
            throw new IllegalArgumentException("Role cannot exceed 255 characters");
        }

        // Create the internship
        Internship internship = new Internship();
        internship.setUserId(userId);
        internship.setCompany(trimmedCompany);
        internship.setRole(trimmedRole);
        internship.setSalary(request.getSalary());
        internship.setLocation(request.getLocation());
        internship.setDescription(request.getDescription());
        internship.setDeadline(request.getDeadline());
        internship.setCreatedAt(OffsetDateTime.now());
        internship.setUpdatedAt(OffsetDateTime.now());

        Internship saved = internshipRepository.save(internship);
        log.info("Created internship {} for user {}", saved.getId(), userId);
        return saved;
    }

    @Override
    public Page<Internship> listInternships(Pageable pageable) {
        log.info("Listing internships");
        return internshipRepository.findAllOrderByCreatedAtDesc(pageable);
    }
}
