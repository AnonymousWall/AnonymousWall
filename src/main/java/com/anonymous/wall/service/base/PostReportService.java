package com.anonymous.wall.service.base;

import com.anonymous.wall.entity.PostReport;

import java.util.UUID;

public interface PostReportService {
    boolean existsByPostIdAndReporterUserId(UUID postId, UUID reporterUserId);
    PostReport save(PostReport report);
}
