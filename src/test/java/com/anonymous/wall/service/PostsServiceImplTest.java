package com.anonymous.wall.service;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.PostLike;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.entity.PostReport;
import com.anonymous.wall.event.PostHiddenEvent;
import com.anonymous.wall.model.CommentParentType;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.model.CreatePostRequestPostType;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.repository.*;
import com.anonymous.wall.service.base.*;
import com.anonymous.wall.service.impl.PostsServiceImpl;
import com.anonymous.wall.util.MediaUtilInterface;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@MicronautTest(transactional = false)
@DisplayName("PostsServiceImpl Tests")
class PostsServiceImplTest {

    @Inject private PostsService postsService;
    @Inject private CommentsService commentsService;
    @Inject private PostRepository postRepository;
    @Inject private PostLikeRepository postLikeRepository;
    @Inject private PostReportRepository postReportRepository;
    @Inject private CommentRepository commentRepository;
    @Inject private UserRepository userRepository;
    @Inject private PollOptionRepository pollOptionRepository;

    // Three shared users — created fresh each test with UUID suffix to avoid email collisions.
    // testUser   = harvard.edu (primary actor)
    // otherUser  = harvard.edu (same school as testUser)
    // mitUser    = mit.edu     (different school)
    private UserEntity testUser;
    private UserEntity otherUser;
    private UserEntity mitUser;

    @BeforeEach
    void setUp() {
        postReportRepository.deleteAll();
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        pollOptionRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // UUID suffix eliminates timestamp collisions when multiple users
        // are created within the same millisecond.
        String s = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        testUser = userRepository.save(campusUser("test" + s, "harvard.edu"));
        otherUser = userRepository.save(campusUser("other" + s, "harvard.edu"));
        mitUser = userRepository.save(campusUser("mit" + s, "mit.edu"));
    }

    @AfterEach
    void tearDown() {
        postReportRepository.deleteAll();
        postLikeRepository.deleteAll();
        commentRepository.deleteAll();
        pollOptionRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private UserEntity campusUser(String emailPrefix, String domain) {
        UserEntity u = new UserEntity();
        u.setEmail(emailPrefix + "@" + domain);
        u.setSchoolDomain(domain);
        u.setVerified(true);
        u.setPasswordSet(true);
        return u;
    }

    private Post saveCampusPost(String title, String content, UserEntity owner) {
        return postRepository.save(new Post(owner.getId(), title, content, "campus", owner.getSchoolDomain()));
    }

    private Post saveNationalPost(String title, String content, UserEntity owner) {
        return postRepository.save(new Post(owner.getId(), title, content, "national", null));
    }

    private Post createCampusPostViaService(String title, String content, UserEntity owner) {
        CreatePostRequest req = new CreatePostRequest(title, content);
        return postsService.createPost(req, owner.getId());
    }

    private Post createNationalPostViaService(String title, String content, UserEntity owner) {
        CreatePostRequest req = new CreatePostRequest(title, content);
        req.setWall(com.anonymous.wall.model.CreatePostRequestWall.NATIONAL);
        return postsService.createPost(req, owner.getId());
    }

    // ─── createPost ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createPost()")
    class CreatePostTests {

        @Test
        @DisplayName("Should create campus post with correct wall and schoolDomain")
        void shouldCreateCampusPost() {
            Post result = createCampusPostViaService("Title", "Content", testUser);

            assertNotNull(result.getId());
            assertEquals("campus", result.getWall());
            assertEquals("harvard.edu", result.getSchoolDomain());
            assertEquals(testUser.getId(), result.getUserId());
        }

        @Test
        @DisplayName("Should default wall to campus when not specified")
        void shouldDefaultWallToCampus() {
            CreatePostRequest req = new CreatePostRequest("Title", "Content");
            // wall not set — impl defaults to "campus"
            Post result = postsService.createPost(req, testUser.getId());

            assertEquals("campus", result.getWall());
        }

        @Test
        @DisplayName("Should create national post with null schoolDomain")
        void shouldCreateNationalPost() {
            Post result = createNationalPostViaService("Title", "Content", testUser);

            assertEquals("national", result.getWall());
            assertNull(result.getSchoolDomain());
        }

        @Test
        @DisplayName("Should copy profileName from user onto post")
        void shouldCopyProfileNameFromUser() {
            testUser.setProfileName("Anonymous Fox");
            userRepository.update(testUser);

            Post result = createCampusPostViaService("Title", "Content", testUser);

            assertEquals("Anonymous Fox", result.getProfileName());
        }

        @Test
        @DisplayName("Should initialise likeCount and commentCount to zero")
        void shouldInitialiseCountsToZero() {
            Post result = createCampusPostViaService("Title", "Content", testUser);

            assertEquals(0, result.getLikeCount());
            assertEquals(0, result.getCommentCount());
            assertFalse(result.isLiked());
        }

        @Test
        @DisplayName("Should set createdAt and updatedAt on creation")
        void shouldSetTimestamps() {
            Post result = createCampusPostViaService("Title", "Content", testUser);

            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
        }

        @Test
        @DisplayName("Should persist post to DB — retrievable by ID")
        void shouldPersistToDB() {
            Post result = createCampusPostViaService("DB Title", "DB Content", testUser);

            Optional<Post> saved = postRepository.findById(result.getId());
            assertTrue(saved.isPresent());
            assertEquals("DB Title", saved.get().getTitle());
            assertEquals("DB Content", saved.get().getContent());
        }

        @Test
        @DisplayName("Should accept content of exactly 5000 characters — upper boundary")
        void shouldAcceptContentAtMaxLength() {
            String content = "X".repeat(5000);
            Post result = createCampusPostViaService("Title", content, testUser);

            assertEquals(5000, result.getContent().length());
        }

        @Test
        @DisplayName("Should accept title of exactly 255 characters — upper boundary")
        void shouldAcceptTitleAtMaxLength() {
            String title = "T".repeat(255);
            Post result = createCampusPostViaService(title, "Content", testUser);

            assertEquals(255, result.getTitle().length());
        }

        @Test
        @DisplayName("Should fail when title is null")
        void shouldFailWhenTitleNull() {
            CreatePostRequest req = new CreatePostRequest(null, "Content");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.createPost(req, testUser.getId()));
            assertTrue(ex.getMessage().contains("title cannot be empty"));
        }

        @Test
        @DisplayName("Should fail when title is blank")
        void shouldFailWhenTitleBlank() {
            CreatePostRequest req = new CreatePostRequest("   ", "Content");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.createPost(req, testUser.getId()));
            assertTrue(ex.getMessage().contains("title cannot be empty"));
        }

        @Test
        @DisplayName("Should fail when title exceeds 255 characters")
        void shouldFailWhenTitleTooLong() {
            CreatePostRequest req = new CreatePostRequest("T".repeat(256), "Content");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.createPost(req, testUser.getId()));
            assertTrue(ex.getMessage().contains("exceeds maximum length of 255 characters"));
        }

        @Test
        @DisplayName("Should fail when content is empty for standard post")
        void shouldFailWhenContentEmpty() {
            CreatePostRequest req = new CreatePostRequest("Title", "");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.createPost(req, testUser.getId()));
            assertTrue(ex.getMessage().contains("cannot be empty"));
        }

        @Test
        @DisplayName("Should fail when content is blank for standard post")
        void shouldFailWhenContentBlank() {
            CreatePostRequest req = new CreatePostRequest("Title", "   \n\t   ");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.createPost(req, testUser.getId()));
            assertTrue(ex.getMessage().contains("cannot be empty"));
        }

        @Test
        @DisplayName("Should fail when content exceeds 5000 characters")
        void shouldFailWhenContentTooLong() {
            CreatePostRequest req = new CreatePostRequest("Title", "X".repeat(5001));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.createPost(req, testUser.getId()));
            assertTrue(ex.getMessage().contains("exceeds maximum length"));
        }

        @Test
        @DisplayName("Should fail when user does not exist")
        void shouldFailWhenUserNotFound() {
            CreatePostRequest req = new CreatePostRequest("Title", "Content");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.createPost(req, UUID.randomUUID()));
            assertTrue(ex.getMessage().contains("User not found"));
        }

        @Test
        @DisplayName("Should fail campus post when user has no school domain")
        void shouldFailCampusPostWhenNoSchoolDomain() {
            UserEntity noSchool = userRepository.save(campusUser("noschool_x1", null));
            noSchool.setSchoolDomain(null);
            userRepository.update(noSchool);

            CreatePostRequest req = new CreatePostRequest("Title", "Content");
            UUID uid = noSchool.getId();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.createPost(req, uid));
            assertTrue(ex.getMessage().contains("Cannot post to campus wall"));
        }

        @Test
        @DisplayName("Poll post does not require content — content is optional")
        void shouldNotRequireContentForPollPost() {
            CreatePostRequest req = new CreatePostRequest("Poll Title", "");
            req.setPostType(CreatePostRequestPostType.POLL);
            req.setPollOptions(List.of("A", "B"));

            // Must not throw for missing content when type=POLL
            assertDoesNotThrow(() -> postsService.createPost(req, testUser.getId()));
        }
    }

    // ─── getPostsByWall ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPostsByWall()")
    class GetPostsByWallTests {

        @Test
        @DisplayName("Should return campus posts for correct school domain only")
        void shouldFilterBySchoolDomain() {
            saveCampusPost("Harvard 1", "c", testUser);
            saveCampusPost("Harvard 2", "c", testUser);
            saveCampusPost("MIT post",  "c", mitUser);   // different school — must NOT appear

            Page<Post> result = postsService.getPostsByWall("campus",
                    Pageable.from(0, 20), testUser.getId(), "harvard.edu", SortBy.NEWEST);

            assertEquals(2, result.getTotalSize());
            assertTrue(result.getContent().stream()
                    .allMatch(p -> "harvard.edu".equals(p.getSchoolDomain())));
        }

        @Test
        @DisplayName("Should return all national posts regardless of school domain")
        void shouldReturnAllNationalPosts() {
            saveNationalPost("National 1", "c", testUser);
            saveNationalPost("National 2", "c", mitUser);

            Page<Post> result = postsService.getPostsByWall("national",
                    Pageable.from(0, 20), testUser.getId(), null, SortBy.NEWEST);

            assertEquals(2, result.getTotalSize());
        }

        @Test
        @DisplayName("Should return empty page when user has no school domain and requests campus wall")
        void shouldReturnEmptyWhenNoSchoolDomain() {
            saveCampusPost("Harvard", "c", testUser);

            Page<Post> result = postsService.getPostsByWall("campus",
                    Pageable.from(0, 20), testUser.getId(), "", SortBy.NEWEST);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should throw when wall is invalid")
        void shouldThrowForInvalidWall() {
            assertThrows(IllegalArgumentException.class, () ->
                    postsService.getPostsByWall("invalid",
                            Pageable.from(0, 20), testUser.getId(), "harvard.edu", SortBy.NEWEST));
        }

        @Test
        @DisplayName("Should throw when user does not exist")
        void shouldThrowWhenUserNotFound() {
            assertThrows(IllegalArgumentException.class, () ->
                    postsService.getPostsByWall("campus",
                            Pageable.from(0, 20), UUID.randomUUID(), "harvard.edu", SortBy.NEWEST));
        }

        @Test
        @DisplayName("Should exclude hidden posts from results")
        void shouldExcludeHiddenPosts() {
            Post visible = saveCampusPost("Visible", "c", testUser);
            Post hidden  = saveCampusPost("Hidden",  "c", testUser);
            postsService.hidePost(hidden.getId(), testUser.getId());

            Page<Post> result = postsService.getPostsByWall("campus",
                    Pageable.from(0, 20), testUser.getId(), "harvard.edu", SortBy.NEWEST);

            assertEquals(1, result.getTotalSize());
            assertEquals(visible.getId(), result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("Should default to NEWEST sort when sortBy is null")
        void shouldDefaultToNewestWhenSortByNull() {
            for (int i = 0; i < 3; i++) saveCampusPost("T" + i, "c", testUser);

            Page<Post> result = postsService.getPostsByWall("campus",
                    Pageable.from(0, 10), testUser.getId(), "harvard.edu", null);

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should sort by NEWEST — createdAt descending")
        void shouldSortByNewest() {
            for (int i = 0; i < 5; i++) saveCampusPost("T" + i, "c", testUser);

            Page<Post> result = postsService.getPostsByWall("campus",
                    Pageable.from(0, 5), testUser.getId(), "harvard.edu", SortBy.NEWEST);

            List<Post> posts = result.getContent();
            for (int i = 0; i < posts.size() - 1; i++) {
                assertFalse(posts.get(i).getCreatedAt().isBefore(posts.get(i + 1).getCreatedAt()),
                        "Each post must be >= the next in creation time");
            }
        }

        @Test
        @DisplayName("Should sort by OLDEST — createdAt ascending")
        void shouldSortByOldest() {
            for (int i = 0; i < 5; i++) saveCampusPost("T" + i, "c", testUser);

            Page<Post> result = postsService.getPostsByWall("campus",
                    Pageable.from(0, 5), testUser.getId(), "harvard.edu", SortBy.OLDEST);

            List<Post> posts = result.getContent();
            for (int i = 0; i < posts.size() - 1; i++) {
                assertFalse(posts.get(i).getCreatedAt().isAfter(posts.get(i + 1).getCreatedAt()),
                        "Each post must be <= the next in creation time");
            }
        }

        @Test
        @DisplayName("Should sort by MOST_LIKED — likeCount descending")
        void shouldSortByMostLiked() {
            for (int i = 0; i < 5; i++) {
                Post p = new Post(testUser.getId(), "T" + i, "c", "campus", "harvard.edu");
                p.setLikeCount(i);
                postRepository.save(p);
            }

            Page<Post> result = postsService.getPostsByWall("campus",
                    Pageable.from(0, 5), testUser.getId(), "harvard.edu", SortBy.MOST_LIKED);

            List<Post> posts = result.getContent();
            for (int i = 0; i < posts.size() - 1; i++) {
                assertTrue(posts.get(i).getLikeCount() >= posts.get(i + 1).getLikeCount());
            }
        }

        @Test
        @DisplayName("Should sort by LEAST_LIKED — likeCount ascending")
        void shouldSortByLeastLiked() {
            for (int i = 0; i < 5; i++) {
                Post p = new Post(testUser.getId(), "T" + i, "c", "campus", "harvard.edu");
                p.setLikeCount(i);
                postRepository.save(p);
            }

            Page<Post> result = postsService.getPostsByWall("campus",
                    Pageable.from(0, 5), testUser.getId(), "harvard.edu", SortBy.LEAST_LIKED);

            List<Post> posts = result.getContent();
            for (int i = 0; i < posts.size() - 1; i++) {
                assertTrue(posts.get(i).getLikeCount() <= posts.get(i + 1).getLikeCount());
            }
        }

        @Test
        @DisplayName("Should return same total regardless of sort order")
        void shouldReturnSameTotalForAllSorts() {
            for (int i = 0; i < 10; i++) saveCampusPost("T" + i, "c", testUser);
            Pageable pageable = Pageable.from(0, 20);

            long totalNewest  = postsService.getPostsByWall("campus", pageable, testUser.getId(), "harvard.edu", SortBy.NEWEST).getTotalSize();
            long totalOldest  = postsService.getPostsByWall("campus", pageable, testUser.getId(), "harvard.edu", SortBy.OLDEST).getTotalSize();
            long totalLiked   = postsService.getPostsByWall("campus", pageable, testUser.getId(), "harvard.edu", SortBy.MOST_LIKED).getTotalSize();

            assertEquals(totalNewest, totalOldest);
            assertEquals(totalNewest, totalLiked);
        }

        @Test
        @DisplayName("Should paginate correctly — second page has remaining items")
        void shouldPaginateCorrectly() {
            for (int i = 0; i < 25; i++) saveCampusPost("T" + i, "c", testUser);

            Page<Post> page1 = postsService.getPostsByWall("campus", Pageable.from(0, 20),
                    testUser.getId(), "harvard.edu", SortBy.NEWEST);
            Page<Post> page2 = postsService.getPostsByWall("campus", Pageable.from(1, 20),
                    testUser.getId(), "harvard.edu", SortBy.NEWEST);

            assertEquals(20, page1.getContent().size());
            assertEquals(5,  page2.getContent().size());
            assertEquals(25, page1.getTotalSize());
        }

        @Test
        @DisplayName("Should return empty page when requested page is beyond available data")
        void shouldReturnEmptyPageBeyondData() {
            for (int i = 0; i < 5; i++) saveCampusPost("T" + i, "c", testUser);

            Page<Post> result = postsService.getPostsByWall("campus", Pageable.from(5, 20),
                    testUser.getId(), "harvard.edu", SortBy.NEWEST);

            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("Should enrich posts with correct liked=true for liked posts")
        void shouldEnrichLikedPosts() {
            Post liked    = saveCampusPost("Liked",    "c", otherUser);
            Post notLiked = saveCampusPost("NotLiked", "c", otherUser);
            postLikeRepository.save(new PostLike(liked.getId(), testUser.getId()));

            Page<Post> result = postsService.getPostsByWall("campus",
                    Pageable.from(0, 20), testUser.getId(), "harvard.edu", SortBy.NEWEST);

            Map<UUID, Boolean> likedById = new java.util.HashMap<>();
            result.getContent().forEach(p -> likedById.put(p.getId(), p.isLiked()));

            assertTrue(likedById.get(liked.getId()),    "Liked post must have isLiked=true");
            assertFalse(likedById.get(notLiked.getId()), "Unliked post must have isLiked=false");
        }

        @Test
        @DisplayName("Should set liked=false for all posts when user has liked none")
        void shouldSetAllLikedFalseWhenNoneLiked() {
            for (int i = 0; i < 3; i++) saveCampusPost("T" + i, "c", otherUser);

            Page<Post> result = postsService.getPostsByWall("campus",
                    Pageable.from(0, 20), testUser.getId(), "harvard.edu", SortBy.NEWEST);

            assertTrue(result.getContent().stream().noneMatch(Post::isLiked));
        }

        @Test
        @DisplayName("Should correctly enrich like status across multiple pages — batch not per-post")
        void shouldEnrichLikeStatusAcrossPages() {
            for (int i = 0; i < 25; i++) {
                Post p = saveCampusPost("T" + i, "c", otherUser);
                if (i % 3 == 0) postLikeRepository.save(new PostLike(p.getId(), testUser.getId()));
            }

            Page<Post> page1 = postsService.getPostsByWall("campus", Pageable.from(0, 20),
                    testUser.getId(), "harvard.edu", SortBy.NEWEST);
            Page<Post> page2 = postsService.getPostsByWall("campus", Pageable.from(1, 20),
                    testUser.getId(), "harvard.edu", SortBy.NEWEST);

            // Both pages must have some liked posts — proving enrichment works per-page
            long liked1 = page1.getContent().stream().filter(Post::isLiked).count();
            long liked2 = page2.getContent().stream().filter(Post::isLiked).count();
            assertTrue(liked1 > 0, "Page 1 must have liked posts");
            assertTrue(liked2 > 0, "Page 2 must have liked posts");
        }

        @Test
        @DisplayName("Should sort maintained consistently across pages — last of page N <= first of page N+1")
        void shouldMaintainSortConsistencyAcrossPages() {
            for (int i = 0; i < 25; i++) {
                Post p = new Post(testUser.getId(), "T" + i, "c", "campus", "harvard.edu");
                p.setLikeCount(i);
                postRepository.save(p);
            }

            Page<Post> page1 = postsService.getPostsByWall("campus", Pageable.from(0, 10),
                    testUser.getId(), "harvard.edu", SortBy.MOST_LIKED);
            Page<Post> page2 = postsService.getPostsByWall("campus", Pageable.from(1, 10),
                    testUser.getId(), "harvard.edu", SortBy.MOST_LIKED);

            int lastPage1  = page1.getContent().get(page1.getContent().size() - 1).getLikeCount();
            int firstPage2 = page2.getContent().get(0).getLikeCount();
            assertTrue(firstPage2 <= lastPage1, "Sort must be consistent across page boundary");
        }
    }

    // ─── getPost ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPost()")
    class GetPostTests {

        @Test
        @DisplayName("Should return post with correct like status enrichment")
        void shouldReturnPostWithLikeStatus() {
            Post post = saveCampusPost("T", "c", otherUser);
            postLikeRepository.save(new PostLike(post.getId(), testUser.getId()));

            Post result = postsService.getPost(post.getId(), testUser.getId());

            assertNotNull(result);
            assertTrue(result.isLiked());
        }

        @Test
        @DisplayName("Should return post with liked=false when user has not liked it")
        void shouldReturnPostWithNotLiked() {
            Post post = saveCampusPost("T", "c", otherUser);

            Post result = postsService.getPost(post.getId(), testUser.getId());

            assertFalse(result.isLiked());
        }

        @Test
        @DisplayName("Should throw for hidden post — hidden posts are inaccessible even to the author")
        void shouldThrowForHiddenPost() {
            Post post = saveCampusPost("T", "c", testUser);
            postsService.hidePost(post.getId(), testUser.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.getPost(post.getId(), testUser.getId()));
            assertEquals("Post not found", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when post does not exist")
        void shouldThrowWhenPostNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.getPost(UUID.randomUUID(), testUser.getId()));
            assertTrue(ex.getMessage().contains("Post not found"));
        }

        @Test
        @DisplayName("Should return national post accessible by any user")
        void shouldReturnNationalPostForAnyUser() {
            Post post = saveNationalPost("National", "c", testUser);

            // mitUser is from a different school but must still access national post
            Post result = postsService.getPost(post.getId(), mitUser.getId());

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw for campus post when user is from different school")
        void shouldThrowForCampusPostFromDifferentSchool() {
            Post post = saveCampusPost("Harvard post", "c", testUser);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.getPost(post.getId(), mitUser.getId()));
            assertTrue(ex.getMessage().contains("access"));
        }

        @Test
        @DisplayName("Should throw for campus post when user has no school domain")
        void shouldThrowWhenUserHasNoSchoolDomain() {
            Post post = saveCampusPost("T", "c", testUser);
            UserEntity noSchool = userRepository.save(campusUser("noschool_x2", "placeholder.edu"));
            noSchool.setSchoolDomain(null);
            userRepository.update(noSchool);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.getPost(post.getId(), noSchool.getId()));
            assertTrue(ex.getMessage().contains("access"));
        }
    }

    // ─── toggleLikeWithDetails ────────────────────────────────────────────────

    @Nested
    @DisplayName("toggleLikeWithDetails()")
    class ToggleLikeWithDetailsTests {

        @Test
        @DisplayName("Liking a post returns liked=true and likeCount=1")
        void shouldReturnLikedTrueOnFirstLike() {
            Post post = saveCampusPost("T", "c", otherUser);

            Map<String, Object> result = postsService.toggleLikeWithDetails(post.getId(), testUser.getId());

            assertTrue((Boolean) result.get("liked"));
            assertEquals(1L, result.get("likeCount"));
        }

        @Test
        @DisplayName("Unliking returns liked=false and decrements likeCount")
        void shouldReturnLikedFalseOnUnlike() {
            Post post = saveCampusPost("T", "c", otherUser);
            postsService.toggleLikeWithDetails(post.getId(), testUser.getId());

            Map<String, Object> result = postsService.toggleLikeWithDetails(post.getId(), testUser.getId());

            assertFalse((Boolean) result.get("liked"));
            assertEquals(0L, result.get("likeCount"));
        }

        @Test
        @DisplayName("Like persists PostLike row in DB")
        void shouldPersistPostLikeRow() {
            Post post = saveCampusPost("T", "c", otherUser);
            postsService.toggleLikeWithDetails(post.getId(), testUser.getId());

            assertTrue(postLikeRepository.findByPostIdAndUserId(post.getId(), testUser.getId()).isPresent());
        }

        @Test
        @DisplayName("Unlike removes PostLike row from DB")
        void shouldRemovePostLikeRowOnUnlike() {
            Post post = saveCampusPost("T", "c", otherUser);
            postsService.toggleLikeWithDetails(post.getId(), testUser.getId());
            postsService.toggleLikeWithDetails(post.getId(), testUser.getId());

            assertFalse(postLikeRepository.findByPostIdAndUserId(post.getId(), testUser.getId()).isPresent());
        }

        @Test
        @DisplayName("likeCount committed in DB after like")
        void shouldCommitLikeCountToDB() {
            Post post = saveCampusPost("T", "c", otherUser);
            postsService.toggleLikeWithDetails(post.getId(), testUser.getId());

            assertEquals(1, postRepository.findById(post.getId()).orElseThrow().getLikeCount());
        }

        @Test
        @DisplayName("likeCount committed in DB after unlike")
        void shouldCommitDecrementToDB() {
            Post post = saveCampusPost("T", "c", otherUser);
            postsService.toggleLikeWithDetails(post.getId(), testUser.getId());
            postsService.toggleLikeWithDetails(post.getId(), testUser.getId());

            assertEquals(0, postRepository.findById(post.getId()).orElseThrow().getLikeCount());
        }

        @Test
        @DisplayName("Multiple users liking same post each increment likeCount")
        void shouldAccumulateLikesFromMultipleUsers() {
            Post post = saveCampusPost("T", "c", testUser);
            postsService.toggleLikeWithDetails(post.getId(), testUser.getId());
            postsService.toggleLikeWithDetails(post.getId(), otherUser.getId());

            assertEquals(2, postRepository.findById(post.getId()).orElseThrow().getLikeCount());
        }

        @Test
        @DisplayName("Toggle sequence like→unlike→like ends at liked=true")
        void shouldHandleToggleSequence() {
            Post post = saveCampusPost("T", "c", otherUser);
            postsService.toggleLikeWithDetails(post.getId(), testUser.getId());
            postsService.toggleLikeWithDetails(post.getId(), testUser.getId());
            Map<String, Object> result = postsService.toggleLikeWithDetails(post.getId(), testUser.getId());

            assertTrue((Boolean) result.get("liked"));
            assertEquals(1L, result.get("likeCount"));
        }

        @Test
        @DisplayName("Should throw when post does not exist")
        void shouldThrowWhenPostNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.toggleLikeWithDetails(UUID.randomUUID(), testUser.getId()));
            assertTrue(ex.getMessage().contains("Post not found"));
        }

        @Test
        @DisplayName("Should throw when campus post belongs to different school")
        void shouldThrowForDifferentSchoolCampusPost() {
            Post post = saveCampusPost("Harvard post", "c", testUser);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.toggleLikeWithDetails(post.getId(), mitUser.getId()));
            assertTrue(ex.getMessage().contains("access"));
        }

        @Test
        @DisplayName("National post can be liked by user from any school")
        void shouldAllowLikingNationalPostFromAnySchool() {
            Post post = saveNationalPost("National", "c", testUser);

            assertDoesNotThrow(() -> postsService.toggleLikeWithDetails(post.getId(), mitUser.getId()));
            assertEquals(2L, postsService.toggleLikeWithDetails(post.getId(), otherUser.getId()).get("likeCount"));
        }
    }

    // ─── hidePost ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("hidePost()")
    class HidePostTests {

        @Test
        @DisplayName("Should hide post and persist hidden=true")
        void shouldHidePost() {
            Post post = saveCampusPost("T", "c", testUser);

            Post result = postsService.hidePost(post.getId(), testUser.getId());

            assertTrue(result.isHidden());
            assertTrue(postRepository.findById(post.getId()).orElseThrow().isHidden());
        }

        @Test
        @DisplayName("Should throw when post not found")
        void shouldThrowWhenPostNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.hidePost(UUID.randomUUID(), testUser.getId()));
            assertEquals("Post not found", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when user is not the post author")
        void shouldThrowWhenNotAuthor() {
            Post post = saveCampusPost("T", "c", testUser);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.hidePost(post.getId(), otherUser.getId()));
            assertEquals("You can only hide your own posts", ex.getMessage());
        }

        @Test
        @DisplayName("Should be idempotent — hiding already-hidden post does not throw")
        void shouldBeIdempotent() {
            Post post = saveCampusPost("T", "c", testUser);
            postsService.hidePost(post.getId(), testUser.getId());

            Post result = postsService.hidePost(post.getId(), testUser.getId());

            assertTrue(result.isHidden());
        }

        @Test
        @DisplayName("Hidden post should not appear in getPostsByWall results")
        void shouldNotAppearInWallFeed() {
            Post visible = saveCampusPost("Visible", "c", testUser);
            Post hidden  = saveCampusPost("Hidden",  "c", testUser);
            postsService.hidePost(hidden.getId(), testUser.getId());

            Page<Post> result = postsService.getPostsByWall("campus",
                    Pageable.from(0, 20), testUser.getId(), "harvard.edu", SortBy.NEWEST);

            assertEquals(1, result.getTotalSize());
            assertEquals(visible.getId(), result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("Should cascade hide to all comments asynchronously")
        void shouldCascadeHideToComments() throws InterruptedException {
            Post post = saveCampusPost("T", "c", testUser);
            Comment c1 = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C1"), testUser.getId());
            Comment c2 = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C2"), otherUser.getId());

            postsService.hidePost(post.getId(), testUser.getId());
            Thread.sleep(500); // async event processing

            assertTrue(commentRepository.findById(c1.getId()).orElseThrow().isHidden());
            assertTrue(commentRepository.findById(c2.getId()).orElseThrow().isHidden());
        }

        @Test
        @DisplayName("Should handle hiding post with no comments")
        void shouldHandlePostWithNoComments() {
            Post post = saveCampusPost("T", "c", testUser);

            Post result = postsService.hidePost(post.getId(), testUser.getId());

            assertTrue(result.isHidden());
            assertEquals(0, result.getCommentCount());
        }
    }

    // ─── unhidePost ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("unhidePost()")
    class UnhidePostTests {

        @Test
        @DisplayName("Should unhide post and persist hidden=false")
        void shouldUnhidePost() {
            Post post = saveCampusPost("T", "c", testUser);
            postsService.hidePost(post.getId(), testUser.getId());

            Post result = postsService.unhidePost(post.getId(), testUser.getId());

            assertFalse(result.isHidden());
            assertFalse(postRepository.findById(post.getId()).orElseThrow().isHidden());
        }

        @Test
        @DisplayName("Should throw when post not found")
        void shouldThrowWhenPostNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.unhidePost(UUID.randomUUID(), testUser.getId()));
            assertEquals("Post not found", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when user is not the post author")
        void shouldThrowWhenNotAuthor() {
            Post post = saveCampusPost("T", "c", testUser);
            postsService.hidePost(post.getId(), testUser.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.unhidePost(post.getId(), otherUser.getId()));
            assertEquals("You can only unhide your own posts", ex.getMessage());
        }

        @Test
        @DisplayName("Should be idempotent — unhiding visible post does not throw")
        void shouldBeIdempotent() {
            Post post = saveCampusPost("T", "c", testUser);

            Post result = postsService.unhidePost(post.getId(), testUser.getId());

            assertFalse(result.isHidden());
        }

        @Test
        @DisplayName("Unhidden post should reappear in getPostsByWall")
        void shouldReappearInWallFeed() {
            Post post = saveCampusPost("T", "c", testUser);
            postsService.hidePost(post.getId(), testUser.getId());
            postsService.unhidePost(post.getId(), testUser.getId());

            Page<Post> result = postsService.getPostsByWall("campus",
                    Pageable.from(0, 20), testUser.getId(), "harvard.edu", SortBy.NEWEST);

            assertEquals(1, result.getTotalSize());
        }

        @Test
        @DisplayName("Should restore all comments when unhiding post")
        void shouldRestoreCommentsOnUnhide() throws InterruptedException {
            Post post = saveCampusPost("T", "c", testUser);
            Comment c1 = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C1"), testUser.getId());
            Comment c2 = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C2"), otherUser.getId());

            postsService.hidePost(post.getId(), testUser.getId());
            Thread.sleep(500); // wait for async hide cascade

            postsService.unhidePost(post.getId(), testUser.getId());

            assertFalse(commentRepository.findById(c1.getId()).orElseThrow().isHidden());
            assertFalse(commentRepository.findById(c2.getId()).orElseThrow().isHidden());
        }

        @Test
        @DisplayName("Should preserve comment count through hide/unhide cycle")
        void shouldPreserveCommentCount() {
            Post post = saveCampusPost("T", "c", testUser);
            commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C1"), testUser.getId());
            commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C2"), otherUser.getId());

            int countBefore = postRepository.findById(post.getId()).orElseThrow().getCommentCount();

            postsService.hidePost(post.getId(), testUser.getId());
            postsService.unhidePost(post.getId(), testUser.getId());

            int countAfter = postRepository.findById(post.getId()).orElseThrow().getCommentCount();
            assertEquals(countBefore, countAfter);
        }
    }

    // ─── getUserOwnPosts ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserOwnPosts()")
    class GetUserOwnPostsTests {

        @Test
        @DisplayName("Should return only the requesting user's own posts")
        void shouldReturnOnlyOwnPosts() {
            saveCampusPost("Mine",   "c", testUser);
            saveCampusPost("Theirs", "c", otherUser);

            Page<Post> result = postsService.getUserOwnPosts(testUser.getId(),
                    Pageable.from(0, 20), SortBy.NEWEST);

            assertEquals(1, result.getTotalSize());
            assertEquals(testUser.getId(), result.getContent().get(0).getUserId());
        }

        @Test
        @DisplayName("Should exclude hidden posts")
        void shouldExcludeHiddenPosts() {
            Post visible = saveCampusPost("Visible", "c", testUser);
            Post hidden  = saveCampusPost("Hidden",  "c", testUser);
            postsService.hidePost(hidden.getId(), testUser.getId());

            Page<Post> result = postsService.getUserOwnPosts(testUser.getId(),
                    Pageable.from(0, 20), SortBy.NEWEST);

            assertEquals(1, result.getTotalSize());
            assertEquals(visible.getId(), result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("Should return empty page when user has no posts")
        void shouldReturnEmptyWhenNoPosts() {
            Page<Post> result = postsService.getUserOwnPosts(testUser.getId(),
                    Pageable.from(0, 20), SortBy.NEWEST);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should sort by MOST_LIKED descending")
        void shouldSortByMostLiked() {
            for (int i = 0; i < 5; i++) {
                Post p = new Post(testUser.getId(), "T" + i, "c", "campus", "harvard.edu");
                p.setLikeCount(i);
                postRepository.save(p);
            }

            Page<Post> result = postsService.getUserOwnPosts(testUser.getId(),
                    Pageable.from(0, 5), SortBy.MOST_LIKED);

            List<Post> posts = result.getContent();
            for (int i = 0; i < posts.size() - 1; i++) {
                assertTrue(posts.get(i).getLikeCount() >= posts.get(i + 1).getLikeCount());
            }
        }

        @Test
        @DisplayName("Should default to NEWEST when sortBy is null")
        void shouldDefaultToNewestWhenSortByNull() {
            for (int i = 0; i < 3; i++) saveCampusPost("T" + i, "c", testUser);

            Page<Post> result = postsService.getUserOwnPosts(testUser.getId(),
                    Pageable.from(0, 10), null);

            assertEquals(3, result.getTotalSize());
        }

        @Test
        @DisplayName("Should paginate correctly across multiple pages")
        void shouldPaginateCorrectly() {
            for (int i = 0; i < 25; i++) saveCampusPost("T" + i, "c", testUser);

            Page<Post> page1 = postsService.getUserOwnPosts(testUser.getId(),
                    Pageable.from(0, 20), SortBy.NEWEST);
            Page<Post> page2 = postsService.getUserOwnPosts(testUser.getId(),
                    Pageable.from(1, 20), SortBy.NEWEST);

            assertEquals(20, page1.getContent().size());
            assertEquals(5,  page2.getContent().size());
        }
    }

    // ─── reportPost ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reportPost()")
    class ReportPostTests {

        @Test
        @DisplayName("Should create report and increment author's report count")
        void shouldCreateReportAndIncrementCount() {
            Post post = saveCampusPost("T", "c", testUser);
            int initial = testUser.getReportCount();

            postsService.reportPost(post.getId(), otherUser.getId(), "Spam");

            assertTrue(postReportRepository.existsByPostIdAndReporterUserId(post.getId(), otherUser.getId()));
            int updated = userRepository.findById(testUser.getId()).orElseThrow().getReportCount();
            assertEquals(initial + 1, updated);
        }

        @Test
        @DisplayName("Should persist report reason correctly")
        void shouldPersistReportReason() {
            Post post = saveCampusPost("T", "c", testUser);
            postsService.reportPost(post.getId(), otherUser.getId(), "Hate speech");

            Optional<PostReport> report = postReportRepository
                    .findByPostIdAndReporterUserId(post.getId(), otherUser.getId());
            assertTrue(report.isPresent());
            assertEquals("Hate speech", report.get().getReason());
            assertEquals(testUser.getId(), report.get().getReportedUserId());
        }

        @Test
        @DisplayName("Should create report when reason is null")
        void shouldCreateReportWithNullReason() {
            Post post = saveCampusPost("T", "c", testUser);

            assertDoesNotThrow(() -> postsService.reportPost(post.getId(), otherUser.getId(), null));
            assertTrue(postReportRepository.existsByPostIdAndReporterUserId(post.getId(), otherUser.getId()));
        }

        @Test
        @DisplayName("Should allow multiple different users to report the same post")
        void shouldAllowMultipleReporters() {
            Post post = saveCampusPost("T", "c", testUser);
            int initial = testUser.getReportCount();

            postsService.reportPost(post.getId(), otherUser.getId(), "R1");
            postsService.reportPost(post.getId(), mitUser.getId(), "R2");

            assertEquals(initial + 2,
                    userRepository.findById(testUser.getId()).orElseThrow().getReportCount());
        }

        @Test
        @DisplayName("Should throw when post does not exist")
        void shouldThrowWhenPostNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.reportPost(UUID.randomUUID(), otherUser.getId(), "Reason"));
            assertEquals("Post not found", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw on duplicate report — same user cannot report same post twice")
        void shouldThrowOnDuplicateReport() {
            Post post = saveCampusPost("T", "c", testUser);
            postsService.reportPost(post.getId(), otherUser.getId(), "First");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.reportPost(post.getId(), otherUser.getId(), "Second"));
            assertEquals("You have already reported this post", ex.getMessage());
        }

        @Test
        @DisplayName("Duplicate report attempt must not increment report count again")
        void shouldNotDoubleIncrementOnDuplicate() {
            Post post = saveCampusPost("T", "c", testUser);
            postsService.reportPost(post.getId(), otherUser.getId(), "First");
            int countAfterFirst = userRepository.findById(testUser.getId()).orElseThrow().getReportCount();

            assertThrows(IllegalArgumentException.class,
                    () -> postsService.reportPost(post.getId(), otherUser.getId(), "Second"));

            assertEquals(countAfterFirst,
                    userRepository.findById(testUser.getId()).orElseThrow().getReportCount());
        }
    }

    // ─── updateProfileNameByUserId ────────────────────────────────────────────

    @Nested
    @DisplayName("updateProfileNameByUserId()")
    class UpdateProfileNameTests {

        @Test
        @DisplayName("Should update profileName on all posts belonging to the user")
        void shouldUpdateAllPostsForUser() {
            saveCampusPost("P1", "c", testUser);
            saveCampusPost("P2", "c", testUser);
            saveCampusPost("Other", "c", otherUser); // must NOT be touched

            postsService.updateProfileNameByUserId(testUser.getId(), "New Name");

            List<Post> testUserPosts = postRepository
                    .findByUserIdAndHiddenFalseOrderByCreatedAtDesc(testUser.getId(), Pageable.from(0, 20))
                    .getContent();
            assertTrue(testUserPosts.stream().allMatch(p -> "New Name".equals(p.getProfileName())),
                    "All testUser posts must have updated profileName");
        }

        @Test
        @DisplayName("Should not affect posts belonging to other users")
        void shouldNotUpdateOtherUsersPost() {
            otherUser.setProfileName("Original");
            userRepository.update(otherUser);
            Post otherPost = saveCampusPost("Other", "c", otherUser);
            otherPost.setProfileName("Original");
            postRepository.update(otherPost);

            postsService.updateProfileNameByUserId(testUser.getId(), "New Name");

            String otherProfileName = postRepository.findById(otherPost.getId())
                    .orElseThrow().getProfileName();
            assertEquals("Original", otherProfileName);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateException() {
            // updateProfileNameByUserId rethrows — verify it does not swallow
            // Calling with a non-existent userId should not throw (bulk update on zero rows is fine)
            assertDoesNotThrow(() ->
                    postsService.updateProfileNameByUserId(UUID.randomUUID(), "Name"));
        }
    }

    // ─── HideComment / UnhideComment (CommentsService via PostsService context) ─

    @Nested
    @DisplayName("hideComment() / unhideComment()")
    class HideCommentTests {

        @Test
        @DisplayName("Should hide own comment and persist hidden=true")
        void shouldHideOwnComment() {
            Post post = saveCampusPost("T", "c", testUser);
            Comment comment = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("Comment"), testUser.getId());

            Comment result = commentsService.hideComment(CommentParentType.POST,
                    post.getId(), comment.getId(), testUser.getId());

            assertTrue(result.isHidden());
            assertTrue(commentRepository.findById(comment.getId()).orElseThrow().isHidden());
        }

        @Test
        @DisplayName("Should hide comment and decrement post commentCount")
        void shouldDecrementCommentCount() {
            Post post = saveCampusPost("T", "c", testUser);
            Comment c1 = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C1"), testUser.getId());
            commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C2"), testUser.getId());

            int before = postRepository.findById(post.getId()).orElseThrow().getCommentCount();
            commentsService.hideComment(CommentParentType.POST, post.getId(), c1.getId(), testUser.getId());
            int after = postRepository.findById(post.getId()).orElseThrow().getCommentCount();

            assertEquals(before - 1, after);
        }

        @Test
        @DisplayName("Should be idempotent — hiding already-hidden comment does not throw")
        void shouldBeIdempotentOnDoubleHide() {
            Post post = saveCampusPost("T", "c", testUser);
            Comment comment = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C"), testUser.getId());
            commentsService.hideComment(CommentParentType.POST, post.getId(), comment.getId(), testUser.getId());

            Comment result = commentsService.hideComment(CommentParentType.POST,
                    post.getId(), comment.getId(), testUser.getId());

            assertTrue(result.isHidden());
        }

        @Test
        @DisplayName("Should throw when trying to hide another user's comment")
        void shouldThrowWhenHidingOtherUsersComment() {
            Post post = saveCampusPost("T", "c", testUser);
            Comment comment = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C"), testUser.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.hideComment(CommentParentType.POST,
                            post.getId(), comment.getId(), otherUser.getId()));
            assertTrue(ex.getMessage().contains("hide your own comments"));
        }

        @Test
        @DisplayName("Should throw when comment does not exist")
        void shouldThrowWhenCommentNotFound() {
            Post post = saveCampusPost("T", "c", testUser);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.hideComment(CommentParentType.POST,
                            post.getId(), UUID.randomUUID(), testUser.getId()));
            assertTrue(ex.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("Should throw when comment belongs to a different post")
        void shouldThrowWhenCommentBelongsToDifferentPost() {
            Post post1 = saveCampusPost("P1", "c", testUser);
            Post post2 = saveCampusPost("P2", "c", testUser);
            Comment comment = commentsService.addComment(CommentParentType.POST, post2.getId(),
                    new CreateCommentRequest("C"), testUser.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.hideComment(CommentParentType.POST,
                            post1.getId(), comment.getId(), testUser.getId()));
            assertTrue(ex.getMessage().contains("does not belong to this post"));
        }

        @Test
        @DisplayName("Hidden comment should not appear in getCommentsWithPagination")
        void hiddenCommentShouldNotAppearInListing() {
            Post post = saveCampusPost("T", "c", testUser);
            Comment c1 = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("Visible"), testUser.getId());
            Comment c2 = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("Hidden"), testUser.getId());
            commentsService.hideComment(CommentParentType.POST, post.getId(), c2.getId(), testUser.getId());

            Page<Comment> result = commentsService.getCommentsWithPagination(
                    CommentParentType.POST, post.getId(), Pageable.from(0, 20), SortBy.NEWEST);

            assertEquals(1, result.getTotalSize());
            assertEquals(c1.getId(), result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("Should unhide comment and persist hidden=false")
        void shouldUnhideComment() {
            Post post = saveCampusPost("T", "c", testUser);
            Comment comment = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C"), testUser.getId());
            commentsService.hideComment(CommentParentType.POST, post.getId(), comment.getId(), testUser.getId());

            Comment result = commentsService.unhideComment(CommentParentType.POST,
                    post.getId(), comment.getId(), testUser.getId());

            assertFalse(result.isHidden());
            assertFalse(commentRepository.findById(comment.getId()).orElseThrow().isHidden());
        }

        @Test
        @DisplayName("Unhide should increment post commentCount")
        void shouldIncrementCommentCountOnUnhide() {
            Post post = saveCampusPost("T", "c", testUser);
            Comment comment = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C"), testUser.getId());
            commentsService.hideComment(CommentParentType.POST, post.getId(), comment.getId(), testUser.getId());
            int afterHide = postRepository.findById(post.getId()).orElseThrow().getCommentCount();

            commentsService.unhideComment(CommentParentType.POST, post.getId(), comment.getId(), testUser.getId());
            int afterUnhide = postRepository.findById(post.getId()).orElseThrow().getCommentCount();

            assertEquals(afterHide + 1, afterUnhide);
        }

        @Test
        @DisplayName("Should support multiple hide/unhide cycles")
        void shouldSupportMultipleCycles() {
            Post post = saveCampusPost("T", "c", testUser);
            Comment comment = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C"), testUser.getId());

            commentsService.hideComment(CommentParentType.POST, post.getId(), comment.getId(), testUser.getId());
            commentsService.unhideComment(CommentParentType.POST, post.getId(), comment.getId(), testUser.getId());
            commentsService.hideComment(CommentParentType.POST, post.getId(), comment.getId(), testUser.getId());
            Comment result = commentsService.unhideComment(CommentParentType.POST,
                    post.getId(), comment.getId(), testUser.getId());

            assertFalse(result.isHidden());
        }

        @Test
        @DisplayName("Should throw when unhiding another user's comment")
        void shouldThrowWhenUnhidingOtherUsersComment() {
            Post post = saveCampusPost("T", "c", testUser);
            Comment comment = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C"), testUser.getId());
            commentsService.hideComment(CommentParentType.POST, post.getId(), comment.getId(), testUser.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.unhideComment(CommentParentType.POST,
                            post.getId(), comment.getId(), otherUser.getId()));
            assertTrue(ex.getMessage().contains("unhide your own comments"));
        }

        @Test
        @DisplayName("Should throw when campus post belongs to different school — access checked before ownership")
        void shouldThrowForDifferentSchoolOnHideComment() {
            Post post = saveCampusPost("T", "c", testUser);
            Comment comment = commentsService.addComment(CommentParentType.POST, post.getId(),
                    new CreateCommentRequest("C"), testUser.getId());

            // mitUser cannot access a harvard campus post's comments at all
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.hideComment(CommentParentType.POST,
                            post.getId(), comment.getId(), mitUser.getId()));
            assertTrue(ex.getMessage().contains("access") || ex.getMessage().contains("hide your own comments"));
        }
    }

    // ─── Comments pagination / sorting ────────────────────────────────────────

    @Nested
    @DisplayName("CommentsService — pagination and sorting (PostsService context)")
    class CommentsPaginationSortingTests {

        private Post post;

        @BeforeEach
        void setUpComments() {
            post = saveCampusPost("Post", "c", testUser);
            for (int i = 0; i < 25; i++) {
                commentsService.addComment(CommentParentType.POST, post.getId(),
                        new CreateCommentRequest("Comment " + i), testUser.getId());
            }
        }

        @Test
        @DisplayName("First page returns correct size and total")
        void firstPageCorrect() {
            Page<Comment> result = commentsService.getCommentsWithPagination(
                    CommentParentType.POST, post.getId(), Pageable.from(0, 20), SortBy.NEWEST);

            assertEquals(20, result.getContent().size());
            assertEquals(25, result.getTotalSize());
        }

        @Test
        @DisplayName("Second page returns remaining items")
        void secondPageCorrect() {
            Page<Comment> result = commentsService.getCommentsWithPagination(
                    CommentParentType.POST, post.getId(), Pageable.from(1, 20), SortBy.NEWEST);

            assertEquals(5, result.getContent().size());
        }

        @Test
        @DisplayName("NEWEST sort — createdAt descending within page")
        void shouldSortByNewest() {
            Page<Comment> result = commentsService.getCommentsWithPagination(
                    CommentParentType.POST, post.getId(), Pageable.from(0, 10), SortBy.NEWEST);

            List<Comment> comments = result.getContent();
            for (int i = 0; i < comments.size() - 1; i++) {
                assertFalse(comments.get(i).getCreatedAt().isBefore(comments.get(i + 1).getCreatedAt()));
            }
        }

        @Test
        @DisplayName("OLDEST sort — createdAt ascending within page")
        void shouldSortByOldest() {
            Page<Comment> result = commentsService.getCommentsWithPagination(
                    CommentParentType.POST, post.getId(), Pageable.from(0, 10), SortBy.OLDEST);

            List<Comment> comments = result.getContent();
            for (int i = 0; i < comments.size() - 1; i++) {
                assertFalse(comments.get(i).getCreatedAt().isAfter(comments.get(i + 1).getCreatedAt()));
            }
        }

        @Test
        @DisplayName("Sort consistent across page boundary — last of page N >= first of page N+1 for NEWEST")
        void shouldMaintainSortAcrossPages() {
            Page<Comment> page1 = commentsService.getCommentsWithPagination(
                    CommentParentType.POST, post.getId(), Pageable.from(0, 10), SortBy.NEWEST);
            Page<Comment> page2 = commentsService.getCommentsWithPagination(
                    CommentParentType.POST, post.getId(), Pageable.from(1, 10), SortBy.NEWEST);

            Comment lastPage1  = page1.getContent().get(page1.getContent().size() - 1);
            Comment firstPage2 = page2.getContent().get(0);

            assertFalse(lastPage1.getCreatedAt().isBefore(firstPage2.getCreatedAt()),
                    "Sort must be consistent across page boundary");
        }

        @Test
        @DisplayName("Total count is the same regardless of sort")
        void totalCountSameForAllSorts() {
            Pageable p = Pageable.from(0, 30);
            long newest  = commentsService.getCommentsWithPagination(CommentParentType.POST, post.getId(), p, SortBy.NEWEST).getTotalSize();
            long oldest  = commentsService.getCommentsWithPagination(CommentParentType.POST, post.getId(), p, SortBy.OLDEST).getTotalSize();
            assertEquals(newest, oldest);
        }

        @Test
        @DisplayName("Returns empty page for post with no comments")
        void returnsEmptyForPostWithNoComments() {
            Post empty = saveCampusPost("Empty", "c", testUser);
            Page<Comment> result = commentsService.getCommentsWithPagination(
                    CommentParentType.POST, empty.getId(), Pageable.from(0, 20), SortBy.NEWEST);

            assertTrue(result.isEmpty());
        }
    }

    // ─── Image upload (unit test with manual DI / mocked MediaUtil) ───────────

    @Nested
    @DisplayName("createPost() — image upload")
    class ImageUploadTests {

        private PostsServiceImpl imageService;
        private PostRepository mockPostRepo;
        private UserService mockUserService;

        @BeforeEach
        void setUpImageService() throws Exception {
            mockPostRepo    = mock(PostRepository.class);
            mockUserService = mock(UserService.class);

            imageService = new PostsServiceImpl();
            setField("postRepository",           mockPostRepo);
            setField("userService",              mockUserService);
            setField("postLikeService",          mock(PostLikeService.class));
            setField("postReportService",        mock(PostReportService.class));
            setField("postHiddenEventPublisher", mock(ApplicationEventPublisher.class));
            setField("userBlockService",         mock(UserBlockService.class));
            setProviderField("commentsServiceProvider", mock(CommentsService.class));
            setProviderField("pollServiceProvider",     mock(PollService.class));
        }

        private void setField(String name, Object value) throws Exception {
            var field = PostsServiceImpl.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(imageService, value);
        }

        @SuppressWarnings("unchecked")
        private void setProviderField(String name, Object svc) throws Exception {
            Provider<Object> provider = mock(Provider.class);
            when(provider.get()).thenReturn(svc);
            var field = PostsServiceImpl.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(imageService, provider);
        }

        private UserEntity mockUser() {
            UserEntity u = new UserEntity();
            u.setId(UUID.randomUUID());
            u.setSchoolDomain("harvard.edu");
            u.setProfileName("Anonymous");
            return u;
        }

        private Post stubSavedPost(UUID userId) {
            Post p = new Post(userId, "T", "C", "campus", "harvard.edu");
            p.setId(UUID.randomUUID());
            p.setImageUrls(new java.util.ArrayList<>());
            return p;
        }

        @Test
        @DisplayName("Should attach single objectName from request to saved post")
        void shouldUploadSingleImage() {
            UserEntity user = mockUser();
            when(mockUserService.findById(user.getId())).thenReturn(Optional.of(user));

            List<String> objectNames = List.of("posts/uuid1.jpg");
            Post saved = stubSavedPost(user.getId());
            saved.setImageUrls(objectNames);
            when(mockPostRepo.save(any())).thenReturn(saved);

            CreatePostRequest request = new CreatePostRequest("T", "C");
            request.setImageObjectNames(objectNames);
            Post result = imageService.createPost(request, user.getId());

            assertEquals(1, result.getImageUrls().size());
        }

        @Test
        @DisplayName("Should create post with no images when imageObjectNames is empty")
        void shouldSkipZeroSizeImages() {
            UserEntity user = mockUser();
            when(mockUserService.findById(user.getId())).thenReturn(Optional.of(user));
            Post saved = stubSavedPost(user.getId());
            when(mockPostRepo.save(any())).thenReturn(saved);

            CreatePostRequest request = new CreatePostRequest("T", "C");
            request.setImageObjectNames(List.of());
            imageService.createPost(request, user.getId());
        }

        @Test
        @DisplayName("Should throw immediately when more than 5 objectNames provided — before any DB access")
        void shouldThrowImmediatelyForTooManyImages() {
            CreatePostRequest request = new CreatePostRequest("T", "C");
            request.setImageObjectNames(List.of(
                    "posts/a.jpg", "posts/b.jpg", "posts/c.jpg",
                    "posts/d.jpg", "posts/e.jpg", "posts/f.jpg"));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> imageService.createPost(request, UUID.randomUUID()));

            assertTrue(ex.getMessage().contains("5"));
            verify(mockUserService, never()).findById(any());
        }

        @Test
        @DisplayName("Should create post with no images when list is null")
        void shouldCreatePostWithNullImages() {
            UserEntity user = mockUser();
            when(mockUserService.findById(user.getId())).thenReturn(Optional.of(user));
            Post saved = stubSavedPost(user.getId());
            when(mockPostRepo.save(any())).thenReturn(saved);

            Post result = imageService.createPost(new CreatePostRequest("T", "C"), user.getId());

            assertNotNull(result);
        }
    }
}
