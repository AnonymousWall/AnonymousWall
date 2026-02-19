# Anonymous Wall - Campus Social Platform

A Micronaut-based REST API for anonymous campus social networking. Users register with school email, create posts on campus or national walls, like posts, and comment anonymously.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Key Features](#key-features)
3. [Project Structure](#project-structure)
4. [Technology Stack](#technology-stack)
5. [Database Schema](#database-schema)
6. [API Documentation](#api-documentation)
7. [Admin API Documentation](#admin-api-documentation)
8. [Authentication & Authorization](#authentication--authorization)
9. [Setup & Running](#setup--running)
10. [Known Flaws & Limitations](#known-flaws--limitations)

---

## Project Overview

**Anonymous Wall** is a Micronaut-based REST API for anonymous campus social networking. Students register with their school email, create posts on campus or national walls, like posts, and comment anonymously.

### What is Anonymous Wall?
- Students register with school email (e.g., student@harvard.edu)
- They can post to **Campus walls** (visible only to students from their school)
- They can post to **National walls** (visible to all authenticated students)
- All posts and comments are anonymous
- Students can like posts and comment on them

### Key Features
✅ Email verification-based registration (no password initially)  
✅ Password-less login with email verification codes  
✅ Optional password setup for security  
✅ Campus-segregated social walls  
✅ National cross-campus walls  
✅ Anonymous posting and commenting  
✅ Like/unlike functionality  
✅ JWT-based authentication with role-based access control (RBAC)  
✅ **Admin/Moderator system for content moderation**  
✅ **User management (block/unblock users)**  
✅ **Content moderation (soft-delete posts/comments)**  
✅ **Report management system**  
✅ **One-to-One Chat with WebSocket** (real-time messaging)  
✅ **Internship postings** (campus and national walls, with comments)  
✅ **Marketplace listings** (campus and national walls, with comments)  
✅ **Polymorphic comment system** (single comment system shared by posts, internships, and marketplace)  

---

## Project Structure

```
src/main/java/com/anonymous/wall/
├── controller/
│   ├── AuthController.java           # Auth endpoints
│   ├── PostsController.java          # Post, like, comment endpoints
│   ├── InternshipController.java     # Internship + comment endpoints
│   ├── MarketplaceController.java    # Marketplace + comment endpoints
│   ├── UserController.java           # User profile endpoints
│   ├── ChatController.java           # Chat REST endpoints
│   └── ChatWebSocketHandler.java     # WebSocket chat handler
│
├── admin/                            # Admin API module
│   ├── controller/
│   │   ├── AdminUserController.java      # User management
│   │   ├── AdminPostController.java      # Post moderation
│   │   ├── AdminCommentController.java   # Comment moderation
│   │   ├── AdminReportController.java    # Report viewing
│   │   └── AdminSchoolDomainController.java  # School domain management
│   └── service/
│       ├── AdminUserService.java         # User management logic
│       ├── AdminPostService.java         # Post moderation logic
│       ├── AdminCommentService.java      # Comment moderation logic
│       └── AdminReportService.java       # Report handling logic
│
├── service/
│   ├── AuthService/AuthServiceImpl.java           # Auth business logic
│   ├── UserService/UserServiceImpl.java           # User management
│   ├── PostsService/PostsServiceImpl.java         # Post operations
│   ├── CommentsService/CommentsServiceImpl.java   # Polymorphic comment operations
│   ├── InternshipService/InternshipServiceImpl.java   # Internship operations
│   ├── MarketplaceService/MarketplaceServiceImpl.java # Marketplace operations
│   ├── ChatService/ChatServiceImpl.java           # Chat operations
│   ├── SchoolDomainService/SchoolDomainServiceImpl.java  # School domain logic
│   └── JwtTokenService.java                       # JWT token generation (with RBAC)
│
├── entity/
│   ├── Commentable.java             # Interface for commentable entities
│   ├── CommentParentType.java       # Enum: POST, INTERNSHIP, MARKETPLACE
│   ├── UserEntity.java              # User model (with role & blocked fields)
│   ├── Post.java                    # Post model (implements Commentable)
│   ├── Comment.java                 # Comment model (polymorphic: parent_id + parent_type)
│   ├── Internship.java              # Internship model (implements Commentable)
│   ├── MarketplaceItem.java         # Marketplace model (implements Commentable)
│   ├── ChatMessage.java             # Chat message model
│   ├── PostLike.java                # Like model
│   ├── EmailVerificationCode.java   # Email verification
│   ├── PostReport.java              # Post reports
│   ├── CommentReport.java           # Comment reports
│   ├── SchoolDomain.java            # School domain model
│   └── PostList.java                # Post list model
│
├── repository/
│   ├── UserRepository.java
│   ├── PostRepository.java
│   ├── CommentRepository.java
│   ├── InternshipRepository.java
│   ├── MarketplaceItemRepository.java
│   ├── ChatMessageRepository.java
│   ├── PostLikeRepository.java
│   ├── PostReportRepository.java
│   ├── CommentReportRepository.java
│   ├── SchoolDomainRepository.java
│   └── EmailVerificationCodeRepository.java
│
├── mapper/
│   └── UserMapper.java              # DTO mapping utilities
│
├── util/
│   ├── EmailValidator.java
│   ├── CodeGenerator.java           # 6-digit verification code
│   ├── PasswordUtil.java            # Password hashing
│   └── EmailUtil.java               # Email sending
│
└── Application.java                 # Main application class

src/test/java/com/anonymous/wall/
├── controller/                        # Controller integration tests
│   ├── AuthControllerTest.java
│   ├── PostsCreateControllerTest.java
│   ├── PostsControllerLikeTests.java
│   ├── PostsControllerCommentTests.java
│   └── [Other controller tests...]
├── admin/controller/                  # Admin API tests
│   ├── AdminUserControllerTest.java      # 12 tests
│   ├── AdminPostControllerTest.java      # 9 tests
│   ├── AdminCommentControllerTest.java   # 7 tests
│   └── AdminReportControllerTest.java    # 5 tests
├── service/                           # Service unit tests
│   ├── AuthServiceImplTest.java
│   ├── UserServiceImplTest.java
│   ├── PostsServiceImplCreatePostTest.java
│   └── [Other service tests...]
├── entity/                            # Entity tests
├── event/                             # Event handling tests
├── listener/                          # Event listener tests
├── transaction/                       # Transaction tests
├── concurrency/                       # Concurrency tests
└── util/                              # Utility tests
```

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Framework | Micronaut 4.11.5 |
| Language | Java 17+ |
| Build | Maven 3.9.4 |
| Database | H2 (Development), PostgreSQL (Production) |
| ORM | Micronaut Data |
| Authentication | JWT (JSON Web Tokens) |
| API Spec | OpenAPI 3.0 |
| Testing | JUnit 5, Micronaut Test |
| Mail | SMTP (configurable) |

---

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    school_domain VARCHAR(255),          -- e.g., "harvard.edu"
    password_hash VARCHAR(255),
    is_verified BOOLEAN DEFAULT false,
    password_set BOOLEAN DEFAULT false,
    report_count INT DEFAULT 0,          -- Number of reports received by this user
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Posts Table
```sql
CREATE TABLE posts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    profile_name VARCHAR(255) DEFAULT 'Anonymous',
    title VARCHAR(255) NOT NULL,         -- Post title (max 255 characters)
    content VARCHAR(5000) NOT NULL,      -- Post content (max 5000 characters)
    wall VARCHAR(20) DEFAULT 'campus',   -- "campus" or "national"
    school_domain VARCHAR(255),          -- NULL for national, set for campus
    like_count INT DEFAULT 0,            -- Atomic counter for likes
    comment_count INT DEFAULT 0,         -- Atomic counter for comments
    is_hidden BOOLEAN DEFAULT false,     -- Soft-delete flag
    version BIGINT DEFAULT 0,            -- Optimistic locking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Comments Table
```sql
CREATE TABLE comments (
    id UUID PRIMARY KEY,
    parent_id CHAR(36) NOT NULL,              -- ID of the parent entity (post, internship, or marketplace item)
    parent_type VARCHAR(50) NOT NULL,         -- "POST", "INTERNSHIP", or "MARKETPLACE"
    user_id UUID NOT NULL REFERENCES users(id),
    profile_name VARCHAR(255) DEFAULT 'Anonymous',
    text TEXT NOT NULL,
    is_hidden BOOLEAN DEFAULT false,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Composite index for efficient lookups by parent entity
CREATE INDEX idx_comments_parent ON comments(parent_type, parent_id);
```

**Polymorphic Design**: Comments use `parent_id` + `parent_type` instead of a direct foreign key to a single table. This allows the same comment system to serve posts, internships, and marketplace items. Referential integrity is enforced at the application level.

### Post Likes Table
```sql
CREATE TABLE post_likes (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id),
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, user_id)             -- One like per user per post
);
```

### Email Verification Codes Table
```sql
CREATE TABLE email_verification_codes (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(10) NOT NULL,
    purpose VARCHAR(50),                 -- "REGISTER", "LOGIN", "RESET_PASSWORD"
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT false
);
```

### Post Reports Table
```sql
CREATE TABLE post_reports (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id),
    reporter_user_id UUID NOT NULL REFERENCES users(id),
    reported_user_id UUID NOT NULL REFERENCES users(id),  -- ID of the user being reported (post author)
    reason VARCHAR(500),                 -- Optional reason for the report
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, reporter_user_id)    -- One report per user per post
);
```

### Comment Reports Table
```sql
CREATE TABLE comment_reports (
    id UUID PRIMARY KEY,
    comment_id UUID NOT NULL REFERENCES comments(id),
    reporter_user_id UUID NOT NULL REFERENCES users(id),
    reported_user_id UUID NOT NULL REFERENCES users(id),  -- ID of the user being reported (comment author)
    reason VARCHAR(500),                 -- Optional reason for the report
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(comment_id, reporter_user_id) -- One report per user per comment
);
```

### Internships Table
```sql
CREATE TABLE internships (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    profile_name VARCHAR(255) DEFAULT 'Anonymous',
    company VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    salary VARCHAR(50),
    location VARCHAR(255),
    description TEXT,
    deadline DATE,
    wall VARCHAR(20) DEFAULT 'campus',   -- "campus" or "national"
    school_domain VARCHAR(255),          -- NULL for national, set for campus
    comment_count INT DEFAULT 0,
    is_hidden BOOLEAN DEFAULT false,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Marketplace Items Table
```sql
CREATE TABLE marketplace_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    profile_name VARCHAR(255) DEFAULT 'Anonymous',
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(100),
    condition VARCHAR(20),               -- "new", "like_new", "good", "fair", "poor"
    sold BOOLEAN DEFAULT false NOT NULL,
    contact_info VARCHAR(255),
    wall VARCHAR(20) DEFAULT 'campus',   -- "campus" or "national"
    school_domain VARCHAR(255),          -- NULL for national, set for campus
    comment_count INT DEFAULT 0,
    is_hidden BOOLEAN DEFAULT false,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## API Documentation

### Common Response Codes

- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request parameters or validation failed
- `401 Unauthorized` - Missing or invalid authentication token
- `403 Forbidden` - Access denied (insufficient permissions or blocked user)
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

**Blocked User Response:**
When a blocked user attempts any authenticated operation, they receive:
```json
HTTP/1.1 403 Forbidden
Content-Type: application/json

{
    "error": "Access denied. Your account has been blocked."
}
```

### Authentication Endpoints

#### 1. Send Email Verification Code
```http
POST /api/v1/auth/email/send-code
Content-Type: application/json

{
    "email": "student@harvard.edu",
    "purpose": "register"  // or "login", "reset_password"
}

Response: 200 OK
{
    "message": "Verification code sent to email"
}
```

#### 2. Register with Email Code
```http
POST /api/v1/auth/register/email
Content-Type: application/json

{
    "email": "student@harvard.edu",
    "code": "123456"
}

Response: 201 Created
{
    "user": {
        "id": "uuid",
        "email": "student@harvard.edu",
        "profileName": "Anonymous",
        "isVerified": true,
        "passwordSet": false,
        "createdAt": "2026-01-28T..."
    },
    "accessToken": "jwt-token-here"
}
```

#### 3. Login with Email Code
```http
POST /api/v1/auth/login/email
Content-Type: application/json

{
    "email": "student@harvard.edu",
    "code": "123456"
}

Response: 200 OK
{
    "user": {...},
    "accessToken": "jwt-token-here"
}
```

#### 4. Login with Password
```http
POST /api/v1/auth/login/password
Content-Type: application/json

{
    "email": "student@harvard.edu",
    "password": "secure_password"
}

Response: 200 OK
{
    "user": {...},
    "accessToken": "jwt-token-here"
}
```

#### 5. Set Password (Requires Authentication)
```http
POST /api/v1/auth/password/set
Header: X-User-Id: {userId}
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "password": "secure_password"
}

Response: 200 OK
{
    "id": "uuid",
    "email": "student@harvard.edu",
    "profileName": "Anonymous",
    "isVerified": true,
    "passwordSet": true,
    "createdAt": "2026-01-28T..."
}
```

#### 6. Change Password (Requires Authentication)
```http
POST /api/v1/auth/password/change
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "oldPassword": "current_password",
    "newPassword": "new_password"
}

Response: 200 OK
{
    "id": "uuid",
    "email": "student@harvard.edu",
    "profileName": "Anonymous",
    "isVerified": true,
    "passwordSet": true,
    "createdAt": "2026-01-28T..."
}
```

#### 7. Request Password Reset (Forgot Password)
```http
POST /api/v1/auth/password/reset-request
Content-Type: application/json

{
    "email": "student@harvard.edu"
}

Response: 200 OK
{
    "message": "Password reset code sent to email"
}
```

**Notes:**
- Sends a 6-digit verification code to the user's email
- User must provide this code to reset their password
- Code expires after 15 minutes

#### 8. Reset Password
```http
POST /api/v1/auth/password/reset
Content-Type: application/json

{
    "email": "student@harvard.edu",
    "code": "123456",
    "newPassword": "new_password"
}

Response: 200 OK
{
    "user": {
        "id": "uuid",
        "email": "student@harvard.edu",
        "profileName": "Anonymous",
        "isVerified": true,
        "passwordSet": true,
        "createdAt": "2026-01-28T..."
    },
    "accessToken": "jwt-token-here"
}
```

**Notes:**
- Requires valid email verification code
- Code must not be expired (15 minute expiration)
- Returns JWT token upon successful password reset

---

### Post Endpoints

#### 1. Create Post
```http
POST /api/v1/posts
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "title": "My First Post Title",    // NEW - REQUIRED (1-255 chars)
    "content": "This is my first post!",
    "wall": "campus"  // or "national", optional, defaults to "campus"
}

Response: 201 Created
{
    "id": "uuid",
    "title": "My First Post Title",     // NEW
    "content": "This is my first post!",
    "wall": "CAMPUS",
    "likes": 0,
    "comments": 0,
    "liked": false,
    "author": {
        "id": "uuid",
        "profileName": "Anonymous",
        "isAnonymous": true
    },
    "createdAt": "2026-01-28T...",
    "updatedAt": "2026-01-28T..."
}
```

**Request Validation:**
- `title` is **required** (cannot be null, empty, or whitespace-only)
- `title` maximum length: **255 characters**
- `content` is **required** (cannot be null, empty, or whitespace-only)
- `content` maximum length: **5000 characters**
- `wall` is optional (defaults to "campus"), must be "campus" or "national"

**Error Responses:**
```json
// Missing or empty title
400 Bad Request
{
    "error": "Post title cannot be empty"
}

// Title exceeds 255 characters
400 Bad Request
{
    "error": "Post title exceeds maximum length of 255 characters"
}
```

#### 2. List Posts
```http
GET /api/v1/posts?wall=campus&page=1&limit=20&sort=NEWEST
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "title": "Post Title",        // NEW
            "content": "Post content",
            "wall": "CAMPUS",
            "likes": 5,
            "comments": 2,
            "liked": false,
            "author": {
                "id": "uuid",
                "profileName": "John Doe",
                "isAnonymous": true
            },
            "createdAt": "2026-01-28T...",
            "updatedAt": "2026-01-28T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 150,
        "totalPages": 8
    }
}
```

**Query Parameters:**
- `wall` (default: "campus") - Filter by "campus" or "national"
- `page` (default: 1) - Page number (1-based)
- `limit` (default: 20) - Posts per page (max: 100)
- `sort` (default: "NEWEST") - Sort order: NEWEST, OLDEST, MOST_LIKED, LEAST_LIKED, MOST_COMMENTED, LEAST_COMMENTED

#### 3. Get Post by ID
```http
GET /api/v1/posts/{postId}
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "id": "uuid",
    "title": "Post Title",
    "content": "Post content",
    "wall": "CAMPUS",
    "likes": 5,
    "comments": 2,
    "liked": false,
    "author": {
        "id": "uuid",
        "profileName": "John Doe",
        "isAnonymous": true
    },
    "createdAt": "2026-01-28T...",
    "updatedAt": "2026-01-28T..."
}

Response: 404 Not Found
{
    "error": "Post not found"
}

Response: 403 Forbidden
{
    "error": "You do not have access to posts from other schools"
}
```

**Notes:**
- Retrieves a single post by its ID
- For campus posts: only users from the same school can access
- For national posts: all authenticated users can access
- Returns 404 if post does not exist
- Returns 403 if user doesn't have access to the post

#### 4. Like/Unlike Post (Toggle)
```http
POST /api/v1/posts/{postId}/likes
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "liked": true,
    "likeCount": 6
}
```

**Notes:**
- Single endpoint that toggles like state (like if not liked, unlike if already liked)
- Returns both the new like state and total like count for the post
- For campus posts: only users from the same school can like
- For national posts: all authenticated users can like
- Response: `liked` (boolean) indicates post is now liked, `likeCount` is total likes on post


#### 5. Add Comment
```http
POST /api/v1/posts/{postId}/comments
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "text": "Great post!"
}

Response: 201 Created
{
    "id": "uuid",
    "postId": "uuid",
    "parentType": "POST",
    "text": "Great post!",
    "author": {
        "id": "uuid",
        "profileName": "Anonymous",
        "isAnonymous": true
    },
    "createdAt": "2026-01-28T..."
}

Response: 400 Bad Request
{
    "error": "Comment text cannot be empty"
}

Response: 400 Bad Request
{
    "error": "Comment text exceeds maximum length of 5000 characters"
}
```

**Validation Rules:**
- `text` is **required** (cannot be null, empty, or whitespace-only)
- `text` maximum length: **5000 characters**

#### 6. Get Comments for Post
```http
GET /api/v1/posts/{postId}/comments?page=1&limit=20&sort=NEWEST
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "postId": "uuid",
            "parentType": "POST",
            "text": "Great post!",
            "author": {
                "id": "uuid",
                "profileName": "Jane Smith",
                "isAnonymous": true
            },
            "createdAt": "2026-01-28T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 5,
        "totalPages": 1
    }
}
```

**Query Parameters:**
- `page` (default: 1) - Page number (1-based)
- `limit` (default: 20) - Comments per page (max: 100)
- `sort` (default: "NEWEST") - Sort order: NEWEST, OLDEST

#### 7. Hide Post
```http
PATCH /api/v1/posts/{postId}/hide
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "message": "Post hidden successfully"
}
```

**Notes:**
- Only the post author can hide their own post
- When a post is hidden, all its comments are also hidden
- This is a soft-delete operation; data is preserved in the database

#### 8. Unhide Post
```http
PATCH /api/v1/posts/{postId}/unhide
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "message": "Post unhidden successfully"
}
```

**Notes:**
- Only the post author can unhide their own post
- When a post is unhidden, all its previously hidden comments are also restored


#### 9. Hide Comment
```http
PATCH /api/v1/posts/{postId}/comments/{commentId}/hide
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "message": "Comment hidden successfully"
}
```

**Notes:**
- Only the comment author can hide their own comment
- This is a soft-delete operation; data is preserved in the database

#### 10. Unhide Comment
```http
PATCH /api/v1/posts/{postId}/comments/{commentId}/unhide
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "message": "Comment unhidden successfully"
}
```

**Notes:**
- Only the comment author can unhide their own comment

#### 11. Report Post
```http
POST /api/v1/posts/{postId}/reports
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "reason": "This post contains inappropriate content"
}

Response: 201 Created
{
    "message": "Post reported successfully"
}
```

**Notes:**
- A user can only report the same post once
- `reason` is optional (max length: 500 characters)
- Reporting a post increments the report count for the post author
- Duplicate reports by the same user will return: `400 Bad Request`

#### 12. Report Comment
```http
POST /api/v1/posts/{postId}/comments/{commentId}/reports
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "reason": "This comment violates community guidelines"
}

Response: 201 Created
{
    "message": "Comment reported successfully"
}
```

**Notes:**
- A user can only report the same comment once
- `reason` is optional (max length: 500 characters)
- Reporting a comment increments the report count for the comment author
- Duplicate reports by the same user will return: `400 Bad Request`

### Internship Endpoints

#### 1. Create Internship Posting
```http
POST /api/v1/internships
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "company": "Google",
    "role": "Software Engineer Intern",
    "salary": "$8000/month",
    "location": "Mountain View, CA",
    "description": "Work on cutting-edge projects with experienced mentors",
    "deadline": "2026-06-30",
    "wall": "campus"
}

Response: 201 Created
{
    "id": "uuid",
    "company": "Google",
    "role": "Software Engineer Intern",
    "salary": "$8000/month",
    "location": "Mountain View, CA",
    "description": "Work on cutting-edge projects with experienced mentors",
    "deadline": "2026-06-30",
    "wall": "CAMPUS",
    "comments": 0,
    "author": {
        "id": "uuid",
        "profileName": "John Recruiter",
        "isAnonymous": false
    },
    "createdAt": "2026-02-18T...",
    "updatedAt": "2026-02-18T..."
}
```

**Request Validation:**
- `company` is **required** (cannot be null, empty, or whitespace-only)
- `company` maximum length: **255 characters**
- `role` is **required** (cannot be null, empty, or whitespace-only)
- `role` maximum length: **255 characters**
- `salary` is optional (VARCHAR(50))
- `location` is optional (VARCHAR(255))
- `description` is optional (TEXT)
- `deadline` is optional (DATE format: YYYY-MM-DD)
- `wall` is optional (defaults to "campus"), must be "campus" or "national"

**Wall Rules:**
- **Campus wall**: Only users from the same school can see the posting
- **National wall**: All authenticated users can see the posting

**Error Responses:**
```json
// Missing or empty company
400 Bad Request
{
    "error": "Company is required"
}

// Company exceeds 255 characters
400 Bad Request
{
    "error": "Company name cannot exceed 255 characters"
}

// Missing or empty role
400 Bad Request
{
    "error": "Role is required"
}

// Role exceeds 255 characters
400 Bad Request
{
    "error": "Role cannot exceed 255 characters"
}

// User not found
400 Bad Request
{
    "error": "User not found"
}
```

#### 2. List Internship Postings
```http
GET /api/v1/internships?wall=campus&page=1&limit=20&sortBy=newest
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "company": "Google",
            "role": "Software Engineer Intern",
            "salary": "$8000/month",
            "location": "Mountain View, CA",
            "description": "Work on cutting-edge projects with experienced mentors",
            "deadline": "2026-06-30",
            "wall": "CAMPUS",
            "comments": 3,
            "author": {
                "id": "uuid",
                "profileName": "John Recruiter",
                "isAnonymous": false
            },
            "createdAt": "2026-02-18T...",
            "updatedAt": "2026-02-18T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 50,
        "totalPages": 3
    }
}
```

**Query Parameters:**
- `wall` (default: "campus") - Filter by "campus" or "national"
- `page` (default: 1): Page number for pagination (1-based indexing)
- `limit` (default: 20): Number of items per page (min: 1, max: 100)
- `sortBy` (default: newest): Sort order - "newest" (newest first) or "oldest" (oldest first)

**Wall Rules:**
- **Campus**: Returns only internships from the same school as the authenticated user
- **National**: Returns all national internships
- Only non-hidden internships are returned

#### 3. Get Internship Posting by ID
```http
GET /api/v1/internships/{internshipId}
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "id": "uuid",
    "company": "Google",
    "role": "Software Engineer Intern",
    "salary": "$8000/month",
    "location": "Mountain View, CA",
    "description": "Work on cutting-edge projects with experienced mentors",
    "deadline": "2026-06-30",
    "wall": "CAMPUS",
    "comments": 3,
    "author": {
        "id": "uuid",
        "profileName": "John Recruiter",
        "isAnonymous": false
    },
    "createdAt": "2026-02-18T...",
    "updatedAt": "2026-02-18T..."
}
```

**Error Responses:**
```json
// Internship not found
404 Not Found

// Campus internship from different school
403 Forbidden
{
    "error": "You do not have access to internships from other schools"
}
```

#### 4. Hide Internship Posting
```http
PATCH /api/v1/internships/{internshipId}/hide
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "message": "Internship posting hidden successfully"
}
```

**Notes:**
- Only the author can hide their own internship posting
- Hidden internships are excluded from list results
- Soft-delete operation (data is not permanently removed)

**Error Responses:**
```json
// Not the author
403 Forbidden
{
    "error": "You can only hide your own internship postings"
}

// Internship not found
404 Not Found
```

#### 5. Unhide Internship Posting
```http
PATCH /api/v1/internships/{internshipId}/unhide
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "message": "Internship posting unhidden successfully"
}
```

**Notes:**
- Only the author can unhide their own internship posting
- Unhidden internships reappear in list results

**Error Responses:**
```json
// Not the author
403 Forbidden
{
    "error": "You can only unhide your own internship postings"
}

// Internship not found
404 Not Found
```

#### 6. Add Comment to Internship
```http
POST /api/v1/internships/{internshipId}/comments
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "text": "Great opportunity!"
}

Response: 201 Created
{
    "id": "uuid",
    "postId": "uuid",
    "parentType": "INTERNSHIP",
    "text": "Great opportunity!",
    "author": {
        "id": "uuid",
        "profileName": "Anonymous",
        "isAnonymous": true
    },
    "createdAt": "2026-02-18T..."
}
```

**Validation Rules:**
- `text` is **required** (cannot be null, empty, or whitespace-only)
- `text` maximum length: **5000 characters**
- For campus internships: only users from the same school can comment
- For national internships: all authenticated users can comment

#### 7. Get Comments for Internship
```http
GET /api/v1/internships/{internshipId}/comments?page=1&limit=20&sort=NEWEST
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "postId": "uuid",
            "parentType": "INTERNSHIP",
            "text": "Great opportunity!",
            "author": {
                "id": "uuid",
                "profileName": "Jane Smith",
                "isAnonymous": true
            },
            "createdAt": "2026-02-18T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 5,
        "totalPages": 1
    }
}
```

**Query Parameters:**
- `page` (default: 1) - Page number (1-based)
- `limit` (default: 20) - Comments per page (max: 100)
- `sort` (default: "NEWEST") - Sort order: NEWEST, OLDEST

#### 8. Hide Comment on Internship
```http
PATCH /api/v1/internships/{internshipId}/comments/{commentId}/hide
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "message": "Comment hidden successfully"
}
```

**Notes:**
- Only the comment author can hide their own comment

#### 9. Unhide Comment on Internship
```http
PATCH /api/v1/internships/{internshipId}/comments/{commentId}/unhide
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "message": "Comment unhidden successfully"
}
```

**Notes:**
- Only the comment author can unhide their own comment

### Marketplace Endpoints

#### 1. Create Marketplace Item
```http
POST /api/v1/marketplace
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "title": "Used Calculus Textbook",
    "price": 45.99,
    "description": "Barely used, excellent condition",
    "category": "books",
    "condition": "like_new",
    "contactInfo": "johndoe@harvard.edu",
    "wall": "campus"
}

Response: 201 Created
{
    "id": "uuid",
    "title": "Used Calculus Textbook",
    "price": 45.99,
    "description": "Barely used, excellent condition",
    "category": "books",
    "condition": "like_new",
    "contactInfo": "johndoe@harvard.edu",
    "sold": false,
    "wall": "CAMPUS",
    "comments": 0,
    "author": {
        "id": "uuid",
        "profileName": "John Doe",
        "isAnonymous": false
    },
    "createdAt": "2026-02-18T...",
    "updatedAt": "2026-02-18T..."
}
```

**Request Validation:**
- `title` is **required** (cannot be null, empty, or whitespace-only)
- `title` maximum length: **255 characters**
- `price` is **required** and must be **≥ 0**
- `price` maximum value: **99,999,999.99** (DECIMAL(10,2))
- `description` is optional (max length: 5000 characters)
- `category` is optional
- `condition` is optional, valid values: "new", "like_new", "good", "fair", "poor"
- `contactInfo` is optional
- `wall` is optional (defaults to "campus"), must be "campus" or "national"

**Wall Rules:**
- **Campus wall**: Only users from the same school can see the item
- **National wall**: All authenticated users can see the item

**Error Responses:**
```json
// Missing or empty title
400 Bad Request
{
    "error": "Title cannot be empty"
}

// Title exceeds 255 characters
400 Bad Request
{
    "error": "Title cannot exceed 255 characters"
}

// Missing or invalid price
400 Bad Request
{
    "error": "Price is required"
}

// Negative price
400 Bad Request
{
    "error": "Price must be greater than or equal to 0"
}

// Invalid condition
400 Bad Request
{
    "error": "Invalid condition. Must be one of: new, like_new, good, fair, poor"
}
```

#### 2. List Marketplace Items
```http
GET /api/v1/marketplace?wall=campus&page=1&limit=20&sortBy=newest&sold=false
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "title": "Used Calculus Textbook",
            "price": 45.99,
            "description": "Barely used, excellent condition",
            "category": "books",
            "condition": "like_new",
            "contactInfo": "johndoe@harvard.edu",
            "sold": false,
            "wall": "CAMPUS",
            "comments": 2,
            "author": {
                "id": "uuid",
                "profileName": "John Doe",
                "isAnonymous": false
            },
            "createdAt": "2026-02-18T...",
            "updatedAt": "2026-02-18T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 95,
        "totalPages": 5
    }
}
```

**Query Parameters:**
- `wall` (default: "campus") - Filter by "campus" or "national"
- `page` (default: 1) - Page number (1-based)
- `limit` (default: 20) - Items per page (max: 100)
- `sortBy` (default: "newest") - Sort order:
  - `newest` - Sort by creation date descending (newest first)
  - `price-asc` - Sort by price ascending (lowest first)
  - `price-desc` - Sort by price descending (highest first)
- `sold` (optional) - Filter by sold status:
  - `true` - Show only sold items
  - `false` - Show only unsold items
  - omit parameter - Show all items (both sold and unsold)

**Wall Rules:**
- **Campus**: Returns only items from the same school as the authenticated user
- **National**: Returns all national items

**Examples:**
```http
GET /api/v1/marketplace?wall=campus&sold=false&sortBy=price-asc
GET /api/v1/marketplace?wall=national&sold=true&page=1&limit=10
GET /api/v1/marketplace?sortBy=newest
```

#### 3. Get Marketplace Item by ID
```http
GET /api/v1/marketplace/{itemId}
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "id": "uuid",
    "title": "Used Calculus Textbook",
    "price": 45.99,
    "description": "Barely used, excellent condition",
    "category": "books",
    "condition": "like_new",
    "contactInfo": "johndoe@harvard.edu",
    "sold": false,
    "wall": "CAMPUS",
    "comments": 2,
    "author": {
        "id": "uuid",
        "profileName": "John Doe",
        "isAnonymous": false
    },
    "createdAt": "2026-02-18T...",
    "updatedAt": "2026-02-18T..."
}

Response: 404 Not Found
{
    "error": "Item not found"
}
```

**Notes:**
- For campus items: only users from the same school can access
- For national items: all authenticated users can access

#### 4. Update Marketplace Item (Partial Update)
```http
PUT /api/v1/marketplace/{itemId}
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "title": "Used Calculus Textbook - Price Reduced",
    "price": 35.99,
    "sold": true
}

Response: 200 OK
{
    "id": "uuid",
    "title": "Used Calculus Textbook - Price Reduced",
    "price": 35.99,
    "description": "Barely used, excellent condition",
    "category": "books",
    "condition": "like_new",
    "contactInfo": "johndoe@harvard.edu",
    "sold": true,
    "wall": "CAMPUS",
    "comments": 2,
    "author": {
        "id": "uuid",
        "profileName": "John Doe",
        "isAnonymous": false
    },
    "createdAt": "2026-02-18T...",
    "updatedAt": "2026-02-18T..."
}
```

**Partial Update Behavior:**
- All fields are **optional** in the update request
- Only provided fields will be updated
- Fields not included in the request remain unchanged
- Null-safe: setting a field to null will not update it

**Updatable Fields:**
- `title` (max 255 characters, cannot be empty/whitespace-only)
- `price` (must be ≥ 0 if provided)
- `description` (max 5000 characters)
- `category`
- `condition` (must be valid enum value)
- `contactInfo`
- `sold` (boolean - mark item as sold/unsold)

**Ownership Validation:**
- Users can only update their own items
- Attempting to update another user's item returns: `403 Forbidden`

**Error Responses:**
```json
// Attempting to update another user's item
403 Forbidden
{
    "error": "You can only update your own items"
}

// Item not found
404 Not Found
{
    "error": "Item not found"
}

// Negative price
400 Bad Request
{
    "error": "Price must be greater than or equal to 0"
}

// Empty title
400 Bad Request
{
    "error": "Title cannot be empty"
}

// Invalid condition
400 Bad Request
{
    "error": "Invalid condition. Must be one of: new, like_new, good, fair, poor"
}
```

**Update Examples:**
```http
// Mark item as sold
PUT /api/v1/marketplace/{itemId}
{
    "sold": true
}

// Update only price
PUT /api/v1/marketplace/{itemId}
{
    "price": 25.00
}

// Update multiple fields
PUT /api/v1/marketplace/{itemId}
{
    "title": "Updated Title",
    "price": 30.00,
    "description": "Updated description",
    "sold": false
}
```

#### 5. Add Comment to Marketplace Item
```http
POST /api/v1/marketplace/{itemId}/comments
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "text": "Is this still available?"
}

Response: 201 Created
{
    "id": "uuid",
    "postId": "uuid",
    "parentType": "MARKETPLACE",
    "text": "Is this still available?",
    "author": {
        "id": "uuid",
        "profileName": "Anonymous",
        "isAnonymous": true
    },
    "createdAt": "2026-02-18T..."
}
```

**Validation Rules:**
- `text` is **required** (cannot be null, empty, or whitespace-only)
- `text` maximum length: **5000 characters**
- For campus items: only users from the same school can comment
- For national items: all authenticated users can comment

#### 6. Get Comments for Marketplace Item
```http
GET /api/v1/marketplace/{itemId}/comments?page=1&limit=20&sort=NEWEST
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "postId": "uuid",
            "parentType": "MARKETPLACE",
            "text": "Is this still available?",
            "author": {
                "id": "uuid",
                "profileName": "Jane Smith",
                "isAnonymous": true
            },
            "createdAt": "2026-02-18T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 3,
        "totalPages": 1
    }
}
```

**Query Parameters:**
- `page` (default: 1) - Page number (1-based)
- `limit` (default: 20) - Comments per page (max: 100)
- `sort` (default: "NEWEST") - Sort order: NEWEST, OLDEST

#### 7. Hide Comment on Marketplace Item
```http
PATCH /api/v1/marketplace/{itemId}/comments/{commentId}/hide
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "message": "Comment hidden successfully"
}
```

**Notes:**
- Only the comment author can hide their own comment

#### 8. Unhide Comment on Marketplace Item
```http
PATCH /api/v1/marketplace/{itemId}/comments/{commentId}/unhide
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "message": "Comment unhidden successfully"
}
```

**Notes:**
- Only the comment author can unhide their own comment

### User Endpoints

#### 1. Get User's Own Comments
```http
GET /api/v1/users/me/comments?page=1&limit=20&sort=NEWEST
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "postId": "uuid",
            "parentType": "POST",
            "text": "Great post!",
            "author": {
                "id": "uuid",
                "profileName": "Jane Smith",
                "isAnonymous": true
            },
            "createdAt": "2026-01-28T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 50,
        "totalPages": 3
    }
}
```

**Query Parameters:**
- `page` (default: 1) - Page number (1-based)
- `limit` (default: 20) - Comments per page (max: 100)
- `sort` (default: "NEWEST") - Sort order: NEWEST, OLDEST

**Notes:**
- Returns all comments made by the authenticated user across all entity types (posts, internships, marketplace items)
- Each comment includes `parentType` ("POST", "INTERNSHIP", or "MARKETPLACE") to identify the parent entity
- Hidden (soft-deleted) comments are automatically excluded
- Uses optimized query with composite database index for efficient retrieval
- Performance: O(log K) where K is the user's total comment count

#### 2. Get User's Own Posts
```http
GET /api/v1/users/me/posts?page=1&limit=20&sort=NEWEST
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "title": "My Post Title",
            "content": "Post content here...",
            "wall": "campus",
            "likes": 42,
            "comments": 15,
            "liked": false,
            "author": {
                "id": "uuid",
                "profileName": "John Doe",
                "isAnonymous": true
            },
            "createdAt": "2026-01-28T...",
            "updatedAt": "2026-01-28T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 100,
        "totalPages": 5
    }
}
```

**Query Parameters:**
- `page` (default: 1) - Page number (1-based)
- `limit` (default: 20) - Posts per page (max: 100)
- `sort` (default: "NEWEST") - Sort order: NEWEST, OLDEST, MOST_LIKED, LEAST_LIKED, MOST_COMMENTED, LEAST_COMMENTED

**Notes:**
- Returns all posts created by the authenticated user
- Hidden (soft-deleted) posts are automatically excluded
- Uses optimized queries with composite database indexes for efficient retrieval
- Performance: O(log K) where K is the user's total post count
- Supports sorting by creation time, like count, or comment count

#### 3. Update Profile Name (Requires Authentication)
```http
PATCH /api/v1/users/me/profile/name
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "profileName": "John Doe"
}

Response: 200 OK
{
    "id": "uuid",
    "email": "student@harvard.edu",
    "profileName": "John Doe",
    "isVerified": true,
    "passwordSet": false,
    "createdAt": "2026-01-28T..."
}
```

**Notes:**
- Default profile name is "Anonymous"
- Sending an empty string will reset the profile name to "Anonymous"
- Profile name can be 1-255 characters
- Profile name changes are **asynchronously propagated** to all user's posts, comments, internships, and marketplace items
- The API returns immediately after updating the user profile
- Posts, comments, internships, and marketplace items are updated in the background for better performance

---

## Chat API Documentation

### Overview

The Chat API provides **one-to-one messaging** capabilities with both REST endpoints and real-time WebSocket support. Users can send direct messages to other users (except blocked users), view conversation history, and receive real-time notifications.

### Features

✅ **Real-time messaging** via WebSocket  
✅ **Message persistence** in database  
✅ **Blocked user enforcement** (cannot send/receive from blocked users)  
✅ **Read receipts** and unread message counts  
✅ **Conversation list** with last message preview  
✅ **Message history** with pagination  
✅ **WebSocket authentication** with JWT  
✅ **Session management** and automatic reconnection support  

### REST Endpoints

#### 1. Send Message
```http
POST /api/v1/chat/messages
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
  "receiverId": "uuid-of-receiver",
  "content": "Hello! This is a test message."
}

Response: 201 Created
{
  "id": "message-uuid",
  "senderId": "sender-uuid",
  "receiverId": "receiver-uuid",
  "content": "Hello! This is a test message.",
  "readStatus": false,
  "createdAt": "2026-02-14T08:00:00Z"
}
```

**Validations:**
- Message content: 1-5000 characters
- Receiver must exist and not be blocked
- Sender must not be blocked

#### 2. Get Message History
```http
GET /api/v1/chat/messages/{otherUserId}?page=1&limit=50
Authorization: Bearer {jwt-token}

Response: 200 OK
{
  "messages": [
    {
      "id": "message-uuid",
      "senderId": "user1-uuid",
      "receiverId": "user2-uuid",
      "content": "First message",
      "readStatus": true,
      "createdAt": "2026-02-14T07:00:00Z"
    },
    {
      "id": "message-uuid-2",
      "senderId": "user2-uuid",
      "receiverId": "user1-uuid",
      "content": "Reply message",
      "readStatus": false,
      "createdAt": "2026-02-14T07:01:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 50,
    "total": 2,
    "totalPages": 1
  }
}
```

**Notes:**
- Messages are returned in chronological order (oldest first)
- Default page size: 50 messages
- Maximum page size: 100 messages

#### 3. Get Conversations
```http
GET /api/v1/chat/conversations
Authorization: Bearer {jwt-token}

Response: 200 OK
{
  "conversations": [
    {
      "userId": "other-user-uuid",
      "profileName": "John Doe",
      "lastMessage": {
        "id": "message-uuid",
        "senderId": "other-user-uuid",
        "receiverId": "current-user-uuid",
        "content": "Last message in conversation",
        "readStatus": false,
        "createdAt": "2026-02-14T08:00:00Z"
      },
      "unreadCount": 3
    }
  ]
}
```

**Notes:**
- Conversations are sorted by last message timestamp (most recent first)
- Only includes conversations with at least one message
- Shows unread message count from each user

#### 4. Mark Message as Read
```http
PUT /api/v1/chat/messages/{messageId}/read
Authorization: Bearer {jwt-token}

Response: 200 OK
{
  "message": "Message marked as read"
}
```

**Access:** Only the message receiver can mark a message as read

#### 5. Mark Conversation as Read
```http
PUT /api/v1/chat/conversations/{otherUserId}/read
Authorization: Bearer {jwt-token}

Response: 200 OK
{
  "message": "Conversation marked as read"
}
```

**Effect:** Marks all messages from the specified user as read

### WebSocket Connection

#### Connection URL
```
ws://localhost:8080/ws/chat
```

#### Authentication
WebSocket connections require JWT authentication via query parameter or header:

**Option 1: Query Parameter**
```javascript
const ws = new WebSocket(`ws://localhost:8080/ws/chat?token=${jwtToken}`);
```

**Option 2: Sec-WebSocket-Protocol Header** (Recommended)
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/chat', ['access_token', jwtToken]);
```

#### Message Format

**Client to Server Messages:**

1. **Send Message**
```json
{
  "type": "message",
  "receiverId": "uuid-of-receiver",
  "content": "Message content"
}
```

2. **Typing Indicator**
```json
{
  "type": "typing",
  "receiverId": "uuid-of-receiver"
}
```

3. **Mark as Read**
```json
{
  "type": "mark_read",
  "messageId": "uuid-of-message"
}
```

**Server to Client Messages:**

1. **Connection Established**
```json
{
  "type": "connected",
  "userId": "your-user-id",
  "timestamp": 1707900000000
}
```

2. **Unread Count**
```json
{
  "type": "unread_count",
  "count": 5
}
```

3. **New Message**
```json
{
  "type": "message",
  "message": {
    "id": "message-uuid",
    "senderId": "sender-uuid",
    "receiverId": "receiver-uuid",
    "content": "Message content",
    "readStatus": false,
    "createdAt": "2026-02-14T08:00:00Z"
  }
}
```

4. **Typing Indicator**
```json
{
  "type": "typing",
  "senderId": "user-uuid"
}
```

5. **Read Receipt**
```json
{
  "type": "read_receipt",
  "messageId": "message-uuid"
}
```

6. **Error**
```json
{
  "type": "error",
  "error": "Error message"
}
```

### JavaScript WebSocket Example

```javascript
// Establish connection
const token = 'your-jwt-token';
const ws = new WebSocket('ws://localhost:8080/ws/chat', ['access_token', token]);

// Connection opened
ws.onopen = (event) => {
  console.log('Connected to chat WebSocket');
};

// Receive messages
ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  
  switch (data.type) {
    case 'connected':
      console.log('Connection confirmed, user ID:', data.userId);
      break;
      
    case 'message':
      console.log('New message:', data.message);
      displayMessage(data.message);
      break;
      
    case 'typing':
      console.log('User is typing:', data.senderId);
      showTypingIndicator(data.senderId);
      break;
      
    case 'unread_count':
      console.log('Unread messages:', data.count);
      updateUnreadBadge(data.count);
      break;
      
    case 'error':
      console.error('Error:', data.error);
      break;
  }
};

// Send a message
function sendMessage(receiverId, content) {
  ws.send(JSON.stringify({
    type: 'message',
    receiverId: receiverId,
    content: content
  }));
}

// Send typing indicator
function sendTypingIndicator(receiverId) {
  ws.send(JSON.stringify({
    type: 'typing',
    receiverId: receiverId
  }));
}

// Mark message as read
function markAsRead(messageId) {
  ws.send(JSON.stringify({
    type: 'mark_read',
    messageId: messageId
  }));
}

// Handle disconnection
ws.onclose = (event) => {
  console.log('WebSocket connection closed:', event.code, event.reason);
  // Implement reconnection logic here
};

// Handle errors
ws.onerror = (error) => {
  console.error('WebSocket error:', error);
};
```

### Error Handling

**Common Error Responses:**

1. **Unauthorized (401)**
```json
{
  "error": "Unauthorized - authentication required"
}
```

2. **Blocked User (400)**
```json
{
  "error": "Cannot send message to a blocked user"
}
```

3. **Invalid Message (400)**
```json
{
  "error": "Message content must not be empty"
}
```

4. **Forbidden (403)**
```json
{
  "error": "Only the receiver can mark a message as read"
}
```

### Database Schema

The chat feature uses the `chat_messages` table:

```sql
CREATE TABLE chat_messages (
  id CHAR(36) PRIMARY KEY,
  sender_id CHAR(36) NOT NULL,
  receiver_id CHAR(36) NOT NULL,
  content TEXT NOT NULL,
  read_status BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (sender_id) REFERENCES users(id),
  FOREIGN KEY (receiver_id) REFERENCES users(id),
  INDEX idx_chat_sender_receiver_time (sender_id, receiver_id, created_at DESC),
  INDEX idx_chat_receiver_unread (receiver_id, read_status)
);
```

### Security Considerations

1. **Authentication**: All endpoints require valid JWT token
2. **Blocked Users**: Messages cannot be sent to or from blocked users
3. **Input Validation**: Message content is sanitized and length-limited
4. **Rate Limiting**: Consider implementing rate limiting for message sending
5. **WebSocket Session Management**: Sessions are tracked per user with automatic cleanup

### Performance Optimization

- Messages are indexed by sender/receiver/timestamp for fast queries
- Unread counts are tracked efficiently with composite indexes
- WebSocket connections use concurrent session management
- Message history supports pagination to prevent large data transfers

---

## Admin API Documentation

### Overview

The Admin API provides endpoints for moderators and administrators to manage users, moderate content, and handle reports. All admin endpoints are protected with role-based access control (RBAC).

**Access Requirements:**
- 🔐 **Authentication**: Valid JWT token required
- 🛡️ **Authorization**: ADMIN or MODERATOR role required
- ⚠️ Regular users (USER role) will receive `403 Forbidden`
- ⚠️ Unauthenticated requests will receive `401 Unauthorized`

**How to Grant Admin Access:**
```sql
-- Make a user an admin
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';

-- Make a user a moderator
UPDATE users SET role = 'MODERATOR' WHERE email = 'moderator@example.com';
```

**Note:** After updating the role in the database, the user must log in again to get a new JWT token with the updated role.

---

### Admin User Management Endpoints

#### 1. List All Users
```http
GET /api/v1/admin/users?page=1&limit=20
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "email": "student@harvard.edu",
            "profileName": "John Doe",
            "schoolDomain": "harvard.edu",
            "role": "USER",
            "blocked": false,
            "verified": true,
            "passwordSet": true,
            "reportCount": 0,
            "createdAt": "2026-01-28T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 150,
        "totalPages": 8
    }
}
```

**Query Parameters:**
- `page` (default: 1) - Page number (1-based)
- `limit` (default: 20, max: 100) - Users per page
- `blocked` (optional) - Filter by blocked status (true/false)
- `sortBy` (optional) - Sort field: `createdAt`, `schoolDomain`, `reportCount`
- `sortOrder` (optional, default: desc) - Sort order: `asc` or `desc`

**Examples:**
```http
# Get blocked users sorted by report count
GET /api/v1/admin/users?blocked=true&sortBy=reportCount&sortOrder=desc

# Get all users sorted by school domain
GET /api/v1/admin/users?sortBy=schoolDomain&sortOrder=asc

# Get recent users (newest first)
GET /api/v1/admin/users?sortBy=createdAt&sortOrder=desc
```

**Access:** ADMIN or MODERATOR

#### 2. Get User Details by ID
```http
GET /api/v1/admin/users/{userId}
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "id": "uuid",
    "email": "student@harvard.edu",
    "profileName": "John Doe",
    "schoolDomain": "harvard.edu",
    "role": "USER",
    "blocked": false,
    "verified": true,
    "passwordSet": true,
    "reportCount": 2,
    "createdAt": "2026-01-28T..."
}
```

**Access:** ADMIN or MODERATOR

#### 3. Block User
```http
POST /api/v1/admin/users/{userId}/block
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "message": "User blocked successfully"
}
```

**Effect:** Blocked users are immediately restricted from:
- **Authentication**: Cannot login via email or password, cannot refresh tokens
- **Password Management**: Cannot request or complete password reset
- **Content Access**: Cannot view posts or comments (protected endpoints only)
- **Content Creation**: Cannot create posts, comments, or reactions
- **Interactions**: Cannot like posts, report content, or perform any authenticated actions
- **Token Generation**: Cannot obtain new JWT tokens

**Enforcement:**
- Blocked status is checked at authentication layer (login, password reset)
- Blocked status is enforced via HTTP server filter for all authenticated requests
- Existing JWT tokens for blocked users return 403 Forbidden on all requests
- Returns `403 Forbidden` with message: `{"error": "Access denied. Your account has been blocked."}`

**Access:** ADMIN or MODERATOR

#### 4. Unblock User
```http
POST /api/v1/admin/users/{userId}/unblock
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "message": "User unblocked successfully"
}
```

**Access:** ADMIN or MODERATOR

#### 5. Get User's Posts
```http
GET /api/v1/admin/users/{userId}/posts?page=1&limit=20
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "userId": "uuid",
            "profileName": "Anonymous",
            "title": "Post Title",
            "content": "Post content...",
            "wall": "campus",
            "schoolDomain": "harvard.edu",
            "likeCount": 42,
            "commentCount": 15,
            "hidden": false,
            "createdAt": "2026-01-28T...",
            "updatedAt": "2026-01-28T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 50,
        "totalPages": 3
    }
}
```

**Query Parameters:**
- `page` (default: 1) - Page number (1-based)
- `limit` (default: 20, max: 100) - Posts per page
- `sortBy` (optional) - Sort field: `createdAt`, `likeCount`, `commentCount`, `userId`
- `sortOrder` (optional, default: desc) - Sort order: `asc` or `desc`

**Examples:**
```http
# Get all posts by a user sorted by likes
GET /api/v1/admin/users/{userId}/posts?sortBy=likeCount&sortOrder=desc

# Get recent posts by a user
GET /api/v1/admin/users/{userId}/posts?sortBy=createdAt&sortOrder=desc
```

**Notes:**
- Returns all posts created by the specified user, including hidden (soft-deleted) posts
- Useful for investigating user activity or content patterns
- More convenient than using `/admin/posts?userId={userId}` when focusing on a single user

**Access:** ADMIN or MODERATOR

#### 6. Get User's Comments
```http
GET /api/v1/admin/users/{userId}/comments?page=1&limit=20
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "postId": "uuid",
            "userId": "uuid",
            "profileName": "Anonymous",
            "text": "Comment text...",
            "hidden": false,
            "createdAt": "2026-01-28T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 120,
        "totalPages": 6
    }
}
```

**Query Parameters:**
- `page` (default: 1) - Page number (1-based)
- `limit` (default: 20, max: 100) - Comments per page
- `sortBy` (optional) - Sort field: `createdAt`, `userId`
- `sortOrder` (optional, default: desc) - Sort order: `asc` or `desc`

**Examples:**
```http
# Get all comments by a user sorted by newest first
GET /api/v1/admin/users/{userId}/comments?sortBy=createdAt&sortOrder=desc

# Get paginated comments by a user
GET /api/v1/admin/users/{userId}/comments?page=2&limit=50
```

**Notes:**
- Returns all comments created by the specified user, including hidden (soft-deleted) comments
- Useful for investigating user activity, comment patterns, or reviewing moderation history
- More convenient than using `/admin/comments?userId={userId}` when focusing on a single user

**Access:** ADMIN or MODERATOR

---

### Admin Post Moderation Endpoints

#### 1. List All Posts
```http
GET /api/v1/admin/posts?page=1&limit=20
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "userId": "uuid",
            "profileName": "Anonymous",
            "title": "Post Title",
            "content": "Post content...",
            "wall": "campus",
            "schoolDomain": "harvard.edu",
            "likeCount": 42,
            "commentCount": 15,
            "hidden": false,
            "createdAt": "2026-01-28T...",
            "updatedAt": "2026-01-28T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 500,
        "totalPages": 25
    }
}
```

**Query Parameters:**
- `page` (default: 1) - Page number (1-based)
- `limit` (default: 20, max: 100) - Posts per page
- `userId` (optional) - Filter by user ID
- `hidden` (optional) - Filter by hidden status (true/false)
- `sortBy` (optional) - Sort field: `createdAt`, `likeCount`, `commentCount`, `userId`
- `sortOrder` (optional, default: desc) - Sort order: `asc` or `desc`

**Examples:**
```http
# Get posts sorted by likes (most liked first)
GET /api/v1/admin/posts?sortBy=likeCount&sortOrder=desc

# Get posts by a specific user
GET /api/v1/admin/posts?userId=<uuid>&sortBy=createdAt

# Get hidden posts only
GET /api/v1/admin/posts?hidden=true
```

**Notes:**
- Returns all posts including hidden (soft-deleted) posts
- Shows complete user information including user IDs

**Access:** ADMIN or MODERATOR

#### 2. Get Post by ID
```http
GET /api/v1/admin/posts/{postId}
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "id": "uuid",
    "userId": "uuid",
    "profileName": "Anonymous",
    "title": "Post Title",
    "content": "Post content...",
    "wall": "campus",
    "schoolDomain": "harvard.edu",
    "likeCount": 42,
    "commentCount": 15,
    "hidden": false,
    "createdAt": "2026-01-28T...",
    "updatedAt": "2026-01-28T..."
}
```

**Effect:**
- Retrieves a specific post by its UUID
- Returns all post details including hidden status
- Useful for investigating reported posts or reviewing specific content

**Error Responses:**
- `404 Not Found` - Post with specified ID does not exist

**Access:** ADMIN or MODERATOR

#### 3. Delete Post (Soft Delete)
```http
DELETE /api/v1/admin/posts/{postId}
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "message": "Post deleted successfully"
}
```

**Effect:**
- Post is marked as `hidden = true`
- Post is no longer visible to regular users
- Post is not physically deleted from database
- Can be unhidden by database update if needed

**Access:** ADMIN or MODERATOR

#### 4. Get Posts by Wall Type with Sorting
```http
GET /api/v1/admin/posts/by-wall?wall=national&sortBy=NEWEST&page=1&limit=20
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "userId": "uuid",
            "profileName": "Anonymous",
            "title": "Post Title",
            "content": "Post content...",
            "wall": "national",
            "schoolDomain": null,
            "likeCount": 42,
            "commentCount": 15,
            "hidden": false,
            "createdAt": "2026-01-28T...",
            "updatedAt": "2026-01-28T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 150,
        "totalPages": 8
    }
}
```

**Query Parameters:**
- `wall` (optional) - Filter by wall type: `national` or `campus`. If omitted, returns all posts.
- `sortBy` (optional, default: NEWEST) - Sort order: `NEWEST`, `OLDEST`, `MOST_LIKED`, `LEAST_LIKED`, `MOST_COMMENTED`, `LEAST_COMMENTED`
- `page` (default: 1) - Page number (1-based)
- `limit` (default: 20, max: 100) - Posts per page

**Examples:**
```http
# Get national posts sorted by newest first
GET /api/v1/admin/posts/by-wall?wall=national&sortBy=NEWEST

# Get campus posts sorted by most liked
GET /api/v1/admin/posts/by-wall?wall=campus&sortBy=MOST_LIKED

# Get all posts sorted by most commented
GET /api/v1/admin/posts/by-wall?sortBy=MOST_COMMENTED

# Get national posts with pagination
GET /api/v1/admin/posts/by-wall?wall=national&page=2&limit=50
```

**Key Features:**
- ✅ Returns posts from specified wall type (national/campus) across all schools
- ✅ Does NOT filter by schoolDomain (unlike regular user endpoints)
- ✅ Includes both hidden and non-hidden posts
- ✅ When `wall` is omitted, returns all posts regardless of wall type
- ✅ Supports same sorting options as regular users: NEWEST, OLDEST, MOST_LIKED, LEAST_LIKED, MOST_COMMENTED, LEAST_COMMENTED

**Comparison with Regular User Endpoint:**
- Regular users: Posts filtered by their school domain (can only see posts from their school)
- Admins: Can see posts from all schools without domain restrictions

**Access:** ADMIN or MODERATOR

---

### Admin Comment Moderation Endpoints

#### 1. List All Comments
```http
GET /api/v1/admin/comments?page=1&limit=20
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "postId": "uuid",
            "userId": "uuid",
            "profileName": "Anonymous",
            "text": "Comment text...",
            "hidden": false,
            "createdAt": "2026-01-28T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 1200,
        "totalPages": 60
    }
}
```

**Query Parameters:**
- `page` (default: 1) - Page number (1-based)
- `limit` (default: 20, max: 100) - Comments per page
- `userId` (optional) - Filter by user ID
- `hidden` (optional) - Filter by hidden status (true/false)
- `sortBy` (optional) - Sort field: `createdAt`, `userId`
- `sortOrder` (optional, default: desc) - Sort order: `asc` or `desc`

**Examples:**
```http
# Get comments sorted by creation time (newest first)
GET /api/v1/admin/comments?sortBy=createdAt&sortOrder=desc

# Get comments by a specific user
GET /api/v1/admin/comments?userId=<uuid>

# Get hidden comments only
GET /api/v1/admin/comments?hidden=true
```

**Notes:**
- Returns all comments including hidden (soft-deleted) comments
- Comments span all parent entity types (posts, internships, marketplace items)
- The `postId` field contains the parent entity ID regardless of parent type

**Access:** ADMIN or MODERATOR

#### 2. Get Comment by ID
```http
GET /api/v1/admin/comments/{commentId}
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "id": "uuid",
    "postId": "uuid",
    "userId": "uuid",
    "profileName": "Anonymous",
    "text": "Comment text...",
    "hidden": false,
    "createdAt": "2026-01-28T..."
}
```

**Effect:**
- Retrieves a specific comment by its UUID
- Returns all comment details including hidden status
- Useful for investigating reported comments or reviewing specific content

**Error Responses:**
- `404 Not Found` - Comment with specified ID does not exist

**Access:** ADMIN or MODERATOR

#### 3. Delete Comment (Soft Delete)
```http
DELETE /api/v1/admin/comments/{commentId}
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "message": "Comment deleted successfully"
}
```

**Effect:**
- Comment is marked as `hidden = true`
- Comment is no longer visible to regular users
- Comment count on the post is decremented
- Not physically deleted from database

**Access:** ADMIN or MODERATOR

---

### Admin Report Management Endpoints

#### 1. List All Reports
```http
GET /api/v1/admin/reports?page=1&limit=20
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "postReports": [
        {
            "id": "uuid",
            "postId": "uuid",
            "reporterUserId": "uuid",
            "reportedUserId": "uuid",
            "reason": "Inappropriate content",
            "createdAt": "2026-01-28T..."
        }
    ],
    "commentReports": [
        {
            "id": "uuid",
            "commentId": "uuid",
            "reporterUserId": "uuid",
            "reportedUserId": "uuid",
            "reason": "Spam",
            "createdAt": "2026-01-28T..."
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 50,
        "totalPages": 3
    }
}
```

**Query Parameters:**
- `page` (default: 1) - Page number (1-based)
- `limit` (default: 20, max: 100) - Reports per page
- `type` (optional) - Filter by report type: `post` or `comment`

**Examples:**
```http
# Get only post reports
GET /api/v1/admin/reports?type=post

# Get only comment reports
GET /api/v1/admin/reports?type=comment

# Get all reports (default)
GET /api/v1/admin/reports
```

**Access:** ADMIN or MODERATOR

---

### Admin School Domain Management Endpoints

#### 1. List All School Domains
```http
GET /api/v1/admin/school-domains
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
[
    {
        "id": "uuid",
        "domain": "harvard.edu",
        "schoolName": "Harvard University",
        "createdAt": "2026-01-15T..."
    },
    {
        "id": "uuid",
        "domain": "mit.edu",
        "schoolName": "MIT",
        "createdAt": "2026-01-16T..."
    }
]
```

**Description:** Retrieve all approved school email domains in the system.

**Access:** ADMIN only

#### 2. Add School Domain
```http
POST /api/v1/admin/school-domains
Authorization: Bearer {admin-jwt-token}
Content-Type: application/json

{
    "domain": "stanford.edu",
    "schoolName": "Stanford University"
}

Response: 200 OK
{
    "id": "uuid",
    "domain": "stanford.edu",
    "schoolName": "Stanford University",
    "createdAt": "2026-01-28T..."
}
```

**Description:** Add a new approved school email domain to the system. Users with emails from this domain will be able to register.

**Access:** ADMIN only

#### 3. Delete School Domain
```http
DELETE /api/v1/admin/school-domains/{id}
Authorization: Bearer {admin-jwt-token}

Response: 200 OK
{
    "message": "School domain deleted successfully"
}
```

**Description:** Remove a school domain from the approved list. This prevents new registrations from that domain but doesn't affect existing users.

**Access:** ADMIN only

**Important Notes:**
- Only ADMIN role can manage school domains (not MODERATOR)
- Deleting a domain doesn't delete existing users from that domain
- Domain validation ensures proper email domain format (e.g., "example.edu")
- Duplicate domains are prevented

---

### Admin API Security

**Role-Based Access Control (RBAC):**
- All admin endpoints require `ADMIN` or `MODERATOR` role
- Role is stored in the JWT token as an authority
- Regular users (`USER` role) receive `403 Forbidden`
- Unauthenticated requests receive `401 Unauthorized`

**JWT Token with Role:**
```json
{
    "sub": "user-id",
    "roles": ["ADMIN"],
    "email": "admin@example.com",
    "verified": true,
    "passwordSet": true,
    "exp": 1706486400
}
```

**Important Notes:**
1. ⚠️ Admin roles cannot be assigned via API (security measure)
2. ⚠️ Must manually update database to grant admin privileges
3. ⚠️ User must re-login after role change to get new JWT
4. ✅ All admin actions are logged for audit purposes
5. ✅ Soft-delete pattern preserves data for compliance

---

## Authentication & Authorization

### JWT Token
- Tokens are generated upon successful login/registration
- Include token in `Authorization: Bearer {token}` header
- Token contains user ID as principal name
- Tokens expire after configured duration
- **Blocked users cannot obtain new tokens and existing tokens are rejected**

### Blocked User Enforcement

**Centralized Security Architecture:**
- Blocked user checks are enforced at multiple layers:
  - **Authentication Layer**: Login and password reset operations check blocked status
  - **HTTP Filter Layer**: All authenticated requests are intercepted by `BlockedUserFilter` (runs after SECURITY phase)
  - **Token Generation**: JWT token service refuses to generate tokens for blocked users

**Blocked User Restrictions:**
When a user is blocked by an administrator:
1. **Immediate Effect**: Existing JWT tokens are rejected with 403 Forbidden on the next request
2. **Authentication Denied**: Cannot login via email code or password
3. **Password Reset Denied**: Cannot request or complete password reset
4. **All Protected Endpoints Blocked**: Returns `403 Forbidden` for any authenticated request
5. **Error Message**: `{"error": "Access denied. Your account has been blocked."}`

**Implementation:**
- HTTP Server Filter (`BlockedUserFilter`) intercepts all authenticated requests
- Filter executes after authentication (SECURITY phase + 10) to ensure user principal is available
- Checks user's blocked status from database on each request (fresh data, not cached from token)
- Returns 403 Forbidden immediately if user is blocked
- No controller-level code duplication required (single responsibility principle)

**How It Works:**
1. User makes request with valid JWT token
2. Micronaut security validates the JWT and extracts user ID
3. BlockedUserFilter checks the database for current blocked status
4. If blocked, request is stopped with 403 Forbidden
5. If not blocked, request proceeds normally

### Visibility Rules

#### Campus Posts / Internships / Marketplace Items
- Only visible to users from **the same school domain**
- Users from other schools receive **403 Forbidden**
- Campus wall requires user to have a school domain

#### National Posts / Internships / Marketplace Items
- Visible to **all authenticated users**
- No school domain restriction

#### Comments & Likes
- Same visibility rules as the parent entity apply
- Users from different schools cannot comment on campus posts/internships/marketplace items
- Comments use a polymorphic design: a single comment system serves all entity types

### User Authentication Flow
1. **Registration**: Email verification → Account creation → JWT issued
2. **Login (Email)**: Email code verification → JWT issued
3. **Login (Password)**: Email + password → JWT issued
4. **All Requests**: Include JWT in Authorization header

---

## Setup & Running

### Prerequisites
- Java 21 or higher
- Maven 3.9.4+
- MySQL 8+ (for local development) or Oracle Autonomous Database (for production on OCI)
- Redis (optional, for caching)
- Docker/Podman and docker-compose/podman-compose (for containerized deployment)

### Configuration Files

The application uses **environment-specific profiles**:

| Profile | File | Usage | Database | Log Directory |
|---------|------|-------|----------|---------------|
| **Default** | `application.properties` | Fallback/production base | Environment variables | `./logs` |
| **Development** | `application-dev.properties` | Local development | Your local MySQL (ziyihuang) | `./logs` (project root) |
| **Production** | `application-prod.properties` | Production deployment | Environment variables (required) | `/var/log/anonymouswall` |
| **Tests** | `src/test/resources/application.properties` | Running tests | Same as development | `./logs` |

#### Local Development Configuration

**File:** `src/main/resources/application-dev.properties`

```properties
# Your local database settings
datasources.default.url=jdbc:mysql://localhost:3306/anonymous_wall
datasources.default.username=ziyihuang
datasources.default.password=HZYhzy@2014
redis.uri=redis://localhost:6379
```

**No setup needed** - just run with the dev profile!

#### Production Configuration

**File:** `src/main/resources/application-prod.properties`

For production on OCI with Oracle Autonomous Database, set **required environment variables**:

```bash
export MICRONAUT_ENVIRONMENTS=prod

# Required - JWT signing key (minimum 32 characters)
export JWT_GENERATOR_SIGNATURE_SECRET="your-secret-key-min-32-chars"

# Required - Database connection (Oracle Autonomous Database)
export DATABASE_URL="jdbc:oracle:thin:@your-adb-connection-string"
export DATABASE_USER="ADMIN"
export DATABASE_PASSWORD="prod_password"

# Required - Redis connection
export REDIS_URI="redis://redis-host:6379"

# Optional - Logging directory (defaults to /var/log/anonymouswall)
export LOG_DIR="/var/log/anonymouswall"
```

#### Logging Configuration

The application uses **Log4j2** with environment-specific log directories:

**Development (application-dev.properties):**
- Logs written to `./logs` directory in project root
- Files: `anonymouswall.log`, `anonymouswall-error.log`, `anonymouswall-debug.log`
- Perfect for local development and debugging

**Production (application-prod.properties):**
- Logs written to `/var/log/anonymouswall` (standard Linux log directory)
- Can be overridden with `LOG_DIR` environment variable

**Log Files Generated:**
- `anonymouswall.log` - Main application logs (daily rotation or 10MB threshold)
- `anonymouswall-error.log` - ERROR level logs only
- `anonymouswall-debug.log` - DEBUG level logs only
- Old logs are compressed (.gz) and retained: 30 days for main/error, 10 days for debug

**Configuration:**
```bash
# Development - logs to project root
MICRONAUT_ENVIRONMENTS=dev ./mvnw mn:run
# Logs appear in ./logs/

# Production - logs to standard location
export MICRONAUT_ENVIRONMENTS=prod
export LOG_DIR=/var/log/anonymouswall
java -jar target/anonymouswall-0.1.jar
# Logs appear in /var/log/anonymouswall/

# Override with custom path
export LOG_DIR=/custom/path/to/logs
java -jar target/anonymouswall-0.1.jar
```

For detailed logging information, see [LOGGING_CONFIGURATION.md](LOGGING_CONFIGURATION.md).

### Build & Run

#### Local Development

```bash
# Option 1: Using Micronaut Maven plugin (recommended for dev)
MICRONAUT_ENVIRONMENTS=dev ./mvnw mn:run

# Option 2: Build JAR and run
mvn clean package
MICRONAUT_ENVIRONMENTS=dev java -jar target/anonymouswall-0.1.jar

# Option 3: Just run (uses application.properties defaults)
./mvnw mn:run
java -jar target/anonymouswall-0.1.jar
```

#### Running Tests

```bash
# Run all tests (uses test profile with local dev settings)
mvn clean test

# Run specific test
mvn test -Dtest=AuthControllerTest

# Run with coverage
mvn clean test jacoco:report
```

#### Production Deployment

**Option 1: Using Podman (OCI Infrastructure)**

See [DEPLOYMENT.md](DEPLOYMENT.md) for complete guide on deploying to OCI instances.

```bash
# Quick start with podman-compose (on OCI)
cp .env.example .env
# Edit .env with your configuration
podman-compose -f docker-compose.prod.yml up -d
```

**Option 2: Direct JAR Deployment**

```bash
# Set environment variables first (see Production Configuration above)
export MICRONAUT_ENVIRONMENTS=prod
export JWT_GENERATOR_SIGNATURE_SECRET="..."
export DATABASE_URL="..."
export DATABASE_USER="..."
export DATABASE_PASSWORD="..."
export REDIS_URI="..."

# Build and run
mvn clean package
java -jar target/anonymouswall-0.1.jar
```

**For OCI Infrastructure Deployment:**

This application is designed to be deployed on Oracle Cloud Infrastructure. See:
- [DEPLOYMENT.md](DEPLOYMENT.md) - Complete deployment guide
- [AnonymousWallInfra](https://github.com/AnonymousWall/AnonymousWallInfra) - Infrastructure as Code (Terraform)

### Database Initialization

The application uses **Liquibase** for automatic schema migrations. The schema is created automatically when the application starts.

**Schema includes:**
- Users table with school domain segregation
- Posts table with campus/national separation
- Comments table with soft-delete support (hidden flag)
- Post likes tracking
- Email verification codes with expiration
- JWT tokens (if using token storage)

### API Documentation

Once running, access Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

OpenAPI spec available at:
```
http://localhost:8080/swagger/anonymouswall-0.0.yml
```

### Quick Start

**Local Development:**
```bash
# 1. Build the project
mvn clean package

# 2. Run with dev profile
MICRONAUT_ENVIRONMENTS=dev ./mvnw mn:run

# 3. Access API
curl http://localhost:8080/health
```

**Docker/Podman Deployment:**
```bash
# 1. Create configuration
cp .env.example .env
# Edit .env with your values

# 2. Start with docker-compose (local) or podman-compose (OCI)
docker-compose up -d  # Local development
# or
podman-compose up -d  # OCI production

# 3. Check health
curl http://localhost:8080/health
```

**OCI Production Deployment:**
See [DEPLOYMENT.md](DEPLOYMENT.md) for complete instructions on deploying to Oracle Cloud Infrastructure.

---

## Deployment

### Container Deployment

This application is containerized and ready for deployment using Docker/Podman and docker-compose/podman-compose.

**Quick Start:**
```bash
# Local testing with dependencies (uses Docker)
docker-compose up -d

# Production deployment on OCI (uses Podman)
podman-compose -f docker-compose.prod.yml up -d
```

**Deployment Files:**
- `Dockerfile` - Multi-stage build for optimized image
- `docker-compose.yml` - Full stack with MySQL and Redis (local development)
- `docker-compose.prod.yml` - Production config for OCI (Oracle ADB)
- `deploy.sh` - Automated deployment script (uses Podman)
- `.env.example` - Configuration template

### OCI Infrastructure

This application is designed to deploy on Oracle Cloud Infrastructure using the infrastructure defined in [AnonymousWallInfra](https://github.com/AnonymousWall/AnonymousWallInfra).

**Architecture:**
- Compute instances in private subnet (no public IPs)
- Load balancer for traffic distribution
- OCI Autonomous Database (ADB) - Oracle Database
- Health checks on `/health` endpoint
- Port 8080 for application traffic

**Deployment Guide:**
See [DEPLOYMENT.md](DEPLOYMENT.md) for:
- Step-by-step deployment instructions
- Environment configuration
- SSH access via bastion host
- Monitoring and troubleshooting
- Scaling and updates

**Infrastructure Setup:**
1. Deploy infrastructure: [QUICKSTART.md](https://github.com/AnonymousWall/AnonymousWallInfra/blob/main/QUICKSTART.md)
2. Configure environment variables
3. Deploy application using `deploy.sh`
4. Verify health via load balancer

---

## Known Flaws & Limitations

### 1. **School Domain Extraction is Basic**
**Issue**: School domain is extracted from email domain without validation
```java
// Example: student@harvard.edu → "harvard.edu"
// No verification that harvard.edu is actually a valid school
```
**Impact**: Any email domain is accepted as a school domain  
**Recommendation**: Maintain a whitelist of valid school domains

### 2. **No Email Verification Code Expiration Enforcement**
**Issue**: Code expiration is checked but old codes aren't cleaned up  
**Impact**: Database accumulates expired codes; no active cleanup job  
**Recommendation**: Add scheduled task to delete expired codes

### 3. **No Rate Limiting on Email Code Requests**
**Issue**: Users can request unlimited email codes in quick succession  
**Impact**: Potential spam/abuse; email quota exhaustion  
**Recommendation**: Implement rate limiting (e.g., max 5 requests per hour per email)

### 4. ✅ **Soft Delete for Posts/Comments** - IMPLEMENTED
**Status**: FIXED in v0.1
- Posts and comments support soft delete (hidden flag)
- Uses `@Transactional` to ensure atomic operations
- Cascade hide: hiding a post hides all its comments
- Comments remain in DB but marked as hidden
- See: `PostsServiceImpl.hidePost()`, `hideComment()`, `unhidePost()`, `unhideComment()`

### 5. ✅ **Atomic Like Count** - IMPLEMENTED
**Status**: FIXED in v0.1
- Like toggle uses atomic SQL operations
- Denormalized `like_count` in Posts table
- Protected with `@Transactional` and `@Retryable`
- Transaction rollback on failure
- Comprehensive concurrency tests verify thread safety
- See: `PostsServiceImpl.toggleLike()`, concurrency tests

### 6. ✅ **Atomic Comment Count** - IMPLEMENTED
**Status**: FIXED in v0.1
- Comment count atomic with comment save
- Denormalized `comment_count` in Posts table
- Protected with `@Transactional` and `@Retryable`
- Cascade operations maintain consistency
- Transaction tests verify atomicity
- See: `PostsServiceImpl.addComment()`, transaction tests

### 7. **No Content Moderation**
**Issue**: Posts and comments are not filtered for inappropriate content  
**Impact**: Potential for harassment/spam  
**Recommendation**: Add content filtering (e.g., keyword blocking, AI moderation)

### 8. ✅ **Pagination on Comments** - IMPLEMENTED
**Status**: FIXED in v0.1
- All endpoints support pagination and sorting
- Default page size: 20, max: 100
- See: `PostsController.getComments()`, `getPostsWithComments()`

### 9. **School Domain Always Stored for Campus Posts**
**Issue**: `school_domain` field stores redundant data (can be extracted from user)  
**Impact**: Data duplication; potential inconsistency if user's school changes  
**Recommendation**: Store only `user_id` and fetch domain via join

### 10. **No User Profile Visibility Controls**
**Issue**: User data exposed in API responses (though marked anonymous)  

---

## Testing & Quality Assurance

### Test Coverage

The application includes **450+ tests** covering:

#### Unit Tests (400+)
- Controller tests (9 test classes)
- Service layer tests (1 test class)
- Utility function tests (1 test class)
- Entity model tests
- Repository tests

#### Concurrency Tests (10)
Tests verify thread safety under concurrent access:
- Concurrent comment addition
- Concurrent like toggling
- Concurrent mixed operations
- High-load scenarios

#### Transaction Tests (20)
Tests verify ACID compliance:
- Atomicity: All-or-nothing operations
- Consistency: Count accuracy and referential integrity
- Isolation: No dirty reads
- Durability: Data persistence

#### Soft Delete Tests
- Hide/unhide operations
- Cascade hide behavior
- Consistency after hide operations
- Visibility filtering

### Running Tests

```bash
# All tests
mvn clean test

# Concurrency tests only
mvn test -Dtest=*Concurrency*

# Transaction tests only
mvn test -Dtest=*Transaction*

# With code coverage
mvn clean test jacoco:report
```

### Fixed Issues in v0.1

✅ **Race Conditions** - Atomic SQL operations with versioning  
✅ **Transaction Safety** - @Transactional on all service methods  
✅ **Resilience** - @Retryable with 3 attempts, 500ms delay  
✅ **Cascade Operations** - Foreign key constraints with CASCADE DELETE  
✅ **Soft Delete** - Hide/unhide with data preservation  
✅ **Consistency** - Comprehensive transaction tests verify ACID compliance  
✅ **Test Database** - H2 in-memory with MySQL compatibility mode  

---**Impact**: User ID is visible in every post/comment  
**Recommendation**: Consider hashing/obfuscating user IDs or randomizing display

### 11. **Password Hash Algorithm Not Specified**
**Issue**: `PasswordUtil.java` implementation not reviewed  
**Impact**: Could be using weak algorithm  
**Recommendation**: Ensure BCrypt or Argon2 is used

### 12. **No Concurrent User Limit**
**Issue**: No check for duplicate logins or concurrent sessions  
**Impact**: Same user can be logged in from multiple locations simultaneously  
**Recommendation**: Track active sessions, enforce single-session-per-device

---

## Error Handling

All errors return JSON response:

```json
{
    "error": "Error message here"
}
```

### HTTP Status Codes
- `200 OK` - Success
- `201 Created` - Resource created
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Missing/invalid JWT token
- `403 Forbidden` - User doesn't have access (wrong school domain)
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource already exists (email already registered)
- `500 Internal Server Error` - Server error

---

## Testing

The project includes comprehensive test coverage:

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=AuthControllerTest

# Run with coverage
mvn clean test jacoco:report
```

### Test Suites
- **AuthControllerTest** - 15+ tests for authentication
- **AuthServiceImplTest** - 20+ tests for auth business logic
- **PostsCreateControllerTest** - 16 tests for post creation
- **PostsServiceImplCreatePostTest** - 20+ tests for post service logic
- **PostEntityTest** - 25+ tests for post entity

Total: **95+ tests with 100% coverage** of core functionality

---

## Future Improvements

1. ✅ Implement email domain whitelist
2. ✅ Add scheduled cleanup of expired verification codes
3. ✅ Implement rate limiting on authentication endpoints
4. ✅ Add soft delete for posts/comments
5. ✅ Denormalize like/comment counts with atomic operations
6. ✅ Implement content moderation
7. ✅ Add pagination to comments
8. ✅ Implement user profile visibility controls
9. ✅ Add audit logging for important actions
10. ✅ Implement notification system for likes/comments
11. ✅ Add user reputation/karma system
12. ✅ Implement post reporting/flagging

---

## License

Proprietary - Anonymous Wall Project

---

## Support

For issues or questions, please refer to the test files and API documentation in Swagger UI.

- [https://www.liquibase.org/](https://www.liquibase.org/)

## Feature jul-to-slf4j documentation

- [https://www.slf4j.org/legacy.html#jul-to-slf4jBridge](https://www.slf4j.org/legacy.html#jul-to-slf4jBridge)

## Feature guice documentation

- [Micronaut Guice documentation](https://micronaut-projects.github.io/micronaut-guice/latest/guide/index.html)

## Feature maven-enforcer-plugin documentation

- [https://maven.apache.org/enforcer/maven-enforcer-plugin/](https://maven.apache.org/enforcer/maven-enforcer-plugin/)

## Feature micronaut-aot documentation

- [Micronaut AOT documentation](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)

## Feature data-jdbc documentation

- [Micronaut Data JDBC documentation](https://micronaut-projects.github.io/micronaut-data/latest/guide/index.html#jdbc)

## Feature reactor documentation

- [Micronaut Reactor documentation](https://micronaut-projects.github.io/micronaut-reactor/snapshot/guide/index.html)

## Feature jdbc-hikari documentation

- [Micronaut Hikari JDBC Connection Pool documentation](https://micronaut-projects.github.io/micronaut-sql/latest/guide/index.html#jdbc)

## Feature jms-core documentation

- [Micronaut JMS documentation](https://micronaut-projects.github.io/micronaut-jms/snapshot/guide/index.html)

## Feature http-client documentation

- [Micronaut HTTP Client documentation](https://docs.micronaut.io/latest/guide/index.html#nettyHttpClient)

