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
     * List internship postings with optional pagination and sorting
     */
    @Get
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> listInternships(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @QueryValue(defaultValue = "newest") String sortBy,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("GET /internships - Listing internships, user={}, page={}, limit={}, sortBy={}", 
                    userId, page, limit, sortBy);

            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            Pageable pageable = Pageable.from(page - 1, limit);
            Page<Internship> internships = internshipService.listInternships(pageable, sortBy);

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

    /**
     * GET /internships/{internshipId}
     * Get a specific internship posting by ID
     */
    @Get("/{internshipId}")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> getInternship(
            @PathVariable String internshipId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            UUID internshipUUID = UUID.fromString(internshipId);
            log.info("GET /internships/{} - Getting internship, user={}", internshipId, userId);

            Internship internship = internshipService.getInternship(internshipUUID);
            InternshipDTO dto = mapInternshipToDTO(internship);

            log.info("GET /internships/{} - Internship retrieved successfully", internshipId);
            return HttpResponse.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("GET /internships/{} - Bad request: {}", internshipId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /internships/{} - Error getting internship", internshipId, e);
            return HttpResponse.badRequest(error("Failed to get internship posting"));
        }
    }

    /**
     * PATCH /internships/{internshipId}/hide
     * Hide an internship posting
     */
    @Patch("/{internshipId}/hide")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> hideInternship(
            @PathVariable UUID internshipId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /internships/{}/hide - Hiding internship, user={}", internshipId, userId);

            internshipService.hideInternship(internshipId, userId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Internship posting hidden successfully");

            log.info("PATCH /internships/{}/hide - Internship hidden successfully", internshipId);
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("PATCH /internships/{}/hide - Bad request: {}", internshipId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("You can only hide your own")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("PATCH /internships/{}/hide - Error hiding internship", internshipId, e);
            return HttpResponse.badRequest(error("Failed to hide internship posting"));
        }
    }

    /**
     * PATCH /internships/{internshipId}/unhide
     * Unhide an internship posting
     */
    @Patch("/{internshipId}/unhide")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> unhideInternship(
            @PathVariable UUID internshipId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /internships/{}/unhide - Unhiding internship, user={}", internshipId, userId);

            internshipService.unhideInternship(internshipId, userId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Internship posting unhidden successfully");

            log.info("PATCH /internships/{}/unhide - Internship unhidden successfully", internshipId);
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("PATCH /internships/{}/unhide - Bad request: {}", internshipId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("You can only unhide your own")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("PATCH /internships/{}/unhide - Error unhiding internship", internshipId, e);
            return HttpResponse.badRequest(error("Failed to unhide internship posting"));
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

        // Set author info (following marketplace pattern)
        InternshipDTOAuthor author = new InternshipDTOAuthor();
        author.setId(internship.getUserId().toString());

        // Get user for author details
        Optional<UserEntity> userOpt = userRepository.findById(internship.getUserId());
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            author.setProfileName(user.getProfileName());
        } else {
            // Log data integrity issue - internship references non-existent user
            log.warn("User {} not found for internship {}", internship.getUserId(), internship.getId());
            author.setProfileName("Unknown User");
        }

        author.setIsAnonymous(false); // Internship postings are not anonymous
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
