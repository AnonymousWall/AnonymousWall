package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.repository.InternshipRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Singleton
public class AdminInternshipServiceImpl implements AdminInternshipService {

    private static final Logger log = LoggerFactory.getLogger(AdminInternshipServiceImpl.class);

    @Inject
    private InternshipRepository internshipRepository;

    @Override
    public Page<Internship> getAllInternships(Pageable pageable, UUID userId, Boolean hidden, String sortBy, String sortOrder) {
        log.info("Admin fetching internships - userId={}, hidden={}, sortBy={}, sortOrder={}", userId, hidden, sortBy, sortOrder);

        boolean isDesc = sortOrder == null || sortOrder.equalsIgnoreCase("desc");

        if (userId == null && hidden == null) {
            if (sortBy == null) {
                return internshipRepository.findAll(pageable);
            }
            return isDesc
                    ? internshipRepository.findAllOrderByCreatedAtDesc(pageable)
                    : internshipRepository.findAllOrderByCreatedAtAsc(pageable);
        }

        if (userId != null && hidden == null) {
            return internshipRepository.findByUserId(userId, pageable);
        }
        if (userId == null && hidden != null) {
            return internshipRepository.findByHidden(hidden, pageable);
        }
        return internshipRepository.findByUserIdAndHidden(userId, hidden, pageable);
    }

    @Override
    public Internship getInternshipById(UUID id) {
        log.info("Admin fetching internship by id: {}", id);
        return internshipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found with ID: " + id));
    }

    @Override
    public void hideInternship(UUID id) {
        log.info("Admin hiding internship: {}", id);
        Internship internship = internshipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found with ID: " + id));
        internship.setHidden(true);
        internshipRepository.update(internship);
    }

    @Override
    public void unhideInternship(UUID id) {
        log.info("Admin unhiding internship: {}", id);
        Internship internship = internshipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found with ID: " + id));
        internship.setHidden(false);
        internshipRepository.update(internship);
    }

    @Override
    public Page<Internship> getInternshipsByUserId(UUID userId, Pageable pageable) {
        log.info("Admin fetching internships for user: {}", userId);
        return internshipRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
