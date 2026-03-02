package com.anonymous.wall.controller;

import com.anonymous.wall.model.NotificationDTO;
import com.anonymous.wall.notification.service.NotificationService;
import io.micronaut.data.model.Page;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller("/api/v1/notifications")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Inject
    private NotificationService notificationService;

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
     * GET /api/v1/notifications?page=0&size=20
     * Returns paginated list of notifications for authenticated user.
     */
    @Get
    public Page<NotificationDTO> getNotifications(
            HttpRequest<?> request,
            @QueryValue(defaultValue = "0") int page,
            @QueryValue(defaultValue = "20") int size) {
        UUID userId = getUserIdFromRequest(request);
        log.debug("Getting notifications for userId={}, page={}, size={}", userId, page, size);
        return notificationService.getNotifications(userId, page, size);
    }

    /**
     * GET /api/v1/notifications/unread-count
     * Returns { "count": N } for authenticated user.
     */
    @Get("/unread-count")
    public Map<String, Long> getUnreadCount(HttpRequest<?> request) {
        UUID userId = getUserIdFromRequest(request);
        long count = notificationService.getUnreadCount(userId);
        return Map.of("count", count);
    }

    /**
     * POST /api/v1/notifications/mark-all-read
     * Marks all notifications as read for authenticated user.
     */
    @Post("/mark-all-read")
    public HttpResponse<Void> markAllRead(HttpRequest<?> request) {
        UUID userId = getUserIdFromRequest(request);
        notificationService.markAllRead(userId);
        return HttpResponse.ok();
    }

    /**
     * POST /api/v1/notifications/{id}/read
     * Marks a single notification as read.
     */
    @Post("/{id}/read")
    public HttpResponse<Void> markRead(@PathVariable UUID id, HttpRequest<?> request) {
        UUID userId = getUserIdFromRequest(request);
        notificationService.markRead(id, userId);
        return HttpResponse.ok();
    }
}
