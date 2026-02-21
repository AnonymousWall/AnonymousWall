package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.model.AdminConversationDTO;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import java.util.UUID;

public interface AdminChatService {
    Page<AdminConversationDTO> getAllConversations(Pageable pageable, UUID userId);
    Page<ChatMessage> getConversationMessages(UUID conversationId, Pageable pageable);
}
