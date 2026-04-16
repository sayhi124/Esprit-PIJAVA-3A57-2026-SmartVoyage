package services.gestionoffres;

import models.gestionoffres.Reservation;
import models.gestionoffres.TravelOffer;
import services.CRUD;
import utils.DbConnexion;

import java.sql.Connection;
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

    private static final String INSERT_SQL = """
            INSERT INTO reservation (
                offer_id, user_id, contact_info, reserved_seats, reservation_date, status, is_paid
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE reservation SET
                offer_id = ?, user_id = ?, contact_info = ?, reserved_seats = ?,
                reservation_date = ?, status = ?, is_paid = ?
            WHERE id = ?
            """;

    private static final String DELETE_SQL = "DELETE FROM reservation WHERE id = ?";

    private static final String UPDATE_STATUS_SQL = "UPDATE reservation SET status = ? WHERE id = ?";

    private static final String SELECT_JOIN_BASE = """
            SELECT
                r.*,
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
            JOIN travel_offer t ON r.offer_id = t.id
            """;

    private static final String SELECT_ALL_SQL = SELECT_JOIN_BASE + " ORDER BY r.id DESC";
    private static final String SELECT_BY_ID_SQL = SELECT_JOIN_BASE + " WHERE r.id = ?";
    private static final String SELECT_BY_OFFER_SQL = SELECT_JOIN_BASE + " WHERE r.offer_id = ? ORDER BY r.id DESC";
    private static final String SELECT_BY_USER_SQL = SELECT_JOIN_BASE + " WHERE r.user_id = ? ORDER BY r.id DESC";

    public void add(Reservation entity) throws SQLException {
        validateReservation(entity, false);
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bindReservation(statement, entity);
            statement.executeUpdate();
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
        validateReservation(entity, true);
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            bindReservation(statement, entity);
            statement.setInt(8, entity.getId());
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
        System.out.println("Reservation supprimee avec succes.");
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
        return updateStatus(reservationId, "confirmed");
    }

    public boolean cancel(int reservationId) throws SQLException {
        return updateStatus(reservationId, "cancelled");
    }

    @Override
    // alias of add()
    public void create(Reservation entity) throws SQLException {
        add(entity);
    }

    @Override
    // alias of add()
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

    private static void validateReservation(Reservation reservation, boolean idRequired) {
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
        List<String> allowedStatuses = List.of("pending", "confirmed", "cancelled");
        if (!allowedStatuses.contains(reservation.getStatus())) {
            throw new IllegalArgumentException("Invalid reservation status.");
        }
    }

    private static void bindReservation(PreparedStatement statement, Reservation reservation) throws SQLException {
        statement.setInt(1, reservation.getOffer().getId());
        ServiceTravelOffer.setNullableInteger(statement, 2, reservation.getUserId());
        statement.setString(3, reservation.getContactInfo());
        ServiceTravelOffer.setNullableInteger(statement, 4, reservation.getReservedSeats());
        if (reservation.getReservationDate() != null) {
            statement.setTimestamp(5, Timestamp.valueOf(reservation.getReservationDate()));
        } else {
            statement.setNull(5, Types.TIMESTAMP);
        }
        statement.setString(6, reservation.getStatus());
        if (reservation.isPaid() == null) {
            statement.setNull(7, Types.BOOLEAN);
        } else {
            statement.setBoolean(7, reservation.isPaid());
        }
    }

    private static Reservation mapReservation(ResultSet resultSet) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setId(resultSet.getInt("id"));

        TravelOffer offer = ServiceTravelOffer.mapOfferFromAliases(resultSet);
        reservation.setOffer(offer);

        reservation.setUserId(ServiceTravelOffer.getNullableInteger(resultSet, "user_id"));
        reservation.setContactInfo(resultSet.getString("contact_info"));
        reservation.setReservedSeats(ServiceTravelOffer.getNullableInteger(resultSet, "reserved_seats"));

        Timestamp reservationDate = resultSet.getTimestamp("reservation_date");
        if (reservationDate != null) {
            reservation.setReservationDate(reservationDate.toLocalDateTime());
        }

        reservation.setStatus(resultSet.getString("status"));

        boolean paid = resultSet.getBoolean("is_paid");
        reservation.setPaid(resultSet.wasNull() ? null : paid);

        return reservation;
    }
}