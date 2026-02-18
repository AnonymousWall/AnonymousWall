package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateInternshipRequest;
import com.anonymous.wall.model.InternshipDTO;
import com.anonymous.wall.model.InternshipDTOAuthor;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.InternshipService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller("/api/v1/internships")
public class InternshipController {

    private static final Logger log = LoggerFactory.getLogger(InternshipController.class);

    @Inject
    private InternshipService internshipService;

    @Inject
    private UserRepository userRepository;

    // Helper to extract user ID from Principal
    private UUID getUserIdFromRequest(HttpRequest<?> request) {
        Optional<Principal> principalOpt = request.getUserPrincipal();

        if (principalOpt.isEmpty()) {
            throw new IllegalArgumentException("User not authenticated");
        }

        String principalName = principalOpt.get().getName();
        try {
            return UUID.fromString(principalName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user ID format in security context: " + principalName, e);
        }
    }

    /**
     * POST /internships
     * Create a new internship posting
     */
    @Post
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> createInternship(@Body CreateInternshipRequest request, HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("POST /internships - Creating new internship, user={}, company={}", userId, request.getCompany());

            Internship internship = internshipService.createInternship(request, userId);
            InternshipDTO dto = mapInternshipToDTO(internship);

            log.info("POST /internships - Internship created successfully, internshipId={}", dto.getId());
            return HttpResponse.created(dto);
        } catch (IllegalArgumentException e) {
            log.warn("POST /internships - Bad request: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /internships - Error creating internship", e);
            return HttpResponse.badRequest(error("Failed to create internship posting"));
        }
    }

    /**
     * GET /internships
     * List internship postings with optional pagination
     */
    @Get
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> listInternships(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("GET /internships - Listing internships, user={}, page={}, limit={}", userId, page, limit);

            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            Pageable pageable = Pageable.from(page - 1, limit);
            Page<Internship> internships = internshipService.listInternships(pageable);

            List<InternshipDTO> dtos = internships.getContent().stream()
                    .map(this::mapInternshipToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", dtos);
            response.put("pagination", createPaginationInfo(internships));

            log.info("GET /internships - Successfully retrieved {} internships", dtos.size());
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("GET /internships - Bad request: {}", e.getMessage());
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /internships - Error listing internships", e);
            return HttpResponse.badRequest(error("Failed to list internship postings"));
        }
    }

    // ================= DTO Mapping Methods =================

    private InternshipDTO mapInternshipToDTO(Internship internship) {
        InternshipDTO dto = new InternshipDTO();
        dto.setId(internship.getId().toString());
        dto.setCompany(internship.getCompany());
        dto.setRole(internship.getRole());
        dto.setSalary(internship.getSalary());
        dto.setLocation(internship.getLocation());
        dto.setDescription(internship.getDescription());
        dto.setDeadline(internship.getDeadline());
        dto.setCreatedAt(internship.getCreatedAt());
        dto.setUpdatedAt(internship.getUpdatedAt());

        // Set author info
        InternshipDTOAuthor author = new InternshipDTOAuthor();
        author.setId(internship.getUserId().toString());

        // Get user for author details (optional, based on API spec)
        Optional<UserEntity> userOpt = userRepository.findById(internship.getUserId());
        if (userOpt.isPresent()) {
            // Author info is minimal in the API spec, just id is required
            // Additional fields can be added if needed
        }

        dto.setAuthor(author);

        return dto;
    }

    private Map<String, Object> createPaginationInfo(Page<?> page) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page.getPageNumber() + 1); // Convert from 0-based to 1-based
        pagination.put("limit", page.getSize());
        pagination.put("total", page.getTotalSize());
        pagination.put("totalPages", page.getTotalPages());
        return pagination;
    }

    private Map<String, String> error(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
}
