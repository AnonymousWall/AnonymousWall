package com.anonymous.wall.listener;

import com.anonymous.wall.event.MarketplaceItemHiddenEvent;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.service.base.CommentsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

@DisplayName("MarketplaceItemHideEventListener Tests")
class MarketplaceItemHideEventListenerTest {

    private MarketplaceItemHideEventListener listener;
    private CommentsService commentService;

    @BeforeEach
    void setUp() {
        commentService = mock(CommentsService.class);
        listener = new MarketplaceItemHideEventListener();

        // Use reflection to inject mock
        try {
            var commentRepoField = MarketplaceItemHideEventListener.class.getDeclaredField("commentService");
            commentRepoField.setAccessible(true);
            commentRepoField.set(listener, commentService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should hide comments when MarketplaceItemHiddenEvent is received")
    void shouldHideCommentsWhenMarketplaceItemHiddenEventReceived() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MarketplaceItemHiddenEvent event = new MarketplaceItemHiddenEvent(itemId, userId);

        listener.onApplicationEvent(event);

        verify(commentService, times(1)).updateByParentTypeAndParentId("MARKETPLACE", itemId, true);
    }

    @Test
    @DisplayName("Should handle service exceptions gracefully")
    void shouldHandleServiceExceptionsGracefully() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MarketplaceItemHiddenEvent event = new MarketplaceItemHiddenEvent(itemId, userId);

        doThrow(new RuntimeException("Database error"))
            .when(commentService).updateByParentTypeAndParentId(any(), any(), anyBoolean());

        // Act - should not throw exception
        listener.onApplicationEvent(event);

        verify(commentService, times(1)).updateByParentTypeAndParentId("MARKETPLACE", itemId, true);
    }
}
