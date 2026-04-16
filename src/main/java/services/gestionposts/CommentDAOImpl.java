package services.gestionposts;

import models.gestionposts.Comment;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation DAO pour Comment utilisant PreparedStatement.
 */
public class CommentDAOImpl implements CommentDAO {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS comment (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                content TEXT NOT NULL,
                user_id INT NOT NULL,
                post_id BIGINT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String INSERT = """
            INSERT INTO comment (content, user_id, post_id, created_at)
            VALUES (?, ?, ?, NOW())
            """;

    private static final String UPDATE = """
            UPDATE comment SET
                content = ?
            WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM comment WHERE id = ?";

    private static final String SELECT_BY_ID = """
            SELECT c.id, c.content, c.user_id, c.post_id, c.created_at,
                   u.username as author_username
            FROM comment c
            LEFT JOIN user u ON c.user_id = u.id
            WHERE c.id = ?
            """;

    private static final String SELECT_BY_POST_ID = """
            SELECT c.id, c.content, c.user_id, c.post_id, c.created_at,
                   u.username as author_username
            FROM comment c
            LEFT JOIN user u ON c.user_id = u.id
            WHERE c.post_id = ?
            """;

    private static final String SELECT_BY_POST_ID_ORDERED = """
            SELECT c.id, c.content, c.user_id, c.post_id, c.created_at,
                   u.username as author_username
            FROM comment c
            LEFT JOIN user u ON c.user_id = u.id
            WHERE c.post_id = ?
            ORDER BY c.created_at DESC
            """;

    private static final String COUNT_BY_POST_ID = """
            SELECT COUNT(*) FROM comment WHERE post_id = ?
            """;

    private static final String CHECK_AUTHOR = """
            SELECT COUNT(*) FROM comment WHERE id = ? AND user_id = ?
            """;

    private boolean tableChecked = false;

    private void ensureTableExists() throws SQLException {
        if (tableChecked) return;
        Connection c = DbConnexion.getInstance().getConnection();
        try (Statement stmt = c.createStatement()) {
            stmt.execute(CREATE_TABLE);
            tableChecked = true;
        } catch (SQLException e) {
            // Table might already exist or other error, continue
            tableChecked = true;
        }
    }

    @Override
    public void create(Comment comment) throws SQLException {
        ensureTableExists();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, comment.getContent());
            ps.setInt(2, comment.getUserId());
            ps.setLong(3, comment.getPostId());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    comment.setId(rs.getLong(1));
                }
            }
        }
    }

    @Override
    public void update(Comment comment) throws SQLException {
        ensureTableExists();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, comment.getContent());
            ps.setLong(2, comment.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Long id) throws SQLException {
        ensureTableExists();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Comment> findById(Long id) throws SQLException {
        ensureTableExists();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Comment> findByPostId(Long postId) throws SQLException {
        ensureTableExists();
        List<Comment> comments = new ArrayList<>();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_POST_ID)) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapRow(rs));
                }
            }
        }
        return comments;
    }

    @Override
    public List<Comment> findByPostIdOrdered(Long postId) throws SQLException {
        ensureTableExists();
        List<Comment> comments = new ArrayList<>();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_POST_ID_ORDERED)) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapRow(rs));
                }
            }
        }
        return comments;
    }

    @Override
    public int countByPostId(Long postId) throws SQLException {
        ensureTableExists();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(COUNT_BY_POST_ID)) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public boolean isAuthor(Long commentId, Integer userId) throws SQLException {
        ensureTableExists();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(CHECK_AUTHOR)) {
            ps.setLong(1, commentId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private Comment mapRow(ResultSet rs) throws SQLException {
        Comment comment = new Comment();
        comment.setId(rs.getLong("id"));
        comment.setContent(rs.getString("content"));
        comment.setUserId(rs.getInt("user_id"));
        comment.setPostId(rs.getLong("post_id"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        comment.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

        comment.setAuthorUsername(rs.getString("author_username"));

        return comment;
    }
}
