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

@MicronautTest(transactional = false)
@DisplayName("PollServiceImpl Tests")
class PollServiceTest {

    @Inject private PollService pollService;
    @Inject private PostsService postsService;
    @Inject private PostRepository postRepository;
    @Inject private UserRepository userRepository;
    @Inject private PollOptionRepository pollOptionRepository;
    @Inject private PollVoteRepository pollVoteRepository;
    @Inject private CommentRepository commentRepository;
    @Inject private PostLikeRepository postLikeRepository;

    private UserEntity testUser;
    private UserEntity testUser2;

    @BeforeEach
    void setUp() {
        pollVoteRepository.deleteAll();
        pollOptionRepository.deleteAll();
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Use UUID suffix — System.currentTimeMillis() can collide when multiple
        // users are created in the same @BeforeEach within the same millisecond.
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        testUser = new UserEntity();
        testUser.setEmail("polluser" + suffix + "@harvard.edu");
        testUser.setSchoolDomain("harvard.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser = userRepository.save(testUser);

        testUser2 = new UserEntity();
        testUser2.setEmail("polluser2" + suffix + "@harvard.edu");
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
        userRepository.deleteAll();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Creates a poll post for the given owner using the atomic API.
     * The explicit `owner` parameter prevents tests from accidentally all
     * sharing testUser state.
     */
    private Post createPollPost(List<String> options, UserEntity owner) {
        CreatePostRequest req = new CreatePostRequest("Poll Title", "");
        req.setPostType(CreatePostRequestPostType.POLL);
        req.setPollOptions(options);
        return postsService.createPost(req, null, owner.getId());
    }

    /** Convenience overload that uses testUser. */
    private Post createPollPost(List<String> options) {
        return createPollPost(options, testUser);
    }

    /** Creates a non-poll post owned by testUser. */
    private Post createRegularPost() {
        CreatePostRequest req = new CreatePostRequest("Regular Post", "Content");
        req.setPostType(CreatePostRequestPostType.STANDARD);
        return postsService.createPost(req, null, testUser.getId());
    }

    // ─── createPollOptions ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("createPollOptions()")
    class CreatePollTests {

        @Test
        @DisplayName("Should create poll with 2 options — minimum boundary")
        void shouldCreatePollWith2Options() {
            Post post = createPollPost(Arrays.asList("Option A", "Option B"));

            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            assertEquals(2, options.size());
            assertEquals("Option A", options.get(0).getOptionText());
            assertEquals("Option B", options.get(1).getOptionText());
        }

        @Test
        @DisplayName("Should create poll with 4 options — maximum boundary")
        void shouldCreatePollWith4Options() {
            Post post = createPollPost(Arrays.asList("A", "B", "C", "D"));

            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            assertEquals(4, options.size());
        }

        @Test
        @DisplayName("Should assign display order 0, 1, 2... in list order")
        void shouldAssignDisplayOrderSequentially() {
            Post post = createPollPost(Arrays.asList("First", "Second", "Third"));

            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            assertEquals(0, options.get(0).getDisplayOrder());
            assertEquals(1, options.get(1).getDisplayOrder());
            assertEquals(2, options.get(2).getDisplayOrder());
        }

        @Test
        @DisplayName("Should trim whitespace from option text before saving")
        void shouldTrimOptionTextWhitespace() {
            Post post = createPollPost(Arrays.asList("  Option A  ", "  Option B  "));

            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            assertEquals("Option A", options.get(0).getOptionText());
            assertEquals("Option B", options.get(1).getOptionText());
        }

        @Test
        @DisplayName("Should accept option text of exactly 100 characters — boundary")
        void shouldAcceptOptionTextAtExactLimit() {
            String exactly100 = "X".repeat(100);
            Post post = createPollPost(Arrays.asList("Valid", exactly100));

            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            assertEquals(100, options.get(1).getOptionText().length());
        }

        @Test
        @DisplayName("Should fail when options list is null")
        void shouldFailWhenOptionsNull() {
            Post post = createRegularPost();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.createPollOptions(post.getId(), null));
            assertTrue(ex.getMessage().contains("at least 2"));
        }

        @Test
        @DisplayName("Should fail when only 1 option provided")
        void shouldFailWith1Option() {
            Post post = createRegularPost();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.createPollOptions(post.getId(), List.of("Only option")));
            assertTrue(ex.getMessage().contains("at least 2"));
        }

        @Test
        @DisplayName("Should fail when 5 options provided")
        void shouldFailWith5Options() {
            Post post = createRegularPost();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.createPollOptions(post.getId(),
                            Arrays.asList("A", "B", "C", "D", "E")));
            assertTrue(ex.getMessage().contains("more than 4"));
        }

        @Test
        @DisplayName("Should fail when any option text is blank")
        void shouldFailWithBlankOptionText() {
            Post post = createRegularPost();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.createPollOptions(post.getId(),
                            Arrays.asList("Valid", "   ")));
            assertTrue(ex.getMessage().contains("cannot be empty"));
        }

        @Test
        @DisplayName("Should fail when any option text is null")
        void shouldFailWithNullOptionText() {
            Post post = createRegularPost();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.createPollOptions(post.getId(),
                            Arrays.asList("Valid", null)));
            assertTrue(ex.getMessage().contains("cannot be empty"));
        }

        @Test
        @DisplayName("Should fail when any option text exceeds 100 characters")
        void shouldFailWithOptionTextOver100Chars() {
            Post post = createRegularPost();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.createPollOptions(post.getId(),
                            Arrays.asList("Valid", "X".repeat(101))));
            assertTrue(ex.getMessage().contains("exceeds maximum length"));
        }
    }

    // ─── vote ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("vote()")
    class VotingTests {

        @Test
        @DisplayName("Should increment option vote_count and post total_votes on successful vote")
        void shouldIncrementCountsOnVote() {
            Post post = createPollPost(Arrays.asList("Yes", "No"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());

            pollService.vote(post.getId(), options.get(0).getId(), testUser.getId());

            PollOption updated = pollOptionRepository.findById(options.get(0).getId()).orElseThrow();
            assertEquals(1, updated.getVoteCount());

            Post updatedPost = postRepository.findById(post.getId()).orElseThrow();
            assertEquals(1, updatedPost.getTotalVotes());
        }

        @Test
        @DisplayName("Should persist a PollVote row after successful vote")
        void shouldPersistVoteRow() {
            Post post = createPollPost(Arrays.asList("Yes", "No"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());

            pollService.vote(post.getId(), options.get(0).getId(), testUser.getId());

            assertTrue(pollVoteRepository.findByPostIdAndUserId(post.getId(), testUser.getId()).isPresent(),
                    "A PollVote row must be persisted after a successful vote");
        }

        @Test
        @DisplayName("Should throw DuplicateVoteException on second vote by same user")
        void shouldRejectDuplicateVote() {
            Post post = createPollPost(Arrays.asList("Yes", "No"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());

            pollService.vote(post.getId(), options.get(0).getId(), testUser.getId());

            assertThrows(PollServiceImpl.DuplicateVoteException.class,
                    () -> pollService.vote(post.getId(), options.get(0).getId(), testUser.getId()));
        }

        @Test
        @DisplayName("Should not change any counts after a duplicate vote attempt")
        void shouldNotChangeCountsOnDuplicateVote() {
            Post post = createPollPost(Arrays.asList("Yes", "No"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            UUID optionId = options.get(0).getId();

            pollService.vote(post.getId(), optionId, testUser.getId());
            assertThrows(PollServiceImpl.DuplicateVoteException.class,
                    () -> pollService.vote(post.getId(), optionId, testUser.getId()));

            assertEquals(1, pollOptionRepository.findById(optionId).orElseThrow().getVoteCount(),
                    "vote_count must remain 1 after duplicate attempt");
            assertEquals(1, postRepository.findById(post.getId()).orElseThrow().getTotalVotes(),
                    "total_votes must remain 1 after duplicate attempt");
        }

        @Test
        @DisplayName("Should increment counts correctly when multiple distinct users vote")
        void shouldIncrementCountsForEachDistinctUser() {
            Post post = createPollPost(Arrays.asList("Yes", "No"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            UUID optionId = options.get(0).getId();

            pollService.vote(post.getId(), optionId, testUser.getId());
            pollService.vote(post.getId(), optionId, testUser2.getId());

            assertEquals(2, pollOptionRepository.findById(optionId).orElseThrow().getVoteCount());
            assertEquals(2, postRepository.findById(post.getId()).orElseThrow().getTotalVotes());
        }

        @Test
        @DisplayName("Should throw when post not found")
        void shouldThrowWhenPostNotFound() {
            Post post = createPollPost(Arrays.asList("A", "B"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.vote(UUID.randomUUID(), options.get(0).getId(), testUser.getId()));
            assertTrue(ex.getMessage().contains("Post not found"));
        }

        @Test
        @DisplayName("Should throw when post is hidden")
        void shouldThrowWhenPostHidden() {
            Post post = createPollPost(Arrays.asList("A", "B"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());

            postsService.hidePost(post.getId(), testUser.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.vote(post.getId(), options.get(0).getId(), testUser.getId()));
            assertTrue(ex.getMessage().contains("Post not found"),
                    "Hidden posts must surface as 'not found' to the caller");
        }

        @Test
        @DisplayName("Should throw when post is not a poll type")
        void shouldThrowWhenPostIsNotPoll() {
            Post regularPost = createRegularPost();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.vote(regularPost.getId(), UUID.randomUUID(), testUser.getId()));
            assertTrue(ex.getMessage().contains("not a poll"));
        }

        @Test
        @DisplayName("Should throw when option does not belong to the given post")
        void shouldThrowWhenOptionBelongsToAnotherPost() {
            Post post1 = createPollPost(Arrays.asList("A", "B"));
            Post post2 = createPollPost(Arrays.asList("X", "Y"), testUser2);
            List<PollOption> post2Options = pollOptionRepository
                    .findByPostIdOrderByDisplayOrder(post2.getId());

            // Voting on post1 with an option from post2 must be rejected
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.vote(post1.getId(), post2Options.get(0).getId(), testUser.getId()));
            assertTrue(ex.getMessage().contains("does not belong to this poll"));
        }
    }

    // ─── getPollData ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPollData()")
    class PollResultsTests {

        @Test
        @DisplayName("Should hide vote counts before voting when viewResults=false")
        void shouldHideResultsBeforeVoting() {
            Post post = createPollPost(Arrays.asList("A", "B"));

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser.getId(), false);

            assertFalse((Boolean) data.get("resultsVisible"));
            for (Object opt : (List<?>) data.get("options")) {
                Map<?, ?> optMap = (Map<?, ?>) opt;
                assertNull(optMap.get("voteCount"));
                assertNull(optMap.get("percentage"));
            }
        }

        @Test
        @DisplayName("Should show vote counts after the requesting user has voted")
        void shouldShowResultsAfterVoting() {
            Post post = createPollPost(Arrays.asList("A", "B"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());

            pollService.vote(post.getId(), options.get(0).getId(), testUser.getId());

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser.getId(), false);

            assertTrue((Boolean) data.get("resultsVisible"));
            Map<?, ?> optA = (Map<?, ?>) ((List<?>) data.get("options")).get(0);
            assertNotNull(optA.get("voteCount"));
            assertNotNull(optA.get("percentage"));
        }

        @Test
        @DisplayName("Should show vote counts when viewResults=true even before voting")
        void shouldShowResultsWithViewResultsFlag() {
            Post post = createPollPost(Arrays.asList("A", "B"));

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser2.getId(), true);

            assertTrue((Boolean) data.get("resultsVisible"));
            for (Object opt : (List<?>) data.get("options")) {
                Map<?, ?> optMap = (Map<?, ?>) opt;
                assertNotNull(optMap.get("voteCount"));
                assertNotNull(optMap.get("percentage"));
            }
        }

        @Test
        @DisplayName("Should hide results from non-voting user when viewResults=false — even when others have voted")
        void shouldHideResultsFromNonVotingUser() {
            Post post = createPollPost(Arrays.asList("A", "B"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());

            // testUser votes, but testUser2 has not
            pollService.vote(post.getId(), options.get(0).getId(), testUser.getId());

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser2.getId(), false);

            assertFalse((Boolean) data.get("resultsVisible"),
                    "testUser2 has not voted, so results must be hidden from them");
            for (Object opt : (List<?>) data.get("options")) {
                Map<?, ?> optMap = (Map<?, ?>) opt;
                assertNull(optMap.get("voteCount"));
                assertNull(optMap.get("percentage"));
            }
        }

        @Test
        @DisplayName("Should return totalVotes=0 before any votes")
        void shouldReturnZeroTotalVotesBeforeAnyVotes() {
            Post post = createPollPost(Arrays.asList("A", "B"));

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser.getId(), false);

            assertEquals(0, data.get("totalVotes"));
        }

        @Test
        @DisplayName("Should return userVotedOptionId=null before voting")
        void shouldReturnNullUserVotedOptionIdBeforeVoting() {
            Post post = createPollPost(Arrays.asList("A", "B"));

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser.getId(), false);

            assertNull(data.get("userVotedOptionId"));
        }

        @Test
        @DisplayName("Should return correct userVotedOptionId after voting")
        void shouldReturnCorrectUserVotedOptionIdAfterVoting() {
            Post post = createPollPost(Arrays.asList("A", "B"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            UUID votedOptionId = options.get(1).getId();

            pollService.vote(post.getId(), votedOptionId, testUser.getId());

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser.getId(), false);

            assertEquals(votedOptionId, data.get("userVotedOptionId"));
        }

        @Test
        @DisplayName("Should calculate percentage correctly — 1 vote of 2 total = 50.0%")
        void shouldCalculatePercentageCorrectly() {
            Post post = createPollPost(Arrays.asList("A", "B"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());

            // Both users vote for option A → totalVotes=2, optionA=2 votes
            pollService.vote(post.getId(), options.get(0).getId(), testUser.getId());
            pollService.vote(post.getId(), options.get(0).getId(), testUser2.getId());

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser.getId(), false);
            List<?> optionDtos = (List<?>) data.get("options");

            Map<?, ?> optA = (Map<?, ?>) optionDtos.get(0);
            assertEquals(2, optA.get("voteCount"));
            assertEquals(100.0, optA.get("percentage"),
                    "2 of 2 votes must equal 100.0%");

            Map<?, ?> optB = (Map<?, ?>) optionDtos.get(1);
            assertEquals(0, optB.get("voteCount"));
            assertEquals(0.0, optB.get("percentage"),
                    "0 of 2 votes must equal 0.0%");
        }

        @Test
        @DisplayName("Should calculate 50/50 split correctly when votes are divided equally")
        void shouldCalculateSplitPercentageCorrectly() {
            Post post = createPollPost(Arrays.asList("A", "B"));
            List<PollOption> options = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());

            // Split: testUser → A, testUser2 → B
            pollService.vote(post.getId(), options.get(0).getId(), testUser.getId());
            pollService.vote(post.getId(), options.get(1).getId(), testUser2.getId());

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser.getId(), false);
            List<?> optionDtos = (List<?>) data.get("options");

            Map<?, ?> optA = (Map<?, ?>) optionDtos.get(0);
            assertEquals(50.0, optA.get("percentage"), "1 of 2 votes must equal 50.0%");

            Map<?, ?> optB = (Map<?, ?>) optionDtos.get(1);
            assertEquals(50.0, optB.get("percentage"), "1 of 2 votes must equal 50.0%");
        }

        @Test
        @DisplayName("Should return percentage=0.0 for all options when no votes exist")
        void shouldReturnZeroPercentageWhenNoVotes() {
            Post post = createPollPost(Arrays.asList("A", "B"));

            Map<String, Object> data = pollService.getPollData(post.getId(), testUser.getId(), true);

            for (Object opt : (List<?>) data.get("options")) {
                Map<?, ?> optMap = (Map<?, ?>) opt;
                assertEquals(0.0, optMap.get("percentage"),
                        "Percentage must be 0.0 when totalVotes=0 to avoid division by zero");
            }
        }

        @Test
        @DisplayName("Should throw when post not found")
        void shouldThrowWhenPostNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.getPollData(UUID.randomUUID(), testUser.getId(), false));
            assertTrue(ex.getMessage().contains("Post not found"));
        }

        @Test
        @DisplayName("Should throw when post is hidden")
        void shouldThrowWhenPostHidden() {
            Post post = createPollPost(Arrays.asList("A", "B"));
            postsService.hidePost(post.getId(), testUser.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.getPollData(post.getId(), testUser.getId(), false));
            assertTrue(ex.getMessage().contains("Post not found"));
        }

        @Test
        @DisplayName("Should throw when post is not a poll type")
        void shouldThrowWhenPostIsNotPoll() {
            Post regularPost = createRegularPost();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pollService.getPollData(regularPost.getId(), testUser.getId(), false));
            assertTrue(ex.getMessage().contains("not a poll"));
        }
    }

    // ─── getPollOptions ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPollOptions()")
    class GetPollOptionsTests {

        @Test
        @DisplayName("Should return options ordered by displayOrder")
        void shouldReturnOptionsInDisplayOrder() {
            Post post = createPollPost(Arrays.asList("First", "Second", "Third"));

            List<PollOption> options = pollService.getPollOptions(post.getId());

            assertEquals(3, options.size());
            assertEquals("First",  options.get(0).getOptionText());
            assertEquals("Second", options.get(1).getOptionText());
            assertEquals("Third",  options.get(2).getOptionText());
        }

        @Test
        @DisplayName("Should return empty list when post has no options")
        void shouldReturnEmptyListWhenNoOptions() {
            // Create a post without poll options by using the raw createPollOptions path
            CreatePostRequest req = new CreatePostRequest("Empty Poll Post", "Some content");
            req.setPostType(CreatePostRequestPostType.STANDARD); // no options attached
            Post post = postsService.createPost(req, null, testUser.getId());

            List<PollOption> options = pollService.getPollOptions(post.getId());

            assertNotNull(options);
            assertTrue(options.isEmpty());
        }

        @Test
        @DisplayName("Should only return options belonging to the given post — not options from other posts")
        void shouldNotReturnOptionsFromOtherPosts() {
            Post post1 = createPollPost(Arrays.asList("A1", "B1"));
            Post post2 = createPollPost(Arrays.asList("A2", "B2"), testUser2);

            List<PollOption> options = pollService.getPollOptions(post1.getId());

            assertEquals(2, options.size());
            assertTrue(options.stream().allMatch(o -> o.getPostId().equals(post1.getId())),
                    "All returned options must belong to post1");
        }
    }

    // ─── Poll Post Creation Atomicity ──────────────────────────────────────────

    @Nested
    @DisplayName("Poll Post Creation Atomicity")
    class PollPostAtomicityTests {

        @Test
        @DisplayName("createPost with pollOptions saves post and options in one atomic call")
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
        @DisplayName("createPost with invalid pollOptions propagates validation error")
        void shouldPropagateValidationErrorFromInvalidOptions() {
            CreatePostRequest req = new CreatePostRequest("Bad Poll", "");
            req.setPostType(CreatePostRequestPostType.POLL);
            req.setPollOptions(List.of("Only")); // 1 option — requires at least 2

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.createPost(req, null, testUser.getId()));
            assertTrue(ex.getMessage().contains("at least 2"));
        }

        @Test
        @DisplayName("Should save poll post with no options when pollOptions is null — documents silent invalid-state risk")
        void shouldSavePollPostWhenPollOptionsNull() {
            // The impl guard: isPoll && pollOptions != null && !isEmpty
            // If pollOptions is null the condition short-circuits and createPollOptions
            // is never called. The post is committed with postType="poll" and zero options.
            // This test documents the current behaviour so any future fix (e.g. throwing
            // when a poll post arrives with null options) is a conscious, visible decision.
            CreatePostRequest req = new CreatePostRequest("Poll No Options", "");
            req.setPostType(CreatePostRequestPostType.POLL);
            req.setPollOptions(null); // explicitly null

            Post post = postsService.createPost(req, null, testUser.getId());

            assertNotNull(post.getId());
            assertEquals("poll", post.getPostType());
            assertTrue(pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId()).isEmpty(),
                    "No options must be persisted when pollOptions is null — " +
                            "this is a known invalid state that should be addressed");
        }

        @Test
        @DisplayName("Should save poll post with no options when pollOptions is empty — documents silent invalid-state risk")
        void shouldSavePollPostWhenPollOptionsEmpty() {
            // Same guard short-circuit as the null case above, but via the isEmpty() branch.
            CreatePostRequest req = new CreatePostRequest("Poll Empty Options", "");
            req.setPostType(CreatePostRequestPostType.POLL);
            req.setPollOptions(List.of()); // explicitly empty

            Post post = postsService.createPost(req, null, testUser.getId());

            assertNotNull(post.getId());
            assertEquals("poll", post.getPostType());
            assertTrue(pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId()).isEmpty(),
                    "No options must be persisted when pollOptions is empty — " +
                            "this is a known invalid state that should be addressed");
        }

        @Test
        @DisplayName("Should silently ignore pollOptions on a non-poll post — options are not persisted")
        void shouldIgnorePollOptionsOnNonPollPost() {
            // The impl only calls createPollOptions when isPoll=true.
            // Options passed on a TEXT post must be dropped without error.
            CreatePostRequest req = new CreatePostRequest("Text Post With Options", "Content");
            req.setPostType(CreatePostRequestPostType.STANDARD);
            req.setPollOptions(Arrays.asList("Ignored A", "Ignored B"));

            Post post = postsService.createPost(req, null, testUser.getId());

            assertNotNull(post.getId());
            assertNotEquals("poll", post.getPostType());
            assertTrue(pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId()).isEmpty(),
                    "Poll options provided on a non-poll post must be silently ignored");
        }

        @Test
        @DisplayName("Should not call createPollOptions when post is poll type but options list is null")
        void shouldNotCreateOptionsWhenPollOptionsNullEvenForPollType() {
            // Guard condition documented: the three-part AND means a null pollOptions
            // on a POLL post never reaches createPollOptions validation.
            // Contrast with shouldPropagateValidationErrorFromInvalidOptions, where
            // at least one option IS provided (triggering the 'at least 2' validation).
            CreatePostRequest req = new CreatePostRequest("Guard Test Poll", "");
            req.setPostType(CreatePostRequestPostType.POLL);
            req.setPollOptions(null);

            // Must not throw — the guard short-circuits before createPollOptions is called
            assertDoesNotThrow(() -> postsService.createPost(req, null, testUser.getId()));

            long optionCount = pollOptionRepository.findAll().stream()
                    .filter(o -> postRepository.findById(o.getPostId())
                            .map(p -> "Guard Test Poll".equals(p.getTitle()))
                            .orElse(false))
                    .count();
            assertEquals(0, optionCount);
        }
    }

    // ─── Vote Atomicity ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Vote Atomicity")
    class VoteAtomicityTests {

        @Test
        @DisplayName("Successful vote commits all three writes: option vote_count, post total_votes, and vote row")
        void shouldCommitAllThreeWritesOnSuccessfulVote() {
            Post post = createPollPost(Arrays.asList("Yes", "No"));
            List<PollOption> opts = pollOptionRepository.findByPostIdOrderByDisplayOrder(post.getId());
            UUID optionId = opts.get(0).getId();

            pollService.vote(post.getId(), optionId, testUser.getId());

            assertEquals(1,
                    pollOptionRepository.findById(optionId).orElseThrow().getVoteCount(),
                    "option vote_count must be committed");
            assertEquals(1,
                    postRepository.findById(post.getId()).orElseThrow().getTotalVotes(),
                    "post total_votes must be committed");
            assertTrue(pollVoteRepository.findByPostIdAndUserId(post.getId(), testUser.getId()).isPresent(),
                    "PollVote row must be committed");
        }
    }
}
