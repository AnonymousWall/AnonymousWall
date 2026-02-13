package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JwtTokenService
 * Verifies JWT token generation with all claims including schoolDomain optimization
 */
@MicronautTest
@DisplayName("JwtTokenService Tests")
class JwtTokenServiceTest {

    @Inject
    private JwtTokenService jwtTokenService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@harvard.edu");
        testUser.setSchoolDomain("harvard.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate token with all standard claims")
        void shouldGenerateTokenWithStandardClaims() throws Exception {
            String token = jwtTokenService.generateToken(testUser);

            assertNotNull(token, "Token should not be null");
            assertFalse(token.isEmpty(), "Token should not be empty");

            // Parse and verify claims
            JWT jwt = JWTParser.parse(token);
            Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();

            assertEquals(testUser.getId().toString(), jwt.getJWTClaimsSet().getSubject(), "Subject should be user ID");
            assertEquals(testUser.getEmail(), claims.get("email"), "Email claim should match");
            assertEquals(testUser.isVerified(), claims.get("verified"), "Verified claim should match");
            assertEquals(testUser.isPasswordSet(), claims.get("passwordSet"), "PasswordSet claim should match");
        }

        @Test
        @DisplayName("Should include schoolDomain in JWT claims when present")
        void shouldIncludeSchoolDomainInClaims() throws Exception {
            String token = jwtTokenService.generateToken(testUser);

            JWT jwt = JWTParser.parse(token);
            Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();

            assertTrue(claims.containsKey("schoolDomain"), "Claims should contain schoolDomain");
            assertEquals("harvard.edu", claims.get("schoolDomain"), "SchoolDomain should match user's school");
        }

        @Test
        @DisplayName("Should not include schoolDomain when user has no school")
        void shouldNotIncludeSchoolDomainWhenNull() throws Exception {
            testUser.setSchoolDomain(null);
            String token = jwtTokenService.generateToken(testUser);

            JWT jwt = JWTParser.parse(token);
            Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();

            assertFalse(claims.containsKey("schoolDomain"), "Claims should not contain schoolDomain when null");
        }

        @Test
        @DisplayName("Should generate token with custom claims")
        void shouldGenerateTokenWithCustomClaims() throws Exception {
            Map<String, Object> customClaims = new HashMap<>();
            customClaims.put("role", "admin");
            customClaims.put("customField", "customValue");

            String token = jwtTokenService.generateToken(testUser, customClaims);

            JWT jwt = JWTParser.parse(token);
            Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();

            assertEquals("admin", claims.get("role"), "Custom role claim should be present");
            assertEquals("customValue", claims.get("customField"), "Custom field claim should be present");
        }

        @Test
        @DisplayName("Should include schoolDomain with custom claims")
        void shouldIncludeSchoolDomainWithCustomClaims() throws Exception {
            Map<String, Object> customClaims = new HashMap<>();
            customClaims.put("role", "student");

            String token = jwtTokenService.generateToken(testUser, customClaims);

            JWT jwt = JWTParser.parse(token);
            Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();

            assertEquals("harvard.edu", claims.get("schoolDomain"), "SchoolDomain should be present with custom claims");
            assertEquals("student", claims.get("role"), "Custom role claim should be present");
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            UserEntity user2 = new UserEntity();
            user2.setId(UUID.randomUUID());
            user2.setEmail("test2@mit.edu");
            user2.setSchoolDomain("mit.edu");
            user2.setVerified(true);
            user2.setPasswordSet(true);

            String token1 = jwtTokenService.generateToken(testUser);
            String token2 = jwtTokenService.generateToken(user2);

            assertNotEquals(token1, token2, "Tokens for different users should be different");
        }
    }

    @Nested
    @DisplayName("School Domain Claim Tests")
    class SchoolDomainClaimTests {

        @Test
        @DisplayName("Should include schoolDomain for user with edu domain")
        void shouldIncludeSchoolDomainForEduDomain() throws Exception {
            testUser.setSchoolDomain("stanford.edu");
            String token = jwtTokenService.generateToken(testUser);

            JWT jwt = JWTParser.parse(token);
            assertEquals("stanford.edu", jwt.getJWTClaimsSet().getClaim("schoolDomain"));
        }

        @Test
        @DisplayName("Should include schoolDomain for user with different school")
        void shouldIncludeSchoolDomainForDifferentSchool() throws Exception {
            testUser.setSchoolDomain("mit.edu");
            String token = jwtTokenService.generateToken(testUser);

            JWT jwt = JWTParser.parse(token);
            assertEquals("mit.edu", jwt.getJWTClaimsSet().getClaim("schoolDomain"));
        }

        @Test
        @DisplayName("Should handle empty schoolDomain as null")
        void shouldHandleEmptySchoolDomainAsNull() throws Exception {
            testUser.setSchoolDomain("");
            String token = jwtTokenService.generateToken(testUser);

            JWT jwt = JWTParser.parse(token);
            Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();

            // Empty string is treated as present but empty
            assertTrue(claims.containsKey("schoolDomain"), "Empty schoolDomain should be included");
            assertEquals("", claims.get("schoolDomain"), "Empty schoolDomain should be empty string");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw exception when user is null")
        void shouldThrowExceptionWhenUserIsNull() {
            assertThrows(IllegalArgumentException.class, () -> {
                jwtTokenService.generateToken(null);
            }, "Should throw IllegalArgumentException for null user");
        }

        @Test
        @DisplayName("Should throw exception when user ID is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            testUser.setId(null);
            assertThrows(IllegalArgumentException.class, () -> {
                jwtTokenService.generateToken(testUser);
            }, "Should throw IllegalArgumentException for null user ID");
        }

        @Test
        @DisplayName("Should throw exception when user is null with custom claims")
        void shouldThrowExceptionWhenUserIsNullWithCustomClaims() {
            Map<String, Object> customClaims = new HashMap<>();
            assertThrows(IllegalArgumentException.class, () -> {
                jwtTokenService.generateToken(null, customClaims);
            }, "Should throw IllegalArgumentException for null user with custom claims");
        }

        @Test
        @DisplayName("Should handle null custom claims gracefully")
        void shouldHandleNullCustomClaimsGracefully() throws Exception {
            String token = jwtTokenService.generateToken(testUser, null);

            assertNotNull(token, "Token should be generated even with null custom claims");

            JWT jwt = JWTParser.parse(token);
            Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();

            // Should still have standard claims
            assertEquals(testUser.getEmail(), claims.get("email"));
            assertEquals("harvard.edu", claims.get("schoolDomain"));
        }
    }

    @Nested
    @DisplayName("Token Expiration Tests")
    class TokenExpirationTests {

        @Test
        @DisplayName("Should set token expiration to 24 hours")
        void shouldSetTokenExpirationTo24Hours() throws Exception {
            String token = jwtTokenService.generateToken(testUser);

            JWT jwt = JWTParser.parse(token);
            long issuedAt = jwt.getJWTClaimsSet().getIssueTime().getTime() / 1000;
            long expiresAt = jwt.getJWTClaimsSet().getExpirationTime().getTime() / 1000;
            long expirationDuration = expiresAt - issuedAt;

            // 24 hours = 86400 seconds
            assertEquals(86400, expirationDuration, "Token should expire in 24 hours (86400 seconds)");
        }
    }

    @Nested
    @DisplayName("Integration with Optimization Tests")
    class OptimizationIntegrationTests {

        @Test
        @DisplayName("Should generate token with schoolDomain for optimization use case")
        void shouldGenerateTokenForOptimizationUseCase() throws Exception {
            // This simulates the real use case: generating token at login
            UserEntity harvardUser = new UserEntity();
            harvardUser.setId(UUID.randomUUID());
            harvardUser.setEmail("student@harvard.edu");
            harvardUser.setSchoolDomain("harvard.edu");
            harvardUser.setVerified(true);
            harvardUser.setPasswordSet(true);

            String token = jwtTokenService.generateToken(harvardUser);

            JWT jwt = JWTParser.parse(token);
            Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();

            // Verify all claims needed for optimization are present
            assertNotNull(jwt.getJWTClaimsSet().getSubject(), "User ID should be in subject");
            assertEquals("student@harvard.edu", claims.get("email"), "Email should be present");
            assertEquals("harvard.edu", claims.get("schoolDomain"), "SchoolDomain should be present for optimization");
            assertTrue((Boolean) claims.get("verified"), "Verified status should be present");
            assertTrue((Boolean) claims.get("passwordSet"), "PasswordSet status should be present");
        }

        @Test
        @DisplayName("Should allow controller to extract schoolDomain from generated token")
        void shouldAllowControllerToExtractSchoolDomain() throws Exception {
            String token = jwtTokenService.generateToken(testUser);

            JWT jwt = JWTParser.parse(token);
            String extractedSchoolDomain = (String) jwt.getJWTClaimsSet().getClaim("schoolDomain");

            assertEquals("harvard.edu", extractedSchoolDomain, "Controller should be able to extract schoolDomain");
            assertNotNull(extractedSchoolDomain, "Extracted schoolDomain should not be null");
        }
    }

    @Nested
    @DisplayName("Blocked User Tests")
    class BlockedUserTests {

        @Test
        @DisplayName("Should reject token generation for blocked user")
        void shouldRejectTokenGenerationForBlockedUser() {
            // Arrange
            testUser.setBlocked(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtTokenService.generateToken(testUser)
            );
            assertTrue(exception.getMessage().contains("blocked"));
        }

        @Test
        @DisplayName("Should reject token generation with custom claims for blocked user")
        void shouldRejectTokenGenerationWithCustomClaimsForBlockedUser() {
            // Arrange
            testUser.setBlocked(true);
            Map<String, Object> customClaims = new HashMap<>();
            customClaims.put("customKey", "customValue");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtTokenService.generateToken(testUser, customClaims)
            );
            assertTrue(exception.getMessage().contains("blocked"));
        }

        @Test
        @DisplayName("Should allow token generation for non-blocked user")
        void shouldAllowTokenGenerationForNonBlockedUser() {
            // Arrange
            testUser.setBlocked(false);

            // Act
            String token = jwtTokenService.generateToken(testUser);

            // Assert
            assertNotNull(token);
            assertFalse(token.isEmpty());
        }
    }
}
