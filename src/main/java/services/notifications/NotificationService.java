package services.notifications;

import models.notifications.Notification;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    private static volatile boolean schemaEnsured;

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS notification (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                content TEXT NOT NULL,
                is_read BOOLEAN DEFAULT FALSE,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_notification_user_created (user_id, created_at),
                INDEX idx_notification_user_unread (user_id, is_read)
            )
            """;

    private static final String INSERT_SQL = """
            INSERT INTO notification (user_id, content, is_read, created_at)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SELECT_BY_USER_SQL = """
            SELECT id, user_id, content, is_read, created_at
            FROM notification
            WHERE user_id = ?
            ORDER BY created_at DESC, id DESC
            """;

    private static final String MARK_READ_SQL = "UPDATE notification SET is_read = TRUE WHERE id = ?";
    private static final String MARK_ALL_READ_SQL = "UPDATE notification SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
    private static final String COUNT_UNREAD_SQL = "SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = FALSE";

    public NotificationService() {
        ensureSchema();
    }

    public Notification createNotification(int userId, String content) throws SQLException {
        ensureSchema();
        if (userId <= 0) {
            throw new IllegalArgumentException("userId is required.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Notification content is required.");
        }

        Connection connection = DbConnexion.getInstance().getConnection();
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setContent(content.trim());
        notification.setRead(false);
        notification.setCreatedAt(java.time.LocalDateTime.now());

        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, notification.getUserId());
            statement.setString(2, notification.getContent());
            statement.setBoolean(3, false);
            statement.setTimestamp(4, Timestamp.valueOf(notification.getCreatedAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    notification.setId(keys.getInt(1));
                }
            }
        }
        return notification;
    }

    public List<Notification> getUserNotifications(int userId) throws SQLException {
        ensureSchema();
        if (userId <= 0) {
            return List.of();
        }

        Connection connection = DbConnexion.getInstance().getConnection();
        List<Notification> notifications = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_USER_SQL)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    notifications.add(mapNotification(resultSet));
                }
            }
        }
        return notifications;
    }

    public void markAsRead(int id) throws SQLException {
        ensureSchema();
        if (id <= 0) {
            throw new IllegalArgumentException("Notification id is required.");
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(MARK_READ_SQL)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public void markAllAsRead(int userId) throws SQLException {
        ensureSchema();
        if (userId <= 0) {
            throw new IllegalArgumentException("userId is required.");
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(MARK_ALL_READ_SQL)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    public int getUnreadCount(int userId) throws SQLException {
        ensureSchema();
        if (userId <= 0) {
            return 0;
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(COUNT_UNREAD_SQL)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    private static Notification mapNotification(ResultSet resultSet) throws SQLException {
        Notification notification = new Notification();
        notification.setId(resultSet.getInt("id"));
        notification.setUserId(resultSet.getInt("user_id"));
        notification.setContent(resultSet.getString("content"));
        notification.setRead(resultSet.getBoolean("is_read"));
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }
        return notification;
    }

    private static void ensureSchema() {
        if (schemaEnsured) {
            return;
        }
        synchronized (NotificationService.class) {
            if (schemaEnsured) {
                return;
            }
            try {
                Connection connection = DbConnexion.getInstance().getConnection();
                try (Statement statement = connection.createStatement()) {
                    statement.execute(CREATE_TABLE_SQL);
                }

                ensureColumn(connection, "user_id", "INT");
                ensureColumn(connection, "content", "TEXT");
                ensureColumn(connection, "is_read", "BOOLEAN DEFAULT FALSE");
                ensureColumn(connection, "created_at", "DATETIME DEFAULT CURRENT_TIMESTAMP");

                schemaEnsured = true;
            } catch (SQLException e) {
                throw new RuntimeException("Unable to ensure notification schema", e);
            }
        }
    }

    private static void ensureColumn(Connection connection, String column, String definition) throws SQLException {
        if (columnExists(connection, "notification", column)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE notification ADD COLUMN " + column + " " + definition);
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, table, column)) {
            return columns.next();
        }
    }
}
