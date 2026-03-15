package com.anonymous.wall.service;

import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreatePostRequest;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.service.base.*;
import com.anonymous.wall.service.impl.PostsServiceImpl;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PostsServiceImpl - Image Upload Tests")
class PostsServiceImplImageUploadTest {

    private PostsServiceImpl postsService;
    private PostRepository postRepository;
    private UserService userService;
    private CommentsService commentsService;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        userService = mock(UserService.class);
        commentsService = mock(CommentsService.class);

        postsService = new PostsServiceImpl();

        try {
            setField("postRepository", postRepository);
            setField("userService", userService);
            setProviderField("commentsServiceProvider", commentsService);
            setField("postLikeService", mock(PostLikeService.class));
            setField("postReportService", mock(PostReportService.class));
            setField("postHiddenEventPublisher", mock(ApplicationEventPublisher.class));
            setField("userBlockService", mock(UserBlockService.class));
            setProviderField("pollServiceProvider", mock(PollService.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(String name, Object value) throws Exception {
        var field = PostsServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(postsService, value);
    }

    private void setProviderField(String name, Object serviceValue) throws Exception {
        @SuppressWarnings("unchecked")
        Provider<Object> provider = mock(Provider.class);
        when(provider.get()).thenReturn(serviceValue);
        var field = PostsServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(postsService, provider);
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
    @DisplayName("createPost with images")
    class CreatePostWithImageTests {

        @Test
        @DisplayName("Should attach single objectName from request to saved post")
        void shouldCreatePostWithSingleImage() {
            UserEntity user = createUser("harvard.edu");
            when(userService.findById(user.getId())).thenReturn(Optional.of(user));

            List<String> objectNames = List.of("posts/uuid1.jpg");
            Post savedPost = createSavedPost(user.getId(), "Test", "Content", "campus", "harvard.edu");
            savedPost.setImageUrls(objectNames);
            when(postRepository.save(any())).thenReturn(savedPost);

            CreatePostRequest request = new CreatePostRequest("Test", "Content");
            request.setImageObjectNames(objectNames);
            Post result = postsService.createPost(request, user.getId());

            assertNotNull(result);
            assertEquals(objectNames, result.getImageUrls());
        }

        @Test
        @DisplayName("Should attach multiple objectNames from request to saved post")
        void shouldCreatePostWithMultipleImages() {
            UserEntity user = createUser("harvard.edu");
            when(userService.findById(user.getId())).thenReturn(Optional.of(user));

            List<String> objectNames = List.of("posts/uuid1.jpg", "posts/uuid2.png");
            Post savedPost = createSavedPost(user.getId(), "Test", "Content", "campus", "harvard.edu");
            savedPost.setImageUrls(objectNames);
            when(postRepository.save(any())).thenReturn(savedPost);

            CreatePostRequest request = new CreatePostRequest("Test", "Content");
            request.setImageObjectNames(objectNames);
            Post result = postsService.createPost(request, user.getId());

            assertNotNull(result);
            assertEquals(2, result.getImageUrls().size());
        }

        @Test
        @DisplayName("Should fail when more than 5 objectNames are provided")
        void shouldFailWhenTooManyImages() {
            UUID userId = UUID.randomUUID();
            CreatePostRequest request = new CreatePostRequest("Title", "Content");
            request.setImageObjectNames(List.of(
                    "posts/a.jpg", "posts/b.jpg", "posts/c.jpg",
                    "posts/d.jpg", "posts/e.jpg", "posts/f.jpg"));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postsService.createPost(request, userId));
            assertTrue(ex.getMessage().contains("5"));
            verify(userService, never()).findById(any());
        }

        @Test
        @DisplayName("Should create post without images when imageObjectNames is null")
        void shouldCreatePostWithoutImagesWhenNull() {
            UserEntity user = createUser("harvard.edu");
            when(userService.findById(user.getId())).thenReturn(Optional.of(user));

            Post savedPost = createSavedPost(user.getId(), "Test", "Content", "campus", "harvard.edu");
            when(postRepository.save(any())).thenReturn(savedPost);

            CreatePostRequest request = new CreatePostRequest("Test", "Content");
            Post result = postsService.createPost(request, user.getId());

            assertNotNull(result);
            assertTrue(result.getImageUrls().isEmpty());
        }

        @Test
        @DisplayName("Should create post without images when imageObjectNames is empty")
        void shouldCreatePostWithoutImagesWhenEmpty() {
            UserEntity user = createUser("harvard.edu");
            when(userService.findById(user.getId())).thenReturn(Optional.of(user));

            Post savedPost = createSavedPost(user.getId(), "Test", "Content", "campus", "harvard.edu");
            when(postRepository.save(any())).thenReturn(savedPost);

            CreatePostRequest request = new CreatePostRequest("Test", "Content");
            request.setImageObjectNames(List.of());
            Post result = postsService.createPost(request, user.getId());

            assertNotNull(result);
            assertTrue(result.getImageUrls().isEmpty());
        }

        @Test
        @DisplayName("Should fail when title is empty")
        void shouldFailWhenTitleEmpty() {
            UserEntity user = createUser("harvard.edu");
            CreatePostRequest request = new CreatePostRequest("", "Content");

            assertThrows(IllegalArgumentException.class,
                    () -> postsService.createPost(request, user.getId()));
        }

        @Test
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            when(userService.findById(any())).thenReturn(Optional.empty());
            CreatePostRequest request = new CreatePostRequest("Title", "Content");

            assertThrows(IllegalArgumentException.class,
                    () -> postsService.createPost(request, UUID.randomUUID()));
        }
    }
}