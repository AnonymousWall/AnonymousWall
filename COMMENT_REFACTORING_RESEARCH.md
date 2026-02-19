# Comment Functionality Refactoring Research

## Executive Summary

This document provides research and recommendations for refactoring the comment functionality in AnonymousWall to make it generic and reusable for future features like **Internships** and **Marketplace**.

**Current State**: Comments are tightly coupled to Posts with a direct foreign key relationship.

**Goal**: Create a flexible, polymorphic comment system that can be attached to multiple parent entity types (Posts, Internships, Marketplace items) while maintaining data integrity and performance.

---

## Table of Contents

1. [Current Implementation Analysis](#current-implementation-analysis)
2. [Identified Challenges](#identified-challenges)
3. [Proposed Refactoring Approaches](#proposed-refactoring-approaches)
4. [Recommended Approach](#recommended-approach)
5. [Implementation Plan](#implementation-plan)
6. [Migration Strategy](#migration-strategy)
7. [Testing Considerations](#testing-considerations)
8. [Performance Implications](#performance-implications)

---

## Current Implementation Analysis

### 1. Architecture Overview

The current comment system is structured across multiple layers:

#### Entity Layer
- **`Comment.java`**: Entity with `post_id` foreign key
  - Fields: id, postId, userId, profileName, text, isHidden, createdAt, version
  - Direct relationship: `post_id` → `posts.id`

#### Repository Layer
- **`CommentRepository.java`**: Micronaut Data repository
  - Query methods: `findByPostId()`, `findByPostIdAndHiddenFalse()`, etc.
  - Pagination support with sorting by `created_at`
  - User-specific queries: `findByUserIdAndHiddenFalse()`

#### Service Layer
- **`CommentsService.java`** / **`CommentsServiceImpl.java`**
  - Business logic for comment operations
  - Key operations:
    - `addComment(postId, request, userId)`
    - `getCommentsWithPagination(postId, pageable, sortBy)`
    - `hideComment()` / `unhideComment()`
    - `getUserOwnComments()`
    - `reportComment()`
  - Post validation logic: checks visibility, wall type (campus/national), school domain

#### Controller Layer
- **`PostsController.java`**: REST endpoints under `/api/v1/posts/{postId}/comments`
  - POST `/posts/{postId}/comments` - Add comment
  - GET `/posts/{postId}/comments` - List comments
  - PATCH `/posts/{postId}/comments/{commentId}/hide` - Hide comment
  - PATCH `/posts/{postId}/comments/{commentId}/unhide` - Unhide comment
  - POST `/posts/{postId}/comments/{commentId}/reports` - Report comment

#### Admin Layer
- **`AdminCommentService.java`** / **`AdminCommentController.java`**
  - Admin operations: list all comments, filter, sort, soft-delete

#### Database Schema
```sql
CREATE TABLE comments (
    id CHAR(36) PRIMARY KEY,
    post_id CHAR(36) NOT NULL,  -- Foreign key to posts
    user_id CHAR(36),
    profile_name VARCHAR(255) DEFAULT 'Anonymous',
    text TEXT,
    is_hidden BOOLEAN DEFAULT false,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE comment_reports (
    id CHAR(36) PRIMARY KEY,
    comment_id CHAR(36) NOT NULL,
    reporter_user_id CHAR(36),
    reported_user_id CHAR(36),
    reason TEXT,
    created_at TIMESTAMP
);
```

### 2. Comment Count Integration

Posts maintain an atomic `comment_count` field that's updated when comments are added/hidden:
- `Post.incrementCommentCount()` - when comment added
- `Post.decrementCommentCount()` - when comment hidden
- Optimistic locking with `@Version` field prevents race conditions

### 3. Parent Entity Validation

Comments inherit access control from their parent Post:
- **Campus posts**: Only users from same school can comment
- **National posts**: All authenticated users can comment
- Hidden posts cannot be commented on

---

## Identified Challenges

### 1. **Tight Coupling**
- `Comment.postId` field is specific to Posts
- Service methods require `postId` parameter
- Repository queries filter by `postId`
- Controller routes nest comments under posts (`/posts/{postId}/comments`)

### 2. **Parent-Specific Validation**
- Post visibility rules (campus vs national, school domain)
- Different parent entities may have different access rules
- Validation logic is embedded in `CommentsServiceImpl`

### 3. **Comment Count Maintenance**
- Each parent entity needs comment count tracking
- Atomic increment/decrement logic specific to Post entity
- Transaction coordination between Comment and parent entity

### 4. **Reporting System**
- `CommentReport` entity tracks reports on comments
- Reporting could be generalized but is currently comment-specific

### 5. **Database Constraints**
- Foreign key constraint from `comments.post_id` to `posts.id`
- Cannot reference multiple parent tables with single FK

### 6. **API Design**
- RESTful routes nest comments under posts
- Future routes would be `/internships/{id}/comments`, `/marketplace/{id}/comments`
- Need consistent API patterns

---

## Proposed Refactoring Approaches

### Approach 1: Polymorphic Association with Type Discriminator

**Concept**: Use a generic parent reference with a type discriminator.

#### Database Schema
```sql
CREATE TABLE comments (
    id CHAR(36) PRIMARY KEY,
    parent_id CHAR(36) NOT NULL,       -- Generic parent reference
    parent_type VARCHAR(50) NOT NULL,   -- 'POST', 'INTERNSHIP', 'MARKETPLACE'
    user_id CHAR(36),
    profile_name VARCHAR(255),
    text TEXT,
    is_hidden BOOLEAN DEFAULT false,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP,
    INDEX idx_parent (parent_type, parent_id, is_hidden, created_at)
);
```

#### Pros
- ✅ Single table for all comments
- ✅ Flexible - easy to add new parent types
- ✅ Maintains existing comment functionality
- ✅ Simple data model

#### Cons
- ❌ No referential integrity (can't use foreign keys)
- ❌ Orphaned comments possible if parent deleted
- ❌ Complex queries to join with specific parent types
- ❌ Need application-level integrity checks

---

### Approach 2: Commentable Interface Pattern

**Concept**: Define a `Commentable` interface/abstraction that parent entities implement.

#### Java Interface
```java
public interface Commentable {
    UUID getId();
    String getCommentableType();
    boolean canUserComment(UUID userId, UserEntity user);
    void incrementCommentCount();
    void decrementCommentCount();
}
```

#### Implementation
```java
@MappedEntity
public class Post implements Commentable {
    // ... existing fields ...
    
    @Override
    public String getCommentableType() { return "POST"; }
    
    @Override
    public boolean canUserComment(UUID userId, UserEntity user) {
        // Campus/national wall validation
    }
    
    // ... other implementations ...
}
```

#### Pros
- ✅ Type-safe polymorphism
- ✅ Common interface for all commentable entities
- ✅ Parent-specific validation encapsulated
- ✅ Clear contract for new entities

#### Cons
- ❌ Still need polymorphic database schema
- ❌ Interface can't solve FK constraint issue
- ❌ Requires all entities to implement interface

---

### Approach 3: Separate Comment Tables per Parent Type

**Concept**: Create dedicated comment tables for each parent entity.

#### Database Schema
```sql
CREATE TABLE post_comments (
    id CHAR(36) PRIMARY KEY,
    post_id CHAR(36) NOT NULL,
    user_id CHAR(36),
    text TEXT,
    -- ... other fields ...
    FOREIGN KEY (post_id) REFERENCES posts(id)
);

CREATE TABLE internship_comments (
    id CHAR(36) PRIMARY KEY,
    internship_id CHAR(36) NOT NULL,
    user_id CHAR(36),
    text TEXT,
    -- ... other fields ...
    FOREIGN KEY (internship_id) REFERENCES internships(id)
);

CREATE TABLE marketplace_comments (
    id CHAR(36) PRIMARY KEY,
    marketplace_id CHAR(36) NOT NULL,
    user_id CHAR(36),
    text TEXT,
    -- ... other fields ...
    FOREIGN KEY (marketplace_id) REFERENCES marketplace(id)
);
```

#### Pros
- ✅ Full referential integrity
- ✅ Optimal query performance
- ✅ Clear separation
- ✅ Type-safe at database level

#### Cons
- ❌ Code duplication across repositories/services
- ❌ Schema maintenance overhead
- ❌ Harder to query "all user comments"
- ❌ Admin queries complex (need UNION)

---

### Approach 4: Hybrid - Generic Service with Type-Specific Repositories

**Concept**: Keep separate tables but abstract common logic in services.

#### Architecture
```java
// Generic interface
public interface CommentService<T extends Commentable> {
    Comment addComment(UUID parentId, CreateCommentRequest request, UUID userId);
    Page<Comment> getComments(UUID parentId, Pageable pageable);
    // ... other methods ...
}

// Implementations
@Singleton
public class PostCommentService implements CommentService<Post> {
    @Inject PostRepository postRepo;
    @Inject PostCommentRepository commentRepo;
    // ...
}

@Singleton  
public class InternshipCommentService implements CommentService<Internship> {
    @Inject InternshipRepository internshipRepo;
    @Inject InternshipCommentRepository commentRepo;
    // ...
}
```

#### Pros
- ✅ Referential integrity maintained
- ✅ Shared business logic
- ✅ Type-safe
- ✅ Good performance

#### Cons
- ❌ Still have multiple tables
- ❌ Abstract base classes can be complex
- ❌ More boilerplate code

---

### Approach 5: Single Table with Conditional Foreign Keys (Current + Extension)

**Concept**: Keep current structure, add new nullable FK fields for other parent types.

#### Database Schema
```sql
CREATE TABLE comments (
    id CHAR(36) PRIMARY KEY,
    post_id CHAR(36),           -- Nullable
    internship_id CHAR(36),     -- Nullable
    marketplace_id CHAR(36),    -- Nullable
    user_id CHAR(36),
    text TEXT,
    -- ... other fields ...
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (internship_id) REFERENCES internships(id),
    FOREIGN KEY (marketplace_id) REFERENCES marketplace(id),
    CHECK (
        (post_id IS NOT NULL AND internship_id IS NULL AND marketplace_id IS NULL) OR
        (post_id IS NULL AND internship_id IS NOT NULL AND marketplace_id IS NULL) OR
        (post_id IS NULL AND internship_id IS NULL AND marketplace_id IS NOT NULL)
    )
);
```

#### Pros
- ✅ Full referential integrity
- ✅ Single table maintains data
- ✅ Check constraint ensures data validity
- ✅ Easier user comment queries

#### Cons
- ❌ Sparse columns (most are NULL)
- ❌ Schema change for each new type
- ❌ Complex CHECK constraints
- ❌ Index inefficiency

---

## Recommended Approach

### **Hybrid Approach: Polymorphic with Commentable Interface**

After analyzing all approaches, I recommend **Approach 1 (Polymorphic) combined with Approach 2 (Commentable Interface)** for the following reasons:

#### Why This Approach?

1. **Flexibility**: Easy to add new commentable entities (Internship, Marketplace, etc.) without schema changes
2. **Single Source**: All comments in one table simplifies queries like "get all user comments"
3. **Maintainability**: Reduces code duplication - one service, one repository, one controller pattern
4. **Extensibility**: Interface contract ensures all parent types implement required behavior
5. **Performance**: Proper indexing on `(parent_type, parent_id, is_hidden, created_at)` maintains query performance

#### Addressing the Cons

**Problem**: No referential integrity
- **Solution**: Application-level validation + cleanup jobs
- Validate parent exists before creating comment
- Cascade soft-delete comments when parent is deleted
- Scheduled job to detect and clean orphaned comments

**Problem**: Complex joins with parent entities
- **Solution**: Comments don't need to join with parents in most queries
- When needed, use application-level joins or repository methods
- Parent information (like post title) not shown in comment listings

---

## Implementation Plan

### Phase 1: Design & Preparation (No Code Changes Yet)

1. **Define Commentable Interface**
   ```java
   public interface Commentable {
       UUID getId();
       String getCommentableType();
       boolean canUserComment(UUID userId, UserEntity user);
       void incrementCommentCount();
       void decrementCommentCount();
       int getCommentCount();
   }
   ```

2. **Define Parent Type Enum**
   ```java
   public enum CommentParentType {
       POST,
       INTERNSHIP,
       MARKETPLACE
   }
   ```

3. **Update Comment Entity Design**
   - Replace `postId` with `parentId`
   - Add `parentType` field
   - Update indexes

### Phase 2: Database Migration

1. **Create Migration Script** (Liquibase)
   ```xml
   <changeSet id="refactor-comments-polymorphic" author="team">
       <!-- Add new columns -->
       <addColumn tableName="comments">
           <column name="parent_id" type="CHAR(36)"/>
           <column name="parent_type" type="VARCHAR(50)"/>
       </addColumn>
       
       <!-- Migrate existing data -->
       <sql>
           UPDATE comments 
           SET parent_id = post_id, 
               parent_type = 'POST';
       </sql>
       
       <!-- Make new columns NOT NULL after migration -->
       <addNotNullConstraint tableName="comments" 
                             columnName="parent_id"/>
       <addNotNullConstraint tableName="comments" 
                             columnName="parent_type"/>
       
       <!-- Create composite index -->
       <createIndex tableName="comments" 
                    indexName="idx_comments_parent">
           <column name="parent_type"/>
           <column name="parent_id"/>
           <column name="is_hidden"/>
           <column name="created_at"/>
       </createIndex>
       
       <!-- Drop old FK constraint and column -->
       <dropForeignKeyConstraint baseTableName="comments" 
                                 constraintName="fk_comments_post"/>
       <dropColumn tableName="comments" columnName="post_id"/>
   </changeSet>
   ```

### Phase 3: Entity & Repository Updates

1. **Update Comment Entity**
   ```java
   @MappedEntity(value = "comments")
   public class Comment {
       @Id
       @AutoPopulated
       private UUID id;
   
       @MappedProperty("parent_id")
       private UUID parentId;
       
       @MappedProperty("parent_type")
       private String parentType;  // or CommentParentType enum
       
       // ... other fields ...
   }
   ```

2. **Update CommentRepository**
   ```java
   public interface CommentRepository extends CrudRepository<Comment, UUID> {
       // Generic methods
       List<Comment> findByParentTypeAndParentId(String parentType, UUID parentId);
       
       Page<Comment> findByParentTypeAndParentIdAndHiddenFalseOrderByCreatedAtDesc(
           String parentType, UUID parentId, Pageable pageable
       );
       
       // ... other refactored methods ...
   }
   ```

### Phase 4: Service Layer Refactoring

1. **Create Generic CommentsService**
   ```java
   public interface CommentsService {
       Comment addComment(
           CommentParentType parentType,
           UUID parentId, 
           CreateCommentRequest request, 
           UUID userId
       );
       
       Page<Comment> getCommentsWithPagination(
           CommentParentType parentType,
           UUID parentId,
           Pageable pageable,
           SortBy sortBy
       );
       
       // ... other methods ...
   }
   ```

2. **Update CommentsServiceImpl**
   - Replace post-specific validation with `Commentable` interface
   - Add factory/strategy pattern to resolve parent entity
   ```java
   @Singleton
   public class CommentsServiceImpl implements CommentsService {
       @Inject CommentRepository commentRepository;
       @Inject CommentableResolver commentableResolver;
       
       @Override
       public Comment addComment(...) {
           Commentable parent = commentableResolver.resolve(parentType, parentId);
           
           // Validate using interface
           if (!parent.canUserComment(userId, user)) {
               throw new IllegalArgumentException("Cannot comment");
           }
           
           // Create comment
           Comment comment = new Comment(
               parentId, 
               parentType.name(), 
               userId, 
               request.getText()
           );
           commentRepository.save(comment);
           
           // Update parent
           parent.incrementCommentCount();
           
           return comment;
       }
   }
   ```

3. **Create CommentableResolver**
   ```java
   @Singleton
   public class CommentableResolver {
       @Inject PostRepository postRepository;
       // Future: @Inject InternshipRepository internshipRepository;
       
       public Commentable resolve(CommentParentType type, UUID id) {
           return switch (type) {
               case POST -> postRepository.findById(id)
                   .orElseThrow(() -> new IllegalArgumentException("Post not found"));
               // Future cases:
               // case INTERNSHIP -> ...
               // case MARKETPLACE -> ...
           };
       }
   }
   ```

### Phase 5: Controller Updates

1. **Update PostsController**
   ```java
   @Post("/{postId}/comments")
   public HttpResponse<Object> addComment(
       @PathVariable UUID postId,
       @Body CreateCommentRequest request,
       HttpRequest<?> httpRequest
   ) {
       UUID userId = getUserIdFromRequest(httpRequest);
       
       // Updated call with parentType
       Comment comment = commentsService.addComment(
           CommentParentType.POST,
           postId,
           request,
           userId
       );
       
       return HttpResponse.created(mapCommentToDTO(comment));
   }
   ```

2. **Create Future Controllers** (when ready)
   ```java
   @Controller("/api/v1/internships")
   public class InternshipsController {
       @Post("/{internshipId}/comments")
       public HttpResponse<Object> addComment(...) {
           commentsService.addComment(
               CommentParentType.INTERNSHIP,
               internshipId,
               request,
               userId
           );
       }
   }
   ```

### Phase 6: Make Post Implement Commentable

```java
@MappedEntity(value = "posts")
public class Post implements Commentable {
    // ... existing fields ...
    
    @Override
    public String getCommentableType() {
        return "POST";
    }
    
    @Override
    public boolean canUserComment(UUID userId, UserEntity user) {
        if (this.hidden) {
            return false;
        }
        
        if ("national".equals(this.wall)) {
            return true;
        }
        
        if ("campus".equals(this.wall)) {
            return user.getSchoolDomain() != null 
                && user.getSchoolDomain().equals(this.schoolDomain);
        }
        
        return false;
    }
    
    @Override
    public void incrementCommentCount() {
        this.commentCount++;
    }
    
    @Override
    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }
    
    @Override
    public int getCommentCount() {
        return this.commentCount;
    }
}
```

### Phase 7: Testing & Validation

1. **Update existing tests** to use new parameters
2. **Add integration tests** for polymorphic behavior
3. **Test data integrity** - ensure no orphaned comments
4. **Performance testing** - validate index effectiveness
5. **Backward compatibility** - ensure existing API works

---

## Migration Strategy

### Zero-Downtime Migration Approach

#### Step 1: Add New Columns (Non-Breaking)
```sql
ALTER TABLE comments ADD COLUMN parent_id CHAR(36);
ALTER TABLE comments ADD COLUMN parent_type VARCHAR(50);
```

#### Step 2: Dual-Write Phase
- Write to both `post_id` and `parent_id`/`parent_type`
- Deploy code that supports both schemas
- Run migration to populate new columns from old

#### Step 3: Migration Script
```sql
UPDATE comments 
SET parent_id = post_id,
    parent_type = 'POST'
WHERE parent_id IS NULL;
```

#### Step 4: Validation
- Verify all rows have `parent_id` and `parent_type`
- Check referential integrity (all parents exist)

#### Step 5: Switch to New Schema
- Deploy code using only new fields
- Add NOT NULL constraints
- Drop old `post_id` column
- Drop old FK constraint

#### Step 6: Add Indexes
```sql
CREATE INDEX idx_comments_parent 
ON comments(parent_type, parent_id, is_hidden, created_at);
```

### Rollback Plan
- Keep old column during migration window
- Can revert to old code that uses `post_id`
- After successful verification, drop old column

---

## Testing Considerations

### Unit Tests
1. **CommentServiceImpl Tests**
   - Test with mock Commentable
   - Verify validation logic
   - Test comment count updates

2. **Repository Tests**
   - Test new query methods
   - Verify pagination with new schema
   - Test different parent types

### Integration Tests
1. **End-to-End Comment Workflows**
   - Create comment on post
   - List comments with pagination
   - Hide/unhide comments
   - Report comments

2. **Multi-Parent Tests** (future)
   - Comments on posts
   - Comments on internships
   - Comments on marketplace
   - User's comments from multiple sources

### Performance Tests
1. **Query Performance**
   - Benchmark before/after migration
   - Verify index effectiveness
   - Test with large datasets

2. **Concurrency Tests**
   - Concurrent comment creation
   - Comment count accuracy under load

---

## Performance Implications

### Index Strategy

#### Before Refactoring
```sql
INDEX idx_comments_post_id (post_id, is_hidden, created_at)
```

#### After Refactoring
```sql
INDEX idx_comments_parent (parent_type, parent_id, is_hidden, created_at)
```

### Query Performance Comparison

#### Before
```sql
SELECT * FROM comments 
WHERE post_id = ? AND is_hidden = false 
ORDER BY created_at DESC 
LIMIT 20;
```

#### After
```sql
SELECT * FROM comments 
WHERE parent_type = 'POST' AND parent_id = ? AND is_hidden = false 
ORDER BY created_at DESC 
LIMIT 20;
```

**Impact**: Minimal - composite index covers all conditions. The additional `parent_type` check is highly selective and indexed.

### Storage Overhead
- **New fields**: `parent_id` (36 bytes) + `parent_type` (up to 50 bytes)
- **Removed field**: `post_id` (36 bytes)
- **Net increase**: ~50 bytes per row for parent_type
- **For 1M comments**: ~50 MB additional storage (negligible)

---

## Future Extensions

### Adding Internship Comments

1. **Create Internship Entity**
   ```java
   @MappedEntity(value = "internships")
   public class Internship implements Commentable {
       private UUID id;
       private String title;
       private String description;
       private int commentCount;
       private boolean hidden;
       
       @Override
       public boolean canUserComment(UUID userId, UserEntity user) {
           // Internship-specific validation
           return !this.hidden && user.isVerified();
       }
       
       // ... implement other Commentable methods ...
   }
   ```

2. **Update CommentableResolver**
   ```java
   public Commentable resolve(CommentParentType type, UUID id) {
       return switch (type) {
           case POST -> postRepository.findById(id)
               .orElseThrow(() -> new NotFoundException("Post not found"));
           case INTERNSHIP -> internshipRepository.findById(id)
               .orElseThrow(() -> new NotFoundException("Internship not found"));
           // ... future types ...
       };
   }
   ```

3. **Create InternshipController**
   ```java
   @Controller("/api/v1/internships")
   public class InternshipsController {
       @Post("/{internshipId}/comments")
       public HttpResponse<Object> addComment(...) {
           Comment comment = commentsService.addComment(
               CommentParentType.INTERNSHIP,
               internshipId,
               request,
               userId
           );
           return HttpResponse.created(comment);
       }
   }
   ```

4. **No changes needed to**:
   - CommentRepository
   - CommentsService interface
   - Comment entity
   - CommentReport entity

---

## Alternative Consideration: GraphQL

If the application might adopt GraphQL in the future, polymorphic associations work naturally with GraphQL unions/interfaces:

```graphql
interface Commentable {
  id: ID!
  comments: [Comment!]!
  commentCount: Int!
}

type Post implements Commentable {
  id: ID!
  title: String!
  comments: [Comment!]!
  commentCount: Int!
}

type Internship implements Commentable {
  id: ID!
  title: String!
  comments: [Comment!]!
  commentCount: Int!
}

type Comment {
  id: ID!
  parent: Commentable!  # Union type
  text: String!
  author: User!
  createdAt: DateTime!
}
```

---

## Risk Assessment

### High Risk
- ❌ **Data migration failure**: Comprehensive testing and rollback plan mitigate this
- ❌ **Orphaned comments**: Application-level validation and cleanup jobs address this

### Medium Risk
- ⚠️ **Performance regression**: Index strategy and benchmarking minimize this risk
- ⚠️ **Breaking API changes**: Backward compatibility layer can maintain old API

### Low Risk
- ✅ **Code complexity**: Well-structured interfaces and patterns keep code maintainable
- ✅ **Testing overhead**: One-time investment in test refactoring

---

## Cost-Benefit Analysis

### Benefits
1. **Reusability**: Comment system works for Posts, Internships, Marketplace with no duplication
2. **Maintainability**: Single codebase for all comment operations
3. **Consistency**: Uniform API patterns across all commentable entities
4. **User Experience**: Users see all their comments in one place
5. **Admin Experience**: Single admin interface for all comments
6. **Future-Proof**: Easy to add new commentable types

### Costs
1. **Migration Effort**: ~2-3 weeks for full implementation and testing
2. **Risk**: Data migration complexity (mitigated with proper planning)
3. **Performance**: Minimal impact with proper indexing
4. **Learning Curve**: Team needs to understand polymorphic pattern

### ROI
- **Initial Investment**: ~2-3 weeks development time
- **Savings per new feature**: ~1-2 weeks (no need to rebuild comment system)
- **Break-even**: After adding 2-3 new commentable features
- **Long-term**: Significant maintenance savings

---

## Summary & Recommendation

### Recommendation: Proceed with Polymorphic + Commentable Interface Refactoring

**Rationale**:
1. AnonymousWall is planning to add Internships and Marketplace features
2. Current comment system is tightly coupled to Posts
3. Refactoring now prevents technical debt accumulation
4. Polymorphic approach provides best balance of flexibility and maintainability
5. Single comment table simplifies queries and admin operations

### Next Steps

1. **Get stakeholder approval** for this refactoring approach
2. **Create detailed JIRA/GitHub issues** for each phase
3. **Set up feature branch** for development
4. **Implement Phase 1** (interfaces and design)
5. **Create migration scripts** and test in staging environment
6. **Implement core refactoring** with backward compatibility
7. **Comprehensive testing** before production deployment
8. **Monitor performance** post-deployment
9. **Document patterns** for future feature teams

### Timeline Estimate

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Design & Interfaces | 3 days | None |
| Database Migration Scripts | 2 days | Phase 1 |
| Entity & Repository Updates | 3 days | Phase 2 |
| Service Layer Refactoring | 5 days | Phase 3 |
| Controller Updates | 3 days | Phase 4 |
| Testing & Validation | 5 days | Phase 5 |
| **Total** | **~3 weeks** | |

---

## Appendix: Code Examples

### Example: CommentableResolver with Strategy Pattern

```java
@Singleton
public class CommentableResolver {
    private final Map<CommentParentType, CommentableRepository> repositories;
    
    @Inject
    public CommentableResolver(
        PostRepository postRepository,
        // Future: InternshipRepository internshipRepository,
        // Future: MarketplaceRepository marketplaceRepository
    ) {
        this.repositories = Map.of(
            CommentParentType.POST, postRepository
            // Future: CommentParentType.INTERNSHIP, internshipRepository,
            // Future: CommentParentType.MARKETPLACE, marketplaceRepository
        );
    }
    
    public Commentable resolve(CommentParentType type, UUID id) {
        CommentableRepository repository = repositories.get(type);
        if (repository == null) {
            throw new IllegalArgumentException("Unknown parent type: " + type);
        }
        
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException(type + " not found: " + id));
    }
}
```

### Example: Generic Comment DTO Mapper

```java
public CommentDTO mapCommentToDTO(Comment comment) {
    CommentDTO dto = new CommentDTO();
    dto.setId(comment.getId());
    dto.setParentId(comment.getParentId());
    dto.setParentType(comment.getParentType());
    dto.setText(comment.getText());
    dto.setCreatedAt(comment.getCreatedAt());
    
    // Set author info
    CommentDTOAuthor author = new CommentDTOAuthor();
    author.setId(comment.getUserId().toString());
    author.setProfileName(comment.getProfileName());
    author.setIsAnonymous(true);
    dto.setAuthor(author);
    
    return dto;
}
```

---

## Questions & Answers

### Q: Why not use JPA inheritance (single table, joined table, or table per class)?

**A**: JPA inheritance is designed for hierarchical entity relationships (e.g., Animal → Dog, Cat). Our case is a polymorphic association where Comment references multiple unrelated parent types. JPA inheritance doesn't naturally model this pattern.

### Q: What about using a junction table?

**A**: A junction table (e.g., `commentable_associations`) would add unnecessary complexity and extra joins without providing significant benefits over the discriminator column approach.

### Q: How do we handle cascade deletes without foreign keys?

**A**: Implement cascade logic in the service layer:
```java
@Transactional
public void deletePost(UUID postId) {
    // Soft-delete all comments
    commentRepository.updateHiddenByParentTypeAndParentId(
        CommentParentType.POST.name(), 
        postId, 
        true
    );
    
    // Soft-delete the post
    postRepository.softDelete(postId);
}
```

### Q: What about database-level integrity?

**A**: Trade-off accepted for flexibility. Mitigation strategies:
1. Application-level validation before creating comments
2. Background jobs to detect and clean orphaned comments
3. Database triggers (optional) for additional validation
4. Comprehensive testing to ensure integrity

---

**Document Version**: 1.0  
**Last Updated**: 2026-02-19  
**Author**: Copilot (GitHub Coding Agent)  
**Status**: Ready for Review
