package services.messaging;

import models.messaging.Message;
import models.gestionutilisateurs.MessageConversation;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageService {

    private static volatile boolean schemaEnsured;

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS message (
                id INT AUTO_INCREMENT PRIMARY KEY,
                sender_id INT NOT NULL,
                receiver_id INT NOT NULL,
                content TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                is_read BOOLEAN DEFAULT FALSE
            )
            """;

    private static final String INSERT_SQL = """
            INSERT INTO message (sender_id, receiver_id, content, created_at, is_read)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String CONVERSATION_SQL = """
            SELECT id, sender_id, receiver_id, content, created_at, is_read
            FROM message
            WHERE (sender_id = ? AND receiver_id = ?)
               OR (sender_id = ? AND receiver_id = ?)
            ORDER BY created_at ASC, id ASC
            """;

    private static final String USER_MESSAGES_SQL = """
            SELECT id, sender_id, receiver_id, content, created_at, is_read
            FROM message
            WHERE sender_id = ? OR receiver_id = ?
            ORDER BY created_at DESC, id DESC
            """;

    private static final String CONVERSATION_USERS_SQL = """
            SELECT DISTINCT CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END AS other_user_id
            FROM message
            WHERE sender_id = ? OR receiver_id = ?
            ORDER BY other_user_id ASC
            """;

    private static final String MARK_READ_SQL = "UPDATE message SET is_read = TRUE WHERE id = ?";

    private static final String MARK_CONVERSATION_READ_SQL = """
            UPDATE message
            SET is_read = TRUE
            WHERE sender_id = ? AND receiver_id = ? AND is_read = FALSE
            """;

    private static final String CONVERSATIONS_SQL = """
            SELECT
                c.other_user_id,
                COALESCE(aa.agency_name, u.username, CONCAT('User #', c.other_user_id)) AS other_name,
                CASE WHEN aa.id IS NOT NULL THEN 'Agency' ELSE 'User' END AS other_role,
                (
                    SELECT m2.content
                    FROM message m2
                    WHERE (m2.sender_id = ? AND m2.receiver_id = c.other_user_id)
                       OR (m2.sender_id = c.other_user_id AND m2.receiver_id = ?)
                    ORDER BY m2.created_at DESC, m2.id DESC
                    LIMIT 1
                ) AS last_message,
                (
                    SELECT m3.created_at
                    FROM message m3
                    WHERE (m3.sender_id = ? AND m3.receiver_id = c.other_user_id)
                       OR (m3.sender_id = c.other_user_id AND m3.receiver_id = ?)
                    ORDER BY m3.created_at DESC, m3.id DESC
                    LIMIT 1
                ) AS last_created_at,
                (
                    SELECT COUNT(*)
                    FROM message um
                    WHERE um.sender_id = c.other_user_id
                      AND um.receiver_id = ?
                      AND um.is_read = FALSE
                ) AS unread_count
            FROM (
                SELECT DISTINCT
                    CASE
                        WHEN sender_id = ? THEN receiver_id
                        ELSE sender_id
                    END AS other_user_id
                FROM message
                WHERE sender_id = ? OR receiver_id = ?
            ) c
            LEFT JOIN user u ON u.id = c.other_user_id
            LEFT JOIN agency_account aa ON aa.responsable_id = c.other_user_id
            ORDER BY last_created_at DESC
            """;

    private static final String COUNT_UNREAD_SQL = "SELECT COUNT(*) FROM message WHERE receiver_id = ? AND is_read = FALSE";

    public MessageService() {
        ensureSchema();
    }

    public void sendMessage(int senderId, int receiverId, String content) throws SQLException {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setRead(false);
        sendMessage(message);
    }

    public void sendMessage(Message msg) throws SQLException {
        ensureSchema();
        validateMessage(msg);
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, msg.getSenderId());
            statement.setInt(2, msg.getReceiverId());
            statement.setString(3, msg.getContent().trim());
            LocalDateTime now = msg.getCreatedAt() == null ? LocalDateTime.now() : msg.getCreatedAt();
            statement.setTimestamp(4, Timestamp.valueOf(now));
            statement.setBoolean(5, msg.isRead());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    msg.setId(keys.getInt(1));
                }
            }
            msg.setCreatedAt(now);
        }
    }

    public List<Message> getMessagesBetweenUsers(int user1, int user2) throws SQLException {
        return getConversation(user1, user2);
    }

    public List<Message> getConversation(int user1, int user2) throws SQLException {
        ensureSchema();
        if (user1 <= 0 || user2 <= 0) {
            throw new IllegalArgumentException("Both users are required.");
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        List<Message> messages = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(CONVERSATION_SQL)) {
            statement.setInt(1, user1);
            statement.setInt(2, user2);
            statement.setInt(3, user2);
            statement.setInt(4, user1);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(mapMessage(resultSet));
                }
            }
        }
        return messages;
    }

    public List<Message> getUserMessages(int userId) throws SQLException {
        ensureSchema();
        if (userId <= 0) {
            throw new IllegalArgumentException("userId is required.");
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        List<Message> messages = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(USER_MESSAGES_SQL)) {
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(mapMessage(resultSet));
                }
            }
        }
        return messages;
    }

    public List<Integer> getConversationUsers(int userId) throws SQLException {
        ensureSchema();
        if (userId <= 0) {
            return List.of();
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        List<Integer> users = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(CONVERSATION_USERS_SQL)) {
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setInt(3, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(resultSet.getInt("other_user_id"));
                }
            }
        }
        return users;
    }

    public void markAsRead(int messageId) throws SQLException {
        ensureSchema();
        if (messageId <= 0) {
            throw new IllegalArgumentException("messageId is required.");
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(MARK_READ_SQL)) {
            statement.setInt(1, messageId);
            statement.executeUpdate();
        }
    }

    public void markConversationAsRead(int currentUserId, int otherUserId) throws SQLException {
        ensureSchema();
        if (currentUserId <= 0 || otherUserId <= 0) {
            throw new IllegalArgumentException("Both users are required.");
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(MARK_CONVERSATION_READ_SQL)) {
            statement.setInt(1, otherUserId);
            statement.setInt(2, currentUserId);
            statement.executeUpdate();
        }
    }

    public List<MessageConversation> getConversationsForUser(int userId) throws SQLException {
        ensureSchema();
        if (userId <= 0) {
            throw new IllegalArgumentException("userId is required.");
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        List<MessageConversation> conversations = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(CONVERSATIONS_SQL)) {
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setInt(3, userId);
            statement.setInt(4, userId);
            statement.setInt(5, userId);
            statement.setInt(6, userId);
            statement.setInt(7, userId);
            statement.setInt(8, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    MessageConversation conversation = new MessageConversation();
                    conversation.setOtherUserId(resultSet.getInt("other_user_id"));
                    conversation.setOtherUserName(resultSet.getString("other_name"));
                    conversation.setOtherUserRole(resultSet.getString("other_role"));
                    conversation.setLastMessage(resultSet.getString("last_message"));
                    Timestamp lastAt = resultSet.getTimestamp("last_created_at");
                    if (lastAt != null) {
                        conversation.setLastMessageAt(lastAt.toLocalDateTime());
                    }
                    conversation.setUnreadCount(resultSet.getInt("unread_count"));
                    conversations.add(conversation);
                }
            }
        }
        return conversations;
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

    private static Message mapMessage(ResultSet resultSet) throws SQLException {
        Message message = new Message();
        message.setId(resultSet.getInt("id"));
        message.setSenderId(resultSet.getInt("sender_id"));
        message.setReceiverId(resultSet.getInt("receiver_id"));
        message.setContent(resultSet.getString("content"));
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            message.setCreatedAt(createdAt.toLocalDateTime());
        }
        message.setRead(resultSet.getBoolean("is_read"));
        return message;
    }

    private static void validateMessage(Message msg) {
        if (msg == null) {
            throw new IllegalArgumentException("Message is required.");
        }
        if (msg.getSenderId() <= 0 || msg.getReceiverId() <= 0) {
            throw new IllegalArgumentException("Sender and receiver are required.");
        }
        if (msg.getContent() == null || msg.getContent().isBlank()) {
            throw new IllegalArgumentException("Message content is required.");
        }
    }

    private static void ensureSchema() {
        if (schemaEnsured) {
            return;
        }
        synchronized (MessageService.class) {
            if (schemaEnsured) {
                return;
            }
            try {
                Connection connection = DbConnexion.getInstance().getConnection();
                try (Statement statement = connection.createStatement()) {
                    statement.execute(CREATE_TABLE_SQL);
                }

                ensureColumn(connection, "sender_id", "INT");
                ensureColumn(connection, "receiver_id", "INT");
                ensureColumn(connection, "content", "TEXT");
                ensureColumn(connection, "created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                ensureColumn(connection, "is_read", "BOOLEAN DEFAULT FALSE");

                schemaEnsured = true;
            } catch (SQLException e) {
                throw new RuntimeException("Unable to ensure message schema", e);
            }
        }
    }

    private static void ensureColumn(Connection connection, String column, String definition) throws SQLException {
        if (columnExists(connection, "message", column)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE message ADD COLUMN " + column + " " + definition);
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, table, column)) {
            return columns.next();
        }
    }
}
