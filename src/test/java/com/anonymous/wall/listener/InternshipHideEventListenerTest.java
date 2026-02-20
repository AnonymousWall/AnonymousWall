package com.anonymous.wall.listener;

import com.anonymous.wall.event.InternshipHiddenEvent;
import com.anonymous.wall.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

@DisplayName("InternshipHideEventListener Tests")
class InternshipHideEventListenerTest {

    private InternshipHideEventListener listener;
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        listener = new InternshipHideEventListener();

        // Use reflection to inject mock
        try {
            var commentRepoField = InternshipHideEventListener.class.getDeclaredField("commentRepository");
            commentRepoField.setAccessible(true);
            commentRepoField.set(listener, commentRepository);
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

        verify(commentRepository, times(1)).updateByParentTypeAndParentId("INTERNSHIP", internshipId, true);
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void shouldHandleRepositoryExceptionsGracefully() {
        UUID internshipId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        InternshipHiddenEvent event = new InternshipHiddenEvent(internshipId, userId);

        doThrow(new RuntimeException("Database error"))
            .when(commentRepository).updateByParentTypeAndParentId(any(), any(), anyBoolean());

        // Act - should not throw exception
        listener.onApplicationEvent(event);

        verify(commentRepository, times(1)).updateByParentTypeAndParentId("INTERNSHIP", internshipId, true);
    }
}
