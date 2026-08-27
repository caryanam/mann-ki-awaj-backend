package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V4__add_voice_note_fields_to_community_audio_attachments extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "community_audio_attachments")) return;

        try (Statement statement = connection.createStatement()) {
            if (!columnExists(connection, "community_audio_attachments", "audio_key")) {
                statement.execute("ALTER TABLE community_audio_attachments ADD COLUMN audio_key VARCHAR(255) NULL");
            }
            if (!columnExists(connection, "community_audio_attachments", "mime_type")) {
                statement.execute("ALTER TABLE community_audio_attachments ADD COLUMN mime_type VARCHAR(100) NULL");
            }
            if (!columnExists(connection, "community_audio_attachments", "file_size")) {
                statement.execute("ALTER TABLE community_audio_attachments ADD COLUMN file_size BIGINT NULL");
            }
            if (!columnExists(connection, "community_audio_attachments", "waveform_data")) {
                statement.execute("ALTER TABLE community_audio_attachments ADD COLUMN waveform_data TEXT NULL");
            }
            if (!columnExists(connection, "community_audio_attachments", "transcript")) {
                statement.execute("ALTER TABLE community_audio_attachments ADD COLUMN transcript TEXT NULL");
            }
        }
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet rows = connection.getMetaData().getTables(connection.getCatalog(), null, null, new String[]{"TABLE"})) {
            while (rows.next()) {
                if (tableName.equalsIgnoreCase(rows.getString("TABLE_NAME"))) return true;
            }
        }
        return false;
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (ResultSet cols = connection.getMetaData().getColumns(connection.getCatalog(), null, null, null)) {
            while (cols.next()) {
                if (tableName.equalsIgnoreCase(cols.getString("TABLE_NAME"))
                        && columnName.equalsIgnoreCase(cols.getString("COLUMN_NAME"))) return true;
            }
        }
        return false;
    }
}
