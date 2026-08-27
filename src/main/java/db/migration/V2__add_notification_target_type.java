package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class V2__add_notification_target_type extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "notifications")
                || columnExists(connection, "notifications", "target_type")) return;
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE notifications ADD COLUMN target_type VARCHAR(30) NULL");
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
}
