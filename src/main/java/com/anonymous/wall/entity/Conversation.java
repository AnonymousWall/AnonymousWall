package com.anonymous.wall.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.OffsetDateTime;
import java.util.UUID;

@MappedEntity(value = "conversations", namingStrategy = NamingStrategies.Raw.class)
public class Conversation {

    @Id
    @AutoPopulated
    private UUID id;  // surrogate PK — add this

    @MappedProperty("conversation_id")
    private UUID conversationId;

    @MappedProperty("user_id")
    private UUID userId;

    @MappedProperty("partner_id")
    private UUID partnerId;

    @MappedProperty("partner_profile_name")
    @Nullable
    private String partnerProfileName;

    @MappedProperty("last_message_content")
    @Nullable
    private String lastMessageContent;

    @MappedProperty("last_message_image_url")
    @Nullable
    private String lastMessageImageUrl;

    @MappedProperty("last_message_sender_id")
    @Nullable
    private UUID lastMessageSenderId;

    @MappedProperty("last_message_receiver_id")
    @Nullable
    private UUID lastMessageReceiverId;

    @MappedProperty("last_message_read_status")
    private boolean lastMessageReadStatus;

    @MappedProperty("last_message_at")
    @Nullable
    private OffsetDateTime lastMessageAt;

    @MappedProperty("unread_count")
    private int unreadCount;

    @MappedProperty("last_message_id")
    @Nullable
    private UUID lastMessageId;

    // getters and setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getPartnerId() { return partnerId; }
    public void setPartnerId(UUID partnerId) { this.partnerId = partnerId; }

    public String getPartnerProfileName() { return partnerProfileName; }
    public void setPartnerProfileName(String partnerProfileName) { this.partnerProfileName = partnerProfileName; }

    public String getLastMessageContent() { return lastMessageContent; }
    public void setLastMessageContent(String lastMessageContent) { this.lastMessageContent = lastMessageContent; }

    public String getLastMessageImageUrl() { return lastMessageImageUrl; }
    public void setLastMessageImageUrl(String lastMessageImageUrl) { this.lastMessageImageUrl = lastMessageImageUrl; }

    public UUID getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(UUID lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public UUID getLastMessageReceiverId() { return lastMessageReceiverId; }
    public void setLastMessageReceiverId(UUID lastMessageReceiverId) { this.lastMessageReceiverId = lastMessageReceiverId; }

    public boolean isLastMessageReadStatus() { return lastMessageReadStatus; }
    public void setLastMessageReadStatus(boolean lastMessageReadStatus) { this.lastMessageReadStatus = lastMessageReadStatus; }

    public OffsetDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(OffsetDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public UUID getLastMessageId() { return lastMessageId; }
    public void setLastMessageId(UUID lastMessageId) { this.lastMessageId = lastMessageId; }
}