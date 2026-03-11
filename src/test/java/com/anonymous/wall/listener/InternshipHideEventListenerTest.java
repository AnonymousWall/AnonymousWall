package com.anonymous.wall.listener;

import com.anonymous.wall.event.InternshipHiddenEvent;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.service.base.CommentsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

@DisplayName("InternshipHideEventListener Tests")
class InternshipHideEventListenerTest {

    private InternshipHideEventListener listener;
    private CommentsService commentService;

    @BeforeEach
    void setUp() {
        commentService = mock(CommentsService.class);
        listener = new InternshipHideEventListener();

        // Use reflection to inject mock
        try {
            var commentRepoField = InternshipHideEventListener.class.getDeclaredField("commentService");
            commentRepoField.setAccessible(true);
            commentRepoField.set(listener, commentService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should hide comments when InternshipHiddenEvent is received")
    void shouldHideCommentsWhenInternshipHiddenEventReceived() {
        UUID internshipId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        InternshipHiddenEvent event = new InternshipHiddenEvent(internshipId, userId);

        listener.onApplicationEvent(event);

        verify(commentService, times(1)).updateByParentTypeAndParentId("INTERNSHIP", internshipId, true);
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void shouldHandleRepositoryExceptionsGracefully() {
        UUID internshipId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        InternshipHiddenEvent event = new InternshipHiddenEvent(internshipId, userId);

        doThrow(new RuntimeException("Database error"))
            .when(commentService).updateByParentTypeAndParentId(any(), any(), anyBoolean());

        // Act - should not throw exception
        listener.onApplicationEvent(event);

        verify(commentService, times(1)).updateByParentTypeAndParentId("INTERNSHIP", internshipId, true);
    }
}
