package com.anonymous.wall.util;

import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Singleton
@Requires(env = "prod")
public class OciMediaUtil implements MediaUtilInterface {
    private static final Logger log = LoggerFactory.getLogger(OciMediaUtil.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final ObjectStorageClient objectStorageClient;
    private final String namespace;
    private final String bucketName;

    public OciMediaUtil(
            @Property(name = "oci.media.namespace") String namespace,
            @Property(name = "oci.media.bucket-name") String bucketName) {
        this.namespace = namespace;
        this.bucketName = bucketName;
        this.objectStorageClient = ObjectStorageClient.builder()
                .build(InstancePrincipalsAuthenticationDetailsProvider.builder().build());
    }

    @Override
    public String uploadPostImage(CompletedFileUpload file, UUID userId) {
        return upload(file, "posts");
    }

    @Override
    public String uploadChatImage(CompletedFileUpload file, UUID userId) {
        return upload(file, "chat");
    }

    @Override
    public String uploadMarketplaceImage(CompletedFileUpload file, UUID userId) {
        return upload(file, "marketplace");
    }

    private String upload(CompletedFileUpload file, String prefix) {
        validateFile(file);
        try {
            String ext = getExtension(file.getContentType().map(MediaType::toString).orElse("image/jpeg"));
            String objectName = prefix + "/" + UUID.randomUUID() + ext;

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(objectName)
                    .contentType(file.getContentType().map(MediaType::toString).orElse("image/jpeg"))
                    .contentLength(file.getSize())
                    .putObjectBody(new ByteArrayInputStream(file.getBytes()))
                    .build();

            objectStorageClient.putObject(putRequest);

            log.info("Uploaded to OCI: {}", objectName);
            return objectName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image to OCI", e);
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
