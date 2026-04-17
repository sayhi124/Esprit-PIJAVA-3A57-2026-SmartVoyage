package services.messaging;

import models.gestionutilisateurs.MessageConversation;
import models.messaging.Message;
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
    private static volatile boolean hasGroupIdColumn;
    private static volatile boolean hasLegacyChatGroupIdColumn;

    private static final String CREATE_CHAT_GROUP_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS chat_group (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NULL,
                is_group BOOLEAN NOT NULL DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String CREATE_CHAT_GROUP_MEMBERS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS chat_group_members (
                id INT AUTO_INCREMENT PRIMARY KEY,
                group_id INT NOT NULL,
                user_id INT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_chat_group_member (group_id, user_id),
                INDEX idx_chat_group_members_user (user_id)
            )
            """;

    private static final String CREATE_MESSAGE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS message (
                id INT AUTO_INCREMENT PRIMARY KEY,
                sender_id INT NOT NULL,
                receiver_id INT NULL,
                content TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                is_read BOOLEAN DEFAULT FALSE,
                group_id INT NULL,
                INDEX idx_message_sender (sender_id),
                INDEX idx_message_receiver (receiver_id),
                INDEX idx_message_group (group_id)
            )
            """;

    private static final String INSERT_DIRECT_SQL = """
            INSERT INTO message (sender_id, receiver_id, content, created_at, updated_at, is_read)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_DIRECT_WITH_GROUP_SQL = """
            INSERT INTO message (group_id, sender_id, receiver_id, content, created_at, updated_at, is_read)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_DIRECT_WITH_BOTH_GROUP_SQL = """
            INSERT INTO message (group_id, chat_group_id, sender_id, receiver_id, content, created_at, updated_at, is_read)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        private static final String INSERT_DIRECT_WITH_CHAT_GROUP_SQL = """
                INSERT INTO message (chat_group_id, sender_id, receiver_id, content, created_at, updated_at, is_read)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        private static final String FIND_DIRECT_GROUP_SQL = """
            SELECT cg.id
            FROM chat_group cg
            JOIN chat_group_members m1 ON m1.group_id = cg.id AND m1.user_id = ?
            JOIN chat_group_members m2 ON m2.group_id = cg.id AND m2.user_id = ?
            WHERE cg.is_group = FALSE
              AND (SELECT COUNT(*) FROM chat_group_members mm WHERE mm.group_id = cg.id) = 2
            LIMIT 1
            """;

        private static final String INSERT_CHAT_GROUP_SQL = """
            INSERT INTO chat_group (name, is_group, created_at, updated_at)
            VALUES (?, ?, ?, ?)
            """;

        private static final String INSERT_CHAT_GROUP_MEMBER_SQL = """
            INSERT INTO chat_group_members (group_id, user_id, created_at, updated_at)
            VALUES (?, ?, ?, ?)
            """;

    private static final String CONVERSATION_SQL = """
            SELECT id, sender_id, receiver_id, content, created_at, is_read
            FROM message
            WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)
            ORDER BY created_at ASC, id ASC
            """;

    private static final String USER_MESSAGES_SQL = """
            SELECT id, sender_id, receiver_id, content, created_at, is_read
            FROM message
            WHERE sender_id = ? OR receiver_id = ?
            ORDER BY created_at DESC, id DESC
            """;

    private static final String CONVERSATIONS_SQL = """
            SELECT
                c.other_user_id,
                COALESCE(aa.agency_name, u.username, CONCAT('User #', c.other_user_id)) AS display_name,
                CASE WHEN aa.id IS NOT NULL THEN 'Agency' ELSE 'User' END AS display_role,
                (
                    SELECT m2.content
                    FROM message m2
                    WHERE ((m2.sender_id = ? AND m2.receiver_id = c.other_user_id)
                       OR  (m2.sender_id = c.other_user_id AND m2.receiver_id = ?))
                    ORDER BY m2.created_at DESC, m2.id DESC
                    LIMIT 1
                ) AS last_message,
                c.last_created_at,
                (
                    SELECT COUNT(*)
                    FROM message um
                    WHERE um.sender_id = c.other_user_id AND um.receiver_id = ? AND um.is_read = FALSE
                ) AS unread_count
            FROM (
                SELECT
                    CASE WHEN m.sender_id = ? THEN m.receiver_id ELSE m.sender_id END AS other_user_id,
                    MAX(m.created_at) AS last_created_at
                FROM message m
                WHERE (m.sender_id = ? OR m.receiver_id = ?)
                  AND m.receiver_id IS NOT NULL
                  AND m.sender_id <> m.receiver_id
                GROUP BY CASE WHEN m.sender_id = ? THEN m.receiver_id ELSE m.sender_id END
            ) c
            LEFT JOIN `user` u ON u.id = c.other_user_id
            LEFT JOIN agency_account aa ON aa.responsable_id = c.other_user_id
            ORDER BY c.last_created_at DESC
            """;

    private static final String CONVERSATION_USERS_SQL = """
            SELECT DISTINCT CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END AS other_user_id
            FROM message
            WHERE (sender_id = ? OR receiver_id = ?)
              AND receiver_id IS NOT NULL
              AND sender_id <> receiver_id
            ORDER BY other_user_id ASC
            """;

    private static final String MARK_READ_SQL = "UPDATE message SET is_read = TRUE WHERE id = ?";

    private static final String MARK_CONVERSATION_READ_SQL = """
            UPDATE message
            SET is_read = TRUE
            WHERE sender_id = ? AND receiver_id = ? AND is_read = FALSE
            """;

    private static final String COUNT_UNREAD_SQL = """
            SELECT COUNT(*)
            FROM message
            WHERE receiver_id = ? AND sender_id <> ? AND is_read = FALSE
            """;

    public MessageService() {
        ensureSchema();
    }

    public void sendMessageToUser(int senderId, int receiverId, String content) throws SQLException {
        ensureSchema();
        if (senderId <= 0 || receiverId <= 0) {
            throw new IllegalArgumentException("Sender and receiver are required.");
        }
        if (senderId == receiverId) {
            throw new IllegalArgumentException("Sender and receiver must be different users.");
        }
        String value = content == null ? "" : content.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Message content is required.");
        }

        Connection connection = DbConnexion.getInstance().getConnection();
        if (!userExists(connection, senderId)) {
            throw new IllegalArgumentException("Sender user does not exist: " + senderId);
        }
        if (!userExists(connection, receiverId)) {
            throw new IllegalArgumentException("Receiver user does not exist: " + receiverId);
        }

        int directGroupId = -1;

        LocalDateTime now = LocalDateTime.now();
        if (hasGroupIdColumn && hasLegacyChatGroupIdColumn) {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_DIRECT_WITH_BOTH_GROUP_SQL)) {
                statement.setInt(1, directGroupId);
                statement.setInt(2, directGroupId);
                statement.setInt(3, senderId);
                statement.setInt(4, receiverId);
                statement.setString(5, value);
                statement.setTimestamp(6, Timestamp.valueOf(now));
                statement.setTimestamp(7, Timestamp.valueOf(now));
                statement.setBoolean(8, false);
                statement.executeUpdate();
            }
            return;
        }

        if (hasGroupIdColumn) {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_DIRECT_WITH_GROUP_SQL)) {
                statement.setInt(1, directGroupId);
                statement.setInt(2, senderId);
                statement.setInt(3, receiverId);
                statement.setString(4, value);
                statement.setTimestamp(5, Timestamp.valueOf(now));
                statement.setTimestamp(6, Timestamp.valueOf(now));
                statement.setBoolean(7, false);
                statement.executeUpdate();
            }
            return;
        }

        if (hasLegacyChatGroupIdColumn) {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_DIRECT_WITH_CHAT_GROUP_SQL)) {
                statement.setInt(1, directGroupId);
                statement.setInt(2, senderId);
                statement.setInt(3, receiverId);
                statement.setString(4, value);
                statement.setTimestamp(5, Timestamp.valueOf(now));
                statement.setTimestamp(6, Timestamp.valueOf(now));
                statement.setBoolean(7, false);
                statement.executeUpdate();
            }
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(INSERT_DIRECT_SQL)) {
            statement.setInt(1, senderId);
            statement.setInt(2, receiverId);
            statement.setString(3, value);
            statement.setTimestamp(4, Timestamp.valueOf(now));
            statement.setTimestamp(5, Timestamp.valueOf(now));
            statement.setBoolean(6, false);
            statement.executeUpdate();
        }
    }

    public void sendMessage(int senderId, int receiverId, String content) throws SQLException {
        sendMessageToUser(senderId, receiverId, content);
    }

    public void sendMessage(Message msg) throws SQLException {
        ensureSchema();
        validateMessage(msg);
        if (msg.getReceiverId() > 0) {
            sendMessageToUser(msg.getSenderId(), msg.getReceiverId(), msg.getContent());
            return;
        }
        if (msg.getGroupId() > 0) {
            sendMessageToGroup(msg.getSenderId(), msg.getGroupId(), msg.getContent());
            return;
        }
        throw new IllegalArgumentException("Receiver is required.");
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
                    int other = resultSet.getInt("other_user_id");
                    if (other > 0) {
                        users.add(other);
                    }
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
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    MessageConversation conversation = new MessageConversation();
                    conversation.setGroupId(0);
                    conversation.setGroup(false);
                    conversation.setGroupName(null);
                    conversation.setOtherUserId(resultSet.getInt("other_user_id"));
                    conversation.setOtherUserName(resultSet.getString("display_name"));
                    conversation.setOtherUserRole(resultSet.getString("display_role"));
                    conversation.setLastMessage(resultSet.getString("last_message"));
                    Timestamp lastAt = resultSet.getTimestamp("last_created_at");
                    if (lastAt != null) {
                        conversation.setLastMessageAt(lastAt.toLocalDateTime());
                    }
                    conversation.setUnreadCount(resultSet.getInt("unread_count"));
                    if (conversation.getOtherUserId() > 0) {
                        conversations.add(conversation);
                    }
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
            statement.setInt(2, userId);
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
        message.setGroupId(0);
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
        if (msg.getSenderId() <= 0) {
            throw new IllegalArgumentException("Sender is required.");
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
                    statement.execute(CREATE_MESSAGE_TABLE_SQL);
                }

            

                ensureMessageColumn(connection, "sender_id", "INT");
                ensureMessageColumn(connection, "receiver_id", "INT NULL");
                ensureMessageColumn(connection, "content", "TEXT");
                ensureMessageColumn(connection, "created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                ensureMessageColumn(connection, "updated_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
                ensureMessageColumn(connection, "is_read", "BOOLEAN DEFAULT FALSE");

                try (Statement st = connection.createStatement()) {
                    st.execute("ALTER TABLE message MODIFY COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
                } catch (SQLException ignored) {
                }

                hasGroupIdColumn = columnExists(connection, "message", "group_id");
                hasLegacyChatGroupIdColumn = columnExists(connection, "message", "chat_group_id");

                if (hasGroupIdColumn) {
                    relaxGroupColumnConstraint(connection);
                }

                migrateReceiverFromDirectGroups(connection);

                schemaEnsured = true;
            } catch (SQLException e) {
                throw new RuntimeException("Unable to ensure message schema", e);
            }
        }
    }

    private static void ensureColumn(Connection connection, String table, String column, String definition) throws SQLException {
        if (columnExists(connection, table, column)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private static void ensureMessageColumn(Connection connection, String column, String definition) throws SQLException {
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

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(connection.getCatalog(), null, table, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private static void relaxGroupColumnConstraint(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE message MODIFY COLUMN group_id INT NULL");
        } catch (SQLException ignored) {
        }
    }

    private static void migrateReceiverFromDirectGroups(Connection connection) {
        try {
            if (!hasGroupIdColumn || !tableExists(connection, "chat_group") || !tableExists(connection, "chat_group_members")) {
                return;
            }
            if (!columnExists(connection, "message", "receiver_id")) {
                return;
            }
            try (Statement st = connection.createStatement()) {
                st.execute("""
                        UPDATE message m
                        JOIN chat_group cg ON cg.id = m.group_id AND cg.is_group = FALSE
                        JOIN chat_group_members other ON other.group_id = cg.id AND other.user_id <> m.sender_id
                        SET m.receiver_id = other.user_id
                        WHERE m.receiver_id IS NULL
                          AND m.group_id IS NOT NULL
                          AND m.group_id <> 0
                        """);
            }
        } catch (SQLException ignored) {
        }
    }

    private static boolean userExists(Connection connection, int userId) throws SQLException {
        if (userId <= 0) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM `user` WHERE id = ? LIMIT 1")) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int resolveOrCreateDirectGroupId(Connection connection, int userAId, int userBId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_DIRECT_GROUP_SQL)) {
            statement.setInt(1, userAId);
            statement.setInt(2, userBId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        int groupId;
        try (PreparedStatement statement = connection.prepareStatement(INSERT_CHAT_GROUP_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setNull(1, java.sql.Types.VARCHAR);
            statement.setBoolean(2, false);
            statement.setTimestamp(3, Timestamp.valueOf(now));
            statement.setTimestamp(4, Timestamp.valueOf(now));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Unable to create direct conversation id.");
                }
                groupId = keys.getInt(1);
            }
        }

        try (PreparedStatement memberStatement = connection.prepareStatement(INSERT_CHAT_GROUP_MEMBER_SQL)) {
            memberStatement.setInt(1, groupId);
            memberStatement.setInt(2, userAId);
            memberStatement.setTimestamp(3, Timestamp.valueOf(now));
            memberStatement.setTimestamp(4, Timestamp.valueOf(now));
            memberStatement.addBatch();

            memberStatement.setInt(1, groupId);
            memberStatement.setInt(2, userBId);
            memberStatement.setTimestamp(3, Timestamp.valueOf(now));
            memberStatement.setTimestamp(4, Timestamp.valueOf(now));
            memberStatement.addBatch();

            memberStatement.executeBatch();
        }

        return groupId;
    }

    // Compatibility wrappers to avoid breaking external callers while group chat is disabled.
    public int resolveOrCreateDirectGroup(int userAId, int userBId) throws SQLException {
        ensureSchema();
        if (userAId <= 0 || userBId <= 0 || userAId == userBId) {
            throw new IllegalArgumentException("Both users are required.");
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        if (!userExists(connection, userAId) || !userExists(connection, userBId)) {
            throw new IllegalArgumentException("Both users must exist.");
        }
        return resolveOrCreateDirectGroupId(connection, userAId, userBId);
    }

    public int createGroup(String name, List<Integer> userIds) {
        throw new UnsupportedOperationException("Group chat is disabled.");
    }

    public void sendMessageToGroup(int senderId, int groupId, String content) throws SQLException {
        sendMessageToUser(senderId, groupId, content);
    }

    public List<Message> getGroupMessages(int groupId) {
        throw new UnsupportedOperationException("Group chat is disabled.");
    }

    public void markGroupAsRead(int currentUserId, int groupId) throws SQLException {
        markConversationAsRead(currentUserId, groupId);
    }
    public void editMessage(int messageId, int senderId, String newContent) throws SQLException {
    if (newContent == null || newContent.isBlank()) {
        throw new IllegalArgumentException("Content is required.");
    }
    Connection connection = DbConnexion.getInstance().getConnection();
    try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE message SET content = ?, updated_at = ? WHERE id = ? AND sender_id = ?")) {
        statement.setString(1, newContent.trim());
        statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
        statement.setInt(3, messageId);
        statement.setInt(4, senderId);
        statement.executeUpdate();
    }
}

public void deleteMessage(int messageId, int senderId) throws SQLException {
    Connection connection = DbConnexion.getInstance().getConnection();
    try (PreparedStatement statement = connection.prepareStatement(
            "DELETE FROM message WHERE id = ? AND sender_id = ?")) {
        statement.setInt(1, messageId);
        statement.setInt(2, senderId);
        statement.executeUpdate();
    }
}
    
}

