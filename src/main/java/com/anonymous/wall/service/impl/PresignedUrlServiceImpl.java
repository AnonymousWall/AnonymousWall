package com.anonymous.wall.service.impl;

import com.anonymous.wall.config.OciObjectStorageClientProvider;
import com.anonymous.wall.service.base.PresignedUrlService;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails;
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest;
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Singleton
@Requires(env = "prod")
public class PresignedUrlServiceImpl implements PresignedUrlService {

    private static final Logger log = LoggerFactory.getLogger(PresignedUrlServiceImpl.class);
    private static final int EXPIRY_MINUTES = 15;

    @Value("${oci.media.namespace}")
    private String namespace;

    @Value("${oci.media.bucket-name}")
    private String bucketName;

    @Inject
    private OciObjectStorageClientProvider objectStorageClientProvider;

    @Override
    public PresignedUploadResult generateUploadUrl(String folder, String originalFilename) {
        String extension = extractExtension(originalFilename);
        String objectName = folder + "/" + UUID.randomUUID() + "." + extension;

        CreatePreauthenticatedRequestDetails details = CreatePreauthenticatedRequestDetails.builder()
                .name("upload-" + UUID.randomUUID())
                .objectName(objectName)
                .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectWrite)
                .timeExpires(Date.from(Instant.now().plus(EXPIRY_MINUTES, ChronoUnit.MINUTES)))
                .build();

        CreatePreauthenticatedRequestRequest request = CreatePreauthenticatedRequestRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucketName)
                .createPreauthenticatedRequestDetails(details)
                .build();

        CreatePreauthenticatedRequestResponse response = objectStorageClientProvider.getClient().createPreauthenticatedRequest(request);

        String parUrl = response.getPreauthenticatedRequest().getFullPath();

        log.debug("Generated presigned URL for objectName={}, expiresIn={}min", objectName, EXPIRY_MINUTES);
        return new PresignedUploadResult(parUrl, objectName);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}