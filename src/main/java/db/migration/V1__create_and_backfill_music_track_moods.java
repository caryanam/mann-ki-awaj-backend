package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class V1__create_and_backfill_music_track_moods extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "music_tracks")) {
            // A genuinely empty development database is still initially created by Hibernate.
            return;
        }

        if (!tableExists(connection, "music_track_moods")) {
            execute(connection, """
                    CREATE TABLE music_track_moods (
                        track_id BIGINT NOT NULL,
                        mood VARCHAR(20) NOT NULL,
                        CONSTRAINT uk_music_track_moods_track_mood UNIQUE (track_id, mood),
                        CONSTRAINT fk_music_track_moods_track FOREIGN KEY (track_id)
                            REFERENCES music_tracks (id) ON DELETE CASCADE
                    )
                    """);
        }

        createIndexIfMissing(connection, "music_track_moods", "idx_music_track_moods_mood_track",
                "CREATE INDEX idx_music_track_moods_mood_track ON music_track_moods (mood, track_id)");
        createIndexIfMissing(connection, "music_track_moods", "idx_music_track_moods_track",
                "CREATE INDEX idx_music_track_moods_track ON music_track_moods (track_id)");

        if (columnExists(connection, "music_tracks", "mood")) {
            execute(connection, """
                    INSERT INTO music_track_moods (track_id, mood)
                    SELECT t.id, t.mood
                    FROM music_tracks t
                    WHERE t.mood IS NOT NULL
                      AND t.mood IN ('ROMANTIC','SAD','CALM','ENERGETIC','CONFUSED','MELANCHOLY','FOCUS')
                      AND NOT EXISTS (
                          SELECT 1 FROM music_track_moods tm
                          WHERE tm.track_id = t.id AND tm.mood = t.mood
                      )
                    """);
            makeLegacyMoodNullable(connection);
        }
    }

    private static void createIndexIfMissing(Connection connection, String table, String index, String sql)
            throws Exception {
        if (!indexExists(connection, table, index)) execute(connection, sql);
    }

    private static void execute(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void makeLegacyMoodNullable(Connection connection) throws Exception {
        String database = connection.getMetaData().getDatabaseProductName().toLowerCase();
        if (database.contains("mysql") || database.contains("mariadb")) {
            execute(connection, "ALTER TABLE music_tracks MODIFY COLUMN mood VARCHAR(20) NULL");
        } else {
            execute(connection, "ALTER TABLE music_tracks ALTER COLUMN mood DROP NOT NULL");
        }
    }

    private static boolean tableExists(Connection connection, String table) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet rows = metadata.getTables(connection.getCatalog(), null, null, new String[]{"TABLE"})) {
            while (rows.next()) if (table.equalsIgnoreCase(rows.getString("TABLE_NAME"))) return true;
        }
        return false;
    }

    private static boolean columnExists(Connection connection, String table, String column) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet rows = metadata.getColumns(connection.getCatalog(), null, null, null)) {
            while (rows.next()) {
                if (table.equalsIgnoreCase(rows.getString("TABLE_NAME"))
                        && column.equalsIgnoreCase(rows.getString("COLUMN_NAME"))) return true;
            }
        }
        return false;
    }

    private static boolean indexExists(Connection connection, String table, String index) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet rows = metadata.getIndexInfo(connection.getCatalog(), null, table, false, false)) {
            while (rows.next()) if (index.equalsIgnoreCase(rows.getString("INDEX_NAME"))) return true;
        }
        try (ResultSet rows = metadata.getIndexInfo(connection.getCatalog(), null,
                table.toUpperCase(), false, false)) {
            while (rows.next()) if (index.equalsIgnoreCase(rows.getString("INDEX_NAME"))) return true;
        }
        return false;
    }
}
