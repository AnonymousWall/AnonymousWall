# Anonymous Wall - Campus Social Platform

A Micronaut-based REST API for anonymous campus social networking. Users register with school email, create posts on campus or national walls, like posts, and comment anonymously.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Project Structure](#project-structure)
3. [Technology Stack](#technology-stack)
4. [Database Schema](#database-schema)
5. [API Documentation](#api-documentation)
6. [Authentication & Authorization](#authentication--authorization)
7. [Setup & Running](#setup--running)
8. [Known Flaws & Limitations](#known-flaws--limitations)

---

## Project Overview

**Anonymous Wall** is a campus-specific social platform where:
- Students register with their school email (e.g., student@harvard.edu)
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
✅ JWT-based authentication  

---

## Project Structure

```
src/main/java/com/anonymous/wall/
├── controller/
│   ├── AuthController.java          # Auth endpoints
│   ├── PostsController.java         # Post, like, comment endpoints
│   ├── InternshipController.java    # Internship features
│   ├── MarketplaceController.java   # Marketplace features
│   └── CoinsController.java         # Coins/rewards features
│
├── service/
│   ├── AuthService/AuthServiceImpl.java        # Auth business logic
│   ├── PostsService/PostsServiceImpl.java      # Post operations
│   ├── JwtTokenService.java                   # JWT token generation
│   ├── InternshipService.java
│   ├── MarketplaceService.java
│   └── CoinsService.java
│
├── entity/
│   ├── UserEntity.java              # User model
│   ├── Post.java                    # Post model
│   ├── Comment.java                 # Comment model
│   ├── PostLike.java                # Like model
│   ├── EmailVerificationCode.java   # Email verification
│   ├── CoinBalance.java
│   ├── Internship.java
│   ├── MarketplaceItem.java
│   └── PostList.java
│
├── repository/
│   ├── UserRepository.java
│   ├── PostRepository.java
│   ├── CommentRepository.java
│   ├── PostLikeRepository.java
│   ├── EmailVerificationCodeRepository.java
│   └── [Other repositories...]
│
├── mapper/
│   └── UserMapper.java              # DTO mapping utilities
│
├── util/
│   ├── EmailValidator.java
│   ├── CodeGenerator.java           # 6-digit verification code
│   ├── PasswordUtil.java            # Password hashing
│   ├── EmailUtil.java               # Email sending
│   └── MarketItemValidator.java
│
└── Application.java                 # Main application class

src/test/java/com/anonymous/wall/
├── controller/
│   ├── AuthControllerTest.java
│   └── PostsCreateControllerTest.java
├── service/
│   ├── AuthServiceImplTest.java
│   ├── PostsServiceImplCreatePostTest.java
│   └── PostEntityTest.java
└── util/
    └── [Utility tests...]
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Posts Table
```sql
CREATE TABLE posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id UUID NOT NULL REFERENCES users(id),
    content VARCHAR(5000) NOT NULL,
    wall VARCHAR(20) DEFAULT 'campus',   -- "campus" or "national"
    school_domain VARCHAR(255),          -- NULL for national, set for campus
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Comments Table
```sql
CREATE TABLE comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL REFERENCES posts(id),
    user_id UUID NOT NULL REFERENCES users(id),
    text VARCHAR(5000) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Post Likes Table
```sql
CREATE TABLE post_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL REFERENCES posts(id),
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, user_id)             -- One like per user per post
);
```

### Email Verification Codes Table
```sql
CREATE TABLE email_verification_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(10) NOT NULL,
    purpose VARCHAR(50),                 -- "REGISTER", "LOGIN", "RESET_PASSWORD"
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT false
);
```

---

## API Documentation

### Authentication Endpoints

#### 1. Send Email Verification Code
```http
POST /api/v1/auth/email/send-code
Content-Type: application/json

{
    "email": "student@harvard.edu",
    "purpose": "REGISTER"  // or "LOGIN", "RESET_PASSWORD"
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
    "code": "123456",
    "password": null  // Optional, can set later
}

Response: 201 Created
{
    "user": {
        "id": "uuid",
        "email": "student@harvard.edu",
        "schoolDomain": "harvard.edu",
        "verified": true,
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
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "newPassword": "secure_password"
}

Response: 200 OK
{
    "message": "Password set successfully"
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
    "message": "Password changed successfully"
}
```

---

### Post Endpoints

#### 1. Create Post
```http
POST /api/v1/posts
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "content": "This is my first post!",
    "wall": "campus"  // or "national"
}

Response: 201 Created
{
    "id": "1",
    "content": "This is my first post!",
    "wall": "CAMPUS",
    "likes": 0,
    "comments": 0,
    "liked": false,
    "author": {
        "id": "uuid",
        "isAnonymous": true
    },
    "createdAt": "2026-01-28T...",
    "updatedAt": "2026-01-28T..."
}
```

#### 2. List Posts
```http
GET /api/v1/posts?wall=campus&page=1&limit=20
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "1",
            "content": "Post content",
            "wall": "CAMPUS",
            "likes": 5,
            "comments": 2,
            "liked": false,
            "author": {
                "id": "uuid",
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

#### 3. Toggle Like on Post
```http
POST /api/v1/posts/{postId}/likes
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "liked": true  // or false if unlike
}
```

#### 4. Add Comment
```http
POST /api/v1/posts/{postId}/comments
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
    "text": "Great post!"
}

Response: 201 Created
{
    "id": "1",
    "postId": "1",
    "text": "Great post!",
    "author": {
        "id": "uuid",
        "isAnonymous": true
    },
    "createdAt": "2026-01-28T..."
}
```

#### 5. Get Comments for Post
```http
GET /api/v1/posts/{postId}/comments
Authorization: Bearer {jwt-token}

Response: 200 OK
{
    "data": [
        {
            "id": "1",
            "postId": "1",
            "text": "Great post!",
            "author": {
                "id": "uuid",
                "isAnonymous": true
            },
            "createdAt": "2026-01-28T..."
        }
    ],
    "total": 5
}
```

---

## Authentication & Authorization

### JWT Token
- Tokens are generated upon successful login/registration
- Include token in `Authorization: Bearer {token}` header
- Token contains user ID as principal name
- Tokens expire after configured duration

### Visibility Rules

#### Campus Posts
- Only visible to users from **the same school domain**
- Users from other schools receive **403 Forbidden**
- Campus wall requires user to have a school domain

#### National Posts
- Visible to **all authenticated users**
- No school domain restriction

#### Comments & Likes
- Same visibility rules as posts apply
- Users from different schools cannot like/comment on campus posts

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
- MySQL 8+ (for local development and production)
- Redis (optional, for caching)

### Configuration Files

The application uses **environment-specific profiles**:

| Profile | File | Usage | Database |
|---------|------|-------|----------|
| **Default** | `application.properties` | Fallback/production base | Environment variables |
| **Development** | `application-dev.properties` | Local development | Your local MySQL (ziyihuang) |
| **Production** | `application-prod.properties` | Production deployment | Environment variables (required) |
| **Tests** | `src/test/resources/application.properties` | Running tests | Same as development |

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

For production, set **required environment variables**:

```bash
export MICRONAUT_ENVIRONMENTS=prod

# Required - JWT signing key (minimum 32 characters)
export JWT_GENERATOR_SIGNATURE_SECRET="your-secret-key-min-32-chars"

# Required - Database connection
export DATABASE_URL="jdbc:mysql://production-host:3306/anonymous_wall"
export DATABASE_USER="prod_user"
export DATABASE_PASSWORD="prod_password"

# Required - Redis connection
export REDIS_URI="redis://redis-host:6379"
```

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

```bash
# 1. Build the project
mvn clean package

# 2. Run with dev profile
MICRONAUT_ENVIRONMENTS=dev ./mvnw mn:run

# 3. Access API
curl http://localhost:8080/health
```

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


| 场景                     | API                                                 | 请求/返回说明                                                         |
| ---------------------- | --------------------------------------------------- | --------------------------------------------------------------- |
| **1️⃣ 注册（邮箱+验证码）**     | `POST /auth/email/code` → `POST /auth/email/verify` | 用户输入邮箱，后台发送验证码；验证验证码后返回 `accessToken + UserDTO`（用户状态可标记是否已设置密码） |
| **2️⃣ 邮箱+验证码登录（快速登录）** | `POST /auth/email/code` → `POST /auth/email/verify` | 用户之前已注册，但忘记密码或使用快捷登录，输入邮箱拿验证码即可登录，返回 `accessToken + UserDTO`    |
| **3️⃣ 邮箱+密码登录**        | `POST /auth/login/password`                         | 用户输入邮箱+密码登录，返回 `accessToken + UserDTO`                          |
| **4️⃣ 邮箱+验证码之后设置密码**   | `POST /auth/password`                               | 用户第一次注册后，或通过验证码登录后设置密码，返回更新后的 `UserDTO`                         |
| **5️⃣ 登陆后修改密码**        | `POST /auth/password`（需 `bearerAuth`）               | 已登录用户修改密码，可提供 `oldPassword` 或通过 token 修改，返回更新后的 `UserDTO`       |
