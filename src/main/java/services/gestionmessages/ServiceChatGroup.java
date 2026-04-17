package services.gestionmessages;

import models.gestionmessages.ChatGroup;
import models.gestionmessages.GroupMember;
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

public class ServiceChatGroup implements CRUD<ChatGroup, Integer> {

    private static final String INSERT = """
            INSERT INTO chat_group (nom)
            VALUES (?)
            """;

    private static final String UPDATE = """
            UPDATE chat_group SET nom = ?
            WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM chat_group WHERE id = ?";

    private static final String SELECT_BY_ID = """
            SELECT id, nom, date_creation
            FROM chat_group WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, nom, date_creation
            FROM chat_group ORDER BY date_creation DESC
            """;

    private static final String INSERT_MEMBER = """
            INSERT INTO group_member (group_id, user_id)
            VALUES (?, ?)
            """;

    private static final String DELETE_MEMBER = """
            DELETE FROM group_member WHERE group_id = ? AND user_id = ?
            """;

    private static final String SELECT_MEMBERS = """
            SELECT gm.id, gm.group_id, gm.user_id
            FROM group_member gm
            WHERE gm.group_id = ?
            """;

    @Override
    public void create(ChatGroup entity) throws SQLException {
        add(entity);
    }

    @Override
    public void add(ChatGroup entity) throws SQLException {
        validate(entity, true);
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getNom());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                }
            }
            System.out.println("✓ Groupe créé avec succès (ID: " + entity.getId() + ")");
        }
    }

    @Override
    public void update(ChatGroup entity) throws SQLException {
        validate(entity, false);
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, entity.getNom());
            ps.setInt(2, entity.getId());
            ps.executeUpdate();
            System.out.println("✓ Groupe mis à jour avec succès (ID: " + entity.getId() + ")");
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            System.out.println("⚠ ID du groupe invalide");
            return;
        }
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✓ Groupe supprimé avec succès (ID: " + id + ")");
        }
    }

    public Optional<ChatGroup> get(Integer id) throws SQLException {
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

    public List<ChatGroup> findAll() throws SQLException {
        List<ChatGroup> groups = new ArrayList<>();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                groups.add(mapRow(rs));
            }
        }
        return groups;
    }

    public void createGroup(String name, int creatorId) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            System.out.println("⚠ Le nom du groupe ne peut pas être vide");
            return;
        }
        ChatGroup group = new ChatGroup();
        group.setNom(name);
        group.setDateCreation(LocalDateTime.now());
        add(group);
        
        // Add creator as first member
        try {
            addUserToGroup(group.getId(), creatorId);
        } catch (SQLException e) {
            System.out.println("⚠ Erreur lors de l'ajout du créateur au groupe: " + e.getMessage());
        }
        System.out.println("✓ Groupe '" + name + "' créé avec succès par l'utilisateur " + creatorId);
    }

    public void addUserToGroup(int groupId, int userId) throws SQLException {
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT_MEMBER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            ps.executeUpdate();
            System.out.println("✓ Utilisateur " + userId + " ajouté au groupe " + groupId);
        }
    }

    public void removeUserFromGroup(int groupId, int userId) throws SQLException {
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE_MEMBER)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            ps.executeUpdate();
            System.out.println("✓ Utilisateur " + userId + " retiré du groupe " + groupId);
        }
    }

    public List<GroupMember> getGroupMembers(int groupId) throws SQLException {
        List<GroupMember> members = new ArrayList<>();
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_MEMBERS)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GroupMember member = new GroupMember();
                    member.setId(rs.getInt("id"));
                    member.setGroupId(rs.getInt("group_id"));
                    member.setUserId(rs.getInt("user_id"));
                    members.add(member);
                }
            }
        }
        System.out.println("✓ " + members.size() + " membre(s) trouvé(s) dans le groupe " + groupId);
        return members;
    }

    private ChatGroup mapRow(ResultSet rs) throws SQLException {
        ChatGroup group = new ChatGroup();
        group.setId(rs.getInt("id"));
        group.setNom(rs.getString("nom"));
        Timestamp creation = rs.getTimestamp("date_creation");
        group.setDateCreation(creation != null ? creation.toLocalDateTime() : LocalDateTime.now());
        return group;
    }

    private void validate(ChatGroup entity, boolean isNew) {
        if (entity.getNom() == null || entity.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("⚠ Le nom du groupe ne peut pas être vide");
        }
        if (!isNew && entity.getId() == null) {
            throw new IllegalArgumentException("⚠ L'ID du groupe est invalide");
        }
    }
}
