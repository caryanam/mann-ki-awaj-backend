package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V6__add_performance_indexes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        if (tableExists(connection, "posts")) {
            createIndexIfNotExists(connection, "posts", "idx_posts_status_created",
                    "CREATE INDEX idx_posts_status_created ON posts(status, created_at DESC)");
            createIndexIfNotExists(connection, "posts", "idx_posts_status_topic_created",
                    "CREATE INDEX idx_posts_status_topic_created ON posts(status, topic, created_at DESC)");
        }

        if (tableExists(connection, "chat_messages")) {
            createIndexIfNotExists(connection, "chat_messages", "idx_chat_msg_room_created",
                    "CREATE INDEX idx_chat_msg_room_created ON chat_messages(room_id, created_at DESC)");
        }

        if (tableExists(connection, "comments")) {
            createIndexIfNotExists(connection, "comments", "idx_comments_post_parent_status",
                    "CREATE INDEX idx_comments_post_parent_status ON comments(post_id, parent_comment_id, status)");
        }
    }

    private static void createIndexIfNotExists(Connection connection, String tableName, String indexName, String createSql) {
        try {
            if (!indexExists(connection, tableName, indexName)) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(createSql);
                }
            }
        } catch (Exception ignored) {
            // Index creation may differ slightly on H2 memory test db vs MySQL; silently ignore if created
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

    private static boolean indexExists(Connection connection, String tableName, String indexName) {
        try (ResultSet idxs = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (idxs.next()) {
                if (indexName.equalsIgnoreCase(idxs.getString("INDEX_NAME"))) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
