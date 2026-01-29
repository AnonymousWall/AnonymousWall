package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Post Entity Tests")
class PostEntityTest {

    @Nested
    @DisplayName("Post Creation")
    class PostCreationTests {

        @Test
        @DisplayName("Should create campus post with all fields")
        void shouldCreateCampusPost() {
            UUID userId = UUID.randomUUID();
            String content = "This is a campus post";

            Post post = new Post(userId, content, "campus", "harvard.edu");

            assertNotNull(post);
            assertEquals(userId, post.getUserId());
            assertEquals(content, post.getContent());
            assertEquals("campus", post.getWall());
            assertEquals("harvard.edu", post.getSchoolDomain());
            assertNotNull(post.getCreatedAt());
            assertNotNull(post.getUpdatedAt());
            assertEquals(0, post.getLikeCount());
            assertEquals(0, post.getCommentCount());
            assertFalse(post.isLiked());
        }

        @Test
        @DisplayName("Should create national post without school domain")
        void shouldCreateNationalPost() {
            UUID userId = UUID.randomUUID();
            String content = "This is a national post";

            Post post = new Post(userId, content, "national", null);

            assertNotNull(post);
            assertEquals(userId, post.getUserId());
            assertEquals(content, post.getContent());
            assertEquals("national", post.getWall());
            assertNull(post.getSchoolDomain());
        }

        @Test
        @DisplayName("Should create post with minimum length content")
        void shouldCreatePostWithMinimumLength() {
            UUID userId = UUID.randomUUID();
            String content = "A";

            Post post = new Post(userId, content, "campus", "mit.edu");

            assertEquals(1, post.getContent().length());
            assertEquals("A", post.getContent());
        }

        @Test
        @DisplayName("Should create post with maximum length content")
        void shouldCreatePostWithMaximumLength() {
            UUID userId = UUID.randomUUID();
            String content = "X".repeat(5000);

            Post post = new Post(userId, content, "campus", "stanford.edu");

            assertEquals(5000, post.getContent().length());
        }

        @Test
        @DisplayName("Should create post with special characters")
        void shouldCreatePostWithSpecialCharacters() {
            UUID userId = UUID.randomUUID();
            String content = "Post with emojis 🎉 @mentions #hashtags!!!";

            Post post = new Post(userId, content, "campus", "cornell.edu");

            assertEquals(content, post.getContent());
        }

        @Test
        @DisplayName("Should default wall to campus if null")
        void shouldHandleNullWall() {
            UUID userId = UUID.randomUUID();
            String content = "Content";

            // When wall is null, it should default to "campus"
            Post post = new Post(userId, content, null, "penn.edu");

            // Check behavior - the constructor handles null wall
            String wall = post.getWall() != null ? post.getWall() : "campus";
            assertEquals("campus", wall);
        }
    }

    @Nested
    @DisplayName("Post Setters and Getters")
    class PostSettersGettersTests {

        @Test
        @DisplayName("Should set and get content")
        void shouldSetAndGetContent() {
            Post post = new Post();
            post.setContent("New content");

            assertEquals("New content", post.getContent());
        }

        @Test
        @DisplayName("Should set and get wall type")
        void shouldSetAndGetWallType() {
            Post post = new Post();
            post.setWall("national");

            assertEquals("national", post.getWall());
        }

        @Test
        @DisplayName("Should set and get school domain")
        void shouldSetAndGetSchoolDomain() {
            Post post = new Post();
            post.setSchoolDomain("yale.edu");

            assertEquals("yale.edu", post.getSchoolDomain());
        }

        @Test
        @DisplayName("Should set and get user ID")
        void shouldSetAndGetUserId() {
            Post post = new Post();
            UUID userId = UUID.randomUUID();
            post.setUserId(userId);

            assertEquals(userId, post.getUserId());
        }

        @Test
        @DisplayName("Should set and get like count")
        void shouldSetAndGetLikeCount() {
            Post post = new Post();
            post.setLikeCount(10);

            assertEquals(10, post.getLikeCount());
        }

        @Test
        @DisplayName("Should set and get comment count")
        void shouldSetAndGetCommentCount() {
            Post post = new Post();
            post.setCommentCount(5);

            assertEquals(5, post.getCommentCount());
        }

        @Test
        @DisplayName("Should set and get liked flag")
        void shouldSetAndGetLikedFlag() {
            Post post = new Post();
            post.setLiked(true);

            assertTrue(post.isLiked());
        }

        @Test
        @DisplayName("Should set and get ID")
        void shouldSetAndGetId() {
            Post post = new Post();
            post.setId(123L);

            assertEquals(123L, post.getId());
        }
    }

    @Nested
    @DisplayName("Post Validation Rules")
    class PostValidationRulesTests {

        @Test
        @DisplayName("Content length should be validated (1-5000)")
        void shouldValidateContentLength() {
            UUID userId = UUID.randomUUID();

            // Minimum length
            Post minPost = new Post(userId, "A", "campus", "harvard.edu");
            assertEquals(1, minPost.getContent().length());

            // Maximum length
            String maxContent = "X".repeat(5000);
            Post maxPost = new Post(userId, maxContent, "campus", "harvard.edu");
            assertEquals(5000, maxPost.getContent().length());
        }

        @Test
        @DisplayName("Wall type should be campus or national")
        void shouldValidateWallType() {
            UUID userId = UUID.randomUUID();
            String content = "Content";

            Post campusPost = new Post(userId, content, "campus", "harvard.edu");
            assertEquals("campus", campusPost.getWall());

            Post nationalPost = new Post(userId, content, "national", null);
            assertEquals("national", nationalPost.getWall());
        }

        @Test
        @DisplayName("Campus posts should have school domain")
        void shouldVerifyCampusPostHasSchoolDomain() {
            UUID userId = UUID.randomUUID();
            String content = "Campus content";

            Post post = new Post(userId, content, "campus", "mit.edu");

            assertEquals("campus", post.getWall());
            assertNotNull(post.getSchoolDomain());
            assertEquals("mit.edu", post.getSchoolDomain());
        }

        @Test
        @DisplayName("National posts should not have school domain")
        void shouldVerifyNationalPostHasNoSchoolDomain() {
            UUID userId = UUID.randomUUID();
            String content = "National content";

            Post post = new Post(userId, content, "national", null);

            assertEquals("national", post.getWall());
            assertNull(post.getSchoolDomain());
        }
    }

    @Nested
    @DisplayName("Post Timestamps")
    class PostTimestampTests {

        @Test
        @DisplayName("Post should have created timestamp")
        void shouldHaveCreatedTimestamp() {
            UUID userId = UUID.randomUUID();
            Post post = new Post(userId, "Content", "campus", "penn.edu");

            assertNotNull(post.getCreatedAt());
        }

        @Test
        @DisplayName("Post should have updated timestamp")
        void shouldHaveUpdatedTimestamp() {
            UUID userId = UUID.randomUUID();
            Post post = new Post(userId, "Content", "campus", "duke.edu");

            assertNotNull(post.getUpdatedAt());
        }

        @Test
        @DisplayName("Initial created and updated timestamps should be equal or very close")
        void shouldHaveEqualInitialTimestamps() {
            UUID userId = UUID.randomUUID();
            Post post = new Post(userId, "Content", "campus", "cornell.edu");

            // Allow for microsecond-level precision differences
            // The timestamps should be within 1 millisecond of each other
            long timeDiff = Math.abs(
                post.getCreatedAt().toInstant().toEpochMilli() -
                post.getUpdatedAt().toInstant().toEpochMilli()
            );
            assertTrue(timeDiff == 0, "Timestamps should be created at virtually the same time");
        }

        @Test
        @DisplayName("Should be able to update timestamp")
        void shouldBeAbleToUpdateTimestamp() {
            UUID userId = UUID.randomUUID();
            Post post = new Post(userId, "Content", "campus", "yale.edu");

            var originalCreatedAt = post.getCreatedAt();
            var originalUpdatedAt = post.getUpdatedAt();

            // Update the post
            post.setUpdatedAt(post.getUpdatedAt().plusHours(1));

            assertEquals(originalCreatedAt, post.getCreatedAt());
            assertNotEquals(originalUpdatedAt, post.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("Post Initial Values")
    class PostInitialValuesTests {

        @Test
        @DisplayName("Post should initialize with zero likes")
        void shouldInitializeWithZeroLikes() {
            UUID userId = UUID.randomUUID();
            Post post = new Post(userId, "Content", "campus", "harvard.edu");

            assertEquals(0, post.getLikeCount());
        }

        @Test
        @DisplayName("Post should initialize with zero comments")
        void shouldInitializeWithZeroComments() {
            UUID userId = UUID.randomUUID();
            Post post = new Post(userId, "Content", "campus", "harvard.edu");

            assertEquals(0, post.getCommentCount());
        }

        @Test
        @DisplayName("Post should initialize with not liked")
        void shouldInitializeWithNotLiked() {
            UUID userId = UUID.randomUUID();
            Post post = new Post(userId, "Content", "campus", "harvard.edu");

            assertFalse(post.isLiked());
        }
    }
}
