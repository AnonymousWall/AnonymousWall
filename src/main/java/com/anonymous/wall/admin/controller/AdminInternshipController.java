package com.anonymous.wall.admin.controller;

import com.anonymous.wall.admin.service.AdminInternshipService;
import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.model.AdminInternshipDTO;
import com.anonymous.wall.model.AdminPostDTOWall;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller("/api/v1/admin/internships")
public class AdminInternshipController {

    private static final Logger log = LoggerFactory.getLogger(AdminInternshipController.class);

    @Inject
    private AdminInternshipService adminInternshipService;

    private AdminInternshipDTO mapToDTO(Internship internship) {
        AdminInternshipDTO dto = new AdminInternshipDTO();
        dto.setId(internship.getId());
        dto.setUserId(internship.getUserId());
        dto.setProfileName(internship.getProfileName());
        dto.setCompany(internship.getCompany());
        dto.setRole(internship.getRole());
        dto.setSalary(internship.getSalary());
        dto.setLocation(internship.getLocation());
        dto.setDescription(internship.getDescription());
        dto.setDeadline(internship.getDeadline());
        dto.setWall(AdminPostDTOWall.fromValue(internship.getWall()));
        dto.setSchoolDomain(internship.getSchoolDomain());
        dto.setCommentCount(internship.getCommentCount());
        dto.setHidden(internship.isHidden());
        dto.setCreatedAt(internship.getCreatedAt());
        dto.setUpdatedAt(internship.getUpdatedAt());
        return dto;
    }

    @Get
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getAllInternships(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue String userId,
            @Nullable @QueryValue Boolean hidden,
            @Nullable @QueryValue String sortBy,
            @Nullable @QueryValue String sortOrder) {

        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;

        Pageable pageable = Pageable.from(page - 1, limit);
        UUID userIdUuid = userId != null ? UUID.fromString(userId) : null;
        Page<Internship> internshipsPage = adminInternshipService.getAllInternships(pageable, userIdUuid, hidden, sortBy, sortOrder);

        List<AdminInternshipDTO> dtos = internshipsPage.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", dtos);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", internshipsPage.getTotalSize());
        pagination.put("totalPages", internshipsPage.getTotalPages());
        response.put("pagination", pagination);

        return HttpResponse.ok(response);
    }

    @Get("/{id}")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<AdminInternshipDTO> getInternshipById(@PathVariable String id) {
        UUID internshipId = UUID.fromString(id);
        return HttpResponse.ok(mapToDTO(adminInternshipService.getInternshipById(internshipId)));
    }

    @Put("/{id}/hide")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> hideInternship(@PathVariable String id) {
        log.info("Admin hiding internship: {}", id);
        adminInternshipService.hideInternship(UUID.fromString(id));
        Map<String, String> response = new HashMap<>();
        response.put("message", "Internship hidden successfully");
        return HttpResponse.ok(response);
    }

    @Put("/{id}/unhide")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> unhideInternship(@PathVariable String id) {
        log.info("Admin unhiding internship: {}", id);
        adminInternshipService.unhideInternship(UUID.fromString(id));
        Map<String, String> response = new HashMap<>();
        response.put("message", "Internship unhidden successfully");
        return HttpResponse.ok(response);
    }
}
