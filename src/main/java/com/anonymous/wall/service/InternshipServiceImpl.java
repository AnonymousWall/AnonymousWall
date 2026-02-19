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
    public Page<Internship> listInternships(Pageable pageable, String sortBy) {
        log.info("Listing internships with sortBy={}", sortBy);

        if (sortBy == null) {
            sortBy = "newest";
        }

        // Only show non-hidden internships
        switch (sortBy.toLowerCase()) {
            case "oldest":
                return internshipRepository.findByHiddenOrderByCreatedAtAsc(false, pageable);
            case "newest":
            default:
                return internshipRepository.findByHiddenOrderByCreatedAtDesc(false, pageable);
        }
    }

    @Override
    public Internship getInternship(UUID internshipId) {
        log.info("Getting internship {}", internshipId);
        return internshipRepository.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found"));
    }

    @Override
    @Transactional
    public void hideInternship(UUID internshipId, UUID userId) {
        log.info("Hiding internship {} for user {}", internshipId, userId);

        Optional<Internship> internshipOpt = internshipRepository.findById(internshipId);
        if (internshipOpt.isEmpty()) {
            throw new IllegalArgumentException("Internship not found");
        }

        Internship internship = internshipOpt.get();

        // Check ownership
        if (!internship.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only hide your own internship postings");
        }

        internship.setHidden(true);
        internship.setUpdatedAt(OffsetDateTime.now());
        internshipRepository.update(internship);
        log.info("Hid internship {}", internshipId);
    }

    @Override
    @Transactional
    public void unhideInternship(UUID internshipId, UUID userId) {
        log.info("Unhiding internship {} for user {}", internshipId, userId);

        Optional<Internship> internshipOpt = internshipRepository.findById(internshipId);
        if (internshipOpt.isEmpty()) {
            throw new IllegalArgumentException("Internship not found");
        }

        Internship internship = internshipOpt.get();

        // Check ownership
        if (!internship.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only unhide your own internship postings");
        }

        internship.setHidden(false);
        internship.setUpdatedAt(OffsetDateTime.now());
        internshipRepository.update(internship);
        log.info("Unhid internship {}", internshipId);
    }
}
