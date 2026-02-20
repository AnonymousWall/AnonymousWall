package com.anonymous.wall.listener;

import com.anonymous.wall.event.MarketplaceItemHiddenEvent;
import com.anonymous.wall.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

@DisplayName("MarketplaceItemHideEventListener Tests")
class MarketplaceItemHideEventListenerTest {

    private MarketplaceItemHideEventListener listener;
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        listener = new MarketplaceItemHideEventListener();

        // Use reflection to inject mock
        try {
            var commentRepoField = MarketplaceItemHideEventListener.class.getDeclaredField("commentRepository");
            commentRepoField.setAccessible(true);
            commentRepoField.set(listener, commentRepository);
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

        verify(commentRepository, times(1)).updateByParentTypeAndParentId("MARKETPLACE", itemId, true);
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void shouldHandleRepositoryExceptionsGracefully() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MarketplaceItemHiddenEvent event = new MarketplaceItemHiddenEvent(itemId, userId);

        doThrow(new RuntimeException("Database error"))
            .when(commentRepository).updateByParentTypeAndParentId(any(), any(), anyBoolean());

        // Act - should not throw exception
        listener.onApplicationEvent(event);

        verify(commentRepository, times(1)).updateByParentTypeAndParentId("MARKETPLACE", itemId, true);
    }
}
