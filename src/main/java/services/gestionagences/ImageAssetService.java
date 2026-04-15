package services.gestionagences;

import models.gestionagences.ImageAsset;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Stockage / lecture des images (couverture agence, logo agence, photo profil utilisateur) via {@code image_asset}.
 */
public class ImageAssetService {

    private static final long MAX_BYTES = 8 * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final String INSERT = """
            INSERT INTO image_asset (mime_type, data) VALUES (?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, mime_type, data, created_at FROM image_asset WHERE id = ?
            """;

    private static final String DELETE = """
            DELETE FROM image_asset WHERE id = ?
            """;

    public void insert(ImageAsset entity) throws SQLException {
        validatePayload(entity);
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getMimeType().trim().toLowerCase(Locale.ROOT));
            ps.setBytes(2, entity.getData());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getLong(1));
                }
            }
        }
        refreshCreatedAt(entity);
    }

    public Optional<ImageAsset> get(Long id) throws SQLException {
        if (id == null) {
            return Optional.empty();
        }
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                ImageAsset a = new ImageAsset();
                a.setId(rs.getLong("id"));
                a.setMimeType(rs.getString("mime_type"));
                a.setData(rs.getBytes("data"));
                Timestamp ts = rs.getTimestamp("created_at");
                a.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
                return Optional.of(a);
            }
        }
    }

    public void delete(Long id) throws SQLException {
        if (id == null) {
            return;
        }
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private void refreshCreatedAt(ImageAsset entity) throws SQLException {
        if (entity.getId() == null) {
            return;
        }
        Optional<ImageAsset> fresh = get(entity.getId());
        fresh.ifPresent(f -> entity.setCreatedAt(f.getCreatedAt()));
    }

    private static void validatePayload(ImageAsset entity) {
        if (entity.getMimeType() == null || entity.getMimeType().isBlank()) {
            throw new IllegalArgumentException("Le type MIME de l'image est obligatoire.");
        }
        String mime = entity.getMimeType().trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_MIME.contains(mime)) {
            throw new IllegalArgumentException("Format d'image non autorise (JPEG, PNG, GIF, WebP uniquement).");
        }
        if (entity.getData() == null || entity.getData().length == 0) {
            throw new IllegalArgumentException("Le contenu de l'image est vide.");
        }
        if (entity.getData().length > MAX_BYTES) {
            throw new IllegalArgumentException("Image trop volumineuse (max 8 Mo).");
        }
        entity.setMimeType(mime);
    }
}
