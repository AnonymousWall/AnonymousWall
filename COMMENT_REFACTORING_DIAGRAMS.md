# Comment System Architecture - Before & After Refactoring

## Current Architecture (Before Refactoring)

```
┌─────────────────────────────────────────────────────────────────┐
│                     PostsController                              │
│  /api/v1/posts/{postId}/comments                                │
│  - POST   /posts/{postId}/comments        (add comment)         │
│  - GET    /posts/{postId}/comments        (list comments)       │
│  - PATCH  /posts/{postId}/comments/{id}/hide                    │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CommentsService                               │
│  - addComment(postId, request, userId)                          │
│  - getCommentsWithPagination(postId, ...)                       │
│  - hideComment(postId, commentId, userId)                       │
│  - reportComment(commentId, userId, reason)                     │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   CommentRepository                              │
│  - findByPostId(postId)                                         │
│  - findByPostIdAndHiddenFalse(postId)                           │
│  - findByPostIdAndHiddenFalseOrderByCreatedAtDesc(...)          │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                Database: comments table                          │
│  ┌───────────┬──────────┬─────────┬──────────┬────────────┐    │
│  │ id        │ post_id  │ user_id │ text     │ is_hidden  │    │
│  ├───────────┼──────────┼─────────┼──────────┼────────────┤    │
│  │ uuid-1    │ post-1   │ user-1  │ "Great!" │ false      │    │
│  │ uuid-2    │ post-1   │ user-2  │ "Nice"   │ false      │    │
│  │ uuid-3    │ post-2   │ user-3  │ "Thanks" │ false      │    │
│  └───────────┴──────────┴─────────┴──────────┴────────────┘    │
│                                                                  │
│  Foreign Key: post_id → posts.id                                │
└─────────────────────────────────────────────────────────────────┘
                         │
                         ▼
                  ┌─────────────┐
                  │    Post     │
                  │  (entity)   │
                  └─────────────┘

Problem: Tightly coupled to Post entity!
Cannot reuse for Internship or Marketplace.
```

---

## Proposed Architecture (After Refactoring)

```
┌────────────────────────┬──────────────────────┬──────────────────────┐
│   PostsController      │ InternshipsController│ MarketplaceController│
│ /posts/{id}/comments   │/internships/{id}/... │/marketplace/{id}/... │
└───────────┬────────────┴──────────┬───────────┴───────────┬──────────┘
            │                       │                       │
            │  parentType=POST      │  parentType=INTERNSHIP│  parentType=MARKETPLACE
            │  parentId={postId}    │  parentId={intId}     │  parentId={marketId}
            │                       │                       │
            └───────────────────────┼───────────────────────┘
                                    ▼
            ┌────────────────────────────────────────────────┐
            │         CommentsService (Generic)              │
            │                                                │
            │  addComment(parentType, parentId, ...)         │
            │  getComments(parentType, parentId, ...)        │
            │  hideComment(parentType, parentId, ...)        │
            └────────────────────┬───────────────────────────┘
                                 │
                                 ▼
            ┌────────────────────────────────────────────────┐
            │        CommentableResolver (Factory)           │
            │                                                │
            │  resolve(parentType, parentId)                 │
            │  → returns Commentable interface               │
            └────────────────────┬───────────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────────┐
         ▼                       ▼                           ▼
    ┌─────────┐          ┌─────────────┐           ┌──────────────┐
    │  Post   │          │ Internship  │           │ Marketplace  │
    │implements│          │ implements  │           │  implements  │
    │Commentable│         │ Commentable │           │ Commentable  │
    └─────────┘          └─────────────┘           └──────────────┘
         │                       │                           │
         └───────────────────────┴───────────────────────────┘
                                 │
                                 ▼
            ┌────────────────────────────────────────────────┐
            │         CommentRepository (Generic)            │
            │                                                │
            │  findByParentTypeAndParentId(type, id)         │
            │  findByParentTypeAndParentIdAndHiddenFalse(...) │
            └────────────────────┬───────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────┐
│              Database: comments table (Polymorphic)              │
│  ┌──────┬───────────┬─────────────┬─────────┬──────┬──────────┐ │
│  │ id   │ parent_id │ parent_type │ user_id │ text │ hidden   │ │
│  ├──────┼───────────┼─────────────┼─────────┼──────┼──────────┤ │
│  │ u-1  │ post-1    │ POST        │ user-1  │ "Hi" │ false    │ │
│  │ u-2  │ post-1    │ POST        │ user-2  │ "OK" │ false    │ │
│  │ u-3  │ int-1     │ INTERNSHIP  │ user-3  │ "??" │ false    │ │
│  │ u-4  │ mkt-1     │ MARKETPLACE │ user-1  │ "Buy"│ false    │ │
│  └──────┴───────────┴─────────────┴─────────┴──────┴──────────┘ │
│                                                                  │
│  Composite Index: (parent_type, parent_id, is_hidden, ...)      │
│  No Foreign Keys: Application-level validation                  │
└──────────────────────────────────────────────────────────────────┘

Benefits: 
✅ Generic and reusable!
✅ Works for any Commentable entity
✅ Single comment table for all types
```

---

## Commentable Interface

```
┌─────────────────────────────────────────────────────────┐
│                <<interface>>                            │
│                 Commentable                             │
├─────────────────────────────────────────────────────────┤
│  + getId(): UUID                                        │
│  + getCommentableType(): String                         │
│  + canUserComment(userId, user): boolean                │
│  + incrementCommentCount(): void                        │
│  + decrementCommentCount(): void                        │
│  + getCommentCount(): int                               │
└─────────────────────────────────────────────────────────┘
                         △
                         │ implements
         ┌───────────────┼───────────────────────┐
         │               │                       │
    ┌────┴────┐    ┌─────┴──────┐    ┌──────────┴───────┐
    │  Post   │    │ Internship │    │   Marketplace    │
    ├─────────┤    ├────────────┤    ├──────────────────┤
    │ + wall  │    │ + company  │    │ + category       │
    │ + title │    │ + position │    │ + price          │
    │ + ...   │    │ + ...      │    │ + ...            │
    └─────────┘    └────────────┘    └──────────────────┘

Each implements:
- canUserComment() with entity-specific validation
- Post: checks campus/national wall, school domain
- Internship: checks if user is verified, internship is active
- Marketplace: checks if user is in same school, item not sold
```

---

## Request Flow Example

### Adding a Comment to a Post

```
1. Client Request
   ↓
   POST /api/v1/posts/123/comments
   {
     "text": "Great post!"
   }

2. PostsController
   ↓
   commentsService.addComment(
     CommentParentType.POST,  ← Specifies parent type
     postId,
     request,
     userId
   )

3. CommentsService
   ↓
   a. Resolve parent entity
      commentableResolver.resolve(POST, postId)
      → Returns Post entity (implements Commentable)
   
   b. Validate permissions
      post.canUserComment(userId, user)
      → true/false (checks wall type, school domain)
   
   c. Create comment
      comment = new Comment(
        parentId: postId,
        parentType: "POST",
        userId: userId,
        text: "Great post!"
      )
   
   d. Save comment
      commentRepository.save(comment)
   
   e. Update parent count
      post.incrementCommentCount()
      postRepository.update(post)

4. Response
   ↓
   {
     "id": "comment-uuid",
     "parentId": "123",
     "parentType": "POST",
     "text": "Great post!",
     "createdAt": "2026-02-19T04:30:00Z"
   }
```

### Adding a Comment to an Internship (Future)

```
1. Client Request
   ↓
   POST /api/v1/internships/456/comments
   {
     "text": "Interested!"
   }

2. InternshipsController
   ↓
   commentsService.addComment(
     CommentParentType.INTERNSHIP,  ← Different parent type
     internshipId,
     request,
     userId
   )

3. CommentsService (SAME CODE!)
   ↓
   a. Resolve parent entity
      commentableResolver.resolve(INTERNSHIP, internshipId)
      → Returns Internship entity (implements Commentable)
   
   b. Validate permissions
      internship.canUserComment(userId, user)
      → true/false (checks if active, user verified)
   
   c. Create comment (SAME LOGIC!)
   d. Save comment (SAME LOGIC!)
   e. Update parent count (SAME LOGIC!)

4. Response
   ↓
   {
     "id": "comment-uuid",
     "parentId": "456",
     "parentType": "INTERNSHIP",
     "text": "Interested!",
     "createdAt": "2026-02-19T04:30:00Z"
   }
```

**Key Insight**: The CommentsService code remains IDENTICAL!
Only the controller specifies which parent type to use.

---

## Database Query Comparison

### Before Refactoring
```sql
-- Get comments for a post
SELECT * FROM comments 
WHERE post_id = '123' 
  AND is_hidden = false 
ORDER BY created_at DESC 
LIMIT 20;

-- Index used: idx_comments_post_id (post_id, is_hidden, created_at)
```

### After Refactoring
```sql
-- Get comments for a post
SELECT * FROM comments 
WHERE parent_type = 'POST' 
  AND parent_id = '123' 
  AND is_hidden = false 
ORDER BY created_at DESC 
LIMIT 20;

-- Get comments for an internship
SELECT * FROM comments 
WHERE parent_type = 'INTERNSHIP' 
  AND parent_id = '456' 
  AND is_hidden = false 
ORDER BY created_at DESC 
LIMIT 20;

-- Get all user's comments (across all types)
SELECT * FROM comments 
WHERE user_id = 'user-1' 
  AND is_hidden = false 
ORDER BY created_at DESC;

-- Index used: idx_comments_parent (parent_type, parent_id, is_hidden, created_at)
```

**Performance Impact**: Minimal - the additional `parent_type` condition is highly selective and indexed.

---

## Data Migration Visualization

### Step 1: Before Migration
```
comments table:
┌───────┬─────────┬─────────┬──────────┐
│ id    │ post_id │ user_id │ text     │
├───────┼─────────┼─────────┼──────────┤
│ c-1   │ p-1     │ u-1     │ "Hello"  │
│ c-2   │ p-1     │ u-2     │ "World"  │
│ c-3   │ p-2     │ u-3     │ "Test"   │
└───────┴─────────┴─────────┴──────────┘
```

### Step 2: Add New Columns (NULL initially)
```
comments table:
┌───────┬─────────┬───────────┬─────────────┬─────────┬──────────┐
│ id    │ post_id │ parent_id │ parent_type │ user_id │ text     │
├───────┼─────────┼───────────┼─────────────┼─────────┼──────────┤
│ c-1   │ p-1     │ NULL      │ NULL        │ u-1     │ "Hello"  │
│ c-2   │ p-1     │ NULL      │ NULL        │ u-2     │ "World"  │
│ c-3   │ p-2     │ NULL      │ NULL        │ u-3     │ "Test"   │
└───────┴─────────┴───────────┴─────────────┴─────────┴──────────┘
```

### Step 3: Data Migration (Copy + Set Type)
```sql
UPDATE comments 
SET parent_id = post_id, 
    parent_type = 'POST'
WHERE parent_id IS NULL;
```

```
comments table:
┌───────┬─────────┬───────────┬─────────────┬─────────┬──────────┐
│ id    │ post_id │ parent_id │ parent_type │ user_id │ text     │
├───────┼─────────┼───────────┼─────────────┼─────────┼──────────┤
│ c-1   │ p-1     │ p-1       │ POST        │ u-1     │ "Hello"  │
│ c-2   │ p-1     │ p-1       │ POST        │ u-2     │ "World"  │
│ c-3   │ p-2     │ p-2       │ POST        │ u-3     │ "Test"   │
└───────┴─────────┴───────────┴─────────────┴─────────┴──────────┘
```

### Step 4: Drop Old Column
```sql
ALTER TABLE comments DROP COLUMN post_id;
```

```
comments table:
┌───────┬───────────┬─────────────┬─────────┬──────────┐
│ id    │ parent_id │ parent_type │ user_id │ text     │
├───────┼───────────┼─────────────┼─────────┼──────────┤
│ c-1   │ p-1       │ POST        │ u-1     │ "Hello"  │
│ c-2   │ p-1       │ POST        │ u-2     │ "World"  │
│ c-3   │ p-2       │ POST        │ u-3     │ "Test"   │
└───────┴───────────┴─────────────┴─────────┴──────────┘
```

### Step 5: Future - New Comment Types
```
comments table (after adding Internships & Marketplace):
┌───────┬───────────┬─────────────┬─────────┬──────────────┐
│ id    │ parent_id │ parent_type │ user_id │ text         │
├───────┼───────────┼─────────────┼─────────┼──────────────┤
│ c-1   │ p-1       │ POST        │ u-1     │ "Hello"      │
│ c-2   │ p-1       │ POST        │ u-2     │ "World"      │
│ c-3   │ i-1       │ INTERNSHIP  │ u-3     │ "Interested" │
│ c-4   │ m-1       │ MARKETPLACE │ u-1     │ "How much?"  │
│ c-5   │ i-2       │ INTERNSHIP  │ u-2     │ "Apply now"  │
└───────┴───────────┴─────────────┴─────────┴──────────────┘
```

---

## Key Takeaways

1. **Polymorphism**: One comment table, multiple parent types
2. **Interface Contract**: All parent entities implement Commentable
3. **Factory Pattern**: CommentableResolver creates correct parent instance
4. **Generic Service**: Same code works for all parent types
5. **Type-Safe**: Enum for parent types prevents typos
6. **Flexible**: Easy to add new commentable entities
7. **Maintainable**: No code duplication
8. **Performant**: Proper indexing maintains query speed

---

**For more details, see:**
- [COMMENT_REFACTORING_RESEARCH.md](./COMMENT_REFACTORING_RESEARCH.md) - Complete analysis
- [COMMENT_REFACTORING_SUMMARY.md](./COMMENT_REFACTORING_SUMMARY.md) - Executive summary
