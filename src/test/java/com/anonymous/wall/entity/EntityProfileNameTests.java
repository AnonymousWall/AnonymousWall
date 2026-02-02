package com.anonymous.wall.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Entity Profile Name Tests")
class EntityProfileNameTests {

    @Nested
    @DisplayName("UserEntity Profile Name Tests")
    class UserEntityProfileNameTests {

        private UserEntity user;

        @BeforeEach
        void setUp() {
            user = new UserEntity();
            user.setEmail("test@harvard.edu");
        }

        @Test
        @DisplayName("User should default to 'Anonymous' profile name")
        void userDefaultsToAnonymous() {
            assertEquals("Anonymous", user.getProfileName());
        }

        @Test
        @DisplayName("User profile name can be set to custom value")
        void userProfileNameCanBeSet() {
            user.setProfileName("John Doe");
            assertEquals("John Doe", user.getProfileName());
        }

        @Test
        @DisplayName("Null profile name should be converted to 'Anonymous'")
        void nullProfileNameConvertedToAnonymous() {
            user.setProfileName(null);
            assertEquals("Anonymous", user.getProfileName());
        }

        @Test
        @DisplayName("Empty string profile name should be set as is")
        void emptyStringProfileNameIsSet() {
            user.setProfileName("");
            assertEquals("", user.getProfileName());
        }

        @Test
        @DisplayName("Profile name supports Unicode characters")
        void profileNameSupportsUnicode() {
            user.setProfileName("张三");
            assertEquals("张三", user.getProfileName());

            user.setProfileName("Müller");
            assertEquals("Müller", user.getProfileName());

            user.setProfileName("Åke");
            assertEquals("Åke", user.getProfileName());
        }

        @Test
        @DisplayName("Profile name can be up to 255 characters")
        void profileNameSupportsMaxLength() {
            String maxLengthName = "A".repeat(255);
            user.setProfileName(maxLengthName);
            assertEquals(maxLengthName, user.getProfileName());
        }
    }

    @Nested
    @DisplayName("Post Entity Profile Name Tests")
    class PostEntityProfileNameTests {

        private Post post;
        private UUID userId;

        @BeforeEach
        void setUp() {
            userId = UUID.randomUUID();
            post = new Post(userId, "Test content", "campus", "harvard.edu");
        }

        @Test
        @DisplayName("Post should default to 'Anonymous' profile name")
        void postDefaultsToAnonymous() {
            assertEquals("Anonymous", post.getProfileName());
        }

        @Test
        @DisplayName("Post profile name can be set to custom value")
        void postProfileNameCanBeSet() {
            post.setProfileName("Alice Wonder");
            assertEquals("Alice Wonder", post.getProfileName());
        }

        @Test
        @DisplayName("Null post profile name should be converted to 'Anonymous'")
        void nullPostProfileNameConvertedToAnonymous() {
            post.setProfileName(null);
            assertEquals("Anonymous", post.getProfileName());
        }

        @Test
        @DisplayName("Post constructor should initialize profile name to Anonymous")
        void postConstructorInitializesProfileName() {
            Post newPost = new Post(userId, "Content", "national", null);
            assertEquals("Anonymous", newPost.getProfileName());
        }

        @Test
        @DisplayName("Post profile name should be different from user ID")
        void postProfileNameDifferentFromUserId() {
            String customName = "Test Name";
            post.setProfileName(customName);

            assertNotEquals(customName, post.getUserId().toString());
            assertEquals(customName, post.getProfileName());
        }

        @Test
        @DisplayName("Multiple posts can have different profile names")
        void multiplePostsWithDifferentNames() {
            Post post1 = new Post(userId, "Content 1", "campus", "harvard.edu");
            post1.setProfileName("Name 1");

            Post post2 = new Post(userId, "Content 2", "campus", "harvard.edu");
            post2.setProfileName("Name 2");

            assertEquals("Name 1", post1.getProfileName());
            assertEquals("Name 2", post2.getProfileName());
            assertNotEquals(post1.getProfileName(), post2.getProfileName());
        }
    }

    @Nested
    @DisplayName("Comment Entity Profile Name Tests")
    class CommentEntityProfileNameTests {

        private Comment comment;
        private UUID userId;

        @BeforeEach
        void setUp() {
            userId = UUID.randomUUID();
            comment = new Comment(1L, userId, "Test comment");
        }

        @Test
        @DisplayName("Comment should default to 'Anonymous' profile name")
        void commentDefaultsToAnonymous() {
            assertEquals("Anonymous", comment.getProfileName());
        }

        @Test
        @DisplayName("Comment profile name can be set to custom value")
        void commentProfileNameCanBeSet() {
            comment.setProfileName("Bob Builder");
            assertEquals("Bob Builder", comment.getProfileName());
        }

        @Test
        @DisplayName("Null comment profile name should be converted to 'Anonymous'")
        void nullCommentProfileNameConvertedToAnonymous() {
            comment.setProfileName(null);
            assertEquals("Anonymous", comment.getProfileName());
        }

        @Test
        @DisplayName("Comment constructor should initialize profile name to Anonymous")
        void commentConstructorInitializesProfileName() {
            Comment newComment = new Comment(1L, userId, "Content");
            assertEquals("Anonymous", newComment.getProfileName());
        }

        @Test
        @DisplayName("Comment profile name should be different from user ID")
        void commentProfileNameDifferentFromUserId() {
            String customName = "Test Name";
            comment.setProfileName(customName);

            assertNotEquals(customName, comment.getUserId().toString());
            assertEquals(customName, comment.getProfileName());
        }

        @Test
        @DisplayName("Multiple comments can have different profile names")
        void multipleCommentsWithDifferentNames() {
            Comment comment1 = new Comment(1L, userId, "Comment 1");
            comment1.setProfileName("Name 1");

            Comment comment2 = new Comment(1L, userId, "Comment 2");
            comment2.setProfileName("Name 2");

            assertEquals("Name 1", comment1.getProfileName());
            assertEquals("Name 2", comment2.getProfileName());
            assertNotEquals(comment1.getProfileName(), comment2.getProfileName());
        }

        @Test
        @DisplayName("Comment from different user should have different profile name")
        void commentsFromDifferentUsersCanHaveDifferentNames() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();

            Comment comment1 = new Comment(1L, userId1, "Comment 1");
            comment1.setProfileName("User 1 Name");

            Comment comment2 = new Comment(1L, userId2, "Comment 2");
            comment2.setProfileName("User 2 Name");

            assertEquals("User 1 Name", comment1.getProfileName());
            assertEquals("User 2 Name", comment2.getProfileName());
        }
    }

    @Nested
    @DisplayName("Profile Name Consistency Tests")
    class ProfileNameConsistencyTests {

        @Test
        @DisplayName("All entities should handle profile name consistently")
        void allEntitiesHandleProfileNameConsistently() {
            // All should default to Anonymous
            UserEntity user = new UserEntity();
            Post post = new Post(UUID.randomUUID(), "Content", "campus", "harvard.edu");
            Comment comment = new Comment(1L, UUID.randomUUID(), "Comment");

            assertEquals("Anonymous", user.getProfileName());
            assertEquals("Anonymous", post.getProfileName());
            assertEquals("Anonymous", comment.getProfileName());

            // All should accept custom values
            String customName = "Custom Name";
            user.setProfileName(customName);
            post.setProfileName(customName);
            comment.setProfileName(customName);

            assertEquals(customName, user.getProfileName());
            assertEquals(customName, post.getProfileName());
            assertEquals(customName, comment.getProfileName());

            // All should handle null conversion
            user.setProfileName(null);
            post.setProfileName(null);
            comment.setProfileName(null);

            assertEquals("Anonymous", user.getProfileName());
            assertEquals("Anonymous", post.getProfileName());
            assertEquals("Anonymous", comment.getProfileName());
        }

        @Test
        @DisplayName("Profile name should be independent from user ID")
        void profileNameIndependentFromUserId() {
            UUID userId = UUID.randomUUID();

            UserEntity user = new UserEntity();
            user.setId(userId);
            user.setProfileName("Custom Name");

            Post post = new Post(userId, "Content", "campus", "harvard.edu");
            post.setProfileName("Custom Name");

            Comment comment = new Comment(1L, userId, "Comment");
            comment.setProfileName("Custom Name");

            // All should have the same profile name
            assertEquals("Custom Name", user.getProfileName());
            assertEquals("Custom Name", post.getProfileName());
            assertEquals("Custom Name", comment.getProfileName());

            // But user IDs should be different
            assertEquals(userId, user.getId());
            assertEquals(userId, post.getUserId());
            assertEquals(userId, comment.getUserId());
        }
    }
}
