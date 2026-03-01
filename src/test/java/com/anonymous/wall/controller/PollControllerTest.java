package com.anonymous.wall.controller;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PollOptionRepository;
import com.anonymous.wall.repository.PollVoteRepository;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.JwtTokenService;
import com.anonymous.wall.service.PollService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Poll Controller Tests")
class PollControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    PostRepository postRepository;

    @Inject
    PollOptionRepository pollOptionRepository;

    @Inject
    PollVoteRepository pollVoteRepository;

    @Inject
    CommentRepository commentRepository;

    @Inject
    PostLikeRepository postLikeRepository;

    @Inject
    PollService pollService;

    @Inject
    JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/posts";

    private UserEntity testUser;
    private UserEntity testUser2;
    private String jwtToken;
    private String jwtToken2;
    private Post pollPost;
    private UUID optionAId;
    private UUID optionBId;

    @BeforeEach
    void setUp() {
        pollVoteRepository.deleteAll();
        pollOptionRepository.deleteAll();
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();

        testUser = new UserEntity();
        testUser.setEmail("pollctrl" + System.currentTimeMillis() + "@harvard.edu");
        testUser.setSchoolDomain("harvard.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser = userRepository.save(testUser);
        jwtToken = jwtTokenService.generateToken(testUser);

        testUser2 = new UserEntity();
        testUser2.setEmail("pollctrl2" + System.currentTimeMillis() + "@harvard.edu");
        testUser2.setSchoolDomain("harvard.edu");
        testUser2.setVerified(true);
        testUser2.setPasswordSet(true);
        testUser2 = userRepository.save(testUser2);
        jwtToken2 = jwtTokenService.generateToken(testUser2);

        // Create a poll post with options
        pollPost = new Post(testUser.getId(), "Favorite language?", "", "campus", "harvard.edu");
        pollPost.setPostType("poll");
        pollPost = postRepository.save(pollPost);

        List<com.anonymous.wall.entity.PollOption> options =
                pollService.createPollOptions(pollPost.getId(), Arrays.asList("Java", "Python"));
        optionAId = options.get(0).getId();
        optionBId = options.get(1).getId();
    }

    @AfterEach
    void tearDown() {
        pollVoteRepository.deleteAll();
        pollOptionRepository.deleteAll();
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
    }

    // ===================================================================
    // POST /posts/{postId}/vote
    // ===================================================================

    @Nested
    @DisplayName("Vote on Poll - Positive Cases")
    class VotePositiveTests {

        @Test
        @DisplayName("Should vote successfully and return poll data with results visible")
        void shouldVoteSuccessfully() {
            Map<String, String> body = new HashMap<>();
            body.put("optionId", optionAId.toString());

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + pollPost.getId() + "/vote", body)
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<?, ?> respBody = response.body();
            assertNotNull(respBody);
            assertEquals("Vote cast successfully", respBody.get("message"));

            Map<?, ?> poll = (Map<?, ?>) respBody.get("poll");
            assertNotNull(poll);
            assertTrue((Boolean) poll.get("resultsVisible"));
            assertEquals(1, poll.get("totalVotes"));
            assertEquals(optionAId.toString(), poll.get("userVotedOptionId").toString());
        }

        @Test
        @DisplayName("Vote response should include options with vote counts")
        void shouldReturnOptionsWithVoteCounts() {
            Map<String, String> body = new HashMap<>();
            body.put("optionId", optionAId.toString());

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + pollPost.getId() + "/vote", body)
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<?, ?> poll = (Map<?, ?>) response.body().get("poll");
            List<?> options = (List<?>) poll.get("options");
            assertNotNull(options);
            assertEquals(2, options.size());

            // The voted option should have voteCount=1
            Map<?, ?> votedOption = (Map<?, ?>) options.stream()
                .filter(o -> optionAId.toString().equals(((Map<?, ?>) o).get("id").toString()))
                .findFirst().orElseThrow();
            assertEquals(1, ((Number) votedOption.get("voteCount")).intValue());
        }

        @Test
        @DisplayName("Two different users can vote on same poll")
        void shouldAllowDifferentUsersToVote() {
            // User 1 votes
            Map<String, String> body1 = new HashMap<>();
            body1.put("optionId", optionAId.toString());
            client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + pollPost.getId() + "/vote", body1)
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            // User 2 votes
            Map<String, String> body2 = new HashMap<>();
            body2.put("optionId", optionBId.toString());
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + pollPost.getId() + "/vote", body2)
                    .header("Authorization", "Bearer " + jwtToken2),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<?, ?> poll = (Map<?, ?>) response.body().get("poll");
            assertEquals(2, poll.get("totalVotes"));
        }
    }

    @Nested
    @DisplayName("Vote on Poll - Negative Cases")
    class VoteNegativeTests {

        @Test
        @DisplayName("Should return 409 on duplicate vote by same user")
        void shouldReturn409OnDuplicateVote() {
            Map<String, String> body = new HashMap<>();
            body.put("optionId", optionAId.toString());

            // First vote succeeds
            client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/" + pollPost.getId() + "/vote", body)
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            // Second vote by same user returns 409
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + pollPost.getId() + "/vote", body)
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        }

        @Test
        @DisplayName("Should return 400 when optionId is missing from request body")
        void shouldReturn400WhenOptionIdMissing() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + pollPost.getId() + "/vote", new HashMap<>())
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should return 400 when optionId is invalid UUID")
        void shouldReturn400WhenOptionIdIsInvalidUUID() {
            Map<String, String> body = new HashMap<>();
            body.put("optionId", "not-a-uuid");

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + pollPost.getId() + "/vote", body)
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should return 404 when post does not exist")
        void shouldReturn404ForNonExistentPost() {
            Map<String, String> body = new HashMap<>();
            body.put("optionId", optionAId.toString());

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + UUID.randomUUID() + "/vote", body)
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("Should return 400 when optionId does not belong to the poll")
        void shouldReturn400WhenOptionDoesNotBelongToPoll() {
            // Create a second poll post with its own option
            Post otherPoll = new Post(testUser.getId(), "Other Poll", "", "campus", "harvard.edu");
            otherPoll.setPostType("poll");
            otherPoll = postRepository.save(otherPoll);
            List<com.anonymous.wall.entity.PollOption> otherOptions =
                    pollService.createPollOptions(otherPoll.getId(), Arrays.asList("X", "Y"));
            UUID foreignOptionId = otherOptions.get(0).getId();

            Map<String, String> body = new HashMap<>();
            body.put("optionId", foreignOptionId.toString());

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + pollPost.getId() + "/vote", body)
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() {
            Map<String, String> body = new HashMap<>();
            body.put("optionId", optionAId.toString());

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + pollPost.getId() + "/vote", body),
                    Map.class
                )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        @DisplayName("Should return 400 when voting on a standard (non-poll) post")
        void shouldReturn400WhenVotingOnStandardPost() {
            Post standardPost = new Post(testUser.getId(), "Standard Post", "Some content", "campus", "harvard.edu");
            standardPost = postRepository.save(standardPost);

            Map<String, String> body = new HashMap<>();
            body.put("optionId", optionAId.toString());

            Post finalStandardPost = standardPost;
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + finalStandardPost.getId() + "/vote", body)
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }
    }

    // ===================================================================
    // GET /posts/{postId}/poll
    // ===================================================================

    @Nested
    @DisplayName("Get Poll - Positive Cases")
    class GetPollPositiveTests {

        @Test
        @DisplayName("Should return poll data with hidden results for user who has not voted")
        void shouldHideResultsForNonVoter() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + pollPost.getId() + "/poll")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<?, ?> body = response.body();
            assertNotNull(body);
            assertFalse((Boolean) body.get("resultsVisible"));
            assertNull(body.get("userVotedOptionId"));
            assertEquals(0, body.get("totalVotes"));

            List<?> options = (List<?>) body.get("options");
            assertNotNull(options);
            for (Object opt : options) {
                Map<?, ?> optMap = (Map<?, ?>) opt;
                assertNull(optMap.get("voteCount"), "voteCount should be null before voting");
                assertNull(optMap.get("percentage"), "percentage should be null before voting");
            }
        }

        @Test
        @DisplayName("Should return results visible after user votes")
        void shouldShowResultsAfterVoting() {
            // Cast vote first
            pollService.vote(pollPost.getId(), optionAId, testUser.getId());

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + pollPost.getId() + "/poll")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<?, ?> body = response.body();
            assertTrue((Boolean) body.get("resultsVisible"));
            assertEquals(optionAId.toString(), body.get("userVotedOptionId").toString());
            assertEquals(1, body.get("totalVotes"));
        }

        @Test
        @DisplayName("Should return results visible with viewResults=true even without voting")
        void shouldShowResultsWithViewResultsFlag() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + pollPost.getId() + "/poll?viewResults=true")
                    .header("Authorization", "Bearer " + jwtToken2),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map<?, ?> body = response.body();
            assertTrue((Boolean) body.get("resultsVisible"));
            assertNull(body.get("userVotedOptionId")); // User hasn't voted

            List<?> options = (List<?>) body.get("options");
            for (Object opt : options) {
                Map<?, ?> optMap = (Map<?, ?>) opt;
                assertNotNull(optMap.get("voteCount"), "voteCount should be visible with viewResults=true");
                assertNotNull(optMap.get("percentage"), "percentage should be visible with viewResults=true");
            }
        }

        @Test
        @DisplayName("Should return correct options count and display order")
        void shouldReturnCorrectOptionsOrderAndCount() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + pollPost.getId() + "/poll")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<?> options = (List<?>) response.body().get("options");
            assertEquals(2, options.size());
            assertEquals(0, ((Number) ((Map<?, ?>) options.get(0)).get("displayOrder")).intValue());
            assertEquals(1, ((Number) ((Map<?, ?>) options.get(1)).get("displayOrder")).intValue());
        }

        @Test
        @DisplayName("totalVotes is always visible regardless of voted status")
        void shouldAlwaysShowTotalVotes() {
            // No votes yet
            HttpResponse<Map> beforeVote = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + pollPost.getId() + "/poll")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );
            assertEquals(0, beforeVote.body().get("totalVotes"));

            // Cast a vote
            pollService.vote(pollPost.getId(), optionAId, testUser.getId());

            HttpResponse<Map> afterVote = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + pollPost.getId() + "/poll")
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );
            assertEquals(1, afterVote.body().get("totalVotes"));
        }
    }

    @Nested
    @DisplayName("Get Poll - Negative Cases")
    class GetPollNegativeTests {

        @Test
        @DisplayName("Should return 404 for non-existent post")
        void shouldReturn404ForNonExistentPost() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + UUID.randomUUID() + "/poll")
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("Should return 400 for standard (non-poll) post")
        void shouldReturn400ForStandardPost() {
            Post standardPost = new Post(testUser.getId(), "Standard Post", "Some content", "campus", "harvard.edu");
            standardPost = postRepository.save(standardPost);

            Post finalStandardPost = standardPost;
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + finalStandardPost.getId() + "/poll")
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + pollPost.getId() + "/poll"),
                    Map.class
                )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }
}
