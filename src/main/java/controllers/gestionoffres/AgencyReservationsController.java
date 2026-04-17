package controllers.gestionoffres;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.gestionoffres.Reservation;
import models.gestionoffres.TravelOffer;
import services.gestionoffres.ServiceReservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AgencyReservationsController {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ServiceReservation reservationService = new ServiceReservation();

    private Integer agencyId;
    private Integer agencyUserId;
    private Runnable onBack;
    private Runnable onChanged;

    private List<Reservation> allReservations = List.of();
    private final Map<String, Integer> offerFilterMap = new LinkedHashMap<>();
    private boolean canManageReservations;

    @FXML
    private ComboBox<String> offerFilterCombo;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private VBox reservationsCardsContainer;
    @FXML
    private Label reservationsBannerLabel;

    @FXML
    private void initialize() {
        statusFilterCombo.getItems().setAll("ALL", ServiceReservation.STATUS_PENDING, ServiceReservation.STATUS_APPROVED, ServiceReservation.STATUS_REJECTED);
        statusFilterCombo.setValue("ALL");

        offerFilterCombo.setOnAction(e -> renderFiltered());
        statusFilterCombo.setOnAction(e -> renderFiltered());

        clearBanner();
    }

    public void prepare(Integer agencyId, Integer agencyUserId, Runnable onBack, Runnable onChanged) {
        this.agencyId = agencyId;
        this.agencyUserId = agencyUserId;
        this.onBack = onBack;
        this.onChanged = onChanged;
        this.canManageReservations = agencyId != null && agencyId > 0 && agencyUserId != null && agencyUserId > 0;
        reload();
    }

    @FXML
    private void onBackClick() {
        if (onBack != null) {
            onBack.run();
        }
    }

    @FXML
    private void onRefresh() {
        reload();
    }

    private void reload() {
        if (agencyId == null || agencyId <= 0) {
            allReservations = List.of();
            updateOfferFilters();
            renderFiltered();
            showBanner("Agency context not found.", true);
            return;
        }
        try {
            allReservations = reservationService.getAgencyReservations(agencyId);
            updateOfferFilters();
            renderFiltered();
            clearBanner();
        } catch (Exception ex) {
            ex.printStackTrace();
            allReservations = List.of();
            updateOfferFilters();
            renderFiltered();
            showBanner(ex.getMessage(), true);
        }
    }

    private void updateOfferFilters() {
        offerFilterMap.clear();
        offerFilterMap.put("ALL", null);

        for (Reservation reservation : allReservations) {
            TravelOffer offer = reservation.getOffer();
            if (offer == null || offer.getId() <= 0) {
                continue;
            }
            String key = offer.getId() + " - " + safe(offer.getTitle(), "Offer");
            offerFilterMap.putIfAbsent(key, offer.getId());
        }

        offerFilterCombo.getItems().setAll(offerFilterMap.keySet());
        if (!offerFilterCombo.getItems().contains(offerFilterCombo.getValue())) {
            offerFilterCombo.setValue("ALL");
        }
    }

    private void renderFiltered() {
        reservationsCardsContainer.getChildren().clear();

        List<Reservation> filtered = new ArrayList<>(allReservations);

        Integer selectedOfferId = offerFilterMap.getOrDefault(offerFilterCombo.getValue(), null);
        if (selectedOfferId != null) {
            filtered = filtered.stream()
                .filter(r -> r.getOffer() != null && r.getOffer().getId() == selectedOfferId)
                .toList();
        }

        String selectedStatus = safe(statusFilterCombo.getValue(), "ALL").toUpperCase(Locale.ROOT);
        if (!"ALL".equals(selectedStatus)) {
            filtered = filtered.stream()
                .filter(r -> selectedStatus.equals(safe(r.getStatus(), "").toUpperCase(Locale.ROOT)))
                .toList();
        }

        if (filtered.isEmpty()) {
            Label empty = new Label("No reservation requests found.");
            empty.getStyleClass().add("event-card-meta");
            reservationsCardsContainer.getChildren().add(empty);
            return;
        }

        for (Reservation reservation : filtered) {
            reservationsCardsContainer.getChildren().add(buildReservationCard(reservation));
        }
    }

    private VBox buildReservationCard(Reservation reservation) {
        VBox card = new VBox(8);
        card.getStyleClass().add("event-card");

        TravelOffer offer = reservation.getOffer();
        String offerTitle = offer == null ? "Offer" : safe(offer.getTitle(), "Offer");

        Label title = new Label(offerTitle);
        title.getStyleClass().add("event-card-title");

        Label user = new Label("User: " + safe(reservation.getRequesterName(), "User #" + safe(Integer.toString(reservation.getUserId() == null ? 0 : reservation.getUserId()), "-")));
        user.getStyleClass().add("event-card-meta");

        Label contact = new Label("Contact: " + safe(reservation.getContactInfo(), "-"));
        contact.getStyleClass().add("event-card-meta");

        Label seats = new Label("Seats: " + safe(Integer.toString(reservation.getReservedSeats() == null ? 0 : reservation.getReservedSeats()), "0"));
        seats.getStyleClass().add("event-card-meta");

        Label total = new Label("Total: " + formatPrice(reservation.getTotalPrice()) + " " + (offer == null ? "" : safe(offer.getCurrency(), "")));
        total.getStyleClass().add("event-card-meta");

        Label date = new Label("Date: " + formatDateTime(reservation.getReservationDate()));
        date.getStyleClass().add("event-card-meta");

        Label status = new Label(safe(reservation.getStatus(), ServiceReservation.STATUS_PENDING));
        status.getStyleClass().addAll("reservation-status-badge", statusClass(reservation.getStatus()));

        HBox actions = new HBox(8);
        actions.setFillHeight(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ButtonType approveType = new ButtonType("Approve");
        ButtonType rejectType = new ButtonType("Reject");

        javafx.scene.control.Button approve = new javafx.scene.control.Button("Approve");
        approve.getStyleClass().add("event-action-primary");

        javafx.scene.control.Button reject = new javafx.scene.control.Button("Reject");
        reject.getStyleClass().add("event-action-danger");

        boolean pending = ServiceReservation.STATUS_PENDING.equalsIgnoreCase(safe(reservation.getStatus(), ""));
        boolean canShowActions = canManageReservations;
        approve.setDisable(!pending || !canShowActions);
        reject.setDisable(!pending || !canShowActions);
        approve.setVisible(canShowActions);
        approve.setManaged(canShowActions);
        reject.setVisible(canShowActions);
        reject.setManaged(canShowActions);

        approve.setOnAction(e -> {
            if (confirmAction("Approve reservation", "Approve this reservation request?", approveType)) {
                handleStatusChange(reservation.getId(), true);
            }
        });

        reject.setOnAction(e -> {
            if (confirmAction("Reject reservation", "Reject this reservation request?", rejectType)) {
                handleStatusChange(reservation.getId(), false);
            }
        });

        actions.getChildren().addAll(status, spacer, approve, reject);

        card.getChildren().addAll(title, user, contact, seats, total, date, actions);
        return card;
    }

    private boolean confirmAction(String title, String content, ButtonType actionType) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, actionType, ButtonType.CANCEL);
        alert.setHeaderText(title);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == actionType;
    }

    private void handleStatusChange(int reservationId, boolean approve) {
        if (agencyUserId == null || agencyUserId <= 0) {
            showBanner("Agency user session missing.", true);
            return;
        }
        try {
            boolean done = approve
                ? reservationService.approveReservation(agencyUserId, reservationId)
                : reservationService.rejectReservation(agencyUserId, reservationId);
            if (done) {
                showBanner(approve ? "Reservation approved." : "Reservation rejected.", false);
                reload();
                if (onChanged != null) {
                    onChanged.run();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showBanner(ex.getMessage(), true);
        }
    }

    private void showBanner(String message, boolean error) {
        reservationsBannerLabel.setText(safe(message, ""));
        reservationsBannerLabel.getStyleClass().removeAll("status-error", "status-success");
        reservationsBannerLabel.getStyleClass().add(error ? "status-error" : "status-success");
        reservationsBannerLabel.setVisible(true);
        reservationsBannerLabel.setManaged(true);
    }

    private void clearBanner() {
        reservationsBannerLabel.setText("");
        reservationsBannerLabel.getStyleClass().removeAll("status-error", "status-success");
        reservationsBannerLabel.setVisible(false);
        reservationsBannerLabel.setManaged(false);
    }

    private String statusClass(String status) {
        String normalized = safe(status, "").toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case ServiceReservation.STATUS_APPROVED -> "reservation-status-approved";
            case ServiceReservation.STATUS_REJECTED -> "reservation-status-rejected";
            default -> "reservation-status-pending";
        };
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "0";
        }
        return price.stripTrailingZeros().toPlainString();
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "-";
        }
        return DATETIME_FMT.format(value);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
