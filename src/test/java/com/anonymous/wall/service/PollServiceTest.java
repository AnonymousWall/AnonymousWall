package com.anonymous.wall.service;

import com.anonymous.wall.entity.PollOption;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.model.CreatePostRequestPostType;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PollOptionRepository;
import com.anonymous.wall.repository.PollVoteRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.base.PollService;
import com.anonymous.wall.service.base.PostsService;
import com.anonymous.wall.service.impl.PollServiceImpl;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@DisplayName("PollService Tests")
class PollServiceTest {

    @Inject
    private PollService pollService;

    @Inject
    private PostsService postsService;

    @Inject
    private PostRepository postRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private PollOptionRepository pollOptionRepository;

    @Inject
    private PollVoteRepository pollVoteRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private PostLikeRepository postLikeRepository;

    private UserEntity testUser;
    private UserEntity testUser2;

    @BeforeEach
    void setUp() {
        pollVoteRepository.deleteAll();
        pollOptionRepository.deleteAll();
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();

        testUser = new UserEntity();
        testUser.setEmail("polluser" + System.currentTimeMillis() + "@harvard.edu");
        testUser.setSchoolDomain("harvard.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser = userRepository.save(testUser);

        testUser2 = new UserEntity();
        testUser2.setEmail("polluser2" + System.currentTimeMillis() + "@harvard.edu");
        testUser2.setSchoolDomain("harvard.edu");
        testUser2.setVerified(true);
        testUser2.setPasswordSet(true);
        testUser2 = userRepository.save(testUser2);
    }

    @AfterEach
    void tearDown() {
        pollVoteRepository.deleteAll();
        pollOptionRepository.deleteAll();
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
    }

    // ================= Helper =================

    /**
     * Creates a poll post using the new atomic API: poll options are passed on the
     * CreatePostRequest and saved in the same transaction as the post row.
     */
    private Post createPollPost(List<String> options) {
        CreatePostRequest req = new CreatePostRequest("Poll Title", "");
        req.setPostType(CreatePostRequestPostType.POLL);
        req.setPollOptions(options);
        return postsService.createPost(req, null, testUser.getId());
    }

    // ================= Create Poll =================

    @Nested
    @DisplayName("Create Poll")
    class CreatePollTests {

        @Test
        @DisplayName("Create poll with 2 options succeeds")
        void shouldCreatePollWith2Options() {
            Post post = createPollPost(Arrays.asList("Option A", "Option B"));

            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            assertEquals(2, options.size());
            assertEquals("Option A", options.get(0).getOptionText());
            assertEquals("Option B", options.get(1).getOptionText());
        }

        @Test
        @DisplayName("Create poll with 4 options succeeds")
        void shouldCreatePollWith4Options() {
            Post post = createPollPost(Arrays.asList("A", "B", "C", "D"));

            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            assertEquals(4, options.size());
        }

        @Test
        @DisplayName("Create poll with 1 option fails with validation error")
        void shouldFailWith1Option() {
            CreatePostRequest req1 = new CreatePostRequest("Poll", "");
            req1.setPostType(CreatePostRequestPostType.POLL);
            Post post = postsService.createPost(req1, null, testUser.getId());
            UUID postId = post.getId();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.createPollOptions(postId, List.of("Only option")));
            assertTrue(ex.getMessage().contains("at least 2"));
        }

        @Test
        @DisplayName("Create poll with 5 options fails with validation error")
        void shouldFailWith5Options() {
            CreatePostRequest req5 = new CreatePostRequest("Poll", "");
            req5.setPostType(CreatePostRequestPostType.POLL);
            Post post = postsService.createPost(req5, null, testUser.getId());
            UUID postId = post.getId();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.createPollOptions(postId,
                            Arrays.asList("A", "B", "C", "D", "E")));
            assertTrue(ex.getMessage().contains("more than 4"));
        }

        @Test
        @DisplayName("Option text over 100 chars fails with validation error")
        void shouldFailWithOptionTextOver100Chars() {
            CreatePostRequest req100 = new CreatePostRequest("Poll", "");
            req100.setPostType(CreatePostRequestPostType.POLL);
            Post post = postsService.createPost(req100, null, testUser.getId());
            UUID postId = post.getId();
            String longText = "X".repeat(101);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.createPollOptions(postId, Arrays.asList("Valid", longText)));
            assertTrue(ex.getMessage().contains("exceeds maximum length"));
        }
    }

    // ================= Voting =================

    @Nested
    @DisplayName("Voting")
    class VotingTests {

        @Test
        @DisplayName("Vote succeeds and increments vote_count on option and total_votes on post")
        void shouldIncrementCountsOnVote() {
            Post post = createPollPost(Arrays.asList("Yes", "No"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());

            pollService.vote(post.getId(), options.get(0).getId(), testUser.getId());

            // Check option vote count
            PollOption updated = pollOptionRepository.findById(options.get(0).getId()).orElseThrow();
            assertEquals(1, updated.getVoteCount());

            // Check post total_votes
            Post updatedPost = postRepository.findById(post.getId()).orElseThrow();
            assertEquals(1, updatedPost.getTotalVotes());
        }

        @Test
        @DisplayName("Second vote by same user returns 409")
        void shouldRejectDuplicateVote() {
            Post post = createPollPost(Arrays.asList("Yes", "No"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());

            pollService.vote(post.getId(), options.get(0).getId(), testUser.getId());

            assertThrows(PollServiceImpl.DuplicateVoteException.class,
                    () -> pollService.vote(post.getId(), options.get(0).getId(), testUser.getId()));
        }
    }

    // ================= Poll Results =================

    @Nested
    @DisplayName("Poll Results")
    class PollResultsTests {

        @Test
        @DisplayName("Results before voting (no viewResults) - voteCount is null on all options")
        void shouldHideResultsBeforeVoting() {
            Post post = createPollPost(Arrays.asList("A", "B"));

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser.getId(), false);

            assertFalse((Boolean) data.get("resultsVisible"));
            List<?> options = (List<?>) data.get("options");
            for (Object opt : options) {
                Map<?, ?> optMap = (Map<?, ?>) opt;
                assertNull(optMap.get("voteCount"));
                assertNull(optMap.get("percentage"));
            }
        }

        @Test
        @DisplayName("Results after voting - voteCount and percentage populated")
        void shouldShowResultsAfterVoting() {
            Post post = createPollPost(Arrays.asList("A", "B"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());

            pollService.vote(post.getId(), options.get(0).getId(), testUser.getId());

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser.getId(), false);

            assertTrue((Boolean) data.get("resultsVisible"));
            List<?> optionDtos = (List<?>) data.get("options");
            Map<?, ?> optA = (Map<?, ?>) optionDtos.get(0);
            assertNotNull(optA.get("voteCount"));
            assertNotNull(optA.get("percentage"));
        }

        @Test
        @DisplayName("Results with viewResults=true without voting - voteCount and percentage populated")
        void shouldShowResultsWithViewResultsFlag() {
            Post post = createPollPost(Arrays.asList("A", "B"));

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser2.getId(), true);

            assertTrue((Boolean) data.get("resultsVisible"));
            List<?> optionDtos = (List<?>) data.get("options");
            for (Object opt : optionDtos) {
                Map<?, ?> optMap = (Map<?, ?>) opt;
                assertNotNull(optMap.get("voteCount"));
                assertNotNull(optMap.get("percentage"));
            }
        }

        @Test
        @DisplayName("total_votes before any votes returns 0")
        void shouldReturnZeroTotalVotesBeforeAnyVotes() {
            Post post = createPollPost(Arrays.asList("A", "B"));

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser.getId(), false);

            assertEquals(0, data.get("totalVotes"));
        }

        @Test
        @DisplayName("userVotedOptionId before voting returns null")
        void shouldReturnNullUserVotedOptionIdBeforeVoting() {
            Post post = createPollPost(Arrays.asList("A", "B"));

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser.getId(), false);

            assertNull(data.get("userVotedOptionId"));
        }

        @Test
        @DisplayName("userVotedOptionId after voting returns the voted option UUID")
        void shouldReturnCorrectUserVotedOptionIdAfterVoting() {
            Post post = createPollPost(Arrays.asList("A", "B"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            UUID votedOptionId = options.get(1).getId();

            pollService.vote(post.getId(), votedOptionId, testUser.getId());

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser.getId(), false);

            assertEquals(votedOptionId, data.get("userVotedOptionId"));
        }
    }

    // ================= Poll Post Creation Atomicity =================

    @Nested
    @DisplayName("Poll Post Creation Atomicity")
    class PollPostAtomicityTests {

        @Test
        @DisplayName("createPost with pollOptions saves post and options in one call")
        void shouldCreatePostAndOptionsAtomically() {
            CreatePostRequest req = new CreatePostRequest("Atomic Poll", "");
            req.setPostType(CreatePostRequestPostType.POLL);
            req.setPollOptions(Arrays.asList("Alpha", "Beta", "Gamma"));

            Post post = postsService.createPost(req, null, testUser.getId());

            assertNotNull(post.getId());
            assertEquals("poll", post.getPostType());

            List<PollOption> saved = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            assertEquals(3, saved.size());
            assertEquals("Alpha", saved.get(0).getOptionText());
            assertEquals("Beta",  saved.get(1).getOptionText());
            assertEquals("Gamma", saved.get(2).getOptionText());
        }

        @Test
        @DisplayName("createPost with invalid pollOptions does not save the post")
        void shouldNotSavePostWhenOptionsAreInvalid() {
            CreatePostRequest req = new CreatePostRequest("Bad Poll", "");
            req.setPostType(CreatePostRequestPostType.POLL);
            // Only 1 option — validation requires at least 2
            req.setPollOptions(List.of("Only"));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.createPost(req, null, testUser.getId()));

            // Verify that the validation error from createPollOptions is propagated
            assertTrue(ex.getMessage().contains("at least 2"),
                    "Exception must report the minimum options constraint");
        }
    }

    // ================= Vote Atomicity =================

    @Nested
    @DisplayName("Vote Atomicity")
    class VoteAtomicityTests {

        @Test
        @DisplayName("Successful vote persists option vote_count, post total_votes, and vote row together")
        void shouldCommitAllThreeWritesOnSuccessfulVote() {
            Post post = createPollPost(Arrays.asList("Yes", "No"));
            List<PollOption> opts = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            UUID optionId = opts.get(0).getId();

            pollService.vote(post.getId(), optionId, testUser.getId());

            // All three writes must be visible after the transaction commits
            PollOption option = pollOptionRepository.findById(optionId).orElseThrow();
            assertEquals(1, option.getVoteCount(), "option vote_count must be incremented");

            Post updatedPost = postRepository.findById(post.getId()).orElseThrow();
            assertEquals(1, updatedPost.getTotalVotes(), "post total_votes must be incremented");

            long voteRowCount = pollVoteRepository.findByPostIdAndUserId(post.getId(), testUser.getId())
                    .map(v -> 1L).orElse(0L);
            assertEquals(1L, voteRowCount, "a PollVote row must be persisted");
        }

        @Test
        @DisplayName("Duplicate vote does not change any counts")
        void shouldNotChangeCountsOnDuplicateVote() {
            Post post = createPollPost(Arrays.asList("Yes", "No"));
            List<PollOption> opts = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            UUID optionId = opts.get(0).getId();

            pollService.vote(post.getId(), optionId, testUser.getId());

            // Try to vote again — must throw and leave counts unchanged
            assertThrows(PollServiceImpl.DuplicateVoteException.class,
                    () -> pollService.vote(post.getId(), optionId, testUser.getId()));

            PollOption option = pollOptionRepository.findById(optionId).orElseThrow();
            assertEquals(1, option.getVoteCount(), "vote_count must remain 1 after duplicate attempt");

            Post updatedPost = postRepository.findById(post.getId()).orElseThrow();
            assertEquals(1, updatedPost.getTotalVotes(), "total_votes must remain 1 after duplicate attempt");
        }

        @Test
        @DisplayName("Multiple users voting increments counts correctly")
        void shouldIncrementCountsForEachDistinctUser() {
            Post post = createPollPost(Arrays.asList("Yes", "No"));
            List<PollOption> opts = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            UUID optionId = opts.get(0).getId();

            pollService.vote(post.getId(), optionId, testUser.getId());
            pollService.vote(post.getId(), optionId, testUser2.getId());

            PollOption option = pollOptionRepository.findById(optionId).orElseThrow();
            assertEquals(2, option.getVoteCount(), "vote_count must be 2 after two users voted");

            Post updatedPost = postRepository.findById(post.getId()).orElseThrow();
            assertEquals(2, updatedPost.getTotalVotes(), "total_votes must be 2");
        }
    }
}
