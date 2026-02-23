package com.anonymous.wall.util;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Singleton
@Requires(notEnv = "prod")
public class LocalMediaUtil implements MediaUtilInterface {
    private static final Logger log = LoggerFactory.getLogger(LocalMediaUtil.class);
    private static final String LOCAL_MEDIA_DIR = "/tmp/anonymouswall-media";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    @Override
    public String uploadPostImage(CompletedFileUpload file, UUID userId) {
        return upload(file, "posts");
    }

    @Override
    public String uploadChatImage(CompletedFileUpload file, UUID userId) {
        return upload(file, "chat");
    }

    private String upload(CompletedFileUpload file, String prefix) {
        validateFile(file);
        try {
            Path dir = Path.of(LOCAL_MEDIA_DIR, prefix);
            Files.createDirectories(dir);

            String ext = getExtension(file.getContentType().map(MediaType::toString).orElse("image/jpeg"));
            String filename = UUID.randomUUID() + ext;
            Path dest = dir.resolve(filename);
            Files.write(dest, file.getBytes());

            String url = "http://localhost:8080/media/" + prefix + "/" + filename;
            log.info("[LOCAL MEDIA] Saved to: {}, url: {}", dest, url);
            return url;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file locally", e);
        }
    }

    private void validateFile(CompletedFileUpload file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image exceeds 5MB limit");
        }
        String contentType = file.getContentType().map(MediaType::toString).orElse("");
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG, and WEBP images are allowed");
        }
    }

    private String getExtension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
