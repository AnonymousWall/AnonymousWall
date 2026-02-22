package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.util.MediaUtilInterface;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PostsServiceImpl - Image Upload Tests")
class PostsServiceImplImageUploadTest {

    private PostsServiceImpl postsService;
    private PostRepository postRepository;
    private UserRepository userRepository;
    private MediaUtilInterface mediaUtil;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        userRepository = mock(UserRepository.class);
        mediaUtil = mock(MediaUtilInterface.class);

        postsService = new PostsServiceImpl();

        try {
            setField("postRepository", postRepository);
            setField("userRepository", userRepository);
            setField("mediaUtil", mediaUtil);
            setField("commentsService", mock(CommentsService.class));
            setField("postLikeRepository", mock(com.anonymous.wall.repository.PostLikeRepository.class));
            setField("commentRepository", mock(com.anonymous.wall.repository.CommentRepository.class));
            setField("postReportRepository", mock(com.anonymous.wall.repository.PostReportRepository.class));
            setField("postHiddenEventPublisher", mock(ApplicationEventPublisher.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(String name, Object value) throws Exception {
        var field = PostsServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(postsService, value);
    }

    private UserEntity createUser(String schoolDomain) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("student@" + schoolDomain);
        user.setSchoolDomain(schoolDomain);
        user.setProfileName("Anonymous");
        return user;
    }

    private Post createSavedPost(UUID userId, String title, String content, String wall, String schoolDomain) {
        Post post = new Post(userId, title, content, wall, schoolDomain);
        post.setId(UUID.randomUUID());
        return post;
    }

    @Nested
    @DisplayName("createPost with image")
    class CreatePostWithImageTests {

        @Test
        @DisplayName("Should create post with image when image is provided")
        void shouldCreatePostWithImage() {
            UserEntity user = createUser("harvard.edu");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            String imageUrl = "http://localhost:8080/media/posts/test.jpg";
            when(mediaUtil.uploadPostImage(any(), any())).thenReturn(imageUrl);

            Post savedPost = createSavedPost(user.getId(), "Test", "Content", "campus", "harvard.edu");
            savedPost.setImageUrl(imageUrl);
            when(postRepository.save(any())).thenReturn(savedPost);

            CompletedFileUpload image = mock(CompletedFileUpload.class);
            when(image.getSize()).thenReturn(1024L);
            when(image.getContentType()).thenReturn(Optional.of(MediaType.IMAGE_JPEG_TYPE));

            CreatePostRequest request = new CreatePostRequest("Test", "Content");
            Post result = postsService.createPost(request, image, user.getId());

            assertNotNull(result);
            assertEquals(imageUrl, result.getImageUrl());
            verify(mediaUtil, times(1)).uploadPostImage(image, user.getId());
        }

        @Test
        @DisplayName("Should create post without image when image is null")
        void shouldCreatePostWithoutImageWhenNull() {
            UserEntity user = createUser("harvard.edu");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            Post savedPost = createSavedPost(user.getId(), "Test", "Content", "campus", "harvard.edu");
            when(postRepository.save(any())).thenReturn(savedPost);

            CreatePostRequest request = new CreatePostRequest("Test", "Content");
            Post result = postsService.createPost(request, null, user.getId());

            assertNotNull(result);
            assertNull(result.getImageUrl());
            verify(mediaUtil, never()).uploadPostImage(any(), any());
        }

        @Test
        @DisplayName("Should create post without image when image size is zero")
        void shouldCreatePostWithoutImageWhenSizeIsZero() {
            UserEntity user = createUser("harvard.edu");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            Post savedPost = createSavedPost(user.getId(), "Test", "Content", "campus", "harvard.edu");
            when(postRepository.save(any())).thenReturn(savedPost);

            CompletedFileUpload emptyImage = mock(CompletedFileUpload.class);
            when(emptyImage.getSize()).thenReturn(0L);

            CreatePostRequest request = new CreatePostRequest("Test", "Content");
            Post result = postsService.createPost(request, emptyImage, user.getId());

            assertNotNull(result);
            verify(mediaUtil, never()).uploadPostImage(any(), any());
        }

        @Test
        @DisplayName("Should fail when title is empty")
        void shouldFailWhenTitleEmpty() {
            UserEntity user = createUser("harvard.edu");
            CreatePostRequest request = new CreatePostRequest("", "Content");

            assertThrows(IllegalArgumentException.class,
                () -> postsService.createPost(request, null, user.getId()));
        }

        @Test
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());
            CreatePostRequest request = new CreatePostRequest("Title", "Content");

            assertThrows(IllegalArgumentException.class,
                () -> postsService.createPost(request, null, UUID.randomUUID()));
        }

        @Test
        @DisplayName("Should propagate exception from media upload")
        void shouldPropagateMediaUploadException() {
            UserEntity user = createUser("harvard.edu");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(mediaUtil.uploadPostImage(any(), any())).thenThrow(new IllegalArgumentException("Image exceeds 5MB limit"));

            CompletedFileUpload image = mock(CompletedFileUpload.class);
            when(image.getSize()).thenReturn(6 * 1024 * 1024L);
            when(image.getContentType()).thenReturn(Optional.of(MediaType.IMAGE_JPEG_TYPE));

            CreatePostRequest request = new CreatePostRequest("Title", "Content");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> postsService.createPost(request, image, user.getId()));
            assertTrue(ex.getMessage().contains("5MB"));
        }
    }
}
