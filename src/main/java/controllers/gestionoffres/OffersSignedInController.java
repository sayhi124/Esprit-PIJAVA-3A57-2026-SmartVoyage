package controllers.gestionoffres;

import atlantafx.base.theme.PrimerDark;
import controllers.messaging.MessagesController;
import controllers.notifications.NotificationsController;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import models.gestionoffres.Reservation;
import models.gestionoffres.TravelOffer;
import models.gestionutilisateurs.User;
import services.gestionagences.AgencyAccountService;
import services.gestionoffres.ServiceReservation;
import services.gestionoffres.ServiceTravelOffer;
import services.notifications.NotificationService;
import utils.NavigationManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class OffersSignedInController {

    private enum ActiveEmbeddedView {
        LIST,
        OFFER_FORM,
        RESERVATION_FORM,
        OFFER_DETAILS,
        AGENCY_RESERVATIONS,
        MESSAGES,
        NOTIFICATIONS
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final double OFFER_CARD_WIDTH = 290;
    private static final String DEFAULT_IMAGE_RESOURCE = "/images/default.png";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @FXML
    private Label userGreetingLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Button createOfferButton;
    @FXML
    private Button viewReservationRequestsButton;
    @FXML
    private Button messagesCenterButton;
    @FXML
    private Button notificationsCenterButton;
    @FXML
    private Label notificationsUnreadBadge;
    @FXML
    private Button offersTabButton;
    @FXML
    private Button myReservationsTabButton;
    @FXML
    private Label offersStatusLabel;
    @FXML
    private TextField offerSearchField;
    @FXML
    private TilePane offersTile;
    @FXML
    private VBox offersListView;
    @FXML
    private VBox offersContainer;
    @FXML
    private VBox reservationsContainer;
    @FXML
    private VBox offersFormView;
    @FXML
    private StackPane embeddedFormHost;
    @FXML
    private Label userReservationsSectionLabel;
    @FXML
    private VBox userReservationsContainer;
    @FXML
    private Label formTitleLabel;
    @FXML
    private Label formSubtitleLabel;
    @FXML
    private Label formStatusLabel;

    private final ServiceTravelOffer offerService = new ServiceTravelOffer();
    private final ServiceReservation reservationService = new ServiceReservation();
    private final AgencyAccountService agencyAccountService = new AgencyAccountService();
    private final NotificationService notificationService = new NotificationService();

    private List<TravelOffer> offers = new ArrayList<>();
    private List<Reservation> userReservations = new ArrayList<>();
    private OfferFormController activeOfferFormController;
    private ReservationController activeReservationController;
    private OfferDetailsController activeOfferDetailsController;
    private AgencyReservationsController activeAgencyReservationsController;
    private MessagesController activeMessagesController;
    private NotificationsController activeNotificationsController;
    private TravelOffer activeOfferTarget;
    private ActiveEmbeddedView activeView = ActiveEmbeddedView.LIST;
    private boolean canCreateOffers;
    private Integer currentUserId;
    private Integer currentAgencyId;
    private boolean reservationsViewActive;
    private final Map<Integer, Integer> offerAgencyReceiverCache = new HashMap<>();

    @FXML
    private void initialize() {
        NavigationManager nav = NavigationManager.getInstance();
        if (!nav.canAccessSignedInShell()) {
            nav.showLogin();
            return;
        }
        installLibraryTheme();

        User currentUser = nav.sessionUser().orElse(null);
        if (currentUser != null) {
            currentUserId = currentUser.getId();
            String displayName = currentUser.getUsername() != null && !currentUser.getUsername().isBlank()
                    ? currentUser.getUsername()
                    : currentUser.getEmail();
            userGreetingLabel.setText("Welcome, " + displayName);
            roleLabel.setText(nav.canAccessAgencyAdminFeatures() ? "Agency admin" : "Utilisateur");

            try {
                var agency = currentUser.getId() != null
                        ? agencyAccountService.findByResponsableId(currentUser.getId())
                        : java.util.Optional.<models.gestionagences.AgencyAccount>empty();
                if (agency.isPresent() && agency.get().getId() != null) {
                    currentAgencyId = agency.get().getId().intValue();
                    canCreateOffers = true;
                } else {
                    currentAgencyId = null;
                    canCreateOffers = false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                currentAgencyId = null;
                canCreateOffers = false;
            }
            applyCreateOfferButtonPermissions();
        } else {
            currentUserId = null;
            currentAgencyId = null;
            canCreateOffers = false;
            applyCreateOfferButtonPermissions();
        }

        reloadOffers();
        refreshNotificationsBadge();
        switchToListView(false);
    }

    private void installLibraryTheme() {
        String current = Application.getUserAgentStylesheet();
        String primer = new PrimerDark().getUserAgentStylesheet();
        if (current == null || current.isBlank() || !current.equals(primer)) {
            Application.setUserAgentStylesheet(primer);
        }
    }

    private void reloadOffers() {
        try {
            offers = offerService.getVisibleToUser(currentUserId);
            offers.sort(Comparator.comparingInt(TravelOffer::getId).reversed());
            reloadReservations();
            renderOffers(offers);
            renderReservationSections();
            refreshNotificationsBadge();
        } catch (SQLException e) {
            showError("Chargement des offres", e.getMessage());
        }
    }

    private void reloadReservations() throws SQLException {
        userReservations = currentUserId == null ? List.of() : reservationService.getUserReservations(currentUserId);
    }

    private void renderOffers(List<TravelOffer> source) {
        offersTile.getChildren().clear();
        for (TravelOffer offer : source) {
            offersTile.getChildren().add(buildOfferCard(offer));
        }
    }

    private VBox buildOfferCard(TravelOffer offer) {
        VBox card = new VBox(10);
        card.getStyleClass().add("event-card");
        card.getStyleClass().add("offer-card-clickable");
        card.setPrefWidth(OFFER_CARD_WIDTH);
        card.setMinWidth(OFFER_CARD_WIDTH);
        card.setMaxWidth(OFFER_CARD_WIDTH);
        card.setOnMouseClicked(e -> showOfferDetails(offer));

        ImageView coverImage = new ImageView();
        coverImage.setFitWidth(OFFER_CARD_WIDTH);
        coverImage.setFitHeight(150);
        coverImage.setPreserveRatio(false);
        coverImage.setSmooth(true);
        coverImage.getStyleClass().add("offer-card-cover-image");
        coverImage.setImage(resolveOfferImage(offer.getImage()));

        Rectangle clip = new Rectangle(OFFER_CARD_WIDTH, 150);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        coverImage.setClip(clip);

        Region gradientOverlay = new Region();
        gradientOverlay.getStyleClass().add("offer-card-image-overlay");
        gradientOverlay.setMouseTransparent(true);

        Label overlayTitle = new Label(safe(offer.getTitle(), "Offer"));
        overlayTitle.getStyleClass().add("offer-card-overlay-title");
        overlayTitle.setWrapText(true);
        overlayTitle.setMaxWidth(OFFER_CARD_WIDTH - 28);
        overlayTitle.setMouseTransparent(true);

        Label statusBadge = new Label(safe(offer.getApprovalStatus(), "pending").toUpperCase(Locale.ROOT));
        statusBadge.getStyleClass().addAll("offer-status-badge", offerStatusClass(offer.getApprovalStatus()));
        statusBadge.setMouseTransparent(true);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topRow = new HBox(8, topSpacer, statusBadge);

        Region middleSpacer = new Region();
        VBox.setVgrow(middleSpacer, Priority.ALWAYS);
        VBox overlayContent = new VBox(8, topRow, middleSpacer, overlayTitle);
        overlayContent.getStyleClass().add("offer-card-overlay-content");
        overlayContent.setMouseTransparent(true);

        StackPane coverWrap = new StackPane(coverImage, gradientOverlay, overlayContent);
        coverWrap.getStyleClass().add("offer-card-cover-wrap");
        coverWrap.setPrefSize(OFFER_CARD_WIDTH, 150);
        coverWrap.setMinSize(OFFER_CARD_WIDTH, 150);
        coverWrap.setMaxSize(OFFER_CARD_WIDTH, 150);
        coverWrap.setMaxWidth(Double.MAX_VALUE);

        VBox body = new VBox(8);
        body.getStyleClass().add("offer-card-body");

        Label countries = new Label("Countries: " + formatCountriesCompact(offer.getCountries(), 2));
        countries.getStyleClass().add("offer-card-countries");

        Label price = new Label("Price: " + formatPrice(offer.getPrice()) + " " + safe(offer.getCurrency(), ""));
        price.getStyleClass().add("offer-card-price");

        Label dates = new Label("Dates: " + formatDate(offer.getDepartureDate()) + " -> " + formatDate(offer.getReturnDate()));
        dates.getStyleClass().add("offer-card-dates");

        Label seats = new Label("Available seats: " + (offer.getAvailableSeats() != null ? offer.getAvailableSeats() : 0));
        seats.getStyleClass().add("event-card-meta");

        Label desc = new Label(safe(offer.getDescription(), "No description"));
        desc.getStyleClass().add("event-card-description");
        desc.setWrapText(true);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("event-card-actions");

        Region actionsSpacer = new Region();
        HBox.setHgrow(actionsSpacer, Priority.ALWAYS);

        Reservation existingReservation = findUserReservationForOffer(offer.getId());

        Button reserveBtn = new Button("Reserve");
        reserveBtn.getStyleClass().add("event-action-primary");
        reserveBtn.setOnAction(e -> {
            e.consume();
            onReserveOffer(offer, existingReservation);
        });

        Integer availableSeats = offer.getAvailableSeats();
        boolean soldOut = availableSeats != null && availableSeats <= 0;

        if (currentAgencyId == null && soldOut) {
            reserveBtn.setText("Sold out");
            reserveBtn.setDisable(true);
            reserveBtn.getStyleClass().remove("event-action-primary");
            reserveBtn.getStyleClass().add("event-action-danger");
        } else if (currentAgencyId == null && existingReservation != null) {
            reserveBtn.setText("Edit reservation");
            reserveBtn.getStyleClass().remove("event-action-primary");
            reserveBtn.getStyleClass().add("event-action-secondary");
        }

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("event-action-secondary");
        editBtn.setOnAction(e -> {
            e.consume();
            onEditOffer(offer);
        });
        boolean canEditOffer = currentAgencyId != null
            && offer.getAgencyId() != null
            && currentAgencyId.intValue() == offer.getAgencyId().intValue();
        editBtn.setVisible(canEditOffer);
        editBtn.setManaged(canEditOffer);

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("event-action-danger");
        deleteBtn.setOnAction(e -> {
            e.consume();
            onDeleteOffer(offer);
        });
        deleteBtn.setVisible(canEditOffer);
        deleteBtn.setManaged(canEditOffer);

        if (currentAgencyId == null) {
            Integer receiverId = resolveOfferReceiverId(offer);
            if (receiverId != null && receiverId > 0 && (currentUserId == null || receiverId.intValue() != currentUserId.intValue())) {
                Button contactBtn = new Button("Contact Agency");
                contactBtn.getStyleClass().add("event-action-secondary");
                contactBtn.setOnAction(e -> {
                    e.consume();
                    try {
                        NavigationManager.getInstance().showMessagesWithReceiver(receiverId);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                actions.getChildren().add(contactBtn);
            }
            actions.getChildren().add(reserveBtn);
        }
        actions.getChildren().addAll(actionsSpacer, editBtn, deleteBtn);

        Label reservationState = null;
        if (existingReservation != null) {
            reservationState = new Label("Reservation: " + safe(existingReservation.getStatus(), ServiceReservation.STATUS_PENDING));
            reservationState.getStyleClass().addAll("event-card-meta", statusClass(existingReservation.getStatus()));
        }

        body.getChildren().addAll(countries, price, dates, seats);
        if (reservationState != null) {
            body.getChildren().add(reservationState);
        }
        body.getChildren().add(desc);
        card.getChildren().addAll(coverWrap, body, actions);
        VBox.setMargin(actions, new Insets(2, 12, 12, 12));
        return card;
    }

    private Reservation findUserReservationForOffer(Integer offerId) {
        if (offerId == null || offerId <= 0 || userReservations == null || userReservations.isEmpty()) {
            return null;
        }
        return userReservations.stream()
            .filter(r -> r.getOffer() != null && r.getOffer().getId() == offerId)
            .findFirst()
            .orElse(null);
    }

    private Integer resolveOfferReceiverId(TravelOffer offer) {
        if (offer == null) {
            return null;
        }
        if (offer.getCreatedById() != null && offer.getCreatedById() > 0) {
            return offer.getCreatedById();
        }
        Integer agencyId = offer.getAgencyId();
        if (agencyId == null || agencyId <= 0) {
            return null;
        }
        if (offerAgencyReceiverCache.containsKey(agencyId)) {
            return offerAgencyReceiverCache.get(agencyId);
        }
        try {
            Integer responsibleId = agencyAccountService.get(agencyId.longValue())
                .map(models.gestionagences.AgencyAccount::getResponsableId)
                .orElse(null);
            offerAgencyReceiverCache.put(agencyId, responsibleId);
            return responsibleId;
        } catch (SQLException ex) {
            return null;
        }
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

    @FXML
    private void onSearchOffers() {
        String query = offerSearchField.getText() == null ? "" : offerSearchField.getText().trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) {
            renderOffers(offers);
            return;
        }
        List<TravelOffer> filtered = offers.stream().filter(o ->
                safe(o.getTitle(), "").toLowerCase(Locale.ROOT).contains(query)
            || formatCountries(o.getCountries()).toLowerCase(Locale.ROOT).contains(query)
                        || safe(o.getDescription(), "").toLowerCase(Locale.ROOT).contains(query)
        ).toList();
        renderOffers(filtered);
    }

    @FXML
    private void onCreateOffer() {
        if (!canCreateOffers) {
            showStatus(offersStatusLabel, "Only agencies can create offers", true);
            return;
        }
        showOfferFormEmbedded(null);
    }

    @FXML
    private void onViewReservationRequests() {
        if (currentAgencyId == null || currentUserId == null) {
            showStatus(offersStatusLabel, "Only agencies can access reservation requests.", true);
            return;
        }
        showAgencyReservationsView();
    }

    @FXML
    private void onMessagesCenter() {
        showMessagesView();
    }

    @FXML
    private void onNotificationsCenter() {
        showNotificationsView();
    }

    private void onEditOffer(TravelOffer offer) {
        showOfferFormEmbedded(offer);
    }

    private void onDeleteOffer(TravelOffer offer) {
        try {
            offerService.delete(offer.getId());
            reloadOffers();
            showStatus(offersStatusLabel, "Offer deleted.", false);
        } catch (SQLException e) {
            showError("Suppression", e.getMessage());
        }
    }

    private void onReserveOffer(TravelOffer offer, Reservation existingReservation) {
        if (currentAgencyId != null) {
            showStatus(offersStatusLabel, "Agencies cannot create reservations.", true);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/offres/reservation_form.fxml"));
            Node formContent = loader.load();
            activeReservationController = loader.getController();
            activeOfferFormController = null;
            activeOfferDetailsController = null;
            activeAgencyReservationsController = null;
            stopMessagesAutoRefresh();
            activeMessagesController = null;
            activeNotificationsController = null;
            activeOfferTarget = offer;

            activeReservationController.prepare(offer, existingReservation);
            activeReservationController.setOnConfirm(this::onInlineSubmit);
            embeddedFormHost.getChildren().setAll(formContent);
            StackPane.setAlignment(formContent, Pos.TOP_CENTER);

            formTitleLabel.setText(existingReservation == null ? "Create Reservation" : "Update Reservation");
            formSubtitleLabel.setText(safe(offer.getTitle(), "Offer") + " - reservation form");

            clearStatus(formStatusLabel);
            switchToFormView(ActiveEmbeddedView.RESERVATION_FORM);
        } catch (IOException e) {
            showError("Reservation", "Unable to load reservation form: " + e.getMessage());
        }
    }

    private void showOfferFormEmbedded(TravelOffer source) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/offres/offer_form.fxml"));
            Node formContent = loader.load();
            activeOfferFormController = loader.getController();
            activeReservationController = null;
            activeOfferDetailsController = null;
            activeAgencyReservationsController = null;
            activeOfferTarget = source;

            activeOfferFormController.setOffer(source);
            activeOfferFormController.setOnSave(this::onInlineSubmit);
            activeOfferFormController.setOnCancel(this::onInlineCancel);
            embeddedFormHost.getChildren().setAll(formContent);
            StackPane.setAlignment(formContent, Pos.TOP_CENTER);

            formTitleLabel.setText(source == null ? "Create Offer" : "Edit Offer");
            formSubtitleLabel.setText(source == null ? "Add a new travel offer" : "Update offer details");
            activeOfferFormController.setEditMode(source != null);

            clearStatus(formStatusLabel);
            switchToFormView(ActiveEmbeddedView.OFFER_FORM);
        } catch (IOException e) {
            showError("Form", "Unable to load offer form: " + e.getMessage());
        }
    }

    private void showAgencyReservationsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/offres/agency_reservations_view.fxml"));
            Node viewContent = loader.load();
            activeAgencyReservationsController = loader.getController();
            activeOfferFormController = null;
            activeReservationController = null;
            activeOfferDetailsController = null;
            stopMessagesAutoRefresh();
            activeMessagesController = null;
            activeNotificationsController = null;
            activeOfferTarget = null;

            activeAgencyReservationsController.prepare(
                currentAgencyId,
                currentUserId,
                this::onInlineCancel,
                this::reloadOffers
            );

            embeddedFormHost.getChildren().setAll(viewContent);
            StackPane.setAlignment(viewContent, Pos.TOP_CENTER);

            formTitleLabel.setText("Reservation Requests");
            formSubtitleLabel.setText("Manage pending reservations for your offers");
            clearStatus(formStatusLabel);
            switchToFormView(ActiveEmbeddedView.AGENCY_RESERVATIONS);
        } catch (IOException e) {
            showError("Reservations", "Unable to load agency reservations view: " + e.getMessage());
        }
    }

    private void showMessagesView() {
        if (currentUserId == null || currentUserId <= 0) {
            showStatus(offersStatusLabel, "Invalid session.", true);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/messaging/messages.fxml"));
            Node viewContent = loader.load();
            MessagesController controller = loader.getController();

            activeOfferFormController = null;
            activeReservationController = null;
            activeOfferDetailsController = null;
            activeAgencyReservationsController = null;
            stopMessagesAutoRefresh();
            activeMessagesController = controller;
            activeNotificationsController = null;
            activeOfferTarget = null;

            controller.prepare(currentUserId, this::refreshUnreadBadges);

            embeddedFormHost.getChildren().setAll(viewContent);
            StackPane.setAlignment(viewContent, Pos.TOP_CENTER);

            formTitleLabel.setText("Messages");
            formSubtitleLabel.setText("Conversations with users and agencies");
            clearStatus(formStatusLabel);
            switchToFormView(ActiveEmbeddedView.MESSAGES);
        } catch (IOException e) {
            showError("Messages", "Unable to load messages view: " + e.getMessage());
        }
    }

    private void showNotificationsView() {
        if (currentUserId == null || currentUserId <= 0) {
            showStatus(offersStatusLabel, "Invalid session.", true);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/notifications/notifications.fxml"));
            Node viewContent = loader.load();
            NotificationsController controller = loader.getController();

            activeOfferFormController = null;
            activeReservationController = null;
            activeOfferDetailsController = null;
            activeAgencyReservationsController = null;
            stopMessagesAutoRefresh();
            activeMessagesController = null;
            activeNotificationsController = controller;
            activeOfferTarget = null;

            controller.prepare(currentUserId, this::refreshUnreadBadges);

            embeddedFormHost.getChildren().setAll(viewContent);
            StackPane.setAlignment(viewContent, Pos.TOP_CENTER);

            formTitleLabel.setText("Notifications");
            formSubtitleLabel.setText("Unread updates and activity feed");
            clearStatus(formStatusLabel);
            switchToFormView(ActiveEmbeddedView.NOTIFICATIONS);
        } catch (IOException e) {
            showError("Notifications", "Unable to load notifications view: " + e.getMessage());
        }
    }

    private void showOfferDetails(TravelOffer offer) {
        if (offer == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/offres/offer_details.fxml"));
            Node detailsContent = loader.load();
            activeOfferDetailsController = loader.getController();
            activeOfferFormController = null;
            activeReservationController = null;
            activeAgencyReservationsController = null;
            stopMessagesAutoRefresh();
            activeMessagesController = null;
            activeNotificationsController = null;
            activeOfferTarget = offer;

            activeOfferDetailsController.setOffer(offer);
            activeOfferDetailsController.setOnBack(this::onInlineCancel);
            activeOfferDetailsController.setOnReserve(() -> {
                Reservation existing = activeOfferDetailsController.getExistingReservation();
                onReserveOffer(offer, existing);
            });

            embeddedFormHost.getChildren().setAll(detailsContent);
            StackPane.setAlignment(detailsContent, Pos.TOP_CENTER);

            formTitleLabel.setText("Offer Details");
            formSubtitleLabel.setText(safe(offer.getTitle(), "Offer") + " - details");

            clearStatus(formStatusLabel);
            switchToFormView(ActiveEmbeddedView.OFFER_DETAILS);
        } catch (IOException e) {
            showError("Offer details", "Unable to load details view: " + e.getMessage());
        }
    }

    @FXML
    private void onInlineSubmit() {
        if (activeView == ActiveEmbeddedView.OFFER_FORM && activeOfferFormController != null) {
            try {
                TravelOffer offer = activeOfferFormController.buildOffer(activeOfferTarget);
                if (activeOfferTarget == null) {
                    if (offer.getApprovalStatus() == null || offer.getApprovalStatus().isBlank()) {
                        offer.setApprovalStatus("pending");
                    }
                    offerService.add(offer);
                    reloadOffers();
                    switchToListView(true);
                    showStatus(offersStatusLabel, "Offer created successfully.", false);
                } else {
                    offerService.update(offer);
                    reloadOffers();
                    switchToListView(true);
                    showStatus(offersStatusLabel, "Offer updated successfully.", false);
                }
            } catch (Exception ex) {
                showStatus(formStatusLabel, ex.getMessage(), true);
            }
            return;
        }

        if (activeView == ActiveEmbeddedView.RESERVATION_FORM && activeReservationController != null) {
            try {
                Reservation reservation = activeReservationController.buildReservation();
                if (reservation.getId() > 0) {
                    reservationService.updateReservation(reservation.getUserId(), reservation.getId(), reservation);
                    switchToListView(true);
                    reloadOffers();
                    showStatus(offersStatusLabel, "Reservation mise a jour", false);
                } else {
                    reservationService.createReservation(reservation.getUserId(), reservation.getOffer(), reservation);
                    switchToListView(true);
                    reloadOffers();
                    showStatus(offersStatusLabel, "Reservation envoyee", false);
                }

            } catch (Exception ex) {
                showStatus(formStatusLabel, ex.getMessage(), true);
            }
        }
    }

    private void renderReservationSections() {
        renderUserReservations();
    }

    private void renderUserReservations() {
        userReservationsContainer.getChildren().clear();
        if (userReservations == null || userReservations.isEmpty()) {
            Label empty = new Label("Aucune reservation pour le moment.");
            empty.getStyleClass().add("event-card-meta");
            userReservationsContainer.getChildren().add(empty);
            return;
        }

        for (Reservation reservation : userReservations) {
            userReservationsContainer.getChildren().add(buildUserReservationCard(reservation));
        }
    }

    private VBox buildUserReservationCard(Reservation reservation) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("event-card", "user-reservation-card");

        TravelOffer offer = reservation.getOffer();
        Label title = new Label(offer == null ? "Offer" : safe(offer.getTitle(), "Offer"));
        title.getStyleClass().add("event-card-title");

        Label seats = new Label("Seats: " + (reservation.getReservedSeats() == null ? 0 : reservation.getReservedSeats()));
        seats.getStyleClass().add("event-card-meta");

        Label contact = new Label("Contact: " + safe(reservation.getContactInfo(), "-"));
        contact.getStyleClass().add("event-card-meta");

        Label price = new Label("Unit: " + (offer == null ? "0" : formatPrice(offer.getPrice())) + " " + (offer == null ? "" : safe(offer.getCurrency(), "")));
        price.getStyleClass().add("event-card-meta");

        Label total = new Label("Total: " + formatPrice(reservation.getTotalPrice()) + " " + (offer == null ? "" : safe(offer.getCurrency(), "")));
        total.getStyleClass().add("event-card-meta");

        Label status = new Label(safe(reservation.getStatus(), ServiceReservation.STATUS_PENDING));
        status.getStyleClass().addAll("reservation-status-badge", statusClass(reservation.getStatus()));

        Label waiting = new Label("En attente de validation par l'agence");
        waiting.getStyleClass().add("event-card-meta");
        waiting.setVisible(ServiceReservation.STATUS_PENDING.equalsIgnoreCase(safe(reservation.getStatus(), "")));
        waiting.setManaged(waiting.isVisible());

        HBox actions = new HBox(8);
        Button viewOffer = new Button("Voir l'offre");
        viewOffer.getStyleClass().add("event-action-secondary");
        viewOffer.setOnAction(e -> {
            if (offer != null) {
                showOfferDetails(offer);
            }
        });

        Button edit = new Button("Modifier");
        edit.getStyleClass().add("event-action-primary");
        boolean canEdit = !ServiceReservation.STATUS_APPROVED.equalsIgnoreCase(safe(reservation.getStatus(), ""));
        edit.setDisable(!canEdit);
        edit.setOnAction(e -> {
            if (offer != null) {
                onReserveOffer(offer, reservation);
            }
        });

        actions.getChildren().addAll(viewOffer, edit);
        card.getChildren().addAll(title, seats, contact, price, total, status, waiting, actions);
        return card;
    }

    private String statusClass(String status) {
        String normalized = safe(status, "").toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case ServiceReservation.STATUS_APPROVED -> "reservation-status-approved";
            case ServiceReservation.STATUS_REJECTED -> "reservation-status-rejected";
            default -> "reservation-status-pending";
        };
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "-";
        }
        return DATETIME_FMT.format(value);
    }

    private void scrollToOffer(TravelOffer offer) {
        if (offer == null) {
            return;
        }
        showOfferDetails(offer);
    }

    @FXML
    public void showOffersView() {
        reservationsViewActive = false;
        applyListToggleView(false, true);
    }

    @FXML
    public void showReservationsView() {
        reservationsViewActive = true;
        loadUserReservations();
        applyListToggleView(true, true);
    }

    private void loadUserReservations() {
        try {
            userReservations = currentUserId == null ? List.of() : reservationService.getUserReservations(currentUserId);
            renderUserReservations();
        } catch (SQLException e) {
            showStatus(offersStatusLabel, "Unable to load reservations: " + e.getMessage(), true);
        }
    }

    private void applyListToggleView(boolean showReservations, boolean animate) {
        offersContainer.setVisible(!showReservations);
        offersContainer.setManaged(!showReservations);

        reservationsContainer.setVisible(showReservations);
        reservationsContainer.setManaged(showReservations);

        updateToggleButtons(showReservations);

        if (animate) {
            Node target = showReservations ? reservationsContainer : offersContainer;
            FadeTransition fade = new FadeTransition(Duration.millis(260), target);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }
    }

    private void updateToggleButtons(boolean reservationsActive) {
        offersTabButton.getStyleClass().remove("active-tab");
        myReservationsTabButton.getStyleClass().remove("active-tab");
        if (reservationsActive) {
            myReservationsTabButton.getStyleClass().add("active-tab");
        } else {
            offersTabButton.getStyleClass().add("active-tab");
        }
    }

    @FXML
    private void onInlineCancel() {
        switchToListView(true);
    }

    private void switchToFormView(ActiveEmbeddedView view) {
        activeView = view;
        clearStatus(offersStatusLabel);

        createOfferButton.setDisable(true);
        createOfferButton.setVisible(false);
        createOfferButton.setManaged(false);

        viewReservationRequestsButton.setDisable(true);
        viewReservationRequestsButton.setVisible(false);
        viewReservationRequestsButton.setManaged(false);

        messagesCenterButton.setDisable(true);
        messagesCenterButton.setVisible(false);
        messagesCenterButton.setManaged(false);

        notificationsCenterButton.setDisable(true);
        notificationsCenterButton.setVisible(false);
        notificationsCenterButton.setManaged(false);

        notificationsUnreadBadge.setVisible(false);
        notificationsUnreadBadge.setManaged(false);

        offersListView.setVisible(false);
        offersListView.setManaged(false);
        offersFormView.setVisible(true);
        offersFormView.setManaged(true);
        animateEntry(offersFormView);
    }

    private void switchToListView(boolean animate) {
        activeView = ActiveEmbeddedView.LIST;
        activeOfferFormController = null;
        activeReservationController = null;
        activeOfferDetailsController = null;
        activeAgencyReservationsController = null;
        stopMessagesAutoRefresh();
        activeMessagesController = null;
        activeNotificationsController = null;
        activeOfferTarget = null;

        applyCreateOfferButtonPermissions();

        clearStatus(formStatusLabel);
        embeddedFormHost.getChildren().clear();

        offersFormView.setVisible(false);
        offersFormView.setManaged(false);
        offersListView.setVisible(true);
        offersListView.setManaged(true);

        applyListToggleView(reservationsViewActive, false);

        if (animate) {
            animateEntry(offersListView);
        }
    }

    private void animateEntry(Node node) {
        node.setOpacity(0);
        node.setTranslateY(10);

        FadeTransition fade = new FadeTransition(Duration.millis(180), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(180), node);
        slide.setFromY(10);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    private void clearStatus(Label statusLabel) {
        statusLabel.setText("");
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    private void showStatus(Label statusLabel, String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add(error ? "status-error" : "status-success");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "TBD";
        }
        return DATE_FMT.format(date);
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "0";
        }
        return price.stripTrailingZeros().toPlainString();
    }

    private String formatCountries(String json) {
        List<String> codes = parseCountryCodes(json);
        if (codes.isEmpty()) {
            return "N/A";
        }

        String formatted = codes.stream()
            .map(code -> code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
            .filter(code -> code.length() == 2)
            .map(code -> {
                String country = new Locale("", code).getDisplayCountry();
                if (country == null || country.isBlank()) {
                    return "";
                }
                return getFlagEmoji(code) + " " + country;
            })
            .filter(value -> value != null && !value.isBlank())
            .collect(Collectors.joining(", "));

        return formatted.isBlank() ? "N/A" : formatted;
    }

    private List<String> parseCountryCodes(String raw) {
        String value = safe(raw, "").trim();
        if (value.isBlank()) {
            return List.of();
        }

        try {
            List<String> jsonCodes = OBJECT_MAPPER.readValue(value, new TypeReference<List<String>>() {
            });
            if (jsonCodes != null && !jsonCodes.isEmpty()) {
                return jsonCodes;
            }
        } catch (Exception ignored) {
        }

        String normalized = value.replace("[", "")
            .replace("]", "")
            .replace("\"", "")
            .trim();
        if (normalized.isBlank()) {
            return List.of();
        }

        List<String> fallbackCodes = new ArrayList<>();
        for (String part : normalized.split(",")) {
            String code = part == null ? "" : part.trim();
            if (!code.isBlank()) {
                fallbackCodes.add(code);
            }
        }
        return fallbackCodes;
    }

    private String formatCountriesCompact(String json, int maxItems) {
        String formatted = formatCountries(json);
        if (formatted == null || formatted.isBlank() || "N/A".equalsIgnoreCase(formatted)) {
            return "N/A";
        }

        String[] rawParts = formatted.split(",");
        List<String> parts = new ArrayList<>();
        for (String part : rawParts) {
            String item = part == null ? "" : part.trim();
            if (!item.isBlank()) {
                parts.add(item);
            }
        }

        if (parts.isEmpty() || parts.size() <= maxItems) {
            return formatted;
        }

        int more = parts.size() - maxItems;
        return String.join(", ", parts.subList(0, maxItems)) + " +" + more + " more";
    }

    private String offerStatusClass(String status) {
        String normalized = safe(status, "").trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "APPROVED", "ACCEPTED", "ACTIVE", "PUBLISHED" -> "offer-status-approved";
            case "REJECTED", "DECLINED", "BLOCKED", "ARCHIVED" -> "offer-status-rejected";
            default -> "offer-status-pending";
        };
    }

    private String getFlagEmoji(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            return "";
        }
        int firstChar = Character.codePointAt(countryCode, 0) - 65 + 0x1F1E6;
        int secondChar = Character.codePointAt(countryCode, 1) - 65 + 0x1F1E6;
        return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
    }

    private void showError(String title, String message) {
        Label target = activeView == ActiveEmbeddedView.LIST ? offersStatusLabel : formStatusLabel;
        showStatus(target, title + ": " + message, true);
    }

    private void applyCreateOfferButtonPermissions() {
        createOfferButton.setVisible(canCreateOffers);
        createOfferButton.setManaged(canCreateOffers);
        createOfferButton.setDisable(!canCreateOffers);

        boolean agencyVisible = currentAgencyId != null;
        viewReservationRequestsButton.setVisible(agencyVisible);
        viewReservationRequestsButton.setManaged(agencyVisible);
        viewReservationRequestsButton.setDisable(!agencyVisible);

        boolean signedIn = currentUserId != null && currentUserId > 0;
        messagesCenterButton.setVisible(signedIn);
        messagesCenterButton.setManaged(signedIn);
        messagesCenterButton.setDisable(!signedIn);

        notificationsCenterButton.setVisible(signedIn);
        notificationsCenterButton.setManaged(signedIn);
        notificationsCenterButton.setDisable(!signedIn);

        refreshUnreadBadges();
    }

    private void refreshUnreadBadges() {
        refreshNotificationsBadge();
    }

    private void refreshNotificationsBadge() {
        if (currentUserId == null || currentUserId <= 0) {
            notificationsUnreadBadge.setVisible(false);
            notificationsUnreadBadge.setManaged(false);
            return;
        }
        try {
            int unread = notificationService.getUnreadCount(currentUserId);
            notificationsUnreadBadge.setText(String.valueOf(unread));
            notificationsUnreadBadge.setVisible(unread > 0);
            notificationsUnreadBadge.setManaged(unread > 0);
        } catch (SQLException ex) {
            notificationsUnreadBadge.setVisible(false);
            notificationsUnreadBadge.setManaged(false);
        }
    }

    private void stopMessagesAutoRefresh() {
        if (activeMessagesController != null) {
            activeMessagesController.stopAutoRefresh();
        }
    }

    @FXML private void onHome() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onOffres() { /* already on page */ }
    @FXML private void onAgences() { NavigationManager.getInstance().showSignedInAgencies(); }
    @FXML private void onMessagerie() { showMessagesView(); }
    @FXML private void onRecommandation() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onEvenement() { NavigationManager.getInstance().showSignedInEvents(); }
    @FXML private void onPremium() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onNotifications() { showNotificationsView(); }
    @FXML private void onProfile() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onDashboardIa() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onThemeToggle() { NavigationManager.getInstance().toggleTheme(); }
    @FXML private void onLogout() { NavigationManager.getInstance().logoutToGuest(); }
}
