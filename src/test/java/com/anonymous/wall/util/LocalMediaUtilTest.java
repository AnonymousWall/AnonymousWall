package com.anonymous.wall.util;

import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("LocalMediaUtil Tests")
class LocalMediaUtilTest {

    private LocalMediaUtil localMediaUtil;

    @BeforeEach
    void setUp() {
        localMediaUtil = new LocalMediaUtil();
    }

    private CompletedFileUpload mockFile(String contentType, long size, byte[] bytes) throws IOException {
        CompletedFileUpload file = mock(CompletedFileUpload.class);
        when(file.getContentType()).thenReturn(Optional.of(MediaType.of(contentType)));
        when(file.getSize()).thenReturn(size);
        when(file.getBytes()).thenReturn(bytes);
        return file;
    }

    @Nested
    @DisplayName("uploadPostImage")
    class UploadPostImageTests {

        @Test
        @DisplayName("Should upload JPEG image and return URL")
        void shouldUploadJpegImage() throws IOException {
            byte[] bytes = new byte[]{(byte) 0xFF, (byte) 0xD8};
            CompletedFileUpload file = mockFile("image/jpeg", 2L, bytes);
            UUID userId = UUID.randomUUID();

            String url = localMediaUtil.uploadPostImage(file, userId);

            assertNotNull(url);
            assertTrue(url.startsWith("http://localhost:8080/media/posts/"));
            assertTrue(url.endsWith(".jpg"));
        }

        @Test
        @DisplayName("Should upload PNG image and return URL")
        void shouldUploadPngImage() throws IOException {
            byte[] bytes = new byte[]{(byte) 0x89, 0x50};
            CompletedFileUpload file = mockFile("image/png", 2L, bytes);
            UUID userId = UUID.randomUUID();

            String url = localMediaUtil.uploadPostImage(file, userId);

            assertNotNull(url);
            assertTrue(url.startsWith("http://localhost:8080/media/posts/"));
            assertTrue(url.endsWith(".png"));
        }

        @Test
        @DisplayName("Should upload WEBP image and return URL")
        void shouldUploadWebpImage() throws IOException {
            byte[] bytes = new byte[]{0x52, 0x49};
            CompletedFileUpload file = mockFile("image/webp", 2L, bytes);
            UUID userId = UUID.randomUUID();

            String url = localMediaUtil.uploadPostImage(file, userId);

            assertNotNull(url);
            assertTrue(url.startsWith("http://localhost:8080/media/posts/"));
            assertTrue(url.endsWith(".webp"));
        }

        @Test
        @DisplayName("Should reject file exceeding 5MB")
        void shouldRejectFileTooLarge() throws IOException {
            long oversizeBytes = 5 * 1024 * 1024 + 1L;
            CompletedFileUpload file = mock(CompletedFileUpload.class);
            when(file.getSize()).thenReturn(oversizeBytes);
            when(file.getContentType()).thenReturn(Optional.of(MediaType.of("image/jpeg")));
            UUID userId = UUID.randomUUID();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> localMediaUtil.uploadPostImage(file, userId));
            assertTrue(ex.getMessage().contains("5MB"));
        }

        @Test
        @DisplayName("Should reject unsupported content type")
        void shouldRejectUnsupportedContentType() throws IOException {
            CompletedFileUpload file = mock(CompletedFileUpload.class);
            when(file.getSize()).thenReturn(100L);
            when(file.getContentType()).thenReturn(Optional.of(MediaType.of("image/gif")));
            UUID userId = UUID.randomUUID();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> localMediaUtil.uploadPostImage(file, userId));
            assertTrue(ex.getMessage().contains("JPEG, PNG, and WEBP"));
        }

        @Test
        @DisplayName("Should persist file to local media directory")
        void shouldPersistFileLocally() throws IOException {
            byte[] bytes = "fake image content".getBytes();
            CompletedFileUpload file = mockFile("image/jpeg", (long) bytes.length, bytes);
            UUID userId = UUID.randomUUID();

            String url = localMediaUtil.uploadPostImage(file, userId);

            // Extract filename from URL and verify file exists
            String filename = url.substring(url.lastIndexOf('/') + 1);
            Path dest = Path.of("/tmp/anonymouswall-media/posts", filename);
            assertTrue(Files.exists(dest));
            assertArrayEquals(bytes, Files.readAllBytes(dest));
        }
    }

    @Nested
    @DisplayName("uploadChatImage")
    class UploadChatImageTests {

        @Test
        @DisplayName("Should upload chat image and return URL with chat prefix")
        void shouldUploadChatImage() throws IOException {
            byte[] bytes = new byte[]{(byte) 0xFF, (byte) 0xD8};
            CompletedFileUpload file = mockFile("image/jpeg", 2L, bytes);
            UUID userId = UUID.randomUUID();

            String url = localMediaUtil.uploadChatImage(file, userId);

            assertNotNull(url);
            assertTrue(url.startsWith("http://localhost:8080/media/chat/"));
            assertTrue(url.endsWith(".jpg"));
        }
    }

    @Nested
    @DisplayName("uploadMarketplaceImage")
    class UploadMarketplaceImageTests {

        @Test
        @DisplayName("Should upload marketplace image and return URL with marketplace prefix")
        void shouldUploadMarketplaceImage() throws IOException {
            byte[] bytes = new byte[]{(byte) 0xFF, (byte) 0xD8};
            CompletedFileUpload file = mockFile("image/jpeg", 2L, bytes);
            UUID userId = UUID.randomUUID();

            String url = localMediaUtil.uploadMarketplaceImage(file, userId);

            assertNotNull(url);
            assertTrue(url.startsWith("http://localhost:8080/media/marketplace/"));
            assertTrue(url.endsWith(".jpg"));
        }
    }
}
