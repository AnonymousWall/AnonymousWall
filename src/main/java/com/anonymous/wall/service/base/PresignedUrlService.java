package com.anonymous.wall.service.base;

public interface PresignedUrlService {

    /**
     * Generate a pre-authenticated OCI URL for a client to PUT an object directly.
     * Returns the PAR URL and the objectName to store in the DB.
     */
    PresignedUploadResult generateUploadUrl(String folder, String originalFilename);

    record PresignedUploadResult(String uploadUrl, String objectName) {}
}