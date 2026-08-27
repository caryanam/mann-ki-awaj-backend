package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V5__add_default_to_posts_share_count extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "posts")) return;

        try (Statement statement = connection.createStatement()) {
            if (!columnExists(connection, "posts", "share_count")) {
                statement.execute("ALTER TABLE posts ADD COLUMN share_count INT NOT NULL DEFAULT 0");
            } else {
                // Backfill existing NULL values (if any) to 0
                statement.execute("UPDATE posts SET share_count = 0 WHERE share_count IS NULL");
                try {
                    statement.execute("ALTER TABLE posts ALTER COLUMN share_count SET DEFAULT 0");
                } catch (Exception ignored) {
                    try {
                        statement.execute("ALTER TABLE posts MODIFY COLUMN share_count INT NOT NULL DEFAULT 0");
                    } catch (Exception ignored2) {}
                }
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
