package services.gestionmessages;

import models.gestionmessages.Notification;
import services.CRUD;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceNotification implements CRUD<Notification, Integer> {

    private static final String INSERT = """
            INSERT INTO notification (contenu, statut, user_id)
            VALUES (?, ?, ?)
            """;

    private static final String UPDATE = """
            UPDATE notification SET contenu = ?, statut = ?
            WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM notification WHERE id = ?";

    private static final String SELECT_BY_ID = """
            SELECT id, contenu, date_notification, statut, user_id
            FROM notification WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, contenu, date_notification, statut, user_id
            FROM notification ORDER BY date_notification DESC
            """;

    private static final String SELECT_BY_USER = """
            SELECT id, contenu, date_notification, statut, user_id
            FROM notification WHERE user_id = ?
            ORDER BY date_notification DESC
            """;

    private static final String UPDATE_STATUS = """
            UPDATE notification SET statut = ? WHERE id = ?
            """;

    @Override
    public void create(Notification entity) throws SQLException {
        add(entity);
    }

    @Override
    public void add(Notification entity) throws SQLException {
        validate(entity, true);
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getContenu());
            ps.setString(2, "non lu");
            ps.setInt(3, entity.getUserId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                }
            }
            System.out.println("✓ Notification ajoutée avec succès (ID: " + entity.getId() + ")");
        }
    }

    @Override
    public void update(Notification entity) throws SQLException {
        validate(entity, false);
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, entity.getContenu());
            ps.setString(2, entity.getStatut() != null ? entity.getStatut() : "non lu");
            ps.setInt(3, entity.getId());
            ps.executeUpdate();
            System.out.println("✓ Notification mise à jour avec succès (ID: " + entity.getId() + ")");
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            System.out.println("⚠ ID de la notification invalide");
            return;
        }
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✓ Notification supprimée avec succès (ID: " + id + ")");
        }
    }

    public Optional<Notification> get(Integer id) throws SQLException {
        if (id == null) {
            return Optional.empty();
        }
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Notification> findAll() throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                notifications.add(mapRow(rs));
            }
        }
        return notifications;
    }

    public List<Notification> getByUser(int userId) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_USER)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRow(rs));
                }
            }
        }
        System.out.println("✓ " + notifications.size() + " notification(s) trouvée(s) pour l'utilisateur " + userId);
        return notifications;
    }

    public void markAsRead(int notificationId) throws SQLException {
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, "lu");
            ps.setInt(2, notificationId);
            ps.executeUpdate();
            System.out.println("✓ Notification marquée comme lue (ID: " + notificationId + ")");
        }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification notif = new Notification();
        notif.setId(rs.getInt("id"));
        notif.setContenu(rs.getString("contenu"));
        Timestamp dateNotif = rs.getTimestamp("date_notification");
        notif.setDateNotification(dateNotif != null ? dateNotif.toLocalDateTime() : LocalDateTime.now());
        notif.setStatut(rs.getString("statut"));
        notif.setUserId(rs.getInt("user_id"));
        return notif;
    }

    private void validate(Notification entity, boolean isNew) {
        if (entity.getContenu() == null || entity.getContenu().trim().isEmpty()) {
            throw new IllegalArgumentException("⚠ Le contenu de la notification ne peut pas être vide");
        }
        if (entity.getUserId() == null || entity.getUserId() <= 0) {
            throw new IllegalArgumentException("⚠ L'ID utilisateur de la notification est invalide");
        }
        if (!isNew && entity.getId() == null) {
            throw new IllegalArgumentException("⚠ L'ID de la notification est invalide");
        }
    }
}
