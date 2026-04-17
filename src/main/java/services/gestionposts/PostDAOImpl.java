package services.gestionposts;

import models.gestionposts.Post;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation DAO pour Post utilisant PreparedStatement.
 * Respecte le pattern DAO et utilise le singleton DbConnexion.
 */
public class PostDAOImpl implements PostDAO {

    private static final String INSERT = """
            INSERT INTO post (title, content, location, image_url, user_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, NOW(), NOW())
            """;

    private static final String UPDATE = """
            UPDATE post SET
                title = ?, content = ?, location = ?, image_url = ?, updated_at = NOW()
            WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM post WHERE id = ?";

    private static final String SELECT_BY_ID = """
            SELECT id, title, content, location, image_url, user_id,
                   created_at, updated_at
            FROM post
            WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, title, content, location, image_url, user_id,
                   created_at, updated_at
            FROM post
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_ALL = "SELECT COUNT(*) FROM post";

    private static final String SELECT_BY_LOCATION = """
            SELECT id, title, content, location, image_url, user_id,
                   created_at, updated_at
            FROM post
            WHERE location = ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_BY_LOCATION = "SELECT COUNT(*) FROM post WHERE location = ?";

        private static final String SELECT_BY_USER_ID = """
            SELECT id, title, content, location, image_url, user_id,
               created_at, updated_at
            FROM post
                WHERE user_id = ?
                  AND title NOT LIKE 'Recommandation voyage - %'
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;

            private static final String COUNT_BY_USER_ID = """
                SELECT COUNT(*) FROM post
                WHERE user_id = ?
                  AND title NOT LIKE 'Recommandation voyage - %'
                """;

    private static final String SEARCH = """
            SELECT id, title, content, location, image_url, user_id,
                   created_at, updated_at
            FROM post
            WHERE title LIKE ? OR content LIKE ? OR location LIKE ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_SEARCH = """
            SELECT COUNT(*) FROM post
            WHERE title LIKE ? OR content LIKE ? OR location LIKE ?
            """;

    private static final String SEARCH_BY_LOCATION_AND_KEYWORD = """
            SELECT id, title, content, location, image_url, user_id,
                   created_at, updated_at
            FROM post
            WHERE location = ? AND (title LIKE ? OR content LIKE ?)
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_SEARCH_BY_LOCATION_AND_KEYWORD = """
            SELECT COUNT(*) FROM post
            WHERE location = ? AND (title LIKE ? OR content LIKE ?)
            """;

    private static final String SELECT_ALL_LOCATIONS = """
            SELECT DISTINCT location FROM post WHERE location IS NOT NULL ORDER BY location
            """;

    @Override
    public void create(Post post) throws SQLException {
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, post.getTitre());
            ps.setString(2, post.getContenu());
            ps.setString(3, post.getLocation());
            ps.setString(4, post.getImageUrl());
            ps.setInt(5, post.getUserId() != null ? post.getUserId() : 1);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    post.setId(keys.getLong(1));
                }
            }
        }
    }

    @Override
    public void update(Post post) throws SQLException {
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, post.getTitre());
            ps.setString(2, post.getContenu());
            ps.setString(3, post.getLocation());
            ps.setString(4, post.getImageUrl());
            ps.setLong(5, post.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Long id) throws SQLException {
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Post> findById(Long id) throws SQLException {
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
    public List<Post> findAll(int offset, int limit) throws SQLException {
        List<Post> posts = new ArrayList<>();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_ALL)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapRow(rs));
                }
            }
        }
        return posts;
    }

    @Override
    public int countAll() throws SQLException {
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(COUNT_ALL);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    @Override
    public List<Post> findByLocation(String location, int offset, int limit) throws SQLException {
        List<Post> posts = new ArrayList<>();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_LOCATION)) {
            ps.setString(1, location);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapRow(rs));
                }
            }
        }
        return posts;
    }

    @Override
    public int countByLocation(String location) throws SQLException {
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(COUNT_BY_LOCATION)) {
            ps.setString(1, location);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public List<Post> findByUserId(Integer userId, int offset, int limit) throws SQLException {
        List<Post> posts = new ArrayList<>();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_USER_ID)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapRow(rs));
                }
            }
        }
        return posts;
    }

    @Override
    public int countByUserId(Integer userId) throws SQLException {
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(COUNT_BY_USER_ID)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public List<Post> search(String keyword, int offset, int limit) throws SQLException {
        List<Post> posts = new ArrayList<>();
        String searchPattern = "%" + keyword + "%";
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SEARCH)) {
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            ps.setInt(4, limit);
            ps.setInt(5, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapRow(rs));
                }
            }
        }
        return posts;
    }

    @Override
    public int countSearch(String keyword) throws SQLException {
        String searchPattern = "%" + keyword + "%";
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(COUNT_SEARCH)) {
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public List<Post> searchByLocationAndKeyword(String location, String keyword, int offset, int limit) throws SQLException {
        List<Post> posts = new ArrayList<>();
        String searchPattern = "%" + keyword + "%";
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SEARCH_BY_LOCATION_AND_KEYWORD)) {
            ps.setString(1, location);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            ps.setInt(4, limit);
            ps.setInt(5, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapRow(rs));
                }
            }
        }
        return posts;
    }

    @Override
    public int countSearchByLocationAndKeyword(String location, String keyword) throws SQLException {
        String searchPattern = "%" + keyword + "%";
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(COUNT_SEARCH_BY_LOCATION_AND_KEYWORD)) {
            ps.setString(1, location);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public List<String> findAllLocations() throws SQLException {
        List<String> locations = new ArrayList<>();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_ALL_LOCATIONS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                locations.add(rs.getString("location"));
            }
        }
        return locations;
    }

    private Post mapRow(ResultSet rs) throws SQLException {
        Post post = new Post();
        post.setId(rs.getLong("id"));
        post.setTitre(rs.getString("title"));
        post.setContenu(rs.getString("content"));
        post.setLocation(rs.getString("location"));
        post.setImageUrl(rs.getString("image_url"));
        post.setUserId(rs.getInt("user_id"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        post.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        post.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

        // Set default 0 for likes and comments (tables may not exist)
        post.setNbLikes(0);
        post.setNbCommentaires(0);

        return post;
    }
}
