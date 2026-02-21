package com.anonymous.wall.admin.controller;

import com.anonymous.wall.admin.service.AdminChatService;
import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.model.AdminConversationDTO;
import com.anonymous.wall.model.ChatMessageDTO;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller("/api/v1/admin/conversations")
public class AdminChatController {

    private static final Logger log = LoggerFactory.getLogger(AdminChatController.class);

    @Inject
    private AdminChatService adminChatService;

    private ChatMessageDTO mapMessageToDTO(ChatMessage msg) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(msg.getId());
        dto.setSenderId(msg.getSenderId());
        dto.setReceiverId(msg.getReceiverId());
        dto.setContent(msg.getContent());
        dto.setReadStatus(msg.isReadStatus());
        dto.setCreatedAt(msg.getCreatedAt());
        return dto;
    }

    @Get
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getAllConversations(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit,
            @Nullable @QueryValue String userId,
            @Nullable @QueryValue String sortBy,
            @Nullable @QueryValue String sortOrder) {

        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;

        UUID userIdUuid = userId != null ? UUID.fromString(userId) : null;
        Pageable pageable = Pageable.from(page - 1, limit);
        Page<AdminConversationDTO> convsPage = adminChatService.getAllConversations(pageable, userIdUuid);

        Map<String, Object> response = new HashMap<>();
        response.put("data", convsPage.getContent());

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", convsPage.getTotalSize());
        pagination.put("totalPages", convsPage.getTotalPages());
        response.put("pagination", pagination);

        return HttpResponse.ok(response);
    }

    @Get("/{id}/messages")
    @Secured({"ADMIN", "MODERATOR"})
    public HttpResponse<Object> getConversationMessages(
            @PathVariable String id,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit) {

        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 20;

        UUID conversationId = UUID.fromString(id);
        Pageable pageable = Pageable.from(page - 1, limit);
        Page<ChatMessage> messagesPage = adminChatService.getConversationMessages(conversationId, pageable);

        List<ChatMessageDTO> dtos = messagesPage.getContent().stream()
                .map(this::mapMessageToDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", dtos);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", messagesPage.getTotalSize());
        pagination.put("totalPages", messagesPage.getTotalPages());
        response.put("pagination", pagination);

        return HttpResponse.ok(response);
    }
}
