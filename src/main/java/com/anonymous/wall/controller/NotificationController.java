package com.anonymous.wall.controller;

import com.anonymous.wall.model.NotificationDTO;
import com.anonymous.wall.model.NotificationDTOType;
import com.anonymous.wall.entity.NotificationEntity;
import com.anonymous.wall.service.retry.NotificationRetryService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller("/api/v1/notifications")
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.BLOCKING)
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Inject
    private NotificationRetryService notificationRetryService;

    private UUID getUserIdFromRequest(HttpRequest<?> request) {
        Optional<Principal> principalOpt = request.getUserPrincipal();
        if (principalOpt.isEmpty()) {
            throw new IllegalArgumentException("User not authenticated");
        }
        String principalName = principalOpt.get().getName();
        try {
            return UUID.fromString(principalName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user ID in token");
        }
    }

    /**
     * GET /api/v1/notifications?page=1&size=20
     * Returns paginated list of notifications for the authenticated user.
     */
    @Get
    public HttpResponse<Object> getNotifications(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int size,
            HttpRequest<?> request) {
        try {
            UUID userId = getUserIdFromRequest(request);
            log.info("GET /notifications - user={}, page={}, size={}", userId, page, size);

            Pageable pageable = Pageable.from(page - 1, size);
            Page<NotificationEntity> notificationsPage = notificationRetryService.getNotifications(userId, pageable);

            Map<String, Object> result = new HashMap<>();
            result.put("content", notificationsPage.getContent().stream()
                    .map(this::mapToDTO)
                    .toList());
            result.put("page", notificationsPage.getPageNumber() + 1);
            result.put("size", notificationsPage.getSize());
            result.put("totalElements", notificationsPage.getTotalSize());
            result.put("totalPages", notificationsPage.getTotalPages());

            return HttpResponse.ok(result);
        } catch (Exception e) {
            log.error("GET /notifications - Error", e);
            return HttpResponse.serverError();
        }
    }

    /**
     * GET /api/v1/notifications/unread-count
     * Returns the count of unread notifications for the authenticated user.
     */
    @Get("/unread-count")
    public HttpResponse<Object> getUnreadCount(HttpRequest<?> request) {
        try {
            UUID userId = getUserIdFromRequest(request);
            log.info("GET /notifications/unread-count - user={}", userId);

            long count = notificationRetryService.getUnreadCount(userId);

            Map<String, Long> result = new HashMap<>();
            result.put("count", count);

            return HttpResponse.ok(result);
        } catch (Exception e) {
            log.error("GET /notifications/unread-count - Error", e);
            return HttpResponse.serverError();
        }
    }

    /**
     * POST /api/v1/notifications/mark-all-read
     * Marks all notifications as read for the authenticated user.
     */
    @Post("/mark-all-read")
    public HttpResponse<Void> markAllRead(HttpRequest<?> request) {
        try {
            UUID userId = getUserIdFromRequest(request);
            log.info("POST /notifications/mark-all-read - user={}", userId);

            notificationRetryService.markAllRead(userId);

            return HttpResponse.ok();
        } catch (Exception e) {
            log.error("POST /notifications/mark-all-read - Error", e);
            return HttpResponse.serverError();
        }
    }

    /**
     * POST /api/v1/notifications/{id}/read
     * Marks a single notification as read.
     */
    @Post("/{id}/read")
    public HttpResponse<Void> markRead(@PathVariable UUID id, HttpRequest<?> request) {
        try {
            UUID userId = getUserIdFromRequest(request);
            log.info("POST /notifications/{}/read - user={}", id, userId);

            notificationRetryService.markRead(id, userId);

            return HttpResponse.ok();
        } catch (HttpStatusException e) {
            log.warn("POST /notifications/{}/read - {}", id, e.getMessage());
            return HttpResponse.status(e.getStatus());
        } catch (Exception e) {
            log.error("POST /notifications/{}/read - Error", id, e);
            return HttpResponse.serverError();
        }
    }

    private NotificationDTO mapToDTO(NotificationEntity entity) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(entity.getId());
        dto.setType(NotificationDTOType.valueOf(entity.getType()));
        dto.setEntityId(entity.getEntityId());
        dto.setEntityTitle(entity.getEntityTitle());
        dto.setActorProfileName(entity.getActorProfileName());
        dto.setRead(entity.isRead());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
