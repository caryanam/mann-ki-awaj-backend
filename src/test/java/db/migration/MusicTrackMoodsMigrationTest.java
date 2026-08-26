package db.migration;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MusicTrackMoodsMigrationTest {

    @Test
    void createsCollectionSchemaAndBackfillsLegacyMoodIdempotently() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:music_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")) {
            connection.createStatement().execute("CREATE TABLE music_tracks (id BIGINT PRIMARY KEY, mood VARCHAR(20) NOT NULL)");
            connection.createStatement().execute("INSERT INTO music_tracks (id, mood) VALUES (10, 'ROMANTIC')");
            Context context = mock(Context.class);
            when(context.getConnection()).thenReturn(connection);

            var migration = new V1__create_and_backfill_music_track_moods();
            migration.migrate(context);
            migration.migrate(context);
            connection.createStatement().execute("INSERT INTO music_tracks (id, mood) VALUES (11, NULL)");

            try (var rows = connection.createStatement().executeQuery(
                    "SELECT track_id, mood FROM music_track_moods")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong("track_id")).isEqualTo(10L);
                assertThat(rows.getString("mood")).isEqualTo("ROMANTIC");
                assertThat(rows.next()).isFalse();
            }
        }
    }
}
