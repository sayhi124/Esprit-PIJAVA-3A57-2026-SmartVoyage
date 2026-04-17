package controllers.gestionoffres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import models.gestionoffres.Reservation;
import models.gestionoffres.TravelOffer;
import services.gestionagences.AgencyAccountService;
import services.gestionoffres.ServiceReservation;
import utils.NavigationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OfferDetailsController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_IMAGE_RESOURCE = "/images/default.png";

    private final ServiceReservation reservationService = new ServiceReservation();
    private final AgencyAccountService agencyAccountService = new AgencyAccountService();

    private Runnable onBack;
    private Runnable onReserve;
    private TravelOffer activeOffer;
    private Reservation existingReservation;

    @FXML
    private ImageView coverImageView;
    @FXML
    private Label titleLabel;
    @FXML
    private Label countriesLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label datesLabel;
    @FXML
    private Label seatsLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label statusBadgeLabel;
    @FXML
    private Label reservationHintLabel;
    @FXML
    private Button reserveButton;

    @FXML
    private void initialize() {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        coverImageView.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            clip.setWidth(newBounds.getWidth());
            clip.setHeight(newBounds.getHeight());
        });
        coverImageView.setClip(clip);

        statusBadgeLabel.setVisible(false);
        statusBadgeLabel.setManaged(false);
        reservationHintLabel.setVisible(false);
        reservationHintLabel.setManaged(false);
    }

    public void setOffer(TravelOffer offer) {
        this.activeOffer = offer;
        this.existingReservation = null;

        if (offer == null) {
            titleLabel.setText("Offer");
            countriesLabel.setText("-");
            priceLabel.setText("0");
            datesLabel.setText("-");
            seatsLabel.setText("0");
            descriptionLabel.setText("-");
            coverImageView.setImage(resolveOfferImage(null));
            reserveButton.setVisible(false);
            reserveButton.setManaged(false);
            return;
        }

        titleLabel.setText(safe(offer.getTitle(), "Offer"));
        countriesLabel.setText(formatCountries(offer.getCountries()));
        priceLabel.setText(formatPrice(offer.getPrice()) + " " + safe(offer.getCurrency(), ""));
        datesLabel.setText(formatDate(offer.getDepartureDate()) + " -> " + formatDate(offer.getReturnDate()));
        seatsLabel.setText(String.valueOf(offer.getAvailableSeats() == null ? 0 : offer.getAvailableSeats()));
        descriptionLabel.setText(safe(offer.getDescription(), "No description"));
        coverImageView.setImage(resolveOfferImage(offer.getImage()));

        refreshReservationState();
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    public void setOnReserve(Runnable onReserve) {
        this.onReserve = onReserve;
    }

    public Reservation getExistingReservation() {
        return existingReservation;
    }

    @FXML
    private void onBackClick() {
        if (onBack != null) {
            onBack.run();
        }
    }

    @FXML
    private void onReserveClick() {
        if (onReserve != null) {
            onReserve.run();
        }
    }

    private void refreshReservationState() {
        Integer userId = NavigationManager.getInstance().sessionUser().map(u -> u.getId()).orElse(null);
        boolean isAgency = false;
        try {
            isAgency = userId != null && agencyAccountService.findByResponsableId(userId).isPresent();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        if (userId == null || isAgency || activeOffer == null || activeOffer.getId() <= 0) {
            reserveButton.setVisible(false);
            reserveButton.setManaged(false);
            statusBadgeLabel.setVisible(false);
            statusBadgeLabel.setManaged(false);
            reservationHintLabel.setVisible(false);
            reservationHintLabel.setManaged(false);
            return;
        }

        reserveButton.setVisible(true);
        reserveButton.setManaged(true);

        try {
            existingReservation = reservationService.findByUserAndOffer(userId, activeOffer.getId()).orElse(null);
        } catch (SQLException ex) {
            ex.printStackTrace();
            existingReservation = null;
        }

        if (existingReservation == null) {
            reserveButton.setText("Reserve");
            statusBadgeLabel.setVisible(false);
            statusBadgeLabel.setManaged(false);
            reservationHintLabel.setVisible(false);
            reservationHintLabel.setManaged(false);
            return;
        }

        reserveButton.setText("Edit Reservation");
        String status = safe(existingReservation.getStatus(), ServiceReservation.STATUS_PENDING).toUpperCase(Locale.ROOT);
        statusBadgeLabel.setText(status);
        statusBadgeLabel.getStyleClass().removeAll(
            "reservation-status-pending",
            "reservation-status-approved",
            "reservation-status-rejected"
        );
        switch (status) {
            case ServiceReservation.STATUS_APPROVED -> statusBadgeLabel.getStyleClass().add("reservation-status-approved");
            case ServiceReservation.STATUS_REJECTED -> statusBadgeLabel.getStyleClass().add("reservation-status-rejected");
            default -> statusBadgeLabel.getStyleClass().add("reservation-status-pending");
        }
        statusBadgeLabel.setVisible(true);
        statusBadgeLabel.setManaged(true);

        boolean pending = ServiceReservation.STATUS_PENDING.equals(status);
        reservationHintLabel.setVisible(pending);
        reservationHintLabel.setManaged(pending);
        reservationHintLabel.setText("En attente de validation par l'agence");
    }

    private Image resolveOfferImage(String imagePath) {
        try {
            String raw = safe(imagePath, "").trim();
            if (!raw.isBlank()) {
                if (raw.startsWith("http://") || raw.startsWith("https://")) {
                    return new Image(raw, true);
                }

                Path path = resolveImagePath(raw);
                if (path != null) {
                    return new Image(path.toUri().toString(), true);
                }
            }
        } catch (Exception ignored) {
        }

        try {
            var fallback = getClass().getResource(DEFAULT_IMAGE_RESOURCE);
            if (fallback != null) {
                return new Image(fallback.toExternalForm(), true);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Path resolveImagePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalizedRaw = raw.trim().replace('\\', '/');
        List<Path> candidates = new ArrayList<>();

        Path direct = Paths.get(normalizedRaw);
        if (direct.isAbsolute()) {
            candidates.add(direct.normalize());
        }

        if (normalizedRaw.startsWith("/uploads/") || normalizedRaw.startsWith("uploads/")) {
            String withoutLeadingSlash = normalizedRaw.startsWith("/") ? normalizedRaw.substring(1) : normalizedRaw;
            candidates.add(Paths.get("").toAbsolutePath().resolve(withoutLeadingSlash).normalize());

            Path projectRoot = findProjectRoot(Paths.get("").toAbsolutePath());
            if (projectRoot != null) {
                candidates.add(projectRoot.resolve(withoutLeadingSlash).normalize());
            }
        }

        candidates.add(Paths.get("").toAbsolutePath().resolve(normalizedRaw).normalize());

        for (Path candidate : candidates) {
            try {
                if (candidate != null && Files.exists(candidate) && Files.isRegularFile(candidate)) {
                    return candidate;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Path findProjectRoot(Path start) {
        Path cursor = start;
        for (int i = 0; i < 8 && cursor != null; i++) {
            if (Files.exists(cursor.resolve("pom.xml"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    private String formatCountries(String json) {
        String raw = safe(json, "N/A");
        if ("N/A".equals(raw)) {
            return raw;
        }

        try {
            List<String> codes = OBJECT_MAPPER.readValue(raw, new TypeReference<List<String>>() {
            });
            List<String> result = new ArrayList<>();
            for (String code : codes) {
                String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
                if (normalized.length() != 2) {
                    continue;
                }
                String name = new Locale("", normalized).getDisplayCountry();
                if (name == null || name.isBlank()) {
                    continue;
                }
                result.add(getFlagEmoji(normalized) + " " + name);
            }
            return result.isEmpty() ? raw : String.join(", ", result);
        } catch (Exception ignored) {
            return raw;
        }
    }

    private String getFlagEmoji(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            return "";
        }
        int firstChar = Character.codePointAt(countryCode, 0) - 65 + 0x1F1E6;
        int secondChar = Character.codePointAt(countryCode, 1) - 65 + 0x1F1E6;
        return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
    }

    private String formatDate(java.time.LocalDate date) {
        if (date == null) {
            return "TBD";
        }
        return DATE_FMT.format(date);
    }

    private String formatPrice(java.math.BigDecimal price) {
        if (price == null) {
            return "0";
        }
        return price.stripTrailingZeros().toPlainString();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
