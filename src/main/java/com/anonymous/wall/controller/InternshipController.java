package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.model.*;
import com.anonymous.wall.service.retry.CommentsRetryService;
import com.anonymous.wall.service.retry.InternshipRetryService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
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
@ExecuteOn(TaskExecutors.BLOCKING)
public class InternshipController {

    private static final Logger log = LoggerFactory.getLogger(InternshipController.class);

    @Inject
    private InternshipRetryService internshipRetryService;

    @Inject
    private CommentsRetryService commentsRetryService;


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

    private String getSchoolDomainFromRequest(HttpRequest<?> request) {
        Optional<Principal> principalOpt = request.getUserPrincipal();
        if (principalOpt.isEmpty()) {
            return null;
        }
        Principal principal = principalOpt.get();
        if (principal instanceof io.micronaut.security.authentication.Authentication) {
            io.micronaut.security.authentication.Authentication auth = (io.micronaut.security.authentication.Authentication) principal;
            Object schoolDomainObj = auth.getAttributes().get("schoolDomain");
            return schoolDomainObj != null ? schoolDomainObj.toString() : null;
        }
        return null;
    }

    @Post
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> createInternship(@Body CreateInternshipRequest request, HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("POST /internships - Creating new internship, user={}, company={}", userId, request.getCompany());
            Internship internship = internshipRetryService.createInternship(request, userId);
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

    @Get
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> listInternships(
            @QueryValue(defaultValue = "campus") String wall,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @QueryValue(defaultValue = "newest") String sortBy,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            String schoolDomain = getSchoolDomainFromRequest(httpRequest);
            log.info("GET /internships - Listing internships, user={}, wall={}, page={}, limit={}, sortBy={}", 
                    userId, wall, page, limit, sortBy);

            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            Pageable pageable = Pageable.from(page - 1, limit);
            Page<Internship> internships = internshipRetryService.getInternshipsByWall(wall, pageable, userId, schoolDomain, sortBy);

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

    @Get("/{internshipId}")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> getInternship(
            @PathVariable String internshipId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            UUID internshipUUID = UUID.fromString(internshipId);
            log.info("GET /internships/{} - Getting internship, user={}", internshipId, userId);
            Internship internship = internshipRetryService.getInternship(internshipUUID, userId);
            InternshipDTO dto = mapInternshipToDTO(internship);
            log.info("GET /internships/{} - Internship retrieved successfully", internshipId);
            return HttpResponse.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("GET /internships/{} - Bad request: {}", internshipId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /internships/{} - Error getting internship", internshipId, e);
            return HttpResponse.badRequest(error("Failed to get internship posting"));
        }
    }

    @Patch("/{internshipId}/hide")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> hideInternship(
            @PathVariable UUID internshipId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /internships/{}/hide - Hiding internship, user={}", internshipId, userId);
            internshipRetryService.hideInternship(internshipId, userId);
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

    @Patch("/{internshipId}/unhide")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> unhideInternship(
            @PathVariable UUID internshipId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /internships/{}/unhide - Unhiding internship, user={}", internshipId, userId);
            internshipRetryService.unhideInternship(internshipId, userId);
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

    // ================= Comment Endpoints =================

    @io.micronaut.http.annotation.Post("/{internshipId}/comments")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> addComment(
            @PathVariable UUID internshipId,
            @Body CreateCommentRequest request,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("POST /internships/{}/comments - Adding comment, user={}", internshipId, userId);
            Comment comment = commentsRetryService.addComment(CommentParentType.INTERNSHIP, internshipId, request, userId);
            CommentDTO dto = mapCommentToDTO(comment);
            log.info("POST /internships/{}/comments - Comment added successfully, commentId={}", internshipId, dto.getId());
            return HttpResponse.created(dto);
        } catch (IllegalArgumentException e) {
            log.warn("POST /internships/{}/comments - Bad request: {}", internshipId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /internships/{}/comments - Error adding comment", internshipId, e);
            return HttpResponse.badRequest(error("Failed to add comment"));
        }
    }

    @Get("/{internshipId}/comments")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> getComments(
            @PathVariable UUID internshipId,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @QueryValue(defaultValue = "NEWEST") String sort,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("GET /internships/{}/comments - Getting comments, user={}", internshipId, userId);

            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            internshipRetryService.getInternship(internshipId, userId);

            Pageable pageable = Pageable.from(page - 1, limit);
            SortBy sortBy = SortBy.parse(sort);
            Page<Comment> commentPage = commentsRetryService.getCommentsWithPagination(CommentParentType.INTERNSHIP, internshipId, pageable, sortBy, userId);

            List<CommentDTO> dtos = commentPage.getContent().stream()
                    .map(this::mapCommentToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", page);
            pagination.put("limit", limit);
            pagination.put("total", commentPage.getTotalSize());
            pagination.put("totalPages", commentPage.getTotalPages());

            Map<String, Object> response = new HashMap<>();
            response.put("data", dtos);
            response.put("pagination", pagination);

            log.info("GET /internships/{}/comments - Successfully retrieved {} comments", internshipId, dtos.size());
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("GET /internships/{}/comments - Bad request: {}", internshipId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /internships/{}/comments - Error getting comments", internshipId, e);
            return HttpResponse.badRequest(error("Failed to get comments"));
        }
    }

    @Patch("/{internshipId}/comments/{commentId}/hide")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> hideComment(
            @PathVariable UUID internshipId,
            @PathVariable UUID commentId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /internships/{}/comments/{}/hide - Hiding comment, user={}", internshipId, commentId, userId);
            commentsRetryService.hideComment(CommentParentType.INTERNSHIP, internshipId, commentId, userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Comment hidden successfully");
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("PATCH /internships/{}/comments/{}/hide - Bad request: {}", internshipId, commentId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("You can only hide your own comments") || e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("PATCH /internships/{}/comments/{}/hide - Error hiding comment", internshipId, commentId, e);
            return HttpResponse.badRequest(error("Failed to hide comment"));
        }
    }

    @Patch("/{internshipId}/comments/{commentId}/unhide")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> unhideComment(
            @PathVariable UUID internshipId,
            @PathVariable UUID commentId,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("PATCH /internships/{}/comments/{}/unhide - Unhiding comment, user={}", internshipId, commentId, userId);
            commentsRetryService.unhideComment(CommentParentType.INTERNSHIP, internshipId, commentId, userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Comment unhidden successfully");
            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("PATCH /internships/{}/comments/{}/unhide - Bad request: {}", internshipId, commentId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            if (e.getMessage().contains("You can only unhide your own comments") || e.getMessage().contains("do not have access")) {
                return HttpResponse.status(io.micronaut.http.HttpStatus.FORBIDDEN).body(error(e.getMessage()));
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("PATCH /internships/{}/comments/{}/unhide - Error unhiding comment", internshipId, commentId, e);
            return HttpResponse.badRequest(error("Failed to unhide comment"));
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

        if (internship.getWall() != null) {
            dto.setWall(InternshipDTOWall.valueOf(internship.getWall().toUpperCase()));
        }
        dto.setComments(internship.getCommentCount());

        InternshipDTOAuthor author = new InternshipDTOAuthor();
        author.setId(internship.getUserId().toString());
        author.setProfileName(internship.getProfileName()); // ← no DB call
        author.setIsAnonymous(false);
        dto.setAuthor(author);

        return dto;
    }

    private CommentDTO mapCommentToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getParentId());
        if (comment.getParentType() != null) {
            dto.setParentType(CommentDTOParentType.valueOf(comment.getParentType()));
        }
        dto.setText(comment.getText());
        dto.setCreatedAt(comment.getCreatedAt());

        CommentDTOAuthor author = new CommentDTOAuthor();
        author.setId(comment.getUserId().toString());
        author.setProfileName(comment.getProfileName());
        author.setIsAnonymous(true);
        dto.setAuthor(author);

        return dto;
    }

    private Map<String, Object> createPaginationInfo(Page<?> page) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page.getPageNumber() + 1);
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
