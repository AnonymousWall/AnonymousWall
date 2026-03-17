package com.anonymous.wall.service.base;

import com.anonymous.wall.entity.PollOption;
import com.anonymous.wall.entity.PollVote;
import com.anonymous.wall.service.impl.PollServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PollService {

    /**
     * Create poll options for a newly created poll post.
     * @param postId the ID of the poll post
     * @param optionTexts list of option texts (2–4 items, each max 100 chars)
     */
    List<PollOption> createPollOptions(UUID postId, List<String> optionTexts);

    /**
     * Cast a vote on a poll.
     * @param postId the poll post ID
     * @param optionId the selected option ID
     * @param userId the voting user ID
     * @return the saved PollVote
     * @throws IllegalArgumentException if post is not a poll, option does not belong to post, or post is hidden
     * @throws PollServiceImpl.DuplicateVoteException if user has already voted (maps to HTTP 409)
     */
    PollVote vote(UUID postId, UUID optionId, UUID userId);

    /**
     * Get poll data for a post.
     * @param postId the poll post ID
     * @param userId the requesting user ID
     * @param viewResults if true, always show vote counts; otherwise only show if user has voted
     * @return a map containing poll metadata including options, totalVotes, userVotedOptionId, resultsVisible
     */
    Map<String, Object> getPollData(UUID postId, UUID userId, boolean viewResults);

    /**
     * Get all options for a poll post.
     */
    List<PollOption> getPollOptions(UUID postId);
}
