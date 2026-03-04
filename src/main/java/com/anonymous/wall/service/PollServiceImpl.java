package com.anonymous.wall.service;

import com.anonymous.wall.entity.PollOption;
import com.anonymous.wall.entity.PollVote;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.repository.PollOptionRepository;
import com.anonymous.wall.repository.PollVoteRepository;
import com.anonymous.wall.repository.PostRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Singleton
public class PollServiceImpl implements PollService {

    private static final Logger log = LoggerFactory.getLogger(PollServiceImpl.class);
    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 4;
    private static final int MAX_OPTION_TEXT_LENGTH = 100;

    @Inject
    private PollOptionRepository pollOptionRepository;

    @Inject
    private PollVoteRepository pollVoteRepository;

    @Inject
    private PostRepository postRepository;

    @Override
    @Transactional
    public List<PollOption> createPollOptions(UUID postId, List<String> optionTexts) {
        validatePollOptions(optionTexts);

        List<PollOption> options = new ArrayList<>();
        for (int i = 0; i < optionTexts.size(); i++) {
            PollOption option = new PollOption(postId, optionTexts.get(i).trim(), i);
            options.add(pollOptionRepository.save(option));
        }

        log.info("Created {} poll options for post={}", options.size(), postId);
        return options;
    }

    @Override
    @Transactional
    public PollVote vote(UUID postId, UUID optionId, UUID userId) {
        // Verify post exists and is a poll
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }
        Post post = postOpt.get();

        if (post.isHidden()) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }

        if (!"poll".equals(post.getPostType())) {
            throw new IllegalArgumentException("Post is not a poll");
        }

        // Check for duplicate vote
        Optional<PollVote> existingVote = pollVoteRepository.findByPostIdAndUserId(postId, userId);
        if (existingVote.isPresent()) {
            throw new DuplicateVoteException("User has already voted on this poll");
        }

        // Verify option belongs to this post
        Optional<PollOption> optionOpt = pollOptionRepository.findById(optionId);
        if (optionOpt.isEmpty() || !optionOpt.get().getPostId().equals(postId)) {
            throw new IllegalArgumentException("Poll option does not belong to this poll: " + optionId);
        }

        // Increment option vote count
        PollOption option = optionOpt.get();
        option.incrementVoteCount();
        pollOptionRepository.update(option);

        // Increment post total_votes
        post.setTotalVotes(post.getTotalVotes() + 1);
        postRepository.update(post);

        // Save vote
        PollVote vote = new PollVote(postId, optionId, userId);
        PollVote saved = pollVoteRepository.save(vote);

        log.info("User={} voted on post={}, option={}", userId, postId, optionId);
        return saved;
    }

    @Override
    public Map<String, Object> getPollData(UUID postId, UUID userId, boolean viewResults) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }
        Post post = postOpt.get();

        if (post.isHidden()) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }

        if (!"poll".equals(post.getPostType())) {
            throw new IllegalArgumentException("Post is not a poll");
        }

        List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(postId);

        // Determine if user has voted
        Optional<PollVote> userVote = pollVoteRepository.findByPostIdAndUserId(postId, userId);
        UUID userVotedOptionId = userVote.map(PollVote::getOptionId).orElse(null);
        boolean resultsVisible = userVote.isPresent() || viewResults;

        int totalVotes = post.getTotalVotes();

        List<Map<String, Object>> optionDtos = new ArrayList<>();
        for (PollOption option : options) {
            Map<String, Object> optionDto = new LinkedHashMap<>();
            optionDto.put("id", option.getId());
            optionDto.put("optionText", option.getOptionText());
            optionDto.put("displayOrder", option.getDisplayOrder());
            if (resultsVisible) {
                optionDto.put("voteCount", option.getVoteCount());
                double pct = totalVotes > 0 ? (option.getVoteCount() * 100.0 / totalVotes) : 0.0;
                optionDto.put("percentage", Math.round(pct * 10.0) / 10.0);
            } else {
                optionDto.put("voteCount", null);
                optionDto.put("percentage", null);
            }
            optionDtos.add(optionDto);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("options", optionDtos);
        result.put("totalVotes", totalVotes);
        result.put("userVotedOptionId", userVotedOptionId);
        result.put("resultsVisible", resultsVisible);

        return result;
    }

    @Override
    public List<PollOption> getPollOptions(UUID postId) {
        return pollOptionRepository.findByPostIdOrderByDisplayOrder(postId);
    }

    // ================= Validation =================

    private void validatePollOptions(List<String> optionTexts) {
        if (optionTexts == null || optionTexts.size() < MIN_OPTIONS) {
            throw new IllegalArgumentException("Poll must have at least " + MIN_OPTIONS + " options");
        }
        if (optionTexts.size() > MAX_OPTIONS) {
            throw new IllegalArgumentException("Poll cannot have more than " + MAX_OPTIONS + " options");
        }
        for (String text : optionTexts) {
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("Poll option text cannot be empty");
            }
            if (text.trim().length() > MAX_OPTION_TEXT_LENGTH) {
                throw new IllegalArgumentException("Poll option text exceeds maximum length of " + MAX_OPTION_TEXT_LENGTH + " characters");
            }
        }
    }

    /**
     * Exception for duplicate vote attempts (maps to HTTP 409)
     */
    public static class DuplicateVoteException extends RuntimeException {
        public DuplicateVoteException(String message) {
            super(message);
        }
    }
}
