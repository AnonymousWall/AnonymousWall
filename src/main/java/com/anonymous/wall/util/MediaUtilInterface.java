package com.anonymous.wall.util;

import io.micronaut.http.multipart.CompletedFileUpload;

import java.util.UUID;

public interface MediaUtilInterface {
    String uploadPostImage(CompletedFileUpload file, UUID userId);
    String uploadChatImage(CompletedFileUpload file, UUID userId);
}
