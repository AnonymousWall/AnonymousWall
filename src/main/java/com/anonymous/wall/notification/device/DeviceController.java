package com.anonymous.wall.notification.device;

import com.anonymous.wall.model.RegisterDeviceRequest;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller("/api/v1/devices")
@ExecuteOn(TaskExecutors.BLOCKING)
public class DeviceController {

    private static final Logger log = LoggerFactory.getLogger(DeviceController.class);

    @Inject
    private DeviceTokenService deviceTokenService;

    @Post("/register")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> registerDevice(@Valid @Body RegisterDeviceRequest request,
                                               HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserId(httpRequest);
            log.info("POST /devices/register - userId={}, platform={}", userId, request.getPlatform());
            deviceTokenService.registerToken(userId, request.getDeviceToken(), request.getPlatform().getValue());
            return HttpResponse.ok();
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("POST /devices/register - Error registering device", e);
            return HttpResponse.badRequest(Map.of("error", "Failed to register device"));
        }
    }

    private UUID getUserId(HttpRequest<?> request) {
        Optional<Principal> principalOpt = request.getUserPrincipal();
        if (principalOpt.isEmpty()) {
            throw new IllegalArgumentException("User not authenticated");
        }
        return UUID.fromString(principalOpt.get().getName());
    }
}
