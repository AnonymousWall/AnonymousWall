package com.anonymous.wall.controller;

import com.anonymous.wall.config.OciObjectStorageClientProvider;
import com.anonymous.wall.model.PresignRequest;
import com.anonymous.wall.model.PresignResponse;
import com.anonymous.wall.service.base.PresignedUrlService;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.server.types.files.StreamedFile;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

@Controller("/api/v1/media")
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.BLOCKING)
@Requires(env = "prod")
public class MediaController {

    private static final Logger log = LoggerFactory.getLogger(MediaController.class);
    private static final Set<String> ALLOWED_FOLDERS = Set.of("posts", "marketplace", "chat");

    private final String namespace;
    private final String bucketName;
    @Inject
    private OciObjectStorageClientProvider objectStorageClientProvider;

    @Inject
    private PresignedUrlService presignedUrlService;

    public MediaController(
            @Property(name = "oci.media.namespace") String namespace,
            @Property(name = "oci.media.bucket-name") String bucketName) {
        this.namespace = namespace;
        this.bucketName = bucketName;
    }

    /**
     * Generate a pre-authenticated OCI URL for direct client upload.
     * Client PUTs the file directly to the returned uploadUrl,
     * then passes the returned objectName to the relevant create endpoint.
     */
    @Post("/presign")
    public HttpResponse<Object> getPresignedUrl(@Body PresignRequest request) {
        try {
            if (request.getFolder() == null) {
                return HttpResponse.badRequest(error("Invalid folder. Must be one of: " + ALLOWED_FOLDERS));
            }
            String folder = request.getFolder().getValue();
            if (!ALLOWED_FOLDERS.contains(folder)) {
                return HttpResponse.badRequest(error("Invalid folder. Must be one of: " + ALLOWED_FOLDERS));
            }
            if (request.getFilename() == null || request.getFilename().isBlank()) {
                return HttpResponse.badRequest(error("Filename is required"));
            }

            PresignedUrlService.PresignedUploadResult result =
                    presignedUrlService.generateUploadUrl(folder, request.getFilename());

            log.info("POST /media/presign - Generated presigned URL for folder={}", folder);
            PresignResponse presignResponse = new PresignResponse();
            presignResponse.setUploadUrl(result.uploadUrl());
            presignResponse.setObjectName(result.objectName());
            return HttpResponse.ok(presignResponse);
        } catch (Exception e) {
            log.error("POST /media/presign - Error generating presigned URL", e);
            return HttpResponse.serverError(error("Failed to generate upload URL"));
        }
    }

    /**
     * Proxy endpoint for private OCI bucket objects.
     * {+objectName} captures the full path including slashes,
     * e.g. "posts/uuid.jpg", "chat/uuid.png", "marketplace/uuid.webp".
     */
    @Get("/{+objectName}")
    public HttpResponse<StreamedFile> getMedia(String objectName) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucketName)
                    .objectName(objectName)
                    .build();
            log.debug("Fetching media object: {}", objectName);
            GetObjectResponse response = objectStorageClientProvider.getClient().getObject(request);

            String contentType = response.getContentType() != null
                    ? response.getContentType()
                    : "application/octet-stream";

            return HttpResponse.ok(
                    new StreamedFile(response.getInputStream(), MediaType.of(contentType))
            ).header("Cache-Control", "private, max-age=86400");

        } catch (Exception e) {
            log.warn("Failed to fetch media object '{}': {}", objectName, e.getMessage());
            return HttpResponse.notFound();
        }
    }

    private Map<String, String> error(String message) {
        return Map.of("error", message);
    }
}