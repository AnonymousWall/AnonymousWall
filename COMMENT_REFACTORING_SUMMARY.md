# Comment Functionality Refactoring - Executive Summary

## Overview

This document provides a quick summary of the comment refactoring research. For complete details, see [COMMENT_REFACTORING_RESEARCH.md](./COMMENT_REFACTORING_RESEARCH.md).

## Current State

- Comments are **tightly coupled to Posts** via `post_id` foreign key
- Cannot be reused for planned Internship and Marketplace features
- All comment logic (service, repository, controller) is Post-specific

## Goal

Make comment functionality **generic and reusable** for:
- ✅ Posts (existing)
- 🔄 Internships (planned)
- 🔄 Marketplace (planned)
- 🔄 Any future commentable entities

## Recommended Approach

**Polymorphic Association with Commentable Interface**

### Key Changes

1. **Database Schema**
   - Replace `post_id` with generic `parent_id`
   - Add `parent_type` column ('POST', 'INTERNSHIP', 'MARKETPLACE')
   - Update indexes: `(parent_type, parent_id, is_hidden, created_at)`

2. **Commentable Interface**
   ```java
   public interface Commentable {
       UUID getId();
       String getCommentableType();
       boolean canUserComment(UUID userId, UserEntity user);
       void incrementCommentCount();
       void decrementCommentCount();
   }
   ```

3. **Generic Service Methods**
   ```java
   Comment addComment(
       CommentParentType parentType,  // NEW: generic parent type
       UUID parentId,
       CreateCommentRequest request,
       UUID userId
   );
   ```

4. **Commentable Resolver**
   - Factory pattern to resolve parent entity by type
   - Returns `Commentable` interface
   - Easy to extend for new types

## Pros & Cons

### ✅ Advantages
- Single comment system for all entities
- No code duplication
- Easy to add new commentable types
- Consistent API patterns
- Simplified admin queries
- Better user experience (all comments in one place)

### ⚠️ Trade-offs
- No database-level referential integrity
- Need application-level validation
- Potential for orphaned comments (mitigated with cleanup jobs)

## Implementation Phases

| Phase | Duration | Description |
|-------|----------|-------------|
| 1. Design | 3 days | Define interfaces and enums |
| 2. Database Migration | 2 days | Schema changes with zero downtime |
| 3. Entity Updates | 3 days | Refactor Comment entity and repository |
| 4. Service Refactoring | 5 days | Generic service with Commentable interface |
| 5. Controller Updates | 3 days | Update REST endpoints |
| 6. Testing | 5 days | Comprehensive testing and validation |
| **Total** | **~3 weeks** | |

## Migration Strategy

1. **Add new columns** (`parent_id`, `parent_type`) - non-breaking
2. **Dual-write phase** - write to both old and new columns
3. **Data migration** - copy `post_id` to `parent_id`, set `parent_type = 'POST'`
4. **Validation** - ensure data integrity
5. **Switch to new schema** - deploy code using new fields
6. **Cleanup** - drop old `post_id` column and FK constraint

## Risk Mitigation

| Risk | Severity | Mitigation |
|------|----------|------------|
| Data migration failure | High | Comprehensive testing, rollback plan, dual-write phase |
| Orphaned comments | Medium | Application validation, cleanup jobs, monitoring |
| Performance regression | Medium | Proper indexing, benchmarking before/after |
| Breaking API changes | Low | Backward compatibility layer, phased rollout |

## Adding New Commentable Entities (Future)

Once refactoring is complete, adding comments to new entities is simple:

### 1. Make Entity Implement Interface
```java
@MappedEntity(value = "internships")
public class Internship implements Commentable {
    // Implement interface methods
}
```

### 2. Update Resolver
```java
case INTERNSHIP -> internshipRepository.findById(id)
    .orElseThrow(...);
```

### 3. Create Controller
```java
@Controller("/api/v1/internships")
public class InternshipsController {
    @Post("/{internshipId}/comments")
    public HttpResponse<Object> addComment(...) {
        commentsService.addComment(
            CommentParentType.INTERNSHIP,  // Just specify the type!
            internshipId,
            request,
            userId
        );
    }
}
```

**That's it!** No changes needed to:
- CommentRepository
- CommentsService interface  
- Comment entity
- CommentReport entity
- Admin comment functionality

## Benefits Summary

- **Reusability**: Comment system works for all future features
- **Maintainability**: Single codebase, no duplication
- **Consistency**: Uniform API patterns
- **User Experience**: All user comments in one place
- **Admin Experience**: Single admin interface
- **Future-Proof**: Easy to extend

## Cost-Benefit Analysis

- **Initial Investment**: ~3 weeks development
- **Savings per new feature**: ~1-2 weeks (no comment system rebuild)
- **Break-even**: After 2-3 new commentable features
- **Long-term**: Significant maintenance savings

## Next Steps

1. ✅ Review this research document
2. ⏭️ Get stakeholder approval
3. ⏭️ Create implementation tickets
4. ⏭️ Set up feature branch
5. ⏭️ Begin Phase 1 (Design & Interfaces)

## Alternative Approaches Considered

We evaluated 5 different approaches:
1. **Polymorphic with Type Discriminator** ⭐ (Recommended)
2. Commentable Interface Pattern
3. Separate Tables per Parent Type
4. Hybrid with Type-Specific Repositories
5. Single Table with Conditional Foreign Keys

See [full research document](./COMMENT_REFACTORING_RESEARCH.md) for detailed analysis of each approach.

## Key Design Patterns

- **Strategy Pattern**: Commentable interface for parent-specific behavior
- **Factory Pattern**: CommentableResolver to create correct parent instance
- **Polymorphic Association**: Generic parent reference with type discriminator

## Questions?

See the [full research document](./COMMENT_REFACTORING_RESEARCH.md) for:
- Detailed code examples
- Complete migration scripts
- Performance analysis
- Testing strategy
- FAQ section

---

**Status**: ✅ Research Complete - Ready for Review  
**Document Version**: 1.0  
**Last Updated**: 2026-02-19  
**Author**: Copilot (GitHub Coding Agent)
