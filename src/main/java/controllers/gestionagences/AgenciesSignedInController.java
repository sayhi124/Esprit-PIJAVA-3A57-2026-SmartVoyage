package controllers.gestionagences;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import models.gestionagences.AgencyAccount;
import models.gestionagences.ImageAsset;
import services.geo.CountryCatalog;
import services.gestionagences.AgencyAccountService;
import utils.NavigationManager;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class AgenciesSignedInController {

    private static final double AGENCY_CARD_WIDTH = 416;
    private static final double AGENCY_IMG_HEIGHT = 256;
    private static final double DIRECTORY_FLAG_OUTER = 48;
    private static final double DIRECTORY_FLAG_INNER = 38;

    /** First entry in the country ChoiceBox; must match REST-backed labels in {@link #applyCountryCatalog}. */
    private static final String LABEL_ALL_COUNTRIES = "All countries";

    private PauseTransition searchDebounce;

    @FXML
    private TextField searchField;
    @FXML
    private ChoiceBox<String> countryFilter;
    @FXML
    private TilePane agenciesGrid;
    @FXML
    private Label resultCountLabel;
    @FXML
    private Button myAgencyButton;
    @FXML
    private Label roleInfoLabel;

    private final AgencyAccountService agencyService = new AgencyAccountService();
    private final List<AgencyAccount> allAgencies = new ArrayList<>();
    private final List<CountryCatalog.CountryRow> countryRows = new ArrayList<>();

    @FXML
    private void initialize() {
        NavigationManager nav = NavigationManager.getInstance();
        if (!nav.canAccessSignedInShell()) {
            nav.showLogin();
            return;
        }

        boolean agencyAdmin = nav.canAccessAgencyAdminFeatures();
        myAgencyButton.setVisible(agencyAdmin);
        myAgencyButton.setManaged(agencyAdmin);
        roleInfoLabel.setText(agencyAdmin ? "Agency Admin mode" : "User mode");

        setupFilters();
        bindSearchDebounce();
        loadAgencies();
        bindResponsiveAgencyGrid();
        applyFilters();
    }

    /**
     * Debounces text search so the grid updates shortly after typing stops (Symfony-like dynamic filters).
     */
    private void bindSearchDebounce() {
        searchDebounce = new PauseTransition(Duration.millis(280));
        searchDebounce.setOnFinished(e -> applyFilters());
        searchField.textProperty().addListener((obs, prev, cur) -> {
            searchDebounce.stop();
            searchDebounce.playFromStart();
        });
    }

    private void bindResponsiveAgencyGrid() {
        if (agenciesGrid == null) {
            return;
        }
        agenciesGrid.setAlignment(Pos.TOP_CENTER);
        agenciesGrid.setMaxWidth(Double.MAX_VALUE);
        Runnable updateColumns = () -> {
            double w = agenciesGrid.getWidth();
            if (w <= 1 && agenciesGrid.getScene() != null) {
                w = agenciesGrid.getScene().getWidth() - 320;
            }
            double gap = 28;
            double card = AGENCY_CARD_WIDTH;
            if (w < card + gap) {
                agenciesGrid.setPrefColumns(1);
                return;
            }
            int cols = (int) Math.floor((w + gap) / (card + gap));
            cols = Math.max(1, Math.min(4, cols));
            agenciesGrid.setPrefColumns(cols);
        };
        agenciesGrid.sceneProperty().addListener((obs, oldSc, newSc) -> {
            if (newSc != null) {
                newSc.widthProperty().addListener((o, a, b) -> updateColumns.run());
                updateColumns.run();
            }
        });
        agenciesGrid.widthProperty().addListener((o, a, b) -> updateColumns.run());
        Platform.runLater(updateColumns);
    }

    private void setupFilters() {
        countryFilter.getItems().setAll(LABEL_ALL_COUNTRIES);
        countryFilter.setValue(LABEL_ALL_COUNTRIES);
        countryFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> applyFilters());
    }

    private void loadAgencies() {
        allAgencies.clear();
        try {
            List<AgencyAccount> dbRows = agencyService.findAll();
            if (dbRows.isEmpty()) {
                allAgencies.addAll(buildMockAgencies());
            } else {
                allAgencies.addAll(dbRows);
            }
        } catch (SQLException e) {
            allAgencies.addAll(buildMockAgencies());
        }
        startCountryCatalogLoad();
    }

    private void startCountryCatalogLoad() {
        Thread t = new Thread(() -> {
            List<CountryCatalog.CountryRow> rows = CountryCatalog.fetchAllOrEmpty();
            if (rows.isEmpty()) {
                rows = new ArrayList<>(CountryCatalog.fallbackSample());
            }
            List<CountryCatalog.CountryRow> loaded = rows;
            Platform.runLater(() -> applyCountryCatalog(loaded));
        }, "agency-directory-countries");
        t.setDaemon(true);
        t.start();
    }

    private void applyCountryCatalog(List<CountryCatalog.CountryRow> rows) {
        countryRows.clear();
        countryRows.addAll(rows);
        String current = countryFilter.getValue();
        List<String> items = new ArrayList<>();
        items.add(LABEL_ALL_COUNTRIES);
        for (CountryCatalog.CountryRow r : countryRows) {
            items.add(r.choiceLabel());
        }
        countryFilter.getItems().setAll(items);
        countryFilter.setValue(items.contains(current) ? current : LABEL_ALL_COUNTRIES);
        applyFilters();
    }

    private String selectedCountryIso2() {
        String v = countryFilter.getValue();
        if (v == null || LABEL_ALL_COUNTRIES.equals(v)) {
            return null;
        }
        for (CountryCatalog.CountryRow r : countryRows) {
            if (r.choiceLabel().equals(v)) {
                return r.cca2();
            }
        }
        if (v.length() == 2 && v.chars().allMatch(Character::isLetter)) {
            return v.toUpperCase(Locale.ROOT);
        }
        return null;
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        Comparator<AgencyAccount> byName = Comparator.comparing(
                a -> safe(a.getAgencyName()), String.CASE_INSENSITIVE_ORDER);

        List<AgencyAccount> filtered = allAgencies.stream()
                .filter(a -> matchesQuery(a, query))
                .filter(this::matchesCountry)
                .sorted(byName)
                .collect(Collectors.toList());

        renderGrid(filtered);
        resultCountLabel.setText(formatAgencyResultCount(filtered.size()));
    }

    private static String formatAgencyResultCount(int n) {
        if (n == 0) {
            return "No agencies found";
        }
        if (n == 1) {
            return "1 agency found";
        }
        return n + " agencies found";
    }

    private boolean matchesQuery(AgencyAccount agency, String query) {
        if (query.isBlank()) {
            return true;
        }
        return safe(agency.getAgencyName()).toLowerCase(Locale.ROOT).contains(query)
                || safe(agency.getDescription()).toLowerCase(Locale.ROOT).contains(query)
                || safe(agency.getAddress()).toLowerCase(Locale.ROOT).contains(query)
                || safe(agency.getCountry()).toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesCountry(AgencyAccount agency) {
        String iso = selectedCountryIso2();
        if (iso == null) {
            return true;
        }
        return iso.equalsIgnoreCase(safe(agency.getCountry()));
    }

    private void renderGrid(List<AgencyAccount> agencies) {
        agenciesGrid.getChildren().clear();
        for (AgencyAccount agency : agencies) {
            agenciesGrid.getChildren().add(buildAgencyCard(agency));
        }
    }

    private VBox buildAgencyCard(AgencyAccount agency) {
        VBox card = new VBox();
        card.getStyleClass().add("agency-directory-card");
        card.setPrefWidth(AGENCY_CARD_WIDTH);
        card.setMinWidth(AGENCY_CARD_WIDTH);
        card.setMaxWidth(AGENCY_CARD_WIDTH);
        card.setFillWidth(true);

        StackPane hero = new StackPane();
        hero.getStyleClass().add("agency-directory-hero");
        hero.setMinHeight(AGENCY_IMG_HEIGHT);
        hero.setPrefHeight(AGENCY_IMG_HEIGHT);
        hero.setMaxHeight(AGENCY_IMG_HEIGHT);

        ImageView cover = new ImageView(resolveAgencyImage(agency));
        cover.getStyleClass().add("agency-directory-hero-image");
        cover.setPreserveRatio(false);
        cover.setFitWidth(AGENCY_CARD_WIDTH);
        cover.setFitHeight(AGENCY_IMG_HEIGHT);
        cover.setSmooth(true);

        Region shade = new Region();
        shade.setMouseTransparent(true);
        shade.getStyleClass().add("agency-directory-hero-shade");

        hero.getChildren().addAll(cover, shade);

        Rectangle heroClip = new Rectangle();
        heroClip.setArcWidth(26);
        heroClip.setArcHeight(26);
        heroClip.setWidth(AGENCY_CARD_WIDTH);
        heroClip.setHeight(AGENCY_IMG_HEIGHT);
        hero.setClip(heroClip);

        VBox body = new VBox(10);
        body.getStyleClass().add("agency-directory-body");
        body.setPadding(new Insets(14, 18, 6, 18));

        Label title = new Label(safe(agency.getAgencyName(), "Agency"));
        title.getStyleClass().add("agency-directory-title");
        title.setWrapText(true);

        StackPane flagBadge = buildCountryFlagBadge(agency);

        Label desc = new Label(safe(agency.getDescription(), "No description yet."));
        desc.getStyleClass().add("agency-directory-desc");
        desc.setWrapText(true);
        desc.setMaxHeight(120);
        desc.setMinHeight(52);

        if (flagBadge != null) {
            HBox titleRow = new HBox(12);
            titleRow.getStyleClass().add("agency-directory-title-row");
            titleRow.setAlignment(Pos.TOP_LEFT);
            Region titleSpring = new Region();
            HBox.setHgrow(titleSpring, Priority.ALWAYS);
            title.maxWidthProperty().bind(titleRow.widthProperty().subtract(DIRECTORY_FLAG_OUTER + 12));
            titleRow.getChildren().addAll(title, titleSpring, flagBadge);
            body.getChildren().addAll(titleRow, desc);
        } else {
            title.setMaxWidth(AGENCY_CARD_WIDTH - 36);
            body.getChildren().addAll(title, desc);
        }

        String addr = safe(agency.getAddress());
        if (!addr.isBlank()) {
            Label addressRow = new Label("\uD83D\uDCCD " + addr);
            addressRow.getStyleClass().add("agency-directory-meta");
            addressRow.setWrapText(true);
            addressRow.setMaxWidth(AGENCY_CARD_WIDTH - 36);
            body.getChildren().add(addressRow);
        }

        Label phoneRow = new Label("\u260E  " + safe(agency.getPhone(), "\u2014"));
        phoneRow.getStyleClass().add("agency-directory-contact");
        phoneRow.setWrapText(true);
        phoneRow.setMaxWidth(AGENCY_CARD_WIDTH - 36);

        String web = safe(agency.getWebsiteUrl());
        Label webRow = new Label("\uD83C\uDF10  " + (web.isBlank() ? "\u2014" : abbreviateUrl(web)));
        webRow.getStyleClass().add("agency-directory-contact");
        webRow.setWrapText(true);
        webRow.setMaxWidth(AGENCY_CARD_WIDTH - 36);

        body.getChildren().addAll(phoneRow, webRow);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("agency-directory-footer");
        footer.setPadding(new Insets(4, 18, 16, 18));
        Button details = new Button("View agency \u2192");
        details.getStyleClass().add("agency-directory-cta");
        details.setOnAction(e -> onAgencyDetails(agency));
        footer.getChildren().add(details);

        card.getChildren().addAll(hero, body, footer);
        VBox.setVgrow(body, Priority.ALWAYS);
        return card;
    }

    /**
     * Circular flag from ISO-3166-1 alpha-2 (see {@link CountryCatalog#resolveIso2} for name/address parsing).
     */
    private StackPane buildCountryFlagBadge(AgencyAccount agency) {
        String iso = CountryCatalog.resolveIso2(agency.getCountry(), agency.getAddress());
        if (iso == null) {
            return null;
        }
        String url = CountryCatalog.flagPngUrl(iso);
        if (url == null) {
            return null;
        }
        StackPane ring = new StackPane();
        ring.getStyleClass().add("agency-directory-flag-badge");
        ring.setMinSize(DIRECTORY_FLAG_OUTER, DIRECTORY_FLAG_OUTER);
        ring.setPrefSize(DIRECTORY_FLAG_OUTER, DIRECTORY_FLAG_OUTER);
        ring.setMaxSize(DIRECTORY_FLAG_OUTER, DIRECTORY_FLAG_OUTER);

        ImageView flagIv = new ImageView(new Image(url, true));
        flagIv.setFitWidth(DIRECTORY_FLAG_INNER);
        flagIv.setFitHeight(DIRECTORY_FLAG_INNER);
        flagIv.setPreserveRatio(false);
        double rad = DIRECTORY_FLAG_INNER / 2.0;
        flagIv.setClip(new Circle(rad, rad, rad));
        ring.getChildren().add(flagIv);
        return ring;
    }

    private static String abbreviateUrl(String url) {
        if (url == null || url.isBlank()) {
            return "\u2014";
        }
        String u = url.replaceFirst("(?i)^https?://", "");
        if (u.length() > 46) {
            return u.substring(0, 43) + "\u2026";
        }
        return u;
    }

    private Image resolveAgencyImage(AgencyAccount agency) {
        Long profileId = agency.getAgencyProfileImageId();
        if (profileId != null) {
            try {
                Optional<ImageAsset> asset = agencyService.loadAgencyProfileImage(agency.getId());
                if (asset.isPresent() && asset.get().getData() != null && asset.get().getData().length > 0) {
                    return new Image(new ByteArrayInputStream(asset.get().getData()));
                }
            } catch (SQLException ignored) {
                // Fallback to mock image url when DB image is unavailable.
            }
        }
        return new Image(mockImageUrlForAgency(agency), true);
    }

    private String mockImageUrlForAgency(AgencyAccount agency) {
        String key = safe(agency.getCountry()).toLowerCase(Locale.ROOT);
        if (key.contains("fr") || key.contains("paris")) {
            return "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=900&q=80";
        }
        if (key.contains("ae") || key.contains("dubai")) {
            return "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?auto=format&fit=crop&w=900&q=80";
        }
        if (key.contains("mv") || key.contains("maldives")) {
            return "https://images.unsplash.com/photo-1573843981267-be1999ff37cd?auto=format&fit=crop&w=900&q=80";
        }
        return "https://images.unsplash.com/photo-1488646953014-85cb44e25828?auto=format&fit=crop&w=900&q=80";
    }

    private List<AgencyAccount> buildMockAgencies() {
        List<AgencyAccount> mocks = new ArrayList<>();
        mocks.add(mockAgency("Blue Dune Travel", "Luxury desert and skyline itineraries.", "https://bluedune.example", "+971 50 112 3344", "Marina Walk", "AE", true));
        mocks.add(mockAgency("Lagoon Signature", "Island escapes and private-villa packages.", "https://lagoon.example", "+960 77 345 228", "Male Center", "MV", true));
        mocks.add(mockAgency("Aurora Routes", "Northern lights, fjords, and curated winter routes.", "https://aurora.example", "+47 91 222 999", "Bergen Harbor", "NO", false));
        mocks.add(mockAgency("Paris Lumiere Agency", "Art, gastronomy, and boutique city journeys.", "https://lumiere.example", "+33 6 11 55 22 90", "Rive Gauche", "FR", true));
        return mocks;
    }

    private AgencyAccount mockAgency(String name, String desc, String web, String phone, String address, String country, boolean verified) {
        AgencyAccount a = new AgencyAccount();
        a.setAgencyName(name);
        a.setDescription(desc);
        a.setWebsiteUrl(web);
        a.setPhone(phone);
        a.setAddress(address);
        a.setCountry(country);
        a.setVerified(verified);
        return a;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @FXML
    private void onViewOnMap() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("https://www.openstreetmap.org/"));
            }
        } catch (Exception ignored) {
            // No browser available; button remains a no-op.
        }
    }

    @FXML
    private void onHome() {
        NavigationManager.getInstance().showPostLoginHome();
    }

    @FXML
    private void onOffres() {
        NavigationManager.getInstance().showPostLoginHome();
    }

    @FXML
    private void onAgences() {
        // Already here.
    }

    @FXML
    private void onMessagerie() {
        NavigationManager.getInstance().showPostLoginHome();
    }

    @FXML
    private void onRecommandation() {
        NavigationManager.getInstance().showPostLoginHome();
    }

    @FXML
    private void onEvenement() {
        NavigationManager.getInstance().showSignedInEvents();
    }

    @FXML
    private void onPremium() {
        NavigationManager.getInstance().showPostLoginHome();
    }

    @FXML
    private void onNotifications() {
        NavigationManager.getInstance().showPostLoginHome();
    }

    @FXML
    private void onProfile() {
        NavigationManager.getInstance().showUserProfile();
    }

    @FXML
    private void onDashboardIa() {
        if (!NavigationManager.getInstance().canAccessAgencyAdminFeatures()) {
            return;
        }
        NavigationManager.getInstance().showPostLoginHome();
    }

    @FXML
    private void onMyAgency() {
        Optional<Integer> userIdOpt = NavigationManager.getInstance().sessionUser().map(u -> u.getId());
        if (userIdOpt.isEmpty()) {
            NavigationManager.getInstance().showLogin();
            return;
        }
        try {
            Optional<AgencyAccount> agency = agencyService.findByResponsableId(userIdOpt.get());
            if (agency.isPresent()) {
                NavigationManager.getInstance().showAgencyProfile(agency.get().getId());
                return;
            }
            NavigationManager.getInstance().showAgencyProposal();
        } catch (SQLException e) {
            NavigationManager.getInstance().showAgencyProposal();
        }
    }

    @FXML
    private void onThemeToggle() {
        NavigationManager.getInstance().toggleTheme();
    }

    @FXML
    private void onLogout() {
        NavigationManager.getInstance().logoutToGuest();
    }

    private void onAgencyDetails(AgencyAccount agency) {
        if (agency.getId() == null) {
            resultCountLabel.setText("Selected: " + safe(agency.getAgencyName(), "Agency"));
            return;
        }
        NavigationManager.getInstance().showAgencyProfile(agency.getId());
    }
}
