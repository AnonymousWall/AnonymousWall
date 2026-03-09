package com.anonymous.wall.service.retry;

import com.anonymous.wall.entity.PollOption;
import com.anonymous.wall.entity.PollVote;
import com.anonymous.wall.service.base.PollService;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Poll retry wrapper.
 */
@Singleton
public class PollRetryService {

    private final PollService pollService;

    public PollRetryService(PollService pollService) {
        this.pollService = pollService;
    }

    @Retryable(attempts = "3", delay = "500ms")
    public List<PollOption> createPollOptions(UUID postId, List<String> optionTexts) {
        return pollService.createPollOptions(postId, optionTexts);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public PollVote vote(UUID postId, UUID optionId, UUID userId) {
        return pollService.vote(postId, optionId, userId);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public Map<String, Object> getPollData(UUID postId, UUID userId, boolean viewResults) {
        return pollService.getPollData(postId, userId, viewResults);
    }

    @Retryable(attempts = "3", delay = "500ms")
    public List<PollOption> getPollOptions(UUID postId) {
        return pollService.getPollOptions(postId);
    }
}