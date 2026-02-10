# Post Hide Propagation - Implementation Documentation

## Overview
This document describes the implementation of **Asynchronous Event-Driven Update** for propagating post hide operations to associated comments. This implementation follows the same pattern as the profile name update feature.

**Note**: Unhide functionality remains synchronous as it is part of the soft-delete pattern and users are not allowed to unhide posts.

## Architecture

### Event-Driven Design
The implementation uses Micronaut's built-in event system to achieve asynchronous comment hiding:

```
Post Hide → Event Published → Async Listener → Hide Comments
```

## Components

### 1. PostHiddenEvent (`event/PostHiddenEvent.java`)
- **Purpose**: Represents a post hidden event
- **Fields**:
  - `postId`: UUID of the post that was hidden
  - `userId`: UUID of the user who hid the post

### 2. PostsServiceImpl (`service/PostsServiceImpl.java`)
- **Purpose**: Handles post hide operations and publishes events
- **Key Methods**:
  - `hidePost(UUID postId, UUID userId)`:
    - Hides post synchronously
    - Publishes `PostHiddenEvent` for async comment hiding
    - Returns immediately (non-blocking)
  - `unhidePost(UUID postId, UUID userId)`:
    - Unhides post synchronously
    - Unhides comments synchronously (within same transaction)
    - Uses traditional transactional approach

### 3. PostHideEventListener (`listener/PostHideEventListener.java`)
- **Purpose**: Listens for post hide events and updates comments
- **Annotations**:
  - `@Singleton`: Ensures single instance
  - `@Async`: Executes event handling asynchronously
  - `@Transactional`: Ensures database operations run in their own transaction context
- **Behavior**:
  - For `PostHiddenEvent`: Updates all comments via `CommentRepository.updateByPostId(postId, true)`
  - Logs errors but doesn't throw exceptions (fault-tolerant)
- **Important**: The `@Transactional` annotation is critical to avoid "Closed Statement" errors when the async method tries to access the database after the original transaction has completed

### 4. Repository Updates
#### CommentRepository (`repository/CommentRepository.java`)
- Uses existing `updateByPostId(UUID postId, boolean hidden)` method
- Uses Micronaut Data's method naming convention for bulk updates
- Automatically generates SQL: `UPDATE comments SET hidden = ? WHERE post_id = ?`

## Flow Diagram

```
┌─────────────────────┐
│  Hide Post Request  │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  PostsServiceImpl   │
│  .hidePost()        │
└──────────┬──────────┘
           │
           ├─────────────────────────┐
           │                         │
           ▼                         ▼
┌─────────────────────┐    ┌────────────────────────┐
│  Update Post        │    │  Publish Event         │
│  (Synchronous)      │    │  PostHiddenEvent       │
└──────────┬──────────┘    └────────────┬───────────┘
           │                             │
           ▼                             │
┌─────────────────────┐                 │
│  Return Updated     │                 │
│  Post (Immediate)   │                 │
└─────────────────────┘                 │
                                        │
                        ┌───────────────┘
                        │
                        ▼
            ┌───────────────────────────┐
            │  Event Listener           │
            │  @Async Processing        │
            └───────────┬───────────────┘
                        │
                        ▼
            ┌───────────────────────────┐
            │  Hide Comments            │
            │  (Asynchronous)           │
            └───────────────────────────┘
```

## Benefits

### ✅ Non-Blocking
- User gets immediate response after hiding a post
- No waiting for bulk comment updates to complete

### ✅ Better UX
- Post state change appears instant to the user
- Background comment propagation is transparent

### ✅ Scalable
- Handles large numbers of comments efficiently
- Async execution manages concurrent updates

### ✅ Simple Implementation
- Uses Micronaut's native event system
- No external dependencies required
- Easy to understand and maintain

### ✅ Consistent Pattern
- Follows the same pattern as profile name update
- Maintains architectural consistency

### ✅ Industry Standard
- Event-driven architecture is a proven pattern
- Follows microservices best practices
- Aligns with eventual consistency model

## Trade-offs

### ❌ Eventual Consistency
- Brief period where comments may not reflect the post's hidden state
- Acceptable for non-critical operations like hiding content

### ❌ No Guaranteed Order
- Multiple rapid hide/unhide operations may process out of order
- Mitigated by storing the hidden state in event

### ❌ Error Handling
- Failed updates are logged but not retried automatically
- Consider adding retry logic or dead letter queue for production

### ❌ Lost on Server Restart
- In-memory events don't persist across restarts
- For critical updates, consider using persistent message queue

### ✅ Transaction Management
- Async listener must use `@Transactional` to create new database connection
- Without this, "Closed Statement" errors occur when async code tries to use the original transaction's closed connection
- This pattern applies to all async event listeners that perform database operations

## Testing

### Unit Tests
1. **PostHiddenEventTest**: Tests event creation and data access
2. **PostHideEventListenerTest**: Tests event listener behavior with mocks

### Integration Tests
1. **PostsServiceHidePostTests**: Tests service-level hide with async comment updates (unhide remains synchronous)
2. **PostsControllerHidePostTests**: Tests controller-level hide with async comment updates (unhide remains synchronous)

### Test Coverage
- Event creation with various inputs
- Event listener updates comments correctly
- Event listener handles exceptions gracefully
- Service publishes events correctly for hide operations
- All edge cases covered (no comments, multiple comments, etc.)

### Test Considerations
- Hide tests include 500ms wait for async processing
- Unhide tests remain synchronous (no wait needed)
- Tests verify eventual consistency of comment hidden state for hide operations
- Tests verify idempotency of hide/unhide operations

## Performance Considerations

### Database Impact
- Bulk SQL updates are efficient (single query per operation)
- Uses indexed `post_id` column for fast lookups
- No N+1 query problem

### Async Execution
- Thread pool managed by Micronaut
- Suitable for medium-scale deployments
- Can scale thread pool based on load

### Memory Usage
- Event objects are lightweight (2 UUID fields)
- No data caching required
- Garbage collected after processing

## Production Recommendations

### Monitoring
- Add metrics for event processing time
- Monitor event queue depth
- Alert on failed updates

### Retry Logic
Consider adding:
```java
@Retryable(attempts = "3", delay = "2s")
public void onApplicationEvent(PostHiddenEvent event) {
    // Update logic
}
```

### Dead Letter Queue
For critical errors, log to a separate queue for manual review:
```java
catch (Exception e) {
    deadLetterQueue.send(event);
    log.error("Comment update failed, sent to DLQ", e);
}
```

### Persistent Events
For production, consider using:
- Redis Pub/Sub
- Apache Kafka
- RabbitMQ
- AWS SQS/SNS

## Comparison with Previous Implementation

### Before (Synchronous)
```java
// Hide post and comments in same transaction
post.setHidden(true);
postRepository.update(post);
commentRepository.updateByPostId(postId, true);  // Blocks until complete
return post;
```

### After (Asynchronous for hide only)
```java
// Hide post synchronously, comments hidden async
post.setHidden(true);
Post updatedPost = postRepository.update(post);

// Publish event for async comment hiding
postHiddenEventPublisher.publishEvent(new PostHiddenEvent(postId, userId));
return updatedPost;  // Returns immediately

// Unhide remains synchronous
post.setHidden(false);
Post updatedPost = postRepository.update(post);
commentRepository.updateByPostId(postId, false);  // Synchronous
return updatedPost;
```

## Migration Guide

### Deployment Steps
1. Deploy code changes (backward compatible)
2. Monitor logs for event processing
3. Verify hide operations work correctly

### Rollback Plan
1. Revert code changes
2. Hide operations will revert to synchronous behavior
3. No data inconsistency risk

### Data Consistency Check
After deployment, verify:
- Hidden posts have all comments hidden
- No orphaned comment states for hide operations

## Conclusion

This implementation provides a balanced solution that:
- Maintains good user experience (instant hide operations)
- Scales to medium-sized deployments
- Uses industry-standard patterns for hide operations
- Keeps unhide synchronous for data integrity (soft-delete pattern)
- Requires no external dependencies
- Is simple to understand and maintain
- Follows existing architectural patterns in the codebase

The eventual consistency trade-off is acceptable for hide operations, which are not critical data and don't require strict consistency guarantees. Unhide operations remain synchronous to ensure data integrity.
