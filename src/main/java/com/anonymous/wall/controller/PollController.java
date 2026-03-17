package com.anonymous.wall.controller;

import com.anonymous.wall.service.impl.PollServiceImpl;
import com.anonymous.wall.service.retry.PollRetryService;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller("/api/v1/posts")
@ExecuteOn(TaskExecutors.BLOCKING)
public class PollController {

    private static final Logger log = LoggerFactory.getLogger(PollController.class);

    @Inject
    private PollRetryService pollRetryService;

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
     * POST /posts/{postId}/vote
     * Cast a vote on a poll post
     */
    @Post("/{postId}/vote")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> voteOnPoll(
            @PathVariable UUID postId,
            @Body Map<String, Object> body,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("POST /posts/{}/vote - Voting, user={}", postId, userId);

            Object optionIdObj = body.get("optionId");
            if (optionIdObj == null) {
                return HttpResponse.badRequest(error("optionId is required"));
            }

            UUID optionId;
            try {
                optionId = UUID.fromString(optionIdObj.toString());
            } catch (IllegalArgumentException e) {
                return HttpResponse.badRequest(error("Invalid optionId format"));
            }

            pollRetryService.vote(postId, optionId, userId);

            // Return full poll data with results visible (user just voted)
            Map<String, Object> pollData = pollRetryService.getPollData(postId, userId, false);

            Map<String, Object> result = new HashMap<>();
            result.put("poll", pollData);
            result.put("message", "Vote cast successfully");

            log.info("POST /posts/{}/vote - Vote cast successfully, user={}", postId, userId);
            return HttpResponse.ok(result);

        } catch (PollServiceImpl.DuplicateVoteException e) {
            log.warn("POST /posts/{}/vote - Duplicate vote: {}", postId, e.getMessage());
            return HttpResponse.status(io.micronaut.http.HttpStatus.CONFLICT).body(error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("POST /posts/{}/vote - Bad request: {}", postId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("POST /posts/{}/vote - Error voting", postId, e);
            return HttpResponse.badRequest(error("Failed to cast vote"));
        }
    }

    /**
     * GET /posts/{postId}/poll
     * Get poll data for a post
     */
    @Get("/{postId}/poll")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Object> getPoll(
            @PathVariable UUID postId,
            @QueryValue(defaultValue = "false") boolean viewResults,
            HttpRequest<?> httpRequest) {
        try {
            UUID userId = getUserIdFromRequest(httpRequest);
            log.info("GET /posts/{}/poll - Getting poll data, user={}, viewResults={}", postId, userId, viewResults);

            Map<String, Object> pollData = pollRetryService.getPollData(postId, userId, viewResults);

            log.info("GET /posts/{}/poll - Poll data retrieved successfully", postId);
            return HttpResponse.ok(pollData);

        } catch (IllegalArgumentException e) {
            log.warn("GET /posts/{}/poll - Bad request: {}", postId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return HttpResponse.notFound();
            }
            return HttpResponse.badRequest(error(e.getMessage()));
        } catch (Exception e) {
            log.error("GET /posts/{}/poll - Error getting poll data", postId, e);
            return HttpResponse.badRequest(error("Failed to get poll data"));
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
}
