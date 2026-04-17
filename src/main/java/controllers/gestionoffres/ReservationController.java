package controllers.gestionoffres;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import models.gestionoffres.Reservation;
import models.gestionoffres.TravelOffer;
import models.gestionutilisateurs.User;
import utils.NavigationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReservationController {

    private Runnable onConfirm;
    private Reservation editingReservation;

    @FXML
    private Label offerSummaryLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label seatsBadgeLabel;
    @FXML
    private Label seatsValueLabel;
    @FXML
    private Label totalPriceLabel;
    @FXML
    private TextField contactInfoField;
    @FXML
    private Button reserveNowButton;

    private TravelOffer activeOffer;
    private int selectedSeats = 1;
    private int maxSeats = 1;

    @FXML
    private void initialize() {
        contactInfoField.textProperty().addListener((obs, oldVal, newVal) -> updateCtaState());
        refreshSeatControls();
        updateTotalPrice();
        updateCtaState();
    }

    public void prepare(TravelOffer offer) {
        prepare(offer, null);
    }

    public void prepare(TravelOffer offer, Reservation existing) {
        this.activeOffer = offer;
        this.editingReservation = existing;

        if (offer == null || offer.getId() <= 0) {
            offerSummaryLabel.setText("Offre: -");
            priceLabel.setText("0");
            maxSeats = 1;
            selectedSeats = 1;
            contactInfoField.clear();
            refreshSeatControls();
            updateTotalPrice();
            updateCtaState();
            return;
        }

        offerSummaryLabel.setText(safe(offer.getTitle()).isBlank() ? "Offre" : safe(offer.getTitle()));
        priceLabel.setText(formatPrice(offer.getPrice()) + " " + safe(offer.getCurrency()));
        maxSeats = Math.max(1, offer.getAvailableSeats() == null ? 1 : offer.getAvailableSeats());
        selectedSeats = existing != null && existing.getReservedSeats() != null
            ? Math.min(Math.max(existing.getReservedSeats(), 1), maxSeats)
            : 1;
        contactInfoField.setText(existing != null ? safe(existing.getContactInfo()) : "");
        refreshSeatControls();
        updateTotalPrice();
        updateCtaState();
    }

    public Reservation buildReservation() {
        if (activeOffer == null || activeOffer.getId() <= 0) {
            throw new IllegalArgumentException("Offer is required.");
        }

        int seats = selectedSeats;
        if (seats < 1) {
            throw new IllegalArgumentException("Reserved seats must be at least 1.");
        }

        if (activeOffer.getAvailableSeats() != null && seats > activeOffer.getAvailableSeats()) {
            throw new IllegalArgumentException("Reserved seats exceed available seats.");
        }

        String contact = safe(contactInfoField.getText()).trim();
        if (contact.isBlank()) {
            throw new IllegalArgumentException("Contact info is required.");
        }

        Integer userId = NavigationManager.getInstance().sessionUser().map(User::getId).orElse(null);
        if (userId == null) {
            throw new IllegalArgumentException("Invalid session.");
        }

        Reservation reservation = new Reservation();
        if (editingReservation != null && editingReservation.getId() > 0) {
            reservation.setId(editingReservation.getId());
        }
        reservation.setOffer(activeOffer);
        reservation.setUserId(userId);
        reservation.setContactInfo(contact);
        reservation.setReservedSeats(seats);
        BigDecimal unitPrice = activeOffer.getPrice() == null ? BigDecimal.ZERO : activeOffer.getPrice();
        reservation.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(seats)));
        reservation.setReservationDate(editingReservation != null && editingReservation.getReservationDate() != null
            ? editingReservation.getReservationDate()
            : LocalDateTime.now());
        reservation.setStatus("PENDING");
        reservation.setPaid(false);
        return reservation;
    }

    public void setOnConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm;
    }

    @FXML
    private void onConfirmReservation() {
        if (onConfirm != null) {
            onConfirm.run();
        }
    }

    @FXML
    private void onDecreaseSeats() {
        if (selectedSeats > 1) {
            selectedSeats--;
            refreshSeatControls();
            updateTotalPrice();
            updateCtaState();
        }
    }

    @FXML
    private void onIncreaseSeats() {
        if (selectedSeats < maxSeats) {
            selectedSeats++;
            refreshSeatControls();
            updateTotalPrice();
            updateCtaState();
        }
    }

    private void refreshSeatControls() {
        seatsValueLabel.setText(String.valueOf(selectedSeats));
        seatsBadgeLabel.setText(maxSeats + " places");
    }

    private void updateTotalPrice() {
        BigDecimal price = activeOffer != null && activeOffer.getPrice() != null ? activeOffer.getPrice() : BigDecimal.ZERO;
        BigDecimal total = price.multiply(BigDecimal.valueOf(selectedSeats));
        String currency = activeOffer != null ? safe(activeOffer.getCurrency()) : "";
        totalPriceLabel.setText(formatPrice(total) + (currency.isBlank() ? "" : " " + currency));
    }

    private void updateCtaState() {
        boolean valid = activeOffer != null
            && activeOffer.getId() > 0
            && selectedSeats >= 1
            && selectedSeats <= maxSeats
            && !safe(contactInfoField.getText()).trim().isBlank()
            && NavigationManager.getInstance().sessionUser().map(User::getId).orElse(null) != null;
        reserveNowButton.setDisable(!valid);
    }

    private String formatPrice(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
