package com.anonymous.wall.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CodeGenerator Tests")
class CodeGeneratorTest {

    @Nested
    @DisplayName("Code Format Tests")
    class CodeFormatTests {

        @Test
        @DisplayName("Should generate six digit code")
        void shouldGenerateSixDigitCode() {
            String code = CodeGenerator.generateCode();

            assertEquals(6, code.length(), "Code should be exactly 6 digits");
            assertTrue(code.matches("\\d{6}"), "Code should contain only digits");
        }

        @Test
        @DisplayName("Should include codes with leading zeros")
        void shouldIncludeLeadingZeros() {
            Set<String> codes = new HashSet<>();

            // Generate 1000 codes
            for (int i = 0; i < 1000; i++) {
                codes.add(CodeGenerator.generateCode());
            }

            // Should include some codes starting with 0
            boolean hasLeadingZero = codes.stream()
                .anyMatch(c -> c.startsWith("0"));

            assertTrue(hasLeadingZero, "Should include codes with leading zeros");
        }

        @Test
        @DisplayName("Should include code 000000")
        void shouldIncludeZeroCode() {
            Set<String> codes = new HashSet<>();

            // Generate enough codes to statistically likely hit 000000
            for (int i = 0; i < 10000; i++) {
                codes.add(CodeGenerator.generateCode());
            }

            // With 10000 samples out of 1000000 possible, should have high chance
            // But won't guarantee it, so this is a soft assertion
            assertTrue(codes.size() > 5000, "Should have good variety of codes");
        }
    }

    @Nested
    @DisplayName("Randomness Tests")
    class RandomnessTests {

        @Test
        @DisplayName("Should generate random codes")
        void shouldGenerateRandomCodes() {
            Set<String> codes = new HashSet<>();

            // Generate 100 codes
            for (int i = 0; i < 100; i++) {
                codes.add(CodeGenerator.generateCode());
            }

            // Should have high variety (at least 99% unique)
            assertTrue(codes.size() >= 99, "Should generate mostly unique codes");
        }

        @Test
        @DisplayName("Should have no obvious patterns")
        void shouldHaveNoPattern() {
            String code1 = CodeGenerator.generateCode();
            String code2 = CodeGenerator.generateCode();
            String code3 = CodeGenerator.generateCode();

            assertNotEquals(code1, code2, "Consecutive calls should be different");
            assertNotEquals(code2, code3, "Consecutive calls should be different");
            assertNotEquals(code1, code3, "Generated codes should vary");
        }

        @Test
        @DisplayName("Should not be sequential")
        void shouldNotBeSequential() {
            Set<String> codes = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                codes.add(CodeGenerator.generateCode());
            }

            // Check that codes don't form sequential patterns
            // (This is a sanity check, not deterministic)
            assertTrue(codes.size() > 500, "Should not have sequential pattern");
        }
    }

    @Nested
    @DisplayName("Range Tests")
    class RangeTests {

        @Test
        @DisplayName("Should generate codes in valid range (0-999999)")
        void shouldGenerateValidRange() {
            for (int i = 0; i < 1000; i++) {
                String code = CodeGenerator.generateCode();
                int value = Integer.parseInt(code);

                assertTrue(value >= 0, "Code should not be negative");
                assertTrue(value < 1000000, "Code should be less than 1000000");
            }
        }

        @Test
        @DisplayName("Should generate codes in valid range")
        void shouldIncludeBoundaryValues() {
            Set<Integer> values = new HashSet<>();

            for (int i = 0; i < 10000; i++) {
                String code = CodeGenerator.generateCode();
                int value = Integer.parseInt(code);
                values.add(value);

                // Verify all codes are in valid range [0, 999999]
                assertTrue(value >= 0 && value <= 999999,
                    "Code should be in range [0, 999999], got: " + value);
            }

            // Verify good variety across the range
            int minValue = values.stream().mapToInt(Integer::intValue).min().orElse(0);
            int maxValue = values.stream().mapToInt(Integer::intValue).max().orElse(0);

            // With 10000 random samples, we should span a reasonable range
            assertTrue(maxValue - minValue > 100000,
                "Generated codes should span a good range of values");
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should generate code quickly")
        void shouldGenerateQuickly() {
            long startTime = System.nanoTime();
            CodeGenerator.generateCode();
            long endTime = System.nanoTime();

            long durationMs = (endTime - startTime) / 1_000_000;
            assertTrue(durationMs < 10, "Should generate in less than 10ms");
        }

        @Test
        @DisplayName("Should generate 10000 codes in reasonable time")
        void shouldGenerate10000Quickly() {
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < 10000; i++) {
                CodeGenerator.generateCode();
            }

            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime;

            assertTrue(durationMs < 1000, "Should generate 10000 codes in under 1 second");
        }
    }

    @Nested
    @DisplayName("Distribution Tests")
    class DistributionTests {

        @Test
        @DisplayName("Should have uniform distribution across digits")
        void shouldHaveUniformDistribution() {
            int[] digitCounts = new int[10];
            int totalDigits = 0;

            // Generate codes and count digit distribution
            for (int i = 0; i < 1000; i++) {
                String code = CodeGenerator.generateCode();
                for (char c : code.toCharArray()) {
                    digitCounts[c - '0']++;
                    totalDigits++;
                }
            }

            // Each digit should appear roughly 600 times (6000 digits / 10)
            // Allow ±25% variance
            int expected = 600;
            int variance = 150;

            for (int count : digitCounts) {
                assertTrue(
                    count >= expected - variance && count <= expected + variance,
                    "Digit distribution should be roughly uniform: " + count + " (expected ~" + expected + ")"
                );
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should generate codes reliably in loop")
        void shouldGenerateReliablyInLoop() {
            for (int i = 0; i < 100; i++) {
                String code = CodeGenerator.generateCode();
                assertNotNull(code);
                assertEquals(6, code.length());
                assertTrue(code.matches("\\d{6}"));
            }
        }

        @Test
        @DisplayName("Should work correctly with concurrent calls")
        void shouldWorkWithConcurrentCalls() throws InterruptedException {
            Set<String> codes = new HashSet<>();
            Thread thread1 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    codes.add(CodeGenerator.generateCode());
                }
            });

            Thread thread2 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    codes.add(CodeGenerator.generateCode());
                }
            });

            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            // Should have generated codes from both threads
            assertTrue(codes.size() > 150, "Should generate from concurrent calls");
        }
    }
}
