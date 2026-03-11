package com.anonymous.wall.listener;

import com.anonymous.wall.event.ProfileNameChangedEvent;
import com.anonymous.wall.service.base.CommentsService;
import com.anonymous.wall.service.base.InternshipService;
import com.anonymous.wall.service.base.MarketplaceService;
import com.anonymous.wall.service.base.PostsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("ProfileNameUpdateEventListener Tests")
class ProfileNameUpdateEventListenerTest {

    private ProfileNameUpdateEventListener listener;
    private PostsService postsService;
    private CommentsService commentsService;
    private InternshipService internshipService;
    private MarketplaceService marketplaceService;

    @BeforeEach
    void setUp() {
        postsService = mock(PostsService.class);
        commentsService = mock(CommentsService.class);
        internshipService = mock(InternshipService.class);
        marketplaceService = mock(MarketplaceService.class);
        listener = new ProfileNameUpdateEventListener();

        try {
            var f = ProfileNameUpdateEventListener.class.getDeclaredField("postsService");
            f.setAccessible(true); f.set(listener, postsService);
            f = ProfileNameUpdateEventListener.class.getDeclaredField("commentsService");
            f.setAccessible(true); f.set(listener, commentsService);
            f = ProfileNameUpdateEventListener.class.getDeclaredField("internshipService");
            f.setAccessible(true); f.set(listener, internshipService);
            f = ProfileNameUpdateEventListener.class.getDeclaredField("marketplaceService");
            f.setAccessible(true); f.set(listener, marketplaceService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should call all four services when event is received")
    void shouldCallAllFourServicesOnEvent() {
        UUID userId = UUID.randomUUID();
        ProfileNameChangedEvent event = new ProfileNameChangedEvent(userId, "OldName", "NewName");

        listener.onApplicationEvent(event);

        verify(postsService).updateProfileNameByUserId(userId, "NewName");
        verify(commentsService).updateProfileNameByUserId(userId, "NewName");
        verify(internshipService).updateProfileNameByUserId(userId, "NewName");
        verify(marketplaceService).updateProfileNameByUserId(userId, "NewName");
    }

    @Test
    @DisplayName("Should propagate exception when a service fails — allows @Retryable to retry")
    void shouldPropagateExceptionWhenServiceFails() {
        UUID userId = UUID.randomUUID();
        ProfileNameChangedEvent event = new ProfileNameChangedEvent(userId, "OldName", "NewName");

        doThrow(new RuntimeException("DB error"))
                .when(postsService).updateProfileNameByUserId(any(), any());

        // Exception must propagate — not swallowed — so @Retryable can catch and retry
        assertThrows(RuntimeException.class, () -> listener.onApplicationEvent(event));

        // postsService was called, but subsequent services were NOT reached
        verify(postsService).updateProfileNameByUserId(userId, "NewName");
        verifyNoInteractions(commentsService);
        verifyNoInteractions(internshipService);
        verifyNoInteractions(marketplaceService);
    }

    @Test
    @DisplayName("Should propagate exception when a middle service fails")
    void shouldPropagateExceptionWhenMiddleServiceFails() {
        UUID userId = UUID.randomUUID();
        ProfileNameChangedEvent event = new ProfileNameChangedEvent(userId, "OldName", "NewName");

        doThrow(new RuntimeException("DB error"))
                .when(commentsService).updateProfileNameByUserId(any(), any());

        assertThrows(RuntimeException.class, () -> listener.onApplicationEvent(event));

        // Posts succeeded before the failure
        verify(postsService).updateProfileNameByUserId(userId, "NewName");
        verify(commentsService).updateProfileNameByUserId(userId, "NewName");
        // Services after the failure were never reached
        verifyNoInteractions(internshipService);
        verifyNoInteractions(marketplaceService);
    }

    @Test
    @DisplayName("Should handle null old name — old name is unused by listener")
    void shouldHandleNullOldName() {
        UUID userId = UUID.randomUUID();
        ProfileNameChangedEvent event = new ProfileNameChangedEvent(userId, null, "NewName");

        listener.onApplicationEvent(event);

        verify(postsService).updateProfileNameByUserId(userId, "NewName");
        verify(commentsService).updateProfileNameByUserId(userId, "NewName");
        verify(internshipService).updateProfileNameByUserId(userId, "NewName");
        verify(marketplaceService).updateProfileNameByUserId(userId, "NewName");
    }
}