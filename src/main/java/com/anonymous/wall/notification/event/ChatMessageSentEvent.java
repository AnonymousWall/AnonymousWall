package com.anonymous.wall.notification.event;

import java.util.UUID;

public class ChatMessageSentEvent {
    private final UUID messageId;
    private final UUID conversationId;
    private final UUID senderUserId;
    private final UUID recipientUserId;
    private final String messagePreview;
    private final String senderProfileName;

    public ChatMessageSentEvent(UUID messageId, UUID conversationId,
            UUID senderUserId, UUID recipientUserId,
            String messagePreview, String senderProfileName) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderUserId = senderUserId;
        this.recipientUserId = recipientUserId;
        this.messagePreview = messagePreview;
        this.senderProfileName = senderProfileName;
    }

    public UUID getMessageId() { return messageId; }
    public UUID getConversationId() { return conversationId; }
    public UUID getSenderUserId() { return senderUserId; }
    public UUID getRecipientUserId() { return recipientUserId; }
    public String getMessagePreview() { return messagePreview; }
    public String getSenderProfileName() { return senderProfileName; }
}
