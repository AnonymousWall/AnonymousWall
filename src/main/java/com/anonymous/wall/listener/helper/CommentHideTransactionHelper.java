package com.anonymous.wall.listener.helper;

import com.anonymous.wall.service.base.CommentsService;
import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class CommentHideTransactionHelper {

    @Inject
    private CommentsService commentsService;

    @Transactional(propagation = TransactionDefinition.Propagation.REQUIRES_NEW)
    public void hideCommentsByParent(String parentType, UUID parentId, boolean hidden) {
        commentsService.updateByParentTypeAndParentId(parentType, parentId, hidden);
    }
}