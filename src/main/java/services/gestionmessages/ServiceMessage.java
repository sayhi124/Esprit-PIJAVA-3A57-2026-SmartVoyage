package services.gestionmessages;

import models.gestionmessages.Message;
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

public class ServiceMessage implements CRUD<Message, Integer> {

    private static final String INSERT = """
            INSERT INTO message (contenu, statut, expediteur_id, destinataire_id, group_id)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String UPDATE = """
            UPDATE message SET contenu = ?, statut = ?
            WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM message WHERE id = ?";

    private static final String SELECT_BY_ID = """
            SELECT id, contenu, date_envoi, statut, expediteur_id, destinataire_id, group_id
            FROM message WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, contenu, date_envoi, statut, expediteur_id, destinataire_id, group_id
            FROM message ORDER BY date_envoi DESC
            """;

    private static final String SELECT_BETWEEN_USERS = """
            SELECT id, contenu, date_envoi, statut, expediteur_id, destinataire_id, group_id
            FROM message 
            WHERE (expediteur_id = ? AND destinataire_id = ?) 
               OR (expediteur_id = ? AND destinataire_id = ?)
            AND group_id IS NULL
            ORDER BY date_envoi ASC
            """;

    private static final String SELECT_BY_GROUP = """
            SELECT id, contenu, date_envoi, statut, expediteur_id, destinataire_id, group_id
            FROM message WHERE group_id = ?
            ORDER BY date_envoi ASC
            """;

    @Override
    public void create(Message entity) throws SQLException {
        add(entity);
    }

    @Override
    public void add(Message entity) throws SQLException {
        validate(entity, true);
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getContenu());
            ps.setString(2, "non lu");
            ps.setInt(3, entity.getExpediteurId());
            ps.setObject(4, entity.getDestinatataireId());
            ps.setObject(5, entity.getGroupId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                }
            }
            System.out.println("✓ Message ajouté avec succès (ID: " + entity.getId() + ")");
        }
    }

    @Override
    public void update(Message entity) throws SQLException {
        validate(entity, false);
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, entity.getContenu());
            ps.setString(2, entity.getStatut() != null ? entity.getStatut() : "non lu");
            ps.setInt(3, entity.getId());
            ps.executeUpdate();
            System.out.println("✓ Message mis à jour avec succès (ID: " + entity.getId() + ")");
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            System.out.println("⚠ ID du message invalide");
            return;
        }
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✓ Message supprimé avec succès (ID: " + id + ")");
        }
    }

    public Optional<Message> get(Integer id) throws SQLException {
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

    public List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                messages.add(mapRow(rs));
            }
        }
        return messages;
    }

    public void sendPrivateMessage(int expediteurId, int destinataireId, String contenu) throws SQLException {
        if (contenu == null || contenu.trim().isEmpty()) {
            System.out.println("⚠ Le contenu du message ne peut pas être vide");
            return;
        }
        Message msg = new Message();
        msg.setExpediteurId(expediteurId);
        msg.setDestinatataireId(destinataireId);
        msg.setContenu(contenu);
        msg.setStatut("non lu");
        msg.setDateEnvoi(LocalDateTime.now());
        add(msg);
        System.out.println("✓ Message privé envoyé de l'utilisateur " + expediteurId + " à " + destinataireId);
    }

    public void sendGroupMessage(int expediteurId, int groupId, String contenu) throws SQLException {
        if (contenu == null || contenu.trim().isEmpty()) {
            System.out.println("⚠ Le contenu du message ne peut pas être vide");
            return;
        }
        Message msg = new Message();
        msg.setExpediteurId(expediteurId);
        msg.setGroupId(groupId);
        msg.setContenu(contenu);
        msg.setStatut("non lu");
        msg.setDateEnvoi(LocalDateTime.now());
        add(msg);
        System.out.println("✓ Message du groupe envoyé par l'utilisateur " + expediteurId + " au groupe " + groupId);
    }

    public List<Message> getMessagesBetweenUsers(int user1Id, int user2Id) throws SQLException {
        List<Message> messages = new ArrayList<>();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BETWEEN_USERS)) {
            ps.setInt(1, user1Id);
            ps.setInt(2, user2Id);
            ps.setInt(3, user2Id);
            ps.setInt(4, user1Id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapRow(rs));
                }
            }
        }
        System.out.println("✓ " + messages.size() + " message(s) trouvé(s) entre les utilisateurs " + user1Id + " et " + user2Id);
        return messages;
    }

    public List<Message> getMessagesByGroup(int groupId) throws SQLException {
        List<Message> messages = new ArrayList<>();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_GROUP)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapRow(rs));
                }
            }
        }
        System.out.println("✓ " + messages.size() + " message(s) trouvé(s) dans le groupe " + groupId);
        return messages;
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        Message msg = new Message();
        msg.setId(rs.getInt("id"));
        msg.setContenu(rs.getString("contenu"));
        Timestamp envoi = rs.getTimestamp("date_envoi");
        msg.setDateEnvoi(envoi != null ? envoi.toLocalDateTime() : LocalDateTime.now());
        msg.setStatut(rs.getString("statut"));
        msg.setExpediteurId(rs.getInt("expediteur_id"));
        Integer destinataire = nullable(rs.getInt("destinataire_id"));
        msg.setDestinatataireId(destinataire);
        Integer groupe = nullable(rs.getInt("group_id"));
        msg.setGroupId(groupe);
        return msg;
    }

    private Integer nullable(int value) {
        return value == 0 ? null : value;
    }

    private void validate(Message entity, boolean isNew) {
        if (entity.getExpediteurId() == null || entity.getExpediteurId() <= 0) {
            throw new IllegalArgumentException("⚠ L'ID de l'expéditeur est invalide");
        }
        if (entity.getContenu() == null || entity.getContenu().trim().isEmpty()) {
            throw new IllegalArgumentException("⚠ Le contenu du message ne peut pas être vide");
        }
        if (entity.getDestinatataireId() == null && entity.getGroupId() == null) {
            throw new IllegalArgumentException("⚠ Le message doit avoir soit un destinataire, soit un groupe");
        }
        if (!isNew && entity.getId() == null) {
            throw new IllegalArgumentException("⚠ L'ID du message est invalide");
        }
    }
}
