# Profile Name Update Propagation - Implementation Documentation

## Overview
This document describes the implementation of **Solution 2: Asynchronous Event-Driven Update (Balanced)** for propagating profile name changes from users to their posts and comments.

## Architecture

### Event-Driven Design
The implementation uses Micronaut's built-in event system to achieve asynchronous profile name propagation:

```
User Profile Update → Event Published → Async Listener → Update Posts & Comments
```

## Components

### 1. ProfileNameChangedEvent (`event/ProfileNameChangedEvent.java`)
- **Purpose**: Represents a profile name change event
- **Fields**:
  - `userId`: UUID of the user whose profile name changed
  - `oldName`: Previous profile name
  - `newName`: New profile name

### 2. UserServiceImpl (`service/UserServiceImpl.java`)
- **Purpose**: Handles user profile updates and publishes events
- **Key Method**: `updateProfileName(UUID userId, String profileName)`
  - Updates user profile synchronously
  - Publishes `ProfileNameChangedEvent` for async propagation
  - Returns immediately (non-blocking)

### 3. ProfileNameUpdateEventListener (`listener/ProfileNameUpdateEventListener.java`)
- **Purpose**: Listens for profile name change events and updates posts/comments
- **Annotations**:
  - `@Singleton`: Ensures single instance
  - `@Async`: Executes event handling asynchronously
- **Behavior**:
  - Updates all posts by the user via `PostRepository.updateProfileNameByUserId()`
  - Updates all comments by the user via `CommentRepository.updateProfileNameByUserId()`
  - Logs errors but doesn't throw exceptions (fault-tolerant)

### 4. Repository Updates
#### PostRepository (`repository/PostRepository.java`)
- Added `updateProfileNameByUserId(UUID userId, String profileName)` method
- Uses Micronaut Data's method naming convention for bulk updates
- Automatically generates SQL: `UPDATE posts SET profile_name = ? WHERE user_id = ?`

#### CommentRepository (`repository/CommentRepository.java`)
- Added `updateProfileNameByUserId(UUID userId, String profileName)` method
- Uses Micronaut Data's method naming convention for bulk updates
- Automatically generates SQL: `UPDATE comments SET profile_name = ? WHERE user_id = ?`

### 5. Configuration (`resources/application.properties`)
- Added async executor configuration:
  ```properties
  micronaut.executors.scheduled.type=scheduled
  micronaut.executors.scheduled.core-pool-size=2
  ```

## Flow Diagram

```
┌─────────────────────┐
│  User Profile       │
│  Update Request     │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  UserServiceImpl    │
│  .updateProfileName │
└──────────┬──────────┘
           │
           ├─────────────────────────┐
           │                         │
           ▼                         ▼
┌─────────────────────┐    ┌────────────────────────┐
│  Update User        │    │  Publish Event         │
│  (Synchronous)      │    │  ProfileNameChanged    │
└──────────┬──────────┘    └────────────┬───────────┘
           │                             │
           ▼                             │
┌─────────────────────┐                 │
│  Return Updated     │                 │
│  User (Immediate)   │                 │
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
            ┌───────────┴────────────┐
            │                        │
            ▼                        ▼
┌──────────────────────┐  ┌──────────────────────┐
│  Update Posts        │  │  Update Comments     │
│  (Asynchronous)      │  │  (Asynchronous)      │
└──────────────────────┘  └──────────────────────┘
```

## Benefits

### ✅ Non-Blocking
- User gets immediate response after profile update
- No waiting for bulk updates to complete

### ✅ Better UX
- Profile change appears instant to the user
- Background propagation is transparent

### ✅ Scalable
- Handles large numbers of posts/comments efficiently
- Thread pool manages concurrent updates

### ✅ Simple Implementation
- Uses Micronaut's native event system
- No external dependencies required
- Easy to understand and maintain

### ✅ Industry Standard
- Event-driven architecture is a proven pattern
- Follows microservices best practices
- Aligns with eventual consistency model

## Trade-offs

### ❌ Eventual Consistency
- Brief period where posts/comments may show old profile name
- Acceptable for non-critical data like display names

### ❌ No Guaranteed Order
- Multiple rapid updates may process out of order
- Mitigated by capturing old/new names in event

### ❌ Error Handling
- Failed updates are logged but not retried automatically
- Consider adding retry logic or dead letter queue for production

### ❌ Lost on Server Restart
- In-memory events don't persist across restarts
- For critical updates, consider using persistent message queue

## Testing

### Unit Tests
1. **ProfileNameChangedEventTest**: Tests event creation and data access
2. **ProfileNameUpdateEventListenerTest**: Tests event listener behavior with mocks
3. **UserServiceImplTest**: Tests event publishing during profile updates

### Test Coverage
- Event creation with various inputs (null, empty, valid)
- Event listener updates both posts and comments
- Event listener handles exceptions gracefully
- User service publishes events correctly
- All edge cases covered (null names, missing users)

## Performance Considerations

### Database Impact
- Bulk SQL updates are efficient (single query per entity type)
- Uses indexed `user_id` columns for fast lookups
- No N+1 query problem

### Async Execution
- Thread pool size: 2 (configurable)
- Suitable for medium-scale deployments
- Can scale thread pool based on load

### Memory Usage
- Event objects are lightweight (3 fields)
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
public void onApplicationEvent(ProfileNameChangedEvent event) {
    // Update logic
}
```

### Dead Letter Queue
For critical errors, log to a separate queue for manual review:
```java
catch (Exception e) {
    deadLetterQueue.send(event);
    log.error("Profile update failed, sent to DLQ", e);
}
```

### Persistent Events
For production, consider using:
- Redis Pub/Sub
- Apache Kafka
- RabbitMQ
- AWS SQS/SNS

## Migration Guide

### Before Deployment
1. Ensure all posts/comments have current profile names
2. Run data consistency check
3. Back up database

### Deployment Steps
1. Deploy code changes
2. Monitor logs for event processing
3. Verify profile updates propagate correctly

### Rollback Plan
1. Revert code changes
2. Profile updates will work but won't propagate
3. No data corruption risk

## Conclusion

This implementation provides a balanced solution that:
- Maintains good user experience (instant updates)
- Scales to medium-sized deployments
- Uses industry-standard patterns
- Requires no external dependencies
- Is simple to understand and maintain

The eventual consistency trade-off is acceptable for profile names, which are not critical data and don't require strict consistency guarantees.
