package services.gestionutilisateurs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import enums.gestionutilisateurs.UserRole;
import models.gestionagences.ImageAsset;
import models.gestionutilisateurs.User;
import services.CRUD;
import services.gestionagences.ImageAssetService;
import utils.DbConnexion;
import utils.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class UserService implements CRUD<User, Integer> {

    private static final Pattern EMAIL_SIMPLE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ImageAssetService imageAssetService;

    public UserService() {
        this(new ImageAssetService());
    }

    public UserService(ImageAssetService imageAssetService) {
        this.imageAssetService = imageAssetService;
    }

    private static final String SELECT_BY_EMAIL = """
            SELECT id, username, email, password, roles, role, is_active, profile_image_id
            FROM `user` WHERE LOWER(email) = LOWER(?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, username, email, password, roles, role, is_active, profile_image_id
            FROM `user` WHERE id = ?
            """;

    private static final String INSERT_USER = """
            INSERT INTO `user` (
                username, email, password, roles, role, is_active, profile_image_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_USER = """
            UPDATE `user` SET
                username = ?, email = ?, password = ?, roles = ?, role = ?, is_active = ?, profile_image_id = ?
            WHERE id = ?
            """;

    private static final String DELETE_USER = """
            DELETE FROM `user` WHERE id = ?
            """;

    public User signUp(String username, String email, String rawPassword) throws SQLException {
        validateSignUp(username, email, rawPassword);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Cet email est deja utilise.");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(PasswordHasher.hash(rawPassword));
        user.setRoles(List.of(UserRole.USER.getValue()));
        user.setRole(UserRole.USER.getValue());
        user.setIsActive(true);
        insert(user);
        user.setPassword(null);
        return user;
    }

    public Optional<User> login(String email, String rawPassword) throws SQLException {
        if (email == null || email.isBlank() || rawPassword == null) {
            return Optional.empty();
        }
        Optional<User> found = findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        User user = found.get();
        if (!PasswordHasher.matches(rawPassword, user.getPassword())) {
            return Optional.empty();
        }
        user.setPassword(null);
        return Optional.of(user);
    }

    private static void validateSignUp(String username, String email, String rawPassword) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Le nom d'utilisateur est obligatoire.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'email est obligatoire.");
        }
        if (!EMAIL_SIMPLE.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("L'email n'est pas valide.");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caracteres.");
        }
    }

    @Override
    public void create(User entity) throws SQLException {
        insert(entity);
    }

    @Override
    public void insert(User entity) throws SQLException {
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
            fillStatementWithoutId(entity, ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(User entity) throws SQLException {
        if (entity.getId() == null) {
            throw new IllegalArgumentException("id obligatoire pour update");
        }
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE_USER)) {
            fillStatementWithoutId(entity, ps);
            ps.setInt(8, entity.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("id obligatoire pour delete");
        }
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE_USER)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<User> get(Integer id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("id obligatoire pour get");
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

    public Optional<User> findByEmail(String email) throws SQLException {
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Ajoute {@link UserRole#AGENCY_ADMIN} aux roles JSON du compte (apres approbation demande agence).
     */
    public void addAgencyAdminRole(Integer userId) throws SQLException {
        if (userId == null) {
            throw new IllegalArgumentException("userId obligatoire.");
        }
        User u = get(userId).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        LinkedHashSet<String> merged = new LinkedHashSet<>(u.getRoles() != null ? u.getRoles() : List.of());
        merged.add(UserRole.AGENCY_ADMIN.getValue());
        u.setRoles(new ArrayList<>(merged));
        update(u);
    }

    /**
     * Enregistre une nouvelle photo de profil (insert dans {@code image_asset}, met a jour {@code user.profile_image_id}, supprime l'ancienne ligne image si besoin).
     */
    public void replaceProfileImage(Integer userId, byte[] imageBytes, String mimeType) throws SQLException {
        if (userId == null) {
            throw new IllegalArgumentException("userId obligatoire.");
        }
        User u = get(userId).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        ImageAsset asset = new ImageAsset();
        asset.setMimeType(mimeType);
        asset.setData(imageBytes);
        imageAssetService.insert(asset);
        Long previous = u.getProfileImageId();
        u.setProfileImageId(asset.getId());
        update(u);
        if (previous != null && !previous.equals(asset.getId())) {
            imageAssetService.delete(previous);
        }
    }

    /** Charge le binaire + MIME pour affichage JavaFX ({@code new Image(stream)}). */
    public Optional<ImageAsset> loadProfileImage(Integer userId) throws SQLException {
        Optional<User> u = get(userId);
        if (u.isEmpty() || u.get().getProfileImageId() == null) {
            return Optional.empty();
        }
        return imageAssetService.get(u.get().getProfileImageId());
    }

    private void fillStatementWithoutId(User entity, PreparedStatement ps) throws SQLException {
        int i = 1;
        ps.setString(i++, entity.getUsername());
        ps.setString(i++, entity.getEmail());
        ps.setString(i++, entity.getPassword());
        ps.setString(i++, toJsonRoles(entity.getRoles()));
        nullableString(ps, i++, entity.getRole());
        nullableBoolean(ps, i++, entity.getIsActive());
        nullableLong(ps, i++, entity.getProfileImageId());
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRoles(readStringList(rs.getString("roles")));
        user.setRole(rs.getString("role"));
        user.setIsActive(readNullableBoolean(rs, "is_active"));
        long profileImg = rs.getLong("profile_image_id");
        user.setProfileImageId(rs.wasNull() ? null : profileImg);
        return user;
    }

    private static String toJsonRoles(List<String> roles) throws SQLException {
        try {
            return JSON.writeValueAsString(roles != null ? roles : List.of());
        } catch (JsonProcessingException e) {
            throw new SQLException("Serialisation roles JSON impossible", e);
        }
    }

    private static List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return JSON.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private static void nullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private static void nullableBoolean(PreparedStatement ps, int index, Boolean value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BOOLEAN);
        } else {
            ps.setBoolean(index, value);
        }
    }

    private static void nullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private static Boolean readNullableBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }
}
