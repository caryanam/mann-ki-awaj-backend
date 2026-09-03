package com.mka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mka.config.DateTimeConfig;
import com.mka.dto.response.CommentResponse;
import com.mka.dto.response.PostResponse;
import com.mka.entity.Comment;
import com.mka.entity.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class TimestampSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        DateTimeConfig config = new DateTimeConfig();
        Jackson2ObjectMapperBuilderCustomizer customizer = config.jsonCustomizer();
        customizer.customize(builder);
        objectMapper = builder.build();
    }

    @Test
    void testPostEntityLifecycleSetsInstantNow() {
        Post post = new Post();
        Instant before = Instant.now();
        post.onCreate();
        Instant after = Instant.now();

        assertNotNull(post.getCreatedAt());
        assertNotNull(post.getUpdatedAt());
        assertFalse(post.getCreatedAt().isBefore(before));
        assertFalse(post.getCreatedAt().isAfter(after));
    }

    @Test
    void testCommentEntityLifecycleSetsInstantNow() {
        Comment comment = new Comment();
        Instant before = Instant.now();
        comment.onCreate();
        Instant after = Instant.now();

        assertNotNull(comment.getCreatedAt());
        assertNotNull(comment.getUpdatedAt());
        assertFalse(comment.getCreatedAt().isBefore(before));
        assertFalse(comment.getCreatedAt().isAfter(after));
    }

    @Test
    void testPostResponseSerializesWithZ() throws Exception {
        Instant fixedInstant = Instant.parse("2026-09-03T05:30:00Z");
        PostResponse response = PostResponse.builder()
                .id(1L)
                .originalContent("Test content")
                .createdAt(fixedInstant)
                .build();

        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"createdAt\":\"2026-09-03T05:30:00Z\""),
                "Expected JSON to contain ISO-8601 string with Z, got: " + json);
    }

    @Test
    void testCommentResponseSerializesWithZ() throws Exception {
        Instant fixedInstant = Instant.parse("2026-09-03T10:15:30.500Z");
        CommentResponse response = CommentResponse.builder()
                .id(10L)
                .originalContent("Comment text")
                .createdAt(fixedInstant)
                .build();

        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"createdAt\":\"2026-09-03T10:15:30.500Z\""),
                "Expected JSON to contain ISO-8601 string with Z, got: " + json);
    }

    @Test
    void testSerializationConsistentAcrossJvmTimezones() throws Exception {
        TimeZone originalTz = TimeZone.getDefault();
        try {
            Instant testInstant = Instant.parse("2026-09-03T12:00:00Z");
            PostResponse post = PostResponse.builder().id(99L).createdAt(testInstant).build();

            // Set timezone to Asia/Kolkata (+05:30)
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
            String kolkataJson = objectMapper.writeValueAsString(post);

            // Set timezone to UTC
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            String utcJson = objectMapper.writeValueAsString(post);

            // Set timezone to America/New_York (-04:00)
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            String nyJson = objectMapper.writeValueAsString(post);

            // Both must produce the exact same UTC representation with 'Z'
            assertEquals(utcJson, kolkataJson);
            assertEquals(utcJson, nyJson);
            assertTrue(kolkataJson.contains("\"2026-09-03T12:00:00Z\""));
        } finally {
            TimeZone.setDefault(originalTz);
        }
    }
}
