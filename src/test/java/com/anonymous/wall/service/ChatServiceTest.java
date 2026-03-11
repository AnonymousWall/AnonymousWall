package com.anonymous.wall.service;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.ConversationDTO;
import com.anonymous.wall.notification.event.ChatMessageSentEvent;
import com.anonymous.wall.repository.ChatMessageRepository;
import com.anonymous.wall.service.base.UserBlockService;
import com.anonymous.wall.service.base.UserService;
import com.anonymous.wall.service.impl.ChatServiceImpl;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("ChatService Unit Tests")
class ChatServiceTest {

    private ChatServiceImpl chatService;
    private ChatMessageRepository chatMessageRepository;
    private UserService userService;
    private UserBlockService userBlockService;
    @SuppressWarnings("unchecked")
    private ApplicationEventPublisher<ChatMessageSentEvent> chatMessageEventPublisher;

    private UUID testUser1Id;
    private UUID testUser2Id;
    private UUID blockedUserId;
    private UserEntity testUser1;
    private UserEntity testUser2;
    private UserEntity blockedUser;

    @BeforeEach
    void setUp() {
        chatMessageRepository = mock(ChatMessageRepository.class);
        userService = mock(UserService.class);
        userBlockService = mock(UserBlockService.class);
        chatMessageEventPublisher = mock(ApplicationEventPublisher.class);

        chatService = new ChatServiceImpl();

        try {
            var chatRepoField = ChatServiceImpl.class.getDeclaredField("chatMessageRepository");
            chatRepoField.setAccessible(true);
            chatRepoField.set(chatService, chatMessageRepository);

            var userServiceField = ChatServiceImpl.class.getDeclaredField("userService");
            userServiceField.setAccessible(true);
            userServiceField.set(chatService, userService);

            var userBlockServiceField = ChatServiceImpl.class.getDeclaredField("userBlockService");
            userBlockServiceField.setAccessible(true);
            userBlockServiceField.set(chatService, userBlockService);

            var publisherField = ChatServiceImpl.class.getDeclaredField("chatMessageEventPublisher");
            publisherField.setAccessible(true);
            publisherField.set(chatService, chatMessageEventPublisher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        testUser1Id = UUID.randomUUID();
        testUser2Id = UUID.randomUUID();
        blockedUserId = UUID.randomUUID();

        testUser1 = new UserEntity();
        testUser1.setId(testUser1Id);
        testUser1.setEmail("user1@harvard.edu");
        testUser1.setSchoolDomain("harvard.edu");
        testUser1.setProfileName("User1");
        testUser1.setBlocked(false);

        testUser2 = new UserEntity();
        testUser2.setId(testUser2Id);
        testUser2.setEmail("user2@harvard.edu");
        testUser2.setSchoolDomain("harvard.edu");
        testUser2.setProfileName("User2");
        testUser2.setBlocked(false);

        blockedUser = new UserEntity();
        blockedUser.setId(blockedUserId);
        blockedUser.setEmail("blocked@harvard.edu");
        blockedUser.setSchoolDomain("harvard.edu");
        blockedUser.setProfileName("BlockedUser");
        blockedUser.setBlocked(true);

        // The impl calls getCombinedBlockedUserIds(senderId).contains(receiverId) — not
        // isBlockedInAnyDirection. Default to empty set so no user-level blocks exist
        // unless a specific test overrides this.
        when(userBlockService.getCombinedBlockedUserIds(any(UUID.class)))
                .thenReturn(Collections.emptySet());
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private void stubValidSend() {
        when(userService.findById(testUser1Id)).thenReturn(Optional.of(testUser1));
        when(userService.findById(testUser2Id)).thenReturn(Optional.of(testUser2));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage m = invocation.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
    }

    private ChatMessage lastMessage(UUID senderId, UUID receiverId) {
        ChatMessage msg = new ChatMessage(senderId, receiverId, "Last message");
        msg.setId(UUID.randomUUID());
        msg.setCreatedAt(OffsetDateTime.now());
        return msg;
    }

    // ─── Send Message — Positive ───────────────────────────────────────────────

    @Nested
    @DisplayName("Send Message — Positive Cases")
    class SendMessagePositiveCases {

        @Test
        @DisplayName("Should send text message successfully")
        void shouldSendMessageSuccessfully() {
            stubValidSend();

            ChatMessage result = chatService.sendMessage(testUser1Id, testUser2Id, "Hello, this is a test message!");

            assertNotNull(result);
            assertEquals("Hello, this is a test message!", result.getContent());
            assertEquals(testUser1Id, result.getSenderId());
            assertEquals(testUser2Id, result.getReceiverId());
            assertNotNull(result.getConversationId());
            assertFalse(result.isReadStatus());
            assertNotNull(result.getCreatedAt());
            verify(chatMessageRepository).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should trim whitespace from message content")
        void shouldTrimMessageContent() {
            stubValidSend();

            ChatMessage result = chatService.sendMessage(testUser1Id, testUser2Id, "  Hello with spaces  ");

            assertEquals("Hello with spaces", result.getContent());
        }

        @Test
        @DisplayName("Should send image-only message — null content accepted when imageUrl present")
        void shouldSendImageOnlyMessage() {
            stubValidSend();

            ChatMessage result = chatService.sendMessage(testUser1Id, testUser2Id, null,
                    "https://example.com/chat/image.jpg");

            assertNotNull(result);
            assertNull(result.getContent());
            assertEquals("https://example.com/chat/image.jpg", result.getImageUrl());
            assertEquals(testUser1Id, result.getSenderId());
            assertEquals(testUser2Id, result.getReceiverId());
            assertFalse(result.isReadStatus());
            verify(chatMessageRepository).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should send message with both content and imageUrl")
        void shouldSendMessageWithContentAndImage() {
            stubValidSend();

            ChatMessage result = chatService.sendMessage(testUser1Id, testUser2Id,
                    "Check this image!", "https://example.com/chat/image.jpg");

            assertNotNull(result);
            assertEquals("Check this image!", result.getContent());
            assertEquals("https://example.com/chat/image.jpg", result.getImageUrl());
        }

        @Test
        @DisplayName("Should accept content of exactly 5000 characters — boundary")
        void shouldAcceptContentAtExactLimit() {
            stubValidSend();

            assertDoesNotThrow(() ->
                    chatService.sendMessage(testUser1Id, testUser2Id, "a".repeat(5000)));
        }

        @Test
        @DisplayName("Should generate deterministic conversation ID — same regardless of send direction")
        void shouldGenerateDeterministicConversationId() {
            stubValidSend();
            ChatMessage msg1 = chatService.sendMessage(testUser1Id, testUser2Id, "A");

            when(userService.findById(testUser2Id)).thenReturn(Optional.of(testUser2));
            when(userService.findById(testUser1Id)).thenReturn(Optional.of(testUser1));
            ChatMessage msg2 = chatService.sendMessage(testUser2Id, testUser1Id, "B");

            assertEquals(msg1.getConversationId(), msg2.getConversationId(),
                    "Conversation ID must be identical regardless of which user is sender");
        }

        @Test
        @DisplayName("Should publish ChatMessageSentEvent after saving message")
        void shouldPublishEventAfterSaving() {
            stubValidSend();
            chatService.sendMessage(testUser1Id, testUser2Id, "Hello!");

            ArgumentCaptor<ChatMessageSentEvent> captor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
            verify(chatMessageEventPublisher).publishEvent(captor.capture());

            ChatMessageSentEvent event = captor.getValue();
            assertEquals(testUser1Id, event.getSenderUserId());
            assertEquals(testUser2Id, event.getRecipientUserId());
            assertNotNull(event.getConversationId());
        }

        @Test
        @DisplayName("Should set preview to truncated content with ellipsis when content exceeds 50 chars")
        void shouldTruncatePreviewForLongContent() {
            stubValidSend();
            chatService.sendMessage(testUser1Id, testUser2Id, "a".repeat(100));

            ArgumentCaptor<ChatMessageSentEvent> captor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
            verify(chatMessageEventPublisher).publishEvent(captor.capture());

            String preview = captor.getValue().getMessagePreview();
            // Impl: messageContent.substring(0, 50) + "…"
            assertEquals(51, preview.length(), "Preview should be 50 chars + ellipsis character");
            assertTrue(preview.endsWith("…"));
        }

        @Test
        @DisplayName("Should set preview to full content when content is 50 chars or fewer")
        void shouldNotTruncateShortPreview() {
            stubValidSend();
            String shortContent = "a".repeat(50);
            chatService.sendMessage(testUser1Id, testUser2Id, shortContent);

            ArgumentCaptor<ChatMessageSentEvent> captor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
            verify(chatMessageEventPublisher).publishEvent(captor.capture());

            assertEquals(shortContent, captor.getValue().getMessagePreview());
        }

        @Test
        @DisplayName("Should set preview to photo emoji when message is image-only")
        void shouldSetPhotoPreviewForImageOnlyMessage() {
            stubValidSend();
            chatService.sendMessage(testUser1Id, testUser2Id, null, "https://example.com/image.jpg");

            ArgumentCaptor<ChatMessageSentEvent> captor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
            verify(chatMessageEventPublisher).publishEvent(captor.capture());

            assertEquals("\uD83D\uDCF7 Photo", captor.getValue().getMessagePreview());
        }

        @Test
        @DisplayName("Should include sender profile name in published event")
        void shouldIncludeSenderProfileNameInEvent() {
            stubValidSend();
            chatService.sendMessage(testUser1Id, testUser2Id, "Hello!");

            ArgumentCaptor<ChatMessageSentEvent> captor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
            verify(chatMessageEventPublisher).publishEvent(captor.capture());

            assertEquals("User1", captor.getValue().getSenderProfileName());
        }
    }

    // ─── Send Message — Validation ─────────────────────────────────────────────

    @Nested
    @DisplayName("Send Message — Validation Errors")
    class SendMessageValidationErrors {

        @Test
        @DisplayName("Should throw when sender ID is null")
        void shouldThrowWhenSenderIdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(null, testUser2Id, "Test message"));

            assertEquals("Sender ID and receiver ID must not be null", ex.getMessage());
            verifyNoInteractions(chatMessageRepository);
        }

        @Test
        @DisplayName("Should throw when receiver ID is null")
        void shouldThrowWhenReceiverIdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, null, "Test message"));

            assertEquals("Sender ID and receiver ID must not be null", ex.getMessage());
            verifyNoInteractions(chatMessageRepository);
        }

        @Test
        @DisplayName("Should throw when both content and imageUrl are null")
        void shouldThrowWhenBothContentAndImageNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, testUser2Id, null, null));

            assertEquals("Message must have content or an image", ex.getMessage());
            verifyNoInteractions(chatMessageRepository);
        }

        @Test
        @DisplayName("Should throw when content is empty string and no imageUrl")
        void shouldThrowWhenContentEmptyAndNoImage() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, testUser2Id, "", null));

            assertEquals("Message must have content or an image", ex.getMessage());
            verifyNoInteractions(chatMessageRepository);
        }

        @Test
        @DisplayName("Should throw when content is only whitespace and no imageUrl")
        void shouldThrowWhenContentWhitespaceAndNoImage() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, testUser2Id, "   ", null));

            assertEquals("Message must have content or an image", ex.getMessage());
            verifyNoInteractions(chatMessageRepository);
        }

        @Test
        @DisplayName("Should throw when content exceeds 5000 characters")
        void shouldThrowWhenContentTooLong() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, testUser2Id, "a".repeat(5001)));

            assertEquals("Message content exceeds maximum length of 5000 characters", ex.getMessage());
            verifyNoInteractions(chatMessageRepository);
        }

        @Test
        @DisplayName("Should throw when sender not found")
        void shouldThrowWhenSenderNotFound() {
            when(userService.findById(testUser1Id)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, testUser2Id, "Test message"));

            assertEquals("Sender not found", ex.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when receiver not found")
        void shouldThrowWhenReceiverNotFound() {
            when(userService.findById(testUser1Id)).thenReturn(Optional.of(testUser1));
            when(userService.findById(testUser2Id)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, testUser2Id, "Test message"));

            assertEquals("Receiver not found", ex.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }
    }

    // ─── Send Message — Block Checks ───────────────────────────────────────────

    @Nested
    @DisplayName("Send Message — Block Checks")
    class SendMessageBlockChecks {

        @Test
        @DisplayName("Should throw when receiver is admin-blocked")
        void shouldThrowWhenReceiverAdminBlocked() {
            when(userService.findById(testUser1Id)).thenReturn(Optional.of(testUser1));
            when(userService.findById(blockedUserId)).thenReturn(Optional.of(blockedUser));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, blockedUserId, "Test message"));

            assertEquals("Cannot send message to a blocked user", ex.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when sender is admin-blocked")
        void shouldThrowWhenSenderAdminBlocked() {
            when(userService.findById(blockedUserId)).thenReturn(Optional.of(blockedUser));
            when(userService.findById(testUser2Id)).thenReturn(Optional.of(testUser2));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(blockedUserId, testUser2Id, "Test message"));

            assertEquals("Blocked users cannot send messages", ex.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when user-level block relationship exists")
        void shouldThrowWhenUserLevelBlockExists() {
            when(userService.findById(testUser1Id)).thenReturn(Optional.of(testUser1));
            when(userService.findById(testUser2Id)).thenReturn(Optional.of(testUser2));
            // Impl: getCombinedBlockedUserIds(senderId).contains(receiverId)
            when(userBlockService.getCombinedBlockedUserIds(testUser1Id))
                    .thenReturn(Set.of(testUser2Id));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(testUser1Id, testUser2Id, "Test message"));

            assertEquals("Cannot send message to this user", ex.getMessage());
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow message when no block relationship exists")
        void shouldAllowMessageWhenNoBlockExists() {
            stubValidSend();
            // getCombinedBlockedUserIds returns empty set by default from @BeforeEach

            assertDoesNotThrow(() -> chatService.sendMessage(testUser1Id, testUser2Id, "Hello!"));
            verify(chatMessageRepository).save(any(ChatMessage.class));
        }
    }

    // ─── Get Message History ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Get Message History")
    class GetMessageHistory {

        @Test
        @DisplayName("Should return paginated history for valid users")
        void shouldReturnHistoryForValidUsers() {
            Pageable pageable = Pageable.from(0, 50);
            Page<ChatMessage> mockPage = mock(Page.class);

            when(userService.existsById(testUser1Id)).thenReturn(true);
            when(userService.existsById(testUser2Id)).thenReturn(true);
            when(chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(any(UUID.class), eq(pageable)))
                    .thenReturn(mockPage);

            Page<ChatMessage> result = chatService.getMessageHistory(testUser1Id, testUser2Id, pageable);

            assertNotNull(result);
            verify(chatMessageRepository).findByConversationIdOrderByCreatedAtAsc(any(UUID.class), eq(pageable));
        }

        @Test
        @DisplayName("Should use same conversation ID regardless of user order")
        void shouldUseSameConversationIdRegardlessOfOrder() {
            Pageable pageable = Pageable.from(0, 50);
            Page<ChatMessage> mockPage = mock(Page.class);

            when(userService.existsById(testUser1Id)).thenReturn(true);
            when(userService.existsById(testUser2Id)).thenReturn(true);
            when(chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(any(UUID.class), eq(pageable)))
                    .thenReturn(mockPage);

            chatService.getMessageHistory(testUser1Id, testUser2Id, pageable);
            chatService.getMessageHistory(testUser2Id, testUser1Id, pageable);

            ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
            verify(chatMessageRepository, times(2))
                    .findByConversationIdOrderByCreatedAtAsc(captor.capture(), eq(pageable));

            assertEquals(captor.getAllValues().get(0), captor.getAllValues().get(1),
                    "Same conversation ID must be used regardless of user order");
        }

        @Test
        @DisplayName("Should throw when user1 ID is null")
        void shouldThrowWhenUser1IdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.getMessageHistory(null, testUser2Id, Pageable.from(0, 50)));

            assertEquals("User IDs must not be null", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when user2 ID is null")
        void shouldThrowWhenUser2IdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.getMessageHistory(testUser1Id, null, Pageable.from(0, 50)));

            assertEquals("User IDs must not be null", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when user1 not found")
        void shouldThrowWhenUser1NotFound() {
            when(userService.existsById(testUser1Id)).thenReturn(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.getMessageHistory(testUser1Id, testUser2Id, Pageable.from(0, 50)));

            assertTrue(ex.getMessage().contains("User not found"));
        }

        @Test
        @DisplayName("Should throw when user2 not found")
        void shouldThrowWhenUser2NotFound() {
            when(userService.existsById(testUser1Id)).thenReturn(true);
            when(userService.existsById(testUser2Id)).thenReturn(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.getMessageHistory(testUser1Id, testUser2Id, Pageable.from(0, 50)));

            assertTrue(ex.getMessage().contains("User not found"));
        }
    }

    // ─── Mark Message As Read ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Mark Message As Read")
    class MarkMessageAsRead {

        @Test
        @DisplayName("Should mark unread message as read")
        void shouldMarkUnreadMessageAsRead() {
            UUID messageId = UUID.randomUUID();
            ChatMessage message = new ChatMessage(testUser1Id, testUser2Id, "Test");
            message.setId(messageId);
            message.setReadStatus(false);

            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
            when(chatMessageRepository.update(any(ChatMessage.class))).thenReturn(message);

            chatService.markMessageAsRead(messageId, testUser2Id);

            assertTrue(message.isReadStatus());
            verify(chatMessageRepository).update(message);
        }

        @Test
        @DisplayName("Should not call update when message is already read — idempotent")
        void shouldNotUpdateIfAlreadyRead() {
            UUID messageId = UUID.randomUUID();
            ChatMessage message = new ChatMessage(testUser1Id, testUser2Id, "Test");
            message.setId(messageId);
            message.setReadStatus(true);

            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

            chatService.markMessageAsRead(messageId, testUser2Id);

            verify(chatMessageRepository, never()).update(any());
        }

        @Test
        @DisplayName("Should throw when caller is the sender, not the receiver")
        void shouldThrowWhenCallerIsNotReceiver() {
            UUID messageId = UUID.randomUUID();
            ChatMessage message = new ChatMessage(testUser1Id, testUser2Id, "Test");
            message.setId(messageId);

            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

            // testUser1 sent this message — only testUser2 (receiver) can mark it read
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.markMessageAsRead(messageId, testUser1Id));

            assertEquals("Only the receiver can mark a message as read", ex.getMessage());
            verify(chatMessageRepository, never()).update(any());
        }

        @Test
        @DisplayName("Should throw when message not found")
        void shouldThrowWhenMessageNotFound() {
            UUID messageId = UUID.randomUUID();
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.markMessageAsRead(messageId, testUser2Id));

            assertEquals("Message not found", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when message ID is null")
        void shouldThrowWhenMessageIdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.markMessageAsRead(null, testUser2Id));

            assertEquals("Message ID and user ID must not be null", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when user ID is null")
        void shouldThrowWhenUserIdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.markMessageAsRead(UUID.randomUUID(), null));

            assertEquals("Message ID and user ID must not be null", ex.getMessage());
        }
    }

    // ─── Mark Conversation As Read ─────────────────────────────────────────────

    @Nested
    @DisplayName("Mark Conversation As Read")
    class MarkConversationAsRead {

        @Test
        @DisplayName("Should bulk-mark all messages in conversation as read")
        void shouldMarkAllMessagesAsRead() {
            chatService.markConversationAsRead(testUser2Id, testUser1Id);

            verify(chatMessageRepository).updateReadStatusByConversationIdAndReceiverId(
                    any(UUID.class), eq(testUser2Id), eq(true));
        }

        @Test
        @DisplayName("Should use same conversation ID regardless of parameter order")
        void shouldUseSameConversationIdRegardlessOfOrder() {
            chatService.markConversationAsRead(testUser1Id, testUser2Id);
            chatService.markConversationAsRead(testUser2Id, testUser1Id);

            ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
            verify(chatMessageRepository, times(2))
                    .updateReadStatusByConversationIdAndReceiverId(captor.capture(), any(), eq(true));

            assertEquals(captor.getAllValues().get(0), captor.getAllValues().get(1),
                    "Conversation ID must be deterministic regardless of parameter order");
        }

        @Test
        @DisplayName("Should throw when receiver ID is null")
        void shouldThrowWhenReceiverIdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.markConversationAsRead(null, testUser1Id));

            assertEquals("Receiver ID and sender ID must not be null", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when sender ID is null")
        void shouldThrowWhenSenderIdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.markConversationAsRead(testUser2Id, null));

            assertEquals("Receiver ID and sender ID must not be null", ex.getMessage());
        }
    }

    // ─── Count Unread Messages ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Count Unread Messages")
    class CountUnreadMessages {

        @Test
        @DisplayName("Should count unread messages from specific sender")
        void shouldCountUnreadMessagesFromSender() {
            when(chatMessageRepository.countByConversationIdAndReceiverIdAndReadStatusFalse(
                    any(UUID.class), eq(testUser2Id))).thenReturn(5L);

            long count = chatService.countUnreadMessages(testUser2Id, testUser1Id);

            assertEquals(5L, count);
        }

        @Test
        @DisplayName("Should return zero when no unread messages exist")
        void shouldReturnZeroWhenNoUnread() {
            when(chatMessageRepository.countByConversationIdAndReceiverIdAndReadStatusFalse(
                    any(UUID.class), eq(testUser2Id))).thenReturn(0L);

            long count = chatService.countUnreadMessages(testUser2Id, testUser1Id);

            assertEquals(0L, count);
        }

        @Test
        @DisplayName("Should throw when receiver ID is null")
        void shouldThrowWhenReceiverIdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.countUnreadMessages(null, testUser1Id));

            assertEquals("Receiver ID and sender ID must not be null", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when sender ID is null")
        void shouldThrowWhenSenderIdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.countUnreadMessages(testUser2Id, null));

            assertEquals("Receiver ID and sender ID must not be null", ex.getMessage());
        }

        @Test
        @DisplayName("Should count total unread messages across all conversations")
        void shouldCountTotalUnreadMessages() {
            when(chatMessageRepository.countByReceiverIdAndReadStatusFalse(testUser2Id))
                    .thenReturn(10L);

            long count = chatService.countTotalUnreadMessages(testUser2Id);

            assertEquals(10L, count);
        }

        @Test
        @DisplayName("Should throw when receiver ID is null for total count")
        void shouldThrowWhenReceiverIdNullForTotalCount() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.countTotalUnreadMessages(null));

            assertEquals("Receiver ID must not be null", ex.getMessage());
        }
    }

    // ─── Get Unread Messages ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Get Unread Messages")
    class GetUnreadMessages {

        @Test
        @DisplayName("Should return unread messages for a conversation")
        void shouldReturnUnreadMessages() {
            ChatMessage msg = new ChatMessage(testUser1Id, testUser2Id, "Unread message");
            msg.setId(UUID.randomUUID());
            msg.setReadStatus(false);

            when(chatMessageRepository.findByConversationIdAndReceiverIdAndReadStatusFalse(
                    any(UUID.class), eq(testUser2Id))).thenReturn(List.of(msg));

            List<ChatMessage> result = chatService.getUnreadMessages(testUser2Id, testUser1Id);

            assertEquals(1, result.size());
            assertFalse(result.get(0).isReadStatus());
        }

        @Test
        @DisplayName("Should return empty list when no unread messages")
        void shouldReturnEmptyWhenNoUnread() {
            when(chatMessageRepository.findByConversationIdAndReceiverIdAndReadStatusFalse(
                    any(UUID.class), eq(testUser2Id))).thenReturn(Collections.emptyList());

            List<ChatMessage> result = chatService.getUnreadMessages(testUser2Id, testUser1Id);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should throw when receiver ID is null")
        void shouldThrowWhenReceiverIdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.getUnreadMessages(null, testUser1Id));

            assertEquals("Receiver ID and sender ID must not be null", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when sender ID is null")
        void shouldThrowWhenSenderIdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.getUnreadMessages(testUser2Id, null));

            assertEquals("Receiver ID and sender ID must not be null", ex.getMessage());
        }
    }

    // ─── Get Conversations ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Get Conversations")
    class GetConversations {

        @Test
        @DisplayName("Should return conversations with correct DTO fields populated")
        void shouldReturnConversationsSuccessfully() {
            UUID conversationId = UUID.randomUUID();

            when(chatMessageRepository.findDistinctConversationIdBySenderIdOrReceiverId(
                    testUser1Id, testUser1Id)).thenReturn(List.of(conversationId));
            when(chatMessageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId))
                    .thenReturn(Optional.of(lastMessage(testUser2Id, testUser1Id)));
            when(userService.findById(testUser2Id)).thenReturn(Optional.of(testUser2));
            when(chatMessageRepository.countByConversationIdAndReceiverIdAndReadStatusFalse(
                    conversationId, testUser1Id)).thenReturn(3L);

            List<ConversationDTO> result = chatService.getConversations(testUser1Id);

            assertEquals(1, result.size());
            ConversationDTO conv = result.get(0);
            assertEquals(testUser2Id, conv.getUserId());
            assertEquals("User2", conv.getProfileName());
            assertEquals(3, conv.getUnreadCount());
            assertNotNull(conv.getLastMessage());
        }

        @Test
        @DisplayName("Should return empty list when user has no conversations")
        void shouldReturnEmptyListWhenNoConversations() {
            when(chatMessageRepository.findDistinctConversationIdBySenderIdOrReceiverId(
                    testUser1Id, testUser1Id)).thenReturn(Collections.emptyList());

            List<ConversationDTO> result = chatService.getConversations(testUser1Id);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should skip conversation when last message not found")
        void shouldSkipConversationWhenLastMessageNotFound() {
            UUID conversationId = UUID.randomUUID();

            when(chatMessageRepository.findDistinctConversationIdBySenderIdOrReceiverId(
                    testUser1Id, testUser1Id)).thenReturn(List.of(conversationId));
            when(chatMessageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId))
                    .thenReturn(Optional.empty());

            List<ConversationDTO> result = chatService.getConversations(testUser1Id);

            assertTrue(result.isEmpty());
            // Impl continues at lastMessageOpt.isEmpty() before reaching userService
            verify(userService, never()).findById(any());
        }

        @Test
        @DisplayName("Should skip conversation when partner user not found in DB")
        void shouldSkipConversationWhenPartnerNotFound() {
            UUID conversationId = UUID.randomUUID();

            when(chatMessageRepository.findDistinctConversationIdBySenderIdOrReceiverId(
                    testUser1Id, testUser1Id)).thenReturn(List.of(conversationId));
            // Must stub last message — impl reaches partner lookup only after this check passes
            when(chatMessageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId))
                    .thenReturn(Optional.of(lastMessage(testUser2Id, testUser1Id)));
            when(userService.findById(testUser2Id)).thenReturn(Optional.empty());

            List<ConversationDTO> result = chatService.getConversations(testUser1Id);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should skip conversation when partner has a user-level block relationship")
        void shouldSkipConversationWithBlockedPartner() {
            UUID conversationId = UUID.randomUUID();

            when(chatMessageRepository.findDistinctConversationIdBySenderIdOrReceiverId(
                    testUser1Id, testUser1Id)).thenReturn(List.of(conversationId));
            when(chatMessageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId))
                    .thenReturn(Optional.of(lastMessage(testUser2Id, testUser1Id)));
            when(userService.findById(testUser2Id)).thenReturn(Optional.of(testUser2));
            // Impl: getCombinedBlockedUserIds(userId).contains(partnerId)
            when(userBlockService.getCombinedBlockedUserIds(testUser1Id))
                    .thenReturn(Set.of(testUser2Id));

            List<ConversationDTO> result = chatService.getConversations(testUser1Id);

            assertTrue(result.isEmpty(), "Conversations with blocked users must be filtered out");
        }

        @Test
        @DisplayName("Should sort conversations by last message timestamp — most recent first")
        void shouldSortConversationsByLastMessageDescending() {
            UUID conv1Id = UUID.randomUUID();
            UUID conv2Id = UUID.randomUUID();
            UUID user3Id = UUID.randomUUID();

            UserEntity user3 = new UserEntity();
            user3.setId(user3Id);
            user3.setProfileName("User3");
            user3.setBlocked(false);

            ChatMessage olderMsg = new ChatMessage(testUser2Id, testUser1Id, "Older");
            olderMsg.setId(UUID.randomUUID());
            olderMsg.setCreatedAt(OffsetDateTime.now().minusHours(2));

            ChatMessage newerMsg = new ChatMessage(user3Id, testUser1Id, "Newer");
            newerMsg.setId(UUID.randomUUID());
            newerMsg.setCreatedAt(OffsetDateTime.now());

            when(chatMessageRepository.findDistinctConversationIdBySenderIdOrReceiverId(
                    testUser1Id, testUser1Id)).thenReturn(List.of(conv1Id, conv2Id));
            when(chatMessageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conv1Id))
                    .thenReturn(Optional.of(olderMsg));
            when(chatMessageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conv2Id))
                    .thenReturn(Optional.of(newerMsg));
            when(userService.findById(testUser2Id)).thenReturn(Optional.of(testUser2));
            when(userService.findById(user3Id)).thenReturn(Optional.of(user3));
            when(chatMessageRepository.countByConversationIdAndReceiverIdAndReadStatusFalse(
                    any(), any())).thenReturn(0L);

            List<ConversationDTO> result = chatService.getConversations(testUser1Id);

            assertEquals(2, result.size());
            assertEquals(user3Id, result.get(0).getUserId(), "Most recent conversation should be first");
            assertEquals(testUser2Id, result.get(1).getUserId());
        }

        @Test
        @DisplayName("Should throw when userId is null")
        void shouldThrowWhenUserIdNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.getConversations(null));

            assertEquals("User ID must not be null", ex.getMessage());
        }
    }
}
