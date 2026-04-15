package controllers.gestionagences;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import models.gestionagences.AgencyAccount;
import models.gestionagences.ImageAsset;
import services.gestionagences.AgencyAccountService;
import utils.NavigationManager;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AgenciesSignedInController {

    @FXML
    private TextField searchField;
    @FXML
    private ChoiceBox<String> countryFilter;
    @FXML
    private ChoiceBox<String> verifiedFilter;
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
        roleInfoLabel.setText(agencyAdmin ? "Agence Admin mode" : "User mode");

        setupFilters();
        loadAgencies();
        applyFilters();
    }

    private void setupFilters() {
        countryFilter.getItems().setAll("All countries");
        countryFilter.setValue("All countries");

        verifiedFilter.getItems().setAll("All", "Verified", "Not verified");
        verifiedFilter.setValue("All");

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        countryFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> applyFilters());
        verifiedFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> applyFilters());
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
        refreshCountryFilter();
    }

    private void refreshCountryFilter() {
        String current = countryFilter.getValue();
        Set<String> countries = allAgencies.stream()
                .map(AgencyAccount::getCountry)
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> sorted = countries.stream().sorted().collect(Collectors.toList());
        List<String> items = new ArrayList<>();
        items.add("All countries");
        items.addAll(sorted);
        countryFilter.getItems().setAll(items);
        countryFilter.setValue(items.contains(current) ? current : "All countries");
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String country = countryFilter.getValue();
        String verified = verifiedFilter.getValue();

        List<AgencyAccount> filtered = allAgencies.stream()
                .filter(a -> matchesQuery(a, query))
                .filter(a -> matchesCountry(a, country))
                .filter(a -> matchesVerified(a, verified))
                .sorted(Comparator.comparing(a -> safe(a.getAgencyName())))
                .collect(Collectors.toList());

        renderGrid(filtered);
        resultCountLabel.setText(filtered.size() + " agencies found");
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

    private boolean matchesCountry(AgencyAccount agency, String selectedCountry) {
        if (selectedCountry == null || "All countries".equals(selectedCountry)) {
            return true;
        }
        return selectedCountry.equalsIgnoreCase(safe(agency.getCountry()));
    }

    private boolean matchesVerified(AgencyAccount agency, String selectedStatus) {
        if (selectedStatus == null || "All".equals(selectedStatus)) {
            return true;
        }
        boolean verified = Boolean.TRUE.equals(agency.getVerified());
        return ("Verified".equals(selectedStatus) && verified)
                || ("Not verified".equals(selectedStatus) && !verified);
    }

    private void renderGrid(List<AgencyAccount> agencies) {
        agenciesGrid.getChildren().clear();
        for (AgencyAccount agency : agencies) {
            agenciesGrid.getChildren().add(buildAgencyCard(agency));
        }
    }

    private VBox buildAgencyCard(AgencyAccount agency) {
        VBox card = new VBox(10);
        card.getStyleClass().add("offer-featured-card");
        card.setPrefWidth(320);
        card.setMinWidth(320);
        card.setMaxWidth(320);
        card.setPrefHeight(430);
        card.setPadding(new Insets(0, 0, 14, 0));

        ImageView cover = new ImageView(resolveAgencyImage(agency));
        cover.getStyleClass().add("offer-featured-image");
        cover.setPreserveRatio(false);
        cover.setFitWidth(320);
        cover.setFitHeight(180);

        Label country = new Label(flagCountry(agency.getCountry()) + "  " + safe(agency.getCountry(), "N/A"));
        country.getStyleClass().add("offer-featured-destination");

        Label title = new Label(safe(agency.getAgencyName(), "Agency"));
        title.getStyleClass().add("offer-featured-title");
        title.setWrapText(true);

        Label desc = new Label(safe(agency.getDescription(), "No description yet."));
        desc.getStyleClass().add("offer-featured-description");
        desc.setWrapText(true);
        desc.setMaxHeight(78);

        Label verified = new Label(Boolean.TRUE.equals(agency.getVerified()) ? "Verified agency" : "Pending verification");
        verified.getStyleClass().add(Boolean.TRUE.equals(agency.getVerified()) ? "offer-agency-name" : "offer-starts-label");

        Label contact = new Label("☎ " + safe(agency.getPhone(), "N/A") + "   •   🌐 " + safe(agency.getWebsiteUrl(), "N/A"));
        contact.getStyleClass().add("offer-starts-label");
        contact.setWrapText(true);

        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("offer-footer-lock-panel");
        Button details = new Button("Voir agence");
        details.getStyleClass().add("offer-guest-button");
        details.setOnAction(e -> onAgencyDetails(agency));
        footer.getChildren().add(details);

        VBox content = new VBox(7, country, title, desc, verified, contact);
        content.getStyleClass().add("offer-featured-content");
        VBox.setVgrow(content, Priority.ALWAYS);

        card.getChildren().addAll(cover, content, footer);
        return card;
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

    private String flagCountry(String country) {
        if (country == null || country.length() < 2) {
            return "🌍";
        }
        String c = country.substring(0, 2).toUpperCase(Locale.ROOT);
        if ("FR".equals(c)) return "🇫🇷";
        if ("AE".equals(c)) return "🇦🇪";
        if ("MV".equals(c)) return "🇲🇻";
        if ("NO".equals(c)) return "🇳🇴";
        return "🌍";
    }

    @FXML
    private void onHome() {
        NavigationManager.getInstance().showSignedInShell();
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
        NavigationManager.getInstance().showSignedInShell();
    }

    @FXML
    private void onRecommandation() {
        NavigationManager.getInstance().showSignedInShell();
    }

    @FXML
    private void onEvenement() {
        NavigationManager.getInstance().showSignedInShell();
    }

    @FXML
    private void onPremium() {
        NavigationManager.getInstance().showSignedInShell();
    }

    @FXML
    private void onNotifications() {
        NavigationManager.getInstance().showSignedInShell();
    }

    @FXML
    private void onProfile() {
        NavigationManager.getInstance().showSignedInShell();
    }

    @FXML
    private void onDashboardIa() {
        if (!NavigationManager.getInstance().canAccessAgencyAdminFeatures()) {
            return;
        }
        NavigationManager.getInstance().showSignedInShell();
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
