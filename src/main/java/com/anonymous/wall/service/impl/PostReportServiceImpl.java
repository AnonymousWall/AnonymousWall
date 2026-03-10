package com.anonymous.wall.service.impl;

import com.anonymous.wall.entity.PostReport;
import com.anonymous.wall.repository.PostReportRepository;
import com.anonymous.wall.service.base.PostReportService;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class PostReportServiceImpl implements PostReportService {
    @Inject
    private PostReportRepository postReportRepository;

    @Override
    @Transactional
    public boolean existsByPostIdAndReporterUserId(UUID postId, UUID reporterUserId) {
        return postReportRepository.existsByPostIdAndReporterUserId(postId, reporterUserId);
    }
    @Override
    @Transactional
    public PostReport save(PostReport report) {
        return postReportRepository.save(report);
    }
}
