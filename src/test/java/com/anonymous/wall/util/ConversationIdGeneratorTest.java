package com.anonymous.wall.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ConversationIdGenerator.
 * Ensures deterministic UUID generation for conversations between two users.
 */
@DisplayName("ConversationIdGenerator Tests")
class ConversationIdGeneratorTest {

    @Nested
    @DisplayName("Deterministic Generation")
    class DeterministicGeneration {

        @Test
        @DisplayName("Should generate same UUID for same input order")
        void shouldGenerateSameUuidForSameInputOrder() {
            // Arrange
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();

            // Act
            UUID conversationId1 = ConversationIdGenerator.generate(user1, user2);
            UUID conversationId2 = ConversationIdGenerator.generate(user1, user2);

            // Assert
            assertNotNull(conversationId1);
            assertNotNull(conversationId2);
            assertEquals(conversationId1, conversationId2, 
                "Same input order should generate identical conversation IDs");
        }

        @Test
        @DisplayName("Should generate same UUID for reversed input order")
        void shouldGenerateSameUuidForReversedInputOrder() {
            // Arrange
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();

            // Act
            UUID conversationId1 = ConversationIdGenerator.generate(user1, user2);
            UUID conversationId2 = ConversationIdGenerator.generate(user2, user1);

            // Assert
            assertNotNull(conversationId1);
            assertNotNull(conversationId2);
            assertEquals(conversationId1, conversationId2, 
                "Reversed input order should generate identical conversation IDs");
        }

        @Test
        @DisplayName("Should generate different UUIDs for different user pairs")
        void shouldGenerateDifferentUuidsForDifferentUserPairs() {
            // Arrange
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();
            UUID user3 = UUID.randomUUID();

            // Act
            UUID conversationId12 = ConversationIdGenerator.generate(user1, user2);
            UUID conversationId13 = ConversationIdGenerator.generate(user1, user3);
            UUID conversationId23 = ConversationIdGenerator.generate(user2, user3);

            // Assert
            assertNotNull(conversationId12);
            assertNotNull(conversationId13);
            assertNotNull(conversationId23);
            
            assertNotEquals(conversationId12, conversationId13, 
                "Different user pairs should generate different conversation IDs");
            assertNotEquals(conversationId12, conversationId23, 
                "Different user pairs should generate different conversation IDs");
            assertNotEquals(conversationId13, conversationId23, 
                "Different user pairs should generate different conversation IDs");
        }

        @Test
        @DisplayName("Should generate same UUID across multiple calls with different orders")
        void shouldGenerateSameUuidAcrossMultipleCalls() {
            // Arrange
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();

            // Act - Multiple calls with different orderings
            UUID id1 = ConversationIdGenerator.generate(user1, user2);
            UUID id2 = ConversationIdGenerator.generate(user2, user1);
            UUID id3 = ConversationIdGenerator.generate(user1, user2);
            UUID id4 = ConversationIdGenerator.generate(user2, user1);

            // Assert
            assertEquals(id1, id2, "First two calls should match");
            assertEquals(id1, id3, "First and third calls should match");
            assertEquals(id1, id4, "First and fourth calls should match");
            assertEquals(id2, id3, "Second and third calls should match");
            assertEquals(id2, id4, "Second and fourth calls should match");
            assertEquals(id3, id4, "Third and fourth calls should match");
        }
    }

    @Nested
    @DisplayName("Same User Edge Case")
    class SameUserEdgeCase {

        @Test
        @DisplayName("Should generate same UUID when user sends message to themselves")
        void shouldHandleSameUserConversation() {
            // Arrange
            UUID user = UUID.randomUUID();

            // Act
            UUID conversationId1 = ConversationIdGenerator.generate(user, user);
            UUID conversationId2 = ConversationIdGenerator.generate(user, user);

            // Assert
            assertNotNull(conversationId1);
            assertNotNull(conversationId2);
            assertEquals(conversationId1, conversationId2, 
                "Same user should generate identical conversation IDs");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Should throw exception when user1 is null")
        void shouldThrowExceptionWhenUser1IsNull() {
            // Arrange
            UUID user2 = UUID.randomUUID();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ConversationIdGenerator.generate(null, user2)
            );
            
            assertEquals("User IDs must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when user2 is null")
        void shouldThrowExceptionWhenUser2IsNull() {
            // Arrange
            UUID user1 = UUID.randomUUID();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ConversationIdGenerator.generate(user1, null)
            );
            
            assertEquals("User IDs must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when both users are null")
        void shouldThrowExceptionWhenBothUsersAreNull() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ConversationIdGenerator.generate(null, null)
            );
            
            assertEquals("User IDs must not be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("UUID Properties")
    class UuidProperties {

        @Test
        @DisplayName("Generated UUID should be valid")
        void generatedUuidShouldBeValid() {
            // Arrange
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();

            // Act
            UUID conversationId = ConversationIdGenerator.generate(user1, user2);

            // Assert
            assertNotNull(conversationId);
            assertNotNull(conversationId.toString());
            assertTrue(conversationId.toString().matches(
                "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"),
                "Generated UUID should have valid format");
        }

        @Test
        @DisplayName("Generated UUID should be version 3 (name-based)")
        void generatedUuidShouldBeVersion3() {
            // Arrange
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();

            // Act
            UUID conversationId = ConversationIdGenerator.generate(user1, user2);

            // Assert
            // UUID version 3 has version bits set to 0011 (3) in the version field
            assertEquals(3, conversationId.version(), 
                "Generated UUID should be version 3 (name-based)");
        }
    }

    @Nested
    @DisplayName("Real World Scenarios")
    class RealWorldScenarios {

        @Test
        @DisplayName("Should work with known UUID values")
        void shouldWorkWithKnownUuidValues() {
            // Arrange - Using predictable UUIDs for testing
            UUID user1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID user2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

            // Act
            UUID conversationId1 = ConversationIdGenerator.generate(user1, user2);
            UUID conversationId2 = ConversationIdGenerator.generate(user2, user1);

            // Assert
            assertNotNull(conversationId1);
            assertNotNull(conversationId2);
            assertEquals(conversationId1, conversationId2);
        }

        @Test
        @DisplayName("Should be consistent across application restarts")
        void shouldBeConsistentAcrossRestarts() {
            // Arrange - Simulating same users across different "sessions"
            UUID user1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UUID user2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

            // Act - Simulate multiple application instances/restarts
            UUID session1Id = ConversationIdGenerator.generate(user1, user2);
            UUID session2Id = ConversationIdGenerator.generate(user2, user1);
            UUID session3Id = ConversationIdGenerator.generate(user1, user2);

            // Assert - All should be identical
            assertEquals(session1Id, session2Id);
            assertEquals(session1Id, session3Id);
            assertEquals(session2Id, session3Id);
        }
    }
}
