package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.CommentReport;
import com.anonymous.wall.entity.PostReport;
import com.anonymous.wall.repository.CommentReportRepository;
import com.anonymous.wall.repository.PostReportRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of admin report management service
 */
@Singleton
public class AdminReportServiceImpl implements AdminReportService {
    
    private static final Logger log = LoggerFactory.getLogger(AdminReportServiceImpl.class);
    
    @Inject
    private PostReportRepository postReportRepository;
    
    @Inject
    private CommentReportRepository commentReportRepository;
    
    @Override
    public Page<PostReport> getAllPostReports(Pageable pageable) {
        log.info("Admin fetching all post reports with pagination: page={}, size={}", 
                 pageable.getNumber(), pageable.getSize());
        return postReportRepository.findAll(pageable);
    }
    
    @Override
    public Page<CommentReport> getAllCommentReports(Pageable pageable) {
        log.info("Admin fetching all comment reports with pagination: page={}, size={}", 
                 pageable.getNumber(), pageable.getSize());
        return commentReportRepository.findAll(pageable);
    }
}
