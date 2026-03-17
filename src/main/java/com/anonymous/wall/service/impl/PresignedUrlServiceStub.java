package com.anonymous.wall.service.impl;

import com.anonymous.wall.service.base.PresignedUrlService;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
@Requires(notEnv = "prod")
public class PresignedUrlServiceStub implements PresignedUrlService {

    @Override
    public PresignedUploadResult generateUploadUrl(String folder, String originalFilename) {
        String objectName = folder + "/stub-" + UUID.randomUUID() + ".jpg";
        return new PresignedUrlService.PresignedUploadResult("https://stub-upload-url/" + objectName, objectName);
    }
}
