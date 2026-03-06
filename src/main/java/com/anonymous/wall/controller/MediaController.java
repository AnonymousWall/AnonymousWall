package com.anonymous.wall.controller;

import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.server.types.files.StreamedFile;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/api/v1/media")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Requires(env = "prod")
public class MediaController {

    private static final Logger log = LoggerFactory.getLogger(MediaController.class);

    private final String namespace;
    private final String bucketName;
    private volatile ObjectStorageClient objectStorageClient;

    public MediaController(
            @Property(name = "oci.media.namespace") String namespace,
            @Property(name = "oci.media.bucket-name") String bucketName) {
        this.namespace = namespace;
        this.bucketName = bucketName;
    }

    @PostConstruct
    void init() {
        this.objectStorageClient = ObjectStorageClient.builder()
                .build(InstancePrincipalsAuthenticationDetailsProvider.builder().build());
    }

    @PreDestroy
    void destroy() {
        if (objectStorageClient != null) {
            objectStorageClient.close();
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
            log.info("Fetching media object: {}", objectName);
            GetObjectResponse response = objectStorageClient.getObject(request);

            String contentType = response.getContentType() != null
                    ? response.getContentType()
                    : "application/octet-stream";

            return HttpResponse.ok(
                    new StreamedFile(response.getInputStream(), MediaType.of(contentType))
            ).header("Cache-Control", "private, max-age=86400"); // cache for 24 hours

        } catch (Exception e) {
            log.warn("Failed to fetch media object '{}': {}", objectName, e.getMessage());
            return HttpResponse.notFound();
        }
    }
}
