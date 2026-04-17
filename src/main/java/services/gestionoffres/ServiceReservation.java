package services.gestionoffres;

import models.gestionagences.AgencyAccount;
import models.gestionoffres.Reservation;
import models.gestionoffres.TravelOffer;
import services.CRUD;
import services.gestionagences.AgencyAccountService;
import services.notifications.NotificationService;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceReservation implements CRUD<Reservation, Integer> {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private static final String UNIQUE_KEY_NAME = "uk_reservation_user_offer";

    private final AgencyAccountService agencyAccountService = new AgencyAccountService();
    private final NotificationService notificationService = new NotificationService();
    private static volatile boolean schemaEnsured;

    private static final String INSERT_SQL = """
            INSERT INTO reservation (
                offer_id, user_id, contact_info, reserved_seats, total_price, reservation_date, status, is_paid, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE reservation SET
                offer_id = ?, user_id = ?, contact_info = ?, reserved_seats = ?,
                total_price = ?, reservation_date = ?, status = ?, is_paid = ?, updated_at = ?
            WHERE id = ?
            """;

    private static final String DELETE_SQL = "DELETE FROM reservation WHERE id = ?";

    private static final String UPDATE_STATUS_SQL = "UPDATE reservation SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        private static final String UPDATE_STATUS_IF_PENDING_SQL = """
            UPDATE reservation
            SET status = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status = ?
            """;
        private static final String SELECT_OFFER_AVAILABLE_SEATS_SQL = "SELECT available_seats FROM travel_offer WHERE id = ?";
        private static final String DECREMENT_OFFER_SEATS_SQL = """
            UPDATE travel_offer
            SET available_seats = available_seats - ?
            WHERE id = ? AND available_seats IS NOT NULL AND available_seats >= ?
            """;

    private static final String SELECT_JOIN_BASE = """
            SELECT
                r.*,
                u.username AS u_username,
                u.email AS u_email,
                t.id AS t_id,
                t.title AS t_title,
                t.countries AS t_countries,
                t.description AS t_description,
                t.departure_date AS t_departure_date,
                t.return_date AS t_return_date,
                t.price AS t_price,
                t.currency AS t_currency,
                t.available_seats AS t_available_seats,
                t.image AS t_image,
                t.agency_id AS t_agency_id,
                t.created_by_id AS t_created_by_id,
                t.created_at AS t_created_at,
                t.approval_status AS t_approval_status
            FROM reservation r
            LEFT JOIN user u ON r.user_id = u.id
            JOIN travel_offer t ON r.offer_id = t.id
            """;

    private static final String SELECT_ALL_SQL = SELECT_JOIN_BASE + " ORDER BY r.id DESC";
    private static final String SELECT_BY_ID_SQL = SELECT_JOIN_BASE + " WHERE r.id = ?";
    private static final String SELECT_BY_OFFER_SQL = SELECT_JOIN_BASE + " WHERE r.offer_id = ? ORDER BY r.id DESC";
    private static final String SELECT_BY_USER_SQL = SELECT_JOIN_BASE + " WHERE r.user_id = ? ORDER BY r.id DESC";
    private static final String SELECT_BY_USER_AND_OFFER_SQL = SELECT_JOIN_BASE + " WHERE r.user_id = ? AND r.offer_id = ? LIMIT 1";
    private static final String SELECT_BY_AGENCY_SQL = SELECT_JOIN_BASE + " WHERE t.agency_id = ? ORDER BY r.id DESC";

    public ServiceReservation() {
        ensureReservationSchema();
    }

    public Reservation createReservation(Integer userId, TravelOffer offer, Reservation draft) throws SQLException {
        ensureReservationSchema();
        ensureUserCanCreateReservation(userId);

        if (offer == null || offer.getId() <= 0) {
            throw new IllegalArgumentException("Offer is required.");
        }
        Reservation reservation = draft == null ? new Reservation() : draft;
        reservation.setOffer(offer);
        reservation.setUserId(userId);
        ensureRequestedSeatsAvailableNow(offer.getId(), reservation.getReservedSeats());
        validateReservationPayload(reservation, false);

        if (findByUserAndOffer(userId, offer.getId()).isPresent()) {
            throw new IllegalArgumentException("You already reserved this offer");
        }

        reservation.setStatus(STATUS_PENDING);
        reservation.setReservationDate(Timestamp.from(java.time.Instant.now()).toLocalDateTime());
        add(reservation);

        notifyAgencyForNewReservation(offer, reservation);
        return reservation;
    }

    public Reservation updateReservation(Integer userId, int reservationId, Reservation draft) throws SQLException {
        ensureReservationSchema();
        ensureUserCanCreateReservation(userId);

        Reservation existing = getById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));

        if (!userId.equals(existing.getUserId())) {
            throw new IllegalArgumentException("You cannot edit this reservation.");
        }

        Reservation updateValue = draft == null ? new Reservation() : draft;
        updateValue.setId(existing.getId());
        updateValue.setUserId(existing.getUserId());
        updateValue.setOffer(existing.getOffer());
        updateValue.setContactInfo(updateValue.getContactInfo() == null ? existing.getContactInfo() : updateValue.getContactInfo());
        updateValue.setReservedSeats(updateValue.getReservedSeats() == null ? existing.getReservedSeats() : updateValue.getReservedSeats());
        updateValue.setTotalPrice(updateValue.getTotalPrice() == null ? existing.getTotalPrice() : updateValue.getTotalPrice());
        updateValue.setPaid(existing.isPaid());
        updateValue.setReservationDate(existing.getReservationDate());
        updateValue.setStatus(STATUS_PENDING);

        ensureRequestedSeatsAvailableNow(existing.getOffer().getId(), updateValue.getReservedSeats());
        validateReservationPayload(updateValue, true);
        update(updateValue);
        return updateValue;
    }

    public boolean approveReservation(Integer agencyUserId, int reservationId) throws SQLException {
        ensureAgencyOwnsReservationOffer(agencyUserId, reservationId);
        return approveReservationWithSeatDecrement(reservationId);
    }

    public boolean rejectReservation(Integer agencyUserId, int reservationId) throws SQLException {
        ensureAgencyOwnsReservationOffer(agencyUserId, reservationId);
        boolean updated = updateStatus(reservationId, STATUS_REJECTED);
        if (updated) {
            notifyUserReservationRejected(reservationId);
        }
        return updated;
    }

    public List<Reservation> getUserReservations(Integer userId) throws SQLException {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        return getByUser(userId);
    }

    public List<Reservation> getAgencyReservations(Integer agencyId) throws SQLException {
        if (agencyId == null || agencyId <= 0) {
            return List.of();
        }
        return queryBySingleIntParam(SELECT_BY_AGENCY_SQL, agencyId);
    }

    public Optional<Reservation> findByUserAndOffer(Integer userId, Integer offerId) throws SQLException {
        if (userId == null || userId <= 0 || offerId == null || offerId <= 0) {
            return Optional.empty();
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_USER_AND_OFFER_SQL)) {
            statement.setInt(1, userId);
            statement.setInt(2, offerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapReservation(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public void add(Reservation entity) throws SQLException {
        ensureReservationSchema();
        validateReservationPayload(entity, false);
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bindReservationForInsert(statement, entity);
            try {
                statement.executeUpdate();
            } catch (SQLException ex) {
                if (isUniqueViolation(ex)) {
                    throw new IllegalArgumentException("You already reserved this offer");
                }
                throw ex;
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                }
            }
        }
        System.out.println("Reservation ajoutee avec succes.");
    }

    @Override
    public void update(Reservation entity) throws SQLException {
        ensureReservationSchema();
        validateReservationPayload(entity, true);
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            bindReservationForUpdate(statement, entity);
            statement.executeUpdate();
        }
        System.out.println("Reservation modifiee avec succes.");
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Reservation id is required for delete.");
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
        System.out.println("Reservation deleted successfully.");
    }

    public Optional<Reservation> getById(int reservationId) throws SQLException {
        if (reservationId <= 0) {
            return Optional.empty();
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {
            statement.setInt(1, reservationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapReservation(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public List<Reservation> getAll() throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                reservations.add(mapReservation(resultSet));
            }
        }
        return reservations;
    }

    public List<Reservation> getByOffer(int offerId) throws SQLException {
        if (offerId <= 0) {
            return List.of();
        }
        return queryBySingleIntParam(SELECT_BY_OFFER_SQL, offerId);
    }

    public List<Reservation> getByUser(int userId) throws SQLException {
        if (userId <= 0) {
            return List.of();
        }
        return queryBySingleIntParam(SELECT_BY_USER_SQL, userId);
    }

    public boolean confirm(int reservationId) throws SQLException {
        return updateStatus(reservationId, STATUS_APPROVED);
    }

    public boolean cancel(int reservationId) throws SQLException {
        return updateStatus(reservationId, STATUS_REJECTED);
    }

    @Override
    public void create(Reservation entity) throws SQLException {
        add(entity);
    }

    @Override
    public void insert(Reservation entity) throws SQLException {
        add(entity);
    }

    private List<Reservation> queryBySingleIntParam(String sql, int value) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reservations.add(mapReservation(resultSet));
                }
            }
        }
        return reservations;
    }

    private boolean updateStatus(int reservationId, String status) throws SQLException {
        if (reservationId <= 0) {
            return false;
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS_SQL)) {
            statement.setString(1, status);
            statement.setInt(2, reservationId);
            boolean updated = statement.executeUpdate() > 0;
            if (updated) {
                System.out.println("Reservation " + reservationId + " -> status: " + status);
            }
            return updated;
        }
    }

    private boolean approveReservationWithSeatDecrement(int reservationId) throws SQLException {
        if (reservationId <= 0) {
            return false;
        }

        Reservation reservation = getById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));

        if (!STATUS_PENDING.equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalArgumentException("Only pending reservations can be approved.");
        }

        TravelOffer offer = reservation.getOffer();
        if (offer == null || offer.getId() <= 0) {
            throw new IllegalArgumentException("Reservation offer not found.");
        }

        int reservedSeats = reservation.getReservedSeats() == null ? 0 : reservation.getReservedSeats();
        if (reservedSeats <= 0) {
            throw new IllegalArgumentException("Invalid reserved seats value.");
        }

        Connection connection = DbConnexion.getInstance().getConnection();
        boolean initialAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement decrementSeatsStmt = connection.prepareStatement(DECREMENT_OFFER_SEATS_SQL)) {
                decrementSeatsStmt.setInt(1, reservedSeats);
                decrementSeatsStmt.setInt(2, offer.getId());
                decrementSeatsStmt.setInt(3, reservedSeats);

                int seatRows = decrementSeatsStmt.executeUpdate();
                if (seatRows <= 0) {
                    throw new IllegalArgumentException("Not enough seats available to approve this reservation.");
                }
            }

            try (PreparedStatement statusStmt = connection.prepareStatement(UPDATE_STATUS_IF_PENDING_SQL)) {
                statusStmt.setString(1, STATUS_APPROVED);
                statusStmt.setInt(2, reservationId);
                statusStmt.setString(3, STATUS_PENDING);

                int statusRows = statusStmt.executeUpdate();
                if (statusRows <= 0) {
                    throw new IllegalArgumentException("Reservation is no longer pending.");
                }
            }

            connection.commit();
            System.out.println("Reservation " + reservationId + " -> status: " + STATUS_APPROVED + " (seats decremented)");
            notifyUserReservationApproved(reservation);
            return true;
        } catch (Exception ex) {
            connection.rollback();
            if (ex instanceof SQLException sqlEx) {
                throw sqlEx;
            }
            if (ex instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new SQLException(ex);
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    private void ensureRequestedSeatsAvailableNow(Integer offerId, Integer requestedSeats) throws SQLException {
        if (offerId == null || offerId <= 0) {
            throw new IllegalArgumentException("Reservation requires a valid offer id.");
        }
        if (requestedSeats == null || requestedSeats <= 0) {
            throw new IllegalArgumentException("Reserved seats must be at least 1.");
        }

        Integer availableSeats = getOfferAvailableSeats(offerId);
        if (availableSeats != null && requestedSeats > availableSeats) {
            throw new IllegalArgumentException("Reserved seats exceed available seats.");
        }
    }

    private Integer getOfferAvailableSeats(int offerId) throws SQLException {
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_OFFER_AVAILABLE_SEATS_SQL)) {
            statement.setInt(1, offerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Offer not found.");
                }
                int value = resultSet.getInt("available_seats");
                return resultSet.wasNull() ? null : value;
            }
        }
    }

    private void notifyAgencyForNewReservation(TravelOffer offer, Reservation reservation) {
        if (offer == null || offer.getAgencyId() == null) {
            return;
        }
        try {
            AgencyAccount agency = agencyAccountService.get(offer.getAgencyId().longValue()).orElse(null);
            if (agency == null || agency.getResponsableId() == null || agency.getResponsableId() <= 0) {
                return;
            }
            String title = offer.getTitle() == null || offer.getTitle().isBlank() ? "an offer" : offer.getTitle();
            notificationService.createNotification(agency.getResponsableId(), "New reservation request received for " + title + ".");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void notifyUserReservationApproved(Reservation reservation) {
        if (reservation == null || reservation.getUserId() == null || reservation.getUserId() <= 0) {
            return;
        }
        try {
            notificationService.createNotification(reservation.getUserId(), "Your reservation has been approved.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void notifyUserReservationRejected(int reservationId) {
        try {
            Reservation reservation = getById(reservationId).orElse(null);
            if (reservation == null || reservation.getUserId() == null || reservation.getUserId() <= 0) {
                return;
            }
            notificationService.createNotification(reservation.getUserId(), "Your reservation has been rejected.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void ensureUserCanCreateReservation(Integer userId) throws SQLException {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user.");
        }
        Optional<AgencyAccount> agency = agencyAccountService.findByResponsableId(userId);
        if (agency.isPresent()) {
            throw new IllegalArgumentException("Agencies cannot create reservations.");
        }
    }

    private void ensureAgencyOwnsReservationOffer(Integer agencyUserId, int reservationId) throws SQLException {
        if (agencyUserId == null || agencyUserId <= 0) {
            throw new IllegalArgumentException("Invalid agency user.");
        }

        AgencyAccount agency = agencyAccountService.findByResponsableId(agencyUserId)
            .orElseThrow(() -> new IllegalArgumentException("Only agencies can manage reservations."));

        Reservation reservation = getById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));

        Integer offerAgencyId = reservation.getOffer() == null ? null : reservation.getOffer().getAgencyId();
        if (offerAgencyId == null || !offerAgencyId.equals(agency.getId().intValue())) {
            throw new IllegalArgumentException("You cannot manage reservations for this offer.");
        }
    }

    private static void validateReservationPayload(Reservation reservation, boolean idRequired) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null.");
        }
        if (idRequired && reservation.getId() <= 0) {
            throw new IllegalArgumentException("Reservation id is required for update.");
        }
        TravelOffer offer = reservation.getOffer();
        if (offer == null || offer.getId() <= 0) {
            throw new IllegalArgumentException("Reservation requires a TravelOffer with a valid id.");
        }
        if (reservation.getUserId() == null || reservation.getUserId() <= 0) {
            throw new IllegalArgumentException("Reservation user is required.");
        }
        if (reservation.getReservedSeats() == null || reservation.getReservedSeats() < 1) {
            throw new IllegalArgumentException("Reserved seats must be at least 1.");
        }
        if (reservation.getOffer().getAvailableSeats() != null && reservation.getReservedSeats() > reservation.getOffer().getAvailableSeats()) {
            throw new IllegalArgumentException("Reserved seats exceed available seats.");
        }
        if (reservation.getContactInfo() == null || reservation.getContactInfo().isBlank()) {
            throw new IllegalArgumentException("Contact info is required.");
        }
        List<String> allowedStatuses = List.of(STATUS_PENDING, STATUS_APPROVED, STATUS_REJECTED);
        if (!allowedStatuses.contains(reservation.getStatus())) {
            throw new IllegalArgumentException("Invalid reservation status.");
        }
    }

    private static void bindReservationForInsert(PreparedStatement statement, Reservation reservation) throws SQLException {
        statement.setInt(1, reservation.getOffer().getId());
        ServiceTravelOffer.setNullableInteger(statement, 2, reservation.getUserId());
        statement.setString(3, reservation.getContactInfo());
        ServiceTravelOffer.setNullableInteger(statement, 4, reservation.getReservedSeats());
        statement.setBigDecimal(5, reservation.getTotalPrice());
        if (reservation.getReservationDate() != null) {
            statement.setTimestamp(6, Timestamp.valueOf(reservation.getReservationDate()));
        } else {
            statement.setNull(6, Types.TIMESTAMP);
        }
        statement.setString(7, reservation.getStatus());
        if (reservation.isPaid() == null) {
            statement.setNull(8, Types.BOOLEAN);
        } else {
            statement.setBoolean(8, reservation.isPaid());
        }
        Timestamp now = Timestamp.from(java.time.Instant.now());
        statement.setTimestamp(9, now);
        statement.setTimestamp(10, now);
    }

    private static void bindReservationForUpdate(PreparedStatement statement, Reservation reservation) throws SQLException {
        statement.setInt(1, reservation.getOffer().getId());
        ServiceTravelOffer.setNullableInteger(statement, 2, reservation.getUserId());
        statement.setString(3, reservation.getContactInfo());
        ServiceTravelOffer.setNullableInteger(statement, 4, reservation.getReservedSeats());
        statement.setBigDecimal(5, reservation.getTotalPrice());
        if (reservation.getReservationDate() != null) {
            statement.setTimestamp(6, Timestamp.valueOf(reservation.getReservationDate()));
        } else {
            statement.setNull(6, Types.TIMESTAMP);
        }
        statement.setString(7, reservation.getStatus());
        if (reservation.isPaid() == null) {
            statement.setNull(8, Types.BOOLEAN);
        } else {
            statement.setBoolean(8, reservation.isPaid());
        }
        statement.setTimestamp(9, Timestamp.from(java.time.Instant.now()));
        statement.setInt(10, reservation.getId());
    }

    private static Reservation mapReservation(ResultSet resultSet) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setId(resultSet.getInt("id"));

        TravelOffer offer = ServiceTravelOffer.mapOfferFromAliases(resultSet);
        reservation.setOffer(offer);

        reservation.setUserId(ServiceTravelOffer.getNullableInteger(resultSet, "user_id"));
        String requesterName = resultSet.getString("u_username");
        if (requesterName == null || requesterName.isBlank()) {
            requesterName = resultSet.getString("u_email");
        }
        reservation.setRequesterName(requesterName);
        reservation.setContactInfo(resultSet.getString("contact_info"));
        reservation.setReservedSeats(ServiceTravelOffer.getNullableInteger(resultSet, "reserved_seats"));
        reservation.setTotalPrice(resultSet.getBigDecimal("total_price"));

        Timestamp reservationDate = resultSet.getTimestamp("reservation_date");
        if (reservationDate != null) {
            reservation.setReservationDate(reservationDate.toLocalDateTime());
        }

        String status = resultSet.getString("status");
        if (status != null) {
            String normalized = status.toUpperCase();
            if ("CONFIRMED".equals(normalized)) {
                status = STATUS_APPROVED;
            } else if ("CANCELLED".equals(normalized)) {
                status = STATUS_REJECTED;
            } else if (!List.of(STATUS_PENDING, STATUS_APPROVED, STATUS_REJECTED).contains(normalized)) {
                status = STATUS_PENDING;
            } else {
                status = normalized;
            }
        }
        reservation.setStatus(status);

        boolean paid = resultSet.getBoolean("is_paid");
        reservation.setPaid(resultSet.wasNull() ? null : paid);

        return reservation;
    }

    private void ensureReservationSchema() {
        if (schemaEnsured) {
            return;
        }
        synchronized (ServiceReservation.class) {
            if (schemaEnsured) {
                return;
            }

            try {
                Connection connection = DbConnexion.getInstance().getConnection();
                try (Statement st = connection.createStatement()) {
                    st.execute("ALTER TABLE reservation ADD COLUMN IF NOT EXISTS total_price DECIMAL(10,2) NULL");
                    st.execute("ALTER TABLE reservation ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
                    st.execute("ALTER TABLE reservation ADD COLUMN IF NOT EXISTS updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
                }

                try (Statement st = connection.createStatement()) {
                    st.execute("UPDATE reservation SET status = UPPER(status) WHERE status IS NOT NULL");
                    st.execute("UPDATE reservation SET status = 'APPROVED' WHERE UPPER(status) IN ('APPROVED','CONFIRMED')");
                    st.execute("UPDATE reservation SET status = 'REJECTED' WHERE UPPER(status) IN ('REJECTED','CANCELLED')");
                    st.execute("UPDATE reservation SET status = 'PENDING' WHERE status IS NULL OR UPPER(status) NOT IN ('PENDING','APPROVED','REJECTED')");
                    st.execute("ALTER TABLE reservation MODIFY COLUMN status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING'");
                }

                if (!hasUniqueIndex(connection, "reservation", UNIQUE_KEY_NAME)) {
                    try (Statement st = connection.createStatement()) {
                        st.execute("CREATE UNIQUE INDEX " + UNIQUE_KEY_NAME + " ON reservation(user_id, offer_id)");
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            schemaEnsured = true;
        }
    }

    private boolean hasUniqueIndex(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getIndexInfo(connection.getCatalog(), null, tableName, true, false)) {
            while (rs.next()) {
                String current = rs.getString("INDEX_NAME");
                if (current != null && current.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isUniqueViolation(SQLException ex) {
        String sqlState = ex.getSQLState();
        return "23000".equals(sqlState) || "23505".equals(sqlState);
    }
}
