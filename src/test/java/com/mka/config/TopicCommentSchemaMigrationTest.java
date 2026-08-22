package com.mka.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TopicCommentSchemaMigrationTest {

    @Test
    void makesLegacyPostForeignKeyNullableWithoutChangingRows() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(contains("information_schema.COLUMNS"), eq(String.class))).thenReturn("NO");

        new TopicCommentSchemaMigration(jdbc).run(null);

        verify(jdbc).execute("ALTER TABLE comments MODIFY COLUMN post_id BIGINT NULL");
    }

    @Test
    void doesNothingWhenMigrationAlreadyApplied() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(contains("information_schema.COLUMNS"), eq(String.class))).thenReturn("YES");

        new TopicCommentSchemaMigration(jdbc).run(null);

        verify(jdbc, never()).execute(contains("ALTER TABLE"));
    }
}
