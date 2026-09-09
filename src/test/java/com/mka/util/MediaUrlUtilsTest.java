package com.mka.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MediaUrlUtilsTest {

    @AfterEach
    void tearDown() {
        MediaUrlUtils.setBaseUrl("");
    }

    @Test
    void toAbsoluteUrl_ReturnsNull_WhenInputIsNullEmptyOrBlank() {
        assertNull(MediaUrlUtils.toAbsoluteUrl(null));
        assertNull(MediaUrlUtils.toAbsoluteUrl(""));
        assertNull(MediaUrlUtils.toAbsoluteUrl("   "));
    }

    @Test
    void toAbsoluteUrl_LocalhostBaseUrl() {
        MediaUrlUtils.setBaseUrl("http://localhost:8080");

        assertEquals("http://localhost:8080/uploads/test.png",
                MediaUrlUtils.toAbsoluteUrl("/uploads/test.png"));
        assertEquals("http://localhost:8080/uploads/test.png",
                MediaUrlUtils.toAbsoluteUrl("uploads/test.png"));
    }

    @Test
    void toAbsoluteUrl_ProductionBaseUrl() {
        MediaUrlUtils.setBaseUrl("https://api.awaazmanki.com");

        assertEquals("https://api.awaazmanki.com/uploads/93888f5d-b623-4f47-8823-ccb050e05c83.png",
                MediaUrlUtils.toAbsoluteUrl("/uploads/93888f5d-b623-4f47-8823-ccb050e05c83.png"));
    }

    @Test
    void toAbsoluteUrl_HandlesTrailingSlashInConfiguredBaseUrl() {
        MediaUrlUtils.setBaseUrl("http://localhost:8080/");

        assertEquals("http://localhost:8080/uploads/test.png",
                MediaUrlUtils.toAbsoluteUrl("/uploads/test.png"));
    }

    @Test
    void toAbsoluteUrl_PreservesExternalAndDataUrls() {
        MediaUrlUtils.setBaseUrl("http://localhost:8080");

        assertEquals("https://images.unsplash.com/photo-12345",
                MediaUrlUtils.toAbsoluteUrl("https://images.unsplash.com/photo-12345"));
        assertEquals("data:image/png;base64,iVBORw0KGgo...",
                MediaUrlUtils.toAbsoluteUrl("data:image/png;base64,iVBORw0KGgo..."));
    }

    @Test
    void toAbsoluteUrl_RebasesInternalUploadsAcrossEnvironments() {
        MediaUrlUtils.setBaseUrl("https://api.awaazmanki.com");
        assertEquals("https://api.awaazmanki.com/uploads/saved.png",
                MediaUrlUtils.toAbsoluteUrl("http://localhost:8080/uploads/saved.png"));

        MediaUrlUtils.setBaseUrl("http://localhost:8080");
        assertEquals("http://localhost:8080/uploads/saved.png",
                MediaUrlUtils.toAbsoluteUrl("https://api.awaazmanki.com/uploads/saved.png"));
    }
}
