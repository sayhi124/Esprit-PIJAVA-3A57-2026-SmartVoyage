package services.gestionoffres;

import models.gestionoffres.TravelOffer;
import services.CRUD;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceTravelOffer implements CRUD<TravelOffer, Integer> {

    private static final String INSERT_SQL = """
            INSERT INTO travel_offer (
                title, countries, description, departure_date, return_date, price, currency,
                available_seats, image, agency_id, created_by_id, created_at, approval_status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE travel_offer SET
                title = ?, countries = ?, description = ?, departure_date = ?, return_date = ?,
                price = ?, currency = ?, available_seats = ?, image = ?, agency_id = ?,
                created_by_id = ?, created_at = ?, approval_status = ?
            WHERE id = ?
            """;

    private static final String DELETE_SQL = "DELETE FROM travel_offer WHERE id = ?";

    private static final String SELECT_BY_ID_SQL = """
            SELECT id, title, countries, description, departure_date, return_date, price, currency,
                   available_seats, image, agency_id, created_by_id, created_at, approval_status
            FROM travel_offer
            WHERE id = ?
            """;

    private static final String SELECT_ALL_SQL = """
            SELECT id, title, countries, description, departure_date, return_date, price, currency,
                   available_seats, image, agency_id, created_by_id, created_at, approval_status
            FROM travel_offer
            ORDER BY id DESC
            """;

    public void add(TravelOffer entity) throws SQLException {
        validateOffer(entity, false);
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bindOffer(statement, entity);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                }
            }
        }
        System.out.println("TravelOffer ajoute avec succes.");
    }

    @Override
    public void update(TravelOffer entity) throws SQLException {
        validateOffer(entity, true);
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            bindOffer(statement, entity);
            statement.setInt(14, entity.getId());
            statement.executeUpdate();
        }
        System.out.println("TravelOffer modifie avec succes.");
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("TravelOffer id is required for delete.");
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
        System.out.println("TravelOffer supprime avec succes.");
    }

    public Optional<TravelOffer> getById(int id) throws SQLException {
        if (id <= 0) {
            return Optional.empty();
        }
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapOffer(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public List<TravelOffer> getAll() throws SQLException {
        List<TravelOffer> offers = new ArrayList<>();
        Connection connection = DbConnexion.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                offers.add(mapOffer(resultSet));
            }
        }
        return offers;
    }

    @Override
    // alias of add()
    public void create(TravelOffer entity) throws SQLException {
        add(entity);
    }

    @Override
    // alias of add()
    public void insert(TravelOffer entity) throws SQLException {
        add(entity);
    }

    private static void validateOffer(TravelOffer offer, boolean idRequired) {
        if (offer == null) {
            throw new IllegalArgumentException("TravelOffer cannot be null.");
        }
        if (idRequired && offer.getId() <= 0) {
            throw new IllegalArgumentException("TravelOffer id is required for update.");
        }
        if (offer.getTitle() == null || offer.getTitle().isBlank()) {
            throw new IllegalArgumentException("TravelOffer title is required.");
        }
    }

    private static void bindOffer(PreparedStatement statement, TravelOffer offer) throws SQLException {
        statement.setString(1, offer.getTitle());
        statement.setString(2, offer.getCountries());
        statement.setString(3, offer.getDescription());
        if (offer.getDepartureDate() != null) {
            statement.setDate(4, Date.valueOf(offer.getDepartureDate()));
        } else {
            statement.setNull(4, Types.DATE);
        }
        if (offer.getReturnDate() != null) {
            statement.setDate(5, Date.valueOf(offer.getReturnDate()));
        } else {
            statement.setNull(5, Types.DATE);
        }
        statement.setBigDecimal(6, offer.getPrice());
        statement.setString(7, offer.getCurrency());
        setNullableInteger(statement, 8, offer.getAvailableSeats());
        statement.setString(9, offer.getImage());
        setNullableInteger(statement, 10, offer.getAgencyId());
        setNullableInteger(statement, 11, offer.getCreatedById());
        if (offer.getCreatedAt() != null) {
            statement.setTimestamp(12, Timestamp.valueOf(offer.getCreatedAt()));
        } else {
            statement.setNull(12, Types.TIMESTAMP);
        }
        statement.setString(13, offer.getApprovalStatus());
    }

    static TravelOffer mapOffer(ResultSet resultSet) throws SQLException {
        TravelOffer offer = new TravelOffer();
        offer.setId(resultSet.getInt("id"));
        offer.setTitle(resultSet.getString("title"));
        offer.setCountries(resultSet.getString("countries"));
        offer.setDescription(resultSet.getString("description"));

        Date departureDate = resultSet.getDate("departure_date");
        if (departureDate != null) {
            offer.setDepartureDate(departureDate.toLocalDate());
        }

        Date returnDate = resultSet.getDate("return_date");
        if (returnDate != null) {
            offer.setReturnDate(returnDate.toLocalDate());
        }

        offer.setPrice(resultSet.getBigDecimal("price"));
        offer.setCurrency(resultSet.getString("currency"));
        offer.setAvailableSeats(getNullableInteger(resultSet, "available_seats"));
        offer.setImage(resultSet.getString("image"));
        offer.setAgencyId(getNullableInteger(resultSet, "agency_id"));
        offer.setCreatedById(getNullableInteger(resultSet, "created_by_id"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            offer.setCreatedAt(createdAt.toLocalDateTime());
        }

        offer.setApprovalStatus(resultSet.getString("approval_status"));
        return offer;
    }

    static TravelOffer mapOfferFromAliases(ResultSet resultSet) throws SQLException {
        TravelOffer offer = new TravelOffer();
        offer.setId(resultSet.getInt("t_id"));
        offer.setTitle(resultSet.getString("t_title"));
        offer.setCountries(resultSet.getString("t_countries"));
        offer.setDescription(resultSet.getString("t_description"));

        Date departureDate = resultSet.getDate("t_departure_date");
        if (departureDate != null) {
            offer.setDepartureDate(departureDate.toLocalDate());
        }

        Date returnDate = resultSet.getDate("t_return_date");
        if (returnDate != null) {
            offer.setReturnDate(returnDate.toLocalDate());
        }

        offer.setPrice(resultSet.getBigDecimal("t_price"));
        offer.setCurrency(resultSet.getString("t_currency"));
        offer.setAvailableSeats(getNullableInteger(resultSet, "t_available_seats"));
        offer.setImage(resultSet.getString("t_image"));
        offer.setAgencyId(getNullableInteger(resultSet, "t_agency_id"));
        offer.setCreatedById(getNullableInteger(resultSet, "t_created_by_id"));

        Timestamp createdAt = resultSet.getTimestamp("t_created_at");
        if (createdAt != null) {
            offer.setCreatedAt(createdAt.toLocalDateTime());
        }

        offer.setApprovalStatus(resultSet.getString("t_approval_status"));
        return offer;
    }

    static Integer getNullableInteger(ResultSet resultSet, String columnLabel) throws SQLException {
        int value = resultSet.getInt(columnLabel);
        return resultSet.wasNull() ? null : value;
    }

    static void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }
}