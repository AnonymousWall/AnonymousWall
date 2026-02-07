# Profile Name Update Propagation - Research & Solutions

## Executive Summary

This document presents several architectural solutions for efficiently propagating profile name changes across all associated Posts and Comments in the AnonymousWall system. The current system stores `profile_name` as a denormalized field in the `users`, `posts`, and `comments` tables, which creates a data consistency challenge when users update their profile name.

---

## Current Architecture Analysis

### Database Schema
- **users table**: Contains `profile_name` (source of truth)
- **posts table**: Contains denormalized `profile_name` field
- **comments table**: Contains denormalized `profile_name` field

### Technology Stack
- **Framework**: Micronaut 4.10.7 (Java 21)
- **Database**: MySQL with Micronaut Data JDBC
- **Available Tools**: 
  - Micronaut Reactor (reactive programming support)
  - Micronaut JMS (message queue support)
  - Micronaut Retry (retry mechanisms)

### Current Implementation
The `UserServiceImpl.updateProfileName()` method currently only updates the `users` table, leaving Posts and Comments with the old profile name.

---

## Solution 1: Synchronous Batch Update (Simple & Direct)

### Overview
Update all Posts and Comments in the same transaction when the user changes their profile name.

### Implementation Approach
```java
@Transactional
public UserEntity updateProfileName(UUID userId, String profileName) {
    // 1. Update user profile
    UserEntity user = userRepository.update(user);
    
    // 2. Batch update all posts
    postRepository.updateProfileNameByUserId(userId, profileName);
    
    // 3. Batch update all comments
    commentRepository.updateProfileNameByUserId(userId, profileName);
    
    return user;
}
```

### Database Queries Needed
```sql
-- Update posts
UPDATE posts SET profile_name = ?, updated_at = CURRENT_TIMESTAMP 
WHERE user_id = ?;

-- Update comments
UPDATE comments SET profile_name = ? 
WHERE user_id = ?;
```

### Pros
✅ **Simplest to implement** - No new infrastructure needed  
✅ **Immediate consistency** - All changes happen in single transaction  
✅ **ACID guarantees** - Database handles rollback on failure  
✅ **No additional dependencies** - Uses existing Micronaut Data JDBC  

### Cons
❌ **Blocks user request** - User waits for all updates to complete  
❌ **Long transaction** - Can lock tables for users with many posts/comments  
❌ **Scalability issues** - Poor performance for power users with 1000+ posts  
❌ **Timeout risk** - HTTP request may timeout on large updates  

### When to Use
- **Small to medium user base** (< 100K users)
- **Infrequent profile changes** (most users change name rarely)
- **Low post/comment volume per user** (< 100 posts per user average)
- **Simplicity is priority** over performance

### Estimated Effort
- **Implementation**: 2-4 hours
- **Testing**: 2 hours
- **Total**: 4-6 hours

---

## Solution 2: Asynchronous Event-Driven Update (Balanced)

### Overview
Update the user profile synchronously, then trigger an asynchronous event to update Posts and Comments in the background.

### Implementation Approach
```java
// 1. Service layer
@Transactional
public UserEntity updateProfileName(UUID userId, String profileName) {
    UserEntity user = userRepository.update(user);
    
    // Fire async event
    eventPublisher.publishEvent(
        new ProfileNameChangedEvent(userId, oldName, profileName)
    );
    
    return user;
}

// 2. Event listener
@EventListener
@Async
public void onProfileNameChanged(ProfileNameChangedEvent event) {
    postRepository.updateProfileNameByUserId(event.getUserId(), event.getNewName());
    commentRepository.updateProfileNameByUserId(event.getUserId(), event.getNewName());
}
```

### Pros
✅ **Non-blocking** - User gets immediate response  
✅ **Better UX** - No waiting for bulk updates  
✅ **Scalable** - Handles large numbers of posts/comments  
✅ **Built-in support** - Micronaut has native event system  
✅ **Simple implementation** - No external dependencies needed  

### Cons
❌ **Eventual consistency** - Brief period where data is inconsistent  
❌ **No guaranteed order** - Multiple updates may race  
❌ **Limited retry logic** - Needs manual retry implementation  
❌ **Lost on server restart** - In-memory events don't persist  

### When to Use
- **Medium user base** (100K - 1M users)
- **Moderate consistency requirements** - Few seconds delay acceptable
- **Want simplicity** but need better performance than Solution 1
- **No external message queue infrastructure**

### Estimated Effort
- **Implementation**: 4-6 hours
- **Testing**: 3 hours
- **Total**: 7-9 hours

---

## Solution 3: Message Queue with JMS (Production-Ready)

### Overview
Use Micronaut JMS (already included in project) with a message broker to reliably process profile updates asynchronously.

### Implementation Approach
```java
// 1. Update service
@Transactional
public UserEntity updateProfileName(UUID userId, String profileName) {
    UserEntity user = userRepository.update(user);
    
    // Send message to queue
    jmsProducer.send("profile-name-updates", 
        new ProfileUpdateMessage(userId, profileName)
    );
    
    return user;
}

// 2. Message consumer
@JMSListener(destination = "profile-name-updates")
public void processProfileUpdate(ProfileUpdateMessage message) {
    try {
        postRepository.updateProfileNameByUserId(
            message.getUserId(), 
            message.getNewName()
        );
        commentRepository.updateProfileNameByUserId(
            message.getUserId(), 
            message.getNewName()
        );
    } catch (Exception e) {
        // Message will be redelivered automatically
        throw e;
    }
}
```

### Infrastructure Options
- **ActiveMQ Artemis** - Lightweight, embeddable
- **RabbitMQ** - Popular, cloud-ready
- **Amazon SQS** - Managed AWS service
- **Azure Service Bus** - Managed Azure service

### Pros
✅ **Reliable delivery** - Messages persist if server crashes  
✅ **Automatic retry** - Dead letter queue for failed messages  
✅ **Non-blocking** - Instant response to user  
✅ **Scalable** - Can add multiple consumers  
✅ **Production-ready** - Battle-tested pattern  
✅ **Already included** - Micronaut JMS dependency exists  

### Cons
❌ **Infrastructure complexity** - Needs message broker deployment  
❌ **Operational overhead** - Monitor queues, dead letter queues  
❌ **Cost** - Managed services have ongoing costs  
❌ **Eventual consistency** - Not immediate updates  

### When to Use
- **Large user base** (1M+ users)
- **High reliability requirements**
- **Production workloads** with SLA requirements
- **Already using message queues** for other features
- **Need guaranteed delivery** and retry logic

### Estimated Effort
- **Implementation**: 6-8 hours
- **Infrastructure setup**: 4-8 hours (depends on broker choice)
- **Testing & monitoring**: 4 hours
- **Total**: 14-20 hours

---

## Solution 4: Reactive Streams with Backpressure (High Performance)

### Overview
Use Micronaut Reactor (already included) to process updates as a reactive stream with automatic backpressure handling.

### Implementation Approach
```java
// 1. Service layer
@Transactional
public Mono<UserEntity> updateProfileName(UUID userId, String profileName) {
    return Mono.fromCallable(() -> userRepository.update(user))
        .doOnSuccess(user -> 
            propagateProfileNameChange(userId, profileName)
        );
}

// 2. Reactive propagation
private Flux<Void> propagateProfileNameChange(UUID userId, String name) {
    return Flux.merge(
        // Update posts in batches
        postRepository.findByUserId(userId)
            .buffer(100) // Process 100 posts at a time
            .flatMap(batch -> updatePostsBatch(batch, name)),
        
        // Update comments in batches
        commentRepository.findByUserId(userId)
            .buffer(100)
            .flatMap(batch -> updateCommentsBatch(batch, name))
    )
    .subscribeOn(Schedulers.boundedElastic())
    .onErrorResume(e -> {
        log.error("Profile propagation failed", e);
        return Mono.empty();
    });
}
```

### Pros
✅ **Memory efficient** - Streams process data without loading all into memory  
✅ **Backpressure** - Automatically throttles when database is slow  
✅ **Parallel processing** - Can update posts and comments simultaneously  
✅ **Non-blocking** - Doesn't tie up threads  
✅ **Fine-grained control** - Can tune batch sizes and concurrency  
✅ **Already available** - Micronaut Reactor is included  

### Cons
❌ **Complexity** - Reactive programming has steeper learning curve  
❌ **Debugging difficulty** - Stack traces are harder to read  
❌ **Not fully reactive** - JDBC is blocking (need R2DBC for full benefits)  
❌ **Migration effort** - May need to refactor other code to be reactive  

### When to Use
- **Very large user base** (10M+ users)
- **High-performance requirements**
- **Team has reactive programming experience**
- **Processing millions of posts/comments**
- **Want fine-grained control** over concurrency

### Estimated Effort
- **Implementation**: 10-12 hours
- **Learning curve**: 8-16 hours (if team is new to reactive)
- **Testing**: 6 hours
- **Total**: 24-34 hours

---

## Solution 5: Hybrid Approach with Batch + Queue (Recommended)

### Overview
Combine synchronous batch updates for small datasets with async queue processing for large datasets. Best of both worlds.

### Implementation Approach
```java
@Transactional
public UserEntity updateProfileName(UUID userId, String profileName) {
    UserEntity user = userRepository.update(user);
    
    // Count posts and comments
    long totalRecords = postRepository.countByUserId(userId) + 
                       commentRepository.countByUserId(userId);
    
    if (totalRecords < THRESHOLD) {
        // Small dataset: update synchronously
        postRepository.updateProfileNameByUserId(userId, profileName);
        commentRepository.updateProfileNameByUserId(userId, profileName);
    } else {
        // Large dataset: use async processing
        jmsProducer.send("profile-name-updates", 
            new ProfileUpdateMessage(userId, profileName)
        );
    }
    
    return user;
}
```

### Configuration
```yaml
profile-update:
  sync-threshold: 50  # Update sync if < 50 total posts+comments
  batch-size: 100     # Process 100 records per batch in async mode
```

### Pros
✅ **Best UX** - Fast users get instant updates, slow users don't timeout  
✅ **Adaptive** - Automatically chooses best strategy  
✅ **Consistent for most** - 90%+ of users likely under threshold  
✅ **Scalable** - Handles power users gracefully  
✅ **Balanced complexity** - Not too simple, not too complex  

### Cons
❌ **Two code paths** - Need to maintain both strategies  
❌ **Testing complexity** - Must test both paths  
❌ **Infrastructure needed** - Still needs message queue for large updates  

### When to Use
- **Growing user base** - Start simple, scale as needed
- **Mixed usage patterns** - Some power users, mostly casual users
- **Want good UX** for everyone
- **Best overall solution** for most real-world scenarios

### Estimated Effort
- **Implementation**: 8-10 hours
- **Infrastructure setup**: 4-8 hours
- **Testing**: 5 hours
- **Total**: 17-23 hours

---

## Solution 6: Database-Level Solution (Alternative)

### Overview
Use database triggers or stored procedures to automatically update Posts and Comments when the user profile changes.

### Implementation Approach
```sql
-- MySQL Trigger
CREATE TRIGGER update_posts_on_profile_change
AFTER UPDATE ON users
FOR EACH ROW
BEGIN
    IF NEW.profile_name != OLD.profile_name THEN
        UPDATE posts 
        SET profile_name = NEW.profile_name,
            updated_at = CURRENT_TIMESTAMP
        WHERE user_id = NEW.id;
        
        UPDATE comments 
        SET profile_name = NEW.profile_name
        WHERE user_id = NEW.id;
    END IF;
END;
```

### Pros
✅ **No application code changes** - Logic lives in database  
✅ **Automatic** - Can't forget to update  
✅ **Transactional** - All changes in same transaction  
✅ **Database-enforced consistency**  

### Cons
❌ **Database coupling** - Logic hidden from application layer  
❌ **Portability issues** - Trigger syntax varies by database  
❌ **Debugging difficulty** - Harder to trace and test  
❌ **Performance** - Can slow down all user updates  
❌ **Long locks** - Blocks users table during updates  
❌ **Not recommended** for modern applications  

### When to Use
- **Legacy systems** where changing application code is difficult
- **Database-first architecture**
- **Very small user base** (< 10K users)
- ⚠️ **Generally NOT recommended** for new development

### Estimated Effort
- **Implementation**: 2-3 hours
- **Testing**: 3 hours
- **Total**: 5-6 hours

---

## Solution Comparison Matrix

| Solution | Consistency | Performance | Scalability | Complexity | Infrastructure | Recommended For |
|----------|-------------|-------------|-------------|------------|----------------|-----------------|
| **1. Sync Batch** | Immediate | Poor (blocks) | Low | Very Low | None | Small apps |
| **2. Async Event** | Eventual | Good | Medium | Low | None | Medium apps |
| **3. Message Queue** | Eventual | Excellent | High | Medium | Message Broker | Large apps |
| **4. Reactive Streams** | Eventual | Excellent | Very High | High | None | High-performance |
| **5. Hybrid** | Mixed | Excellent | High | Medium | Message Broker | **Most apps** |
| **6. Database Trigger** | Immediate | Poor | Low | Low | None | Legacy only |

---

## Recommendations

### For AnonymousWall Project

Based on the current tech stack and typical usage patterns of a campus social platform:

#### 🥇 **Primary Recommendation: Solution 5 (Hybrid Approach)**

**Reasoning:**
- Most users will have few posts/comments → instant updates
- Power users won't cause timeouts
- Grows with the platform
- Good balance of complexity and benefits
- Uses existing Micronaut JMS dependency

**Implementation Plan:**
1. Start with threshold of 50 total records
2. Use ActiveMQ Artemis (lightweight, Java-based)
3. Add monitoring for queue depth
4. Implement dead letter queue handling

#### 🥈 **Alternative: Solution 2 (Async Event)**

**If you want to avoid message queue infrastructure:**
- Simpler to deploy (no external dependencies)
- Good enough for most campus social platforms
- Can migrate to Solution 5 later if needed
- Start here if team is small or infrastructure is limited

#### 🥉 **Fallback: Solution 1 (Sync Batch)**

**For MVP or beta testing:**
- Get working quickly
- Prove the feature works
- Migrate to better solution once usage patterns are known
- Add timeout protection (e.g., max 1000 records)

### Migration Strategy

**Phase 1: MVP (1-2 days)**
- Implement Solution 1 with record limit
- Add logging to track update sizes
- Monitor user complaints about slowness

**Phase 2: Optimize (1-2 weeks)**
- Analyze logs to determine threshold
- Implement Solution 5 (Hybrid)
- Keep Solution 1 code as fallback

**Phase 3: Scale (ongoing)**
- Monitor queue metrics
- Tune batch sizes and concurrency
- Add dead letter queue monitoring

---

## Additional Considerations

### Data Consistency Strategy

**Accept Eventual Consistency:**
- Profile name changes are rare (not time-critical)
- Brief inconsistency (< 1 minute) is acceptable
- Posts/comments already have timestamps - can show "updated" indicator

**UI/UX Recommendations:**
1. Show loading spinner: "Updating your posts and comments..."
2. Display success message: "Profile updated! Changes will appear shortly."
3. Add optimistic UI updates in client-side code
4. Cache-bust user's own posts immediately

### Error Handling

**What if update fails?**
1. Log error with user ID for manual investigation
2. Send admin notification for stuck updates
3. Implement retry logic (3 attempts with exponential backoff)
4. Keep track of failed updates in separate table
5. Provide admin UI to manually trigger re-sync

### Performance Optimization

**Database Indexing:**
```sql
-- Essential indexes
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);

-- For faster counting
CREATE INDEX idx_posts_user_id_hidden ON posts(user_id, is_hidden);
CREATE INDEX idx_comments_user_id_hidden ON comments(user_id, is_hidden);
```

**Query Optimization:**
- Use batch updates (100-1000 records per query)
- Consider pagination for very large updates
- Add query timeouts to prevent long-running queries

### Monitoring & Observability

**Key Metrics to Track:**
1. Profile update frequency (updates/day)
2. Average posts+comments per user
3. 95th percentile update time
4. Queue depth (if using Solution 3/5)
5. Failed update count

**Alerting:**
- Alert if queue depth > 1000
- Alert if update failure rate > 5%
- Alert if average update time > 5 seconds

---

## Conclusion

For the AnonymousWall project, **Solution 5 (Hybrid Approach)** offers the best balance of:
- User experience (fast updates for most users)
- Scalability (handles power users)
- Reliability (uses proven message queue pattern)
- Maintainability (clear code, good observability)

**Quick Start Path:**
1. Implement Solution 1 with 50-record limit for MVP
2. Add metrics and logging
3. Upgrade to Solution 5 once usage patterns are known
4. Monitor and tune threshold based on real data

**Time to Production:**
- MVP (Solution 1): 1-2 days
- Production-ready (Solution 5): 1-2 weeks
- Total effort: 17-23 hours of development

---

## Appendix: Code Snippets

### A. Repository Methods to Add

```java
// PostRepository.java
@Modifying
@Query("UPDATE posts SET profile_name = :profileName, updated_at = CURRENT_TIMESTAMP WHERE user_id = :userId")
void updateProfileNameByUserId(UUID userId, String profileName);

long countByUserId(UUID userId);

// CommentRepository.java
@Modifying
@Query("UPDATE comments SET profile_name = :profileName WHERE user_id = :userId")
void updateProfileNameByUserId(UUID userId, String profileName);

long countByUserId(UUID userId);
```

### B. Event Class

```java
// ProfileNameChangedEvent.java
public class ProfileNameChangedEvent {
    private final UUID userId;
    private final String oldName;
    private final String newName;
    private final ZonedDateTime timestamp;
    
    // Constructor, getters...
}
```

### C. Configuration

```yaml
# application.yml
micronaut:
  jms:
    enabled: true
    
profile-update:
  strategy: hybrid
  sync-threshold: 50
  batch-size: 100
  retry-attempts: 3
  retry-delay: 5s
```

---

**Document Version:** 1.0  
**Date:** February 7, 2026  
**Author:** GitHub Copilot Research  
**Status:** Research Complete - Ready for Implementation Discussion
