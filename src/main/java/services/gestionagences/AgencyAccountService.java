package services.gestionagences;

import models.gestionagences.AgencyAccount;
import models.gestionagences.ImageAsset;
import services.CRUD;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * CRUD JDBC pour {@code agency_account} (meme style que {@link services.gestionutilisateurs.UserService}), aligne sur l'integration Symfony {@code AgencyAccount}.
 */
public class AgencyAccountService implements CRUD<AgencyAccount, Long> {

    private final ImageAssetService imageAssetService;

    public AgencyAccountService() {
        this(new ImageAssetService());
    }

    public AgencyAccountService(ImageAssetService imageAssetService) {
        this.imageAssetService = imageAssetService;
    }

    private static final String SELECT_BY_ID = """
            SELECT id, agency_name, description, website_url, phone, address, country, latitude, longitude,
                   verified, created_at, updated_at, responsable_id, cover_image_id, agency_profile_image_id
            FROM agency_account WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, agency_name, description, website_url, phone, address, country, latitude, longitude,
                   verified, created_at, updated_at, responsable_id, cover_image_id, agency_profile_image_id
            FROM agency_account ORDER BY id
            """;

    private static final String SELECT_BY_RESPONSABLE = """
            SELECT id, agency_name, description, website_url, phone, address, country, latitude, longitude,
                   verified, created_at, updated_at, responsable_id, cover_image_id, agency_profile_image_id
            FROM agency_account WHERE responsable_id = ? LIMIT 1
            """;

    private static final String INSERT = """
            INSERT INTO agency_account (
                agency_name, description, website_url, phone, address, country, latitude, longitude,
                verified, responsable_id, cover_image_id, agency_profile_image_id, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE = """
            UPDATE agency_account SET
                agency_name = ?, description = ?, website_url = ?, phone = ?, address = ?, country = ?,
                latitude = ?, longitude = ?, verified = ?, responsable_id = ?, cover_image_id = ?, agency_profile_image_id = ?,
                updated_at = ?
            WHERE id = ?
            """;

    private static final String DELETE = """
            DELETE FROM agency_account WHERE id = ?
            """;

    @Override
    public void create(AgencyAccount entity) throws SQLException {
        insert(entity);
    }

    @Override
    public void insert(AgencyAccount entity) throws SQLException {
        validateForInsert(entity);
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bindForInsert(entity, ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getLong(1));
                }
            }
        }
        refreshTimestampsAfterWrite(entity);
    }

    @Override
    public void update(AgencyAccount entity) throws SQLException {
        if (entity.getId() == null) {
            throw new IllegalArgumentException("id obligatoire pour update");
        }
        validateForWrite(entity);
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE)) {
            bindForUpdate(entity, ps);
            ps.setLong(14, entity.getId());
            ps.executeUpdate();
        }
        refreshTimestampsAfterWrite(entity);
    }

    @Override
    public void delete(Long id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("id obligatoire pour delete");
        }
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<AgencyAccount> get(Long id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("id obligatoire pour get");
        }
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

    public List<AgencyAccount> findAll() throws SQLException {
        Connection c = DbConnexion.getInstance().getConnection();
        List<AgencyAccount> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Filtre par nom ou description (LIKE) et par pays (code ISO 2 lettres). {@code countryCode} vide ou null = tous les pays.
     */
    public List<AgencyAccount> findFiltered(String search, String countryCode) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT id, agency_name, description, website_url, phone, address, country, latitude, longitude,
                       verified, created_at, updated_at, responsable_id, cover_image_id, agency_profile_image_id
                FROM agency_account WHERE 1=1
                """);
        List<String> params = new ArrayList<>();
        String s = search != null ? search.trim() : "";
        if (!s.isEmpty()) {
            sql.append(" AND (LOWER(agency_name) LIKE ? OR LOWER(description) LIKE ?)");
            String like = "%" + s.toLowerCase(Locale.ROOT) + "%";
            params.add(like);
            params.add(like);
        }
        String cc = countryCode != null ? countryCode.trim() : "";
        if (cc.length() == 2) {
            sql.append(" AND country = ?");
            params.add(cc.toUpperCase(Locale.ROOT));
        }
        sql.append(" ORDER BY id");
        Connection c = DbConnexion.getInstance().getConnection();
        List<AgencyAccount> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public Optional<AgencyAccount> findByResponsableId(Integer userId) throws SQLException {
        if (userId == null) {
            return Optional.empty();
        }
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_RESPONSABLE)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Remplace la banniere (cover) : nouvelle ligne {@code image_asset}, mise a jour {@code cover_image_id}, suppression de l'ancienne image si elle existait.
     */
    public void replaceCoverImage(Long agencyAccountId, byte[] imageBytes, String mimeType) throws SQLException {
        replaceAgencyImageField(agencyAccountId, imageBytes, mimeType, true);
    }

    /**
     * Remplace le logo / image de profil de l'agence ({@code agency_profile_image_id}).
     */
    public void replaceAgencyProfileImage(Long agencyAccountId, byte[] imageBytes, String mimeType) throws SQLException {
        replaceAgencyImageField(agencyAccountId, imageBytes, mimeType, false);
    }

    private void replaceAgencyImageField(Long agencyAccountId, byte[] imageBytes, String mimeType, boolean cover) throws SQLException {
        if (agencyAccountId == null) {
            throw new IllegalArgumentException("agencyAccountId obligatoire.");
        }
        AgencyAccount a = get(agencyAccountId).orElseThrow(() -> new IllegalArgumentException("Agence introuvable."));
        ImageAsset asset = new ImageAsset();
        asset.setMimeType(mimeType);
        asset.setData(imageBytes);
        imageAssetService.insert(asset);
        Long previous = cover ? a.getCoverImageId() : a.getAgencyProfileImageId();
        if (cover) {
            a.setCoverImageId(asset.getId());
        } else {
            a.setAgencyProfileImageId(asset.getId());
        }
        update(a);
        if (previous != null && !previous.equals(asset.getId())) {
            imageAssetService.delete(previous);
        }
    }

    public Optional<ImageAsset> loadCoverImage(Long agencyAccountId) throws SQLException {
        Optional<AgencyAccount> a = get(agencyAccountId);
        if (a.isEmpty() || a.get().getCoverImageId() == null) {
            return Optional.empty();
        }
        return imageAssetService.get(a.get().getCoverImageId());
    }

    public Optional<ImageAsset> loadAgencyProfileImage(Long agencyAccountId) throws SQLException {
        Optional<AgencyAccount> a = get(agencyAccountId);
        if (a.isEmpty() || a.get().getAgencyProfileImageId() == null) {
            return Optional.empty();
        }
        return imageAssetService.get(a.get().getAgencyProfileImageId());
    }

    private static void validateForInsert(AgencyAccount e) {
        validateForWrite(e);
        if (e.getResponsableId() == null) {
            throw new IllegalArgumentException("responsable_id (utilisateur) est obligatoire.");
        }
    }

    private static void validateForWrite(AgencyAccount e) {
        if (e.getAgencyName() == null || e.getAgencyName().isBlank()) {
            throw new IllegalArgumentException("Le nom de l'agence est obligatoire.");
        }
        if (e.getDescription() == null) {
            throw new IllegalArgumentException("La description est obligatoire.");
        }
    }

    private void bindForInsert(AgencyAccount e, PreparedStatement ps) throws SQLException {
        int i = 1;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = e.getCreatedAt() != null ? e.getCreatedAt() : now;
        LocalDateTime updatedAt = now;
        ps.setString(i++, e.getAgencyName());
        ps.setString(i++, e.getDescription());
        nullableString(ps, i++, e.getWebsiteUrl());
        nullableString(ps, i++, e.getPhone());
        nullableString(ps, i++, e.getAddress());
        nullableString(ps, i++, e.getCountry());
        nullableDouble(ps, i++, e.getLatitude());
        nullableDouble(ps, i++, e.getLongitude());
        boolean verified = e.getVerified() != null && e.getVerified();
        ps.setBoolean(i++, verified);
        ps.setInt(i++, e.getResponsableId());
        nullableLong(ps, i++, e.getCoverImageId());
        nullableLong(ps, i++, e.getAgencyProfileImageId());
        ps.setTimestamp(i++, Timestamp.valueOf(createdAt));
        ps.setTimestamp(i++, Timestamp.valueOf(updatedAt));
    }

    private void bindForUpdate(AgencyAccount e, PreparedStatement ps) throws SQLException {
        int i = 1;
        LocalDateTime updatedAt = LocalDateTime.now();
        ps.setString(i++, e.getAgencyName());
        ps.setString(i++, e.getDescription());
        nullableString(ps, i++, e.getWebsiteUrl());
        nullableString(ps, i++, e.getPhone());
        nullableString(ps, i++, e.getAddress());
        nullableString(ps, i++, e.getCountry());
        nullableDouble(ps, i++, e.getLatitude());
        nullableDouble(ps, i++, e.getLongitude());
        boolean verified = e.getVerified() != null && e.getVerified();
        ps.setBoolean(i++, verified);
        ps.setInt(i++, e.getResponsableId());
        nullableLong(ps, i++, e.getCoverImageId());
        nullableLong(ps, i++, e.getAgencyProfileImageId());
        ps.setTimestamp(i++, Timestamp.valueOf(updatedAt));
    }

    private AgencyAccount mapRow(ResultSet rs) throws SQLException {
        AgencyAccount a = new AgencyAccount();
        a.setId(rs.getLong("id"));
        a.setAgencyName(rs.getString("agency_name"));
        a.setDescription(rs.getString("description"));
        a.setWebsiteUrl(rs.getString("website_url"));
        if (rs.wasNull()) {
            a.setWebsiteUrl(null);
        }
        a.setPhone(rs.getString("phone"));
        if (rs.wasNull()) {
            a.setPhone(null);
        }
        a.setAddress(rs.getString("address"));
        if (rs.wasNull()) {
            a.setAddress(null);
        }
        a.setCountry(rs.getString("country"));
        if (rs.wasNull()) {
            a.setCountry(null);
        }
        double lat = rs.getDouble("latitude");
        a.setLatitude(rs.wasNull() ? null : lat);
        double lon = rs.getDouble("longitude");
        a.setLongitude(rs.wasNull() ? null : lon);
        a.setVerified(rs.getBoolean("verified"));
        Timestamp ca = rs.getTimestamp("created_at");
        a.setCreatedAt(ca == null ? null : ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        a.setUpdatedAt(ua == null ? null : ua.toLocalDateTime());
        int rid = rs.getInt("responsable_id");
        a.setResponsableId(rs.wasNull() ? null : rid);
        long cover = rs.getLong("cover_image_id");
        a.setCoverImageId(rs.wasNull() ? null : cover);
        long prof = rs.getLong("agency_profile_image_id");
        a.setAgencyProfileImageId(rs.wasNull() ? null : prof);
        return a;
    }

    private void refreshTimestampsAfterWrite(AgencyAccount entity) throws SQLException {
        if (entity.getId() == null) {
            return;
        }
        Optional<AgencyAccount> fresh = get(entity.getId());
        if (fresh.isPresent()) {
            AgencyAccount f = fresh.get();
            entity.setCreatedAt(f.getCreatedAt());
            entity.setUpdatedAt(f.getUpdatedAt());
        }
    }

    private static void nullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private static void nullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DOUBLE);
        } else {
            ps.setDouble(index, value);
        }
    }

    private static void nullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }
}
