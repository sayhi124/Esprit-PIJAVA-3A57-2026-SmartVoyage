package controllers.gestionagences;

import enums.gestionagences.AgencyApplicationStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import models.gestionagences.AgencyAccount;
import models.gestionagences.AgencyAdminApplication;
import services.geo.CountryCatalog;
import services.gestionagences.AgencyAccountService;
import services.gestionagences.AgencyAdminApplicationService;
import utils.NavigationManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AgencyProposalController {

    @FXML
    private VBox formContainer;
    @FXML
    private VBox statusContainer;
    @FXML
    private Label statusGlyphLabel;
    @FXML
    private Label statusTitleLabel;
    @FXML
    private Label statusMessageLabel;
    @FXML
    private Button statusActionButton;
    @FXML
    private TextField agencyNameField;
    @FXML
    private ComboBox<CountryCatalog.CountryRow> countryCombo;
    @FXML
    private StackPane countryFlagFrame;
    @FXML
    private ImageView countryFlagImageView;
    @FXML
    private TextArea messageToAdminField;
    @FXML
    private Label feedbackLabel;
    @FXML
    private Button simulateApprovalButton;

    private final AgencyAccountService agencyService = new AgencyAccountService();
    private final AgencyAdminApplicationService applicationService = new AgencyAdminApplicationService();

    @FXML
    private void initialize() {
        NavigationManager nav = NavigationManager.getInstance();
        if (!nav.canAccessSignedInShell()) {
            nav.showLogin();
            return;
        }

        clipFlagFrame();
        wireCountryCombo();
        loadCountriesAsync();
        refreshState();
    }

    private void clipFlagFrame() {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(14);
        clip.setArcHeight(14);
        clip.widthProperty().bind(countryFlagFrame.widthProperty());
        clip.heightProperty().bind(countryFlagFrame.heightProperty());
        countryFlagFrame.setClip(clip);
    }

    private void wireCountryCombo() {
        countryCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CountryCatalog.CountryRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setText(null);
                } else {
                    setText(row.name() + " (" + row.cca2() + ")");
                }
            }
        });
        countryCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(CountryCatalog.CountryRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setText(null);
                } else {
                    setText(row.name() + " (" + row.cca2() + ")");
                }
            }
        });
        countryCombo.valueProperty().addListener((obs, prev, cur) -> updateCountryFlagPreview(cur));
        updateCountryFlagPreview(countryCombo.getValue());
    }

    private void updateCountryFlagPreview(CountryCatalog.CountryRow row) {
        if (row == null || row.cca2() == null || row.cca2().length() != 2) {
            countryFlagImageView.setImage(null);
            return;
        }
        String url = row.flagImageUrl();
        if (url == null) {
            countryFlagImageView.setImage(null);
            return;
        }
        countryFlagImageView.setImage(new Image(url, 160, 107, true, true, true));
    }

    private void refreshState() {
        Optional<Integer> currentUserId = NavigationManager.getInstance().sessionUser().map(u -> u.getId());
        if (currentUserId.isEmpty()) {
            NavigationManager.getInstance().showLogin();
            return;
        }

        try {
            Optional<AgencyAccount> agency = agencyService.findByResponsableId(currentUserId.get());
            if (agency.isPresent()) {
                showAlreadyAgencyState(agency.get());
                return;
            }

            Optional<AgencyAdminApplication> latest = applicationService.findLatestByApplicant(currentUserId.get());
            if (latest.isEmpty()) {
                showFormState();
                return;
            }

            AgencyAdminApplication app = latest.get();
            if (app.getStatus() == AgencyApplicationStatus.APPROVED) {
                showApprovedState();
            } else if (app.getStatus() == AgencyApplicationStatus.PENDING) {
                showPendingState(app);
            } else {
                showRejectedState(app);
            }
        } catch (SQLException e) {
            showFormState();
            feedbackLabel.setText("Unable to load application status: " + e.getMessage());
        }
    }

    private void showFormState() {
        formContainer.setVisible(true);
        formContainer.setManaged(true);
        statusContainer.setVisible(false);
        statusContainer.setManaged(false);
        statusActionButton.setVisible(false);
        statusActionButton.setManaged(false);
        hideStatusGlyph();
    }

    private void hideStatusGlyph() {
        statusGlyphLabel.setVisible(false);
        statusGlyphLabel.setManaged(false);
        statusGlyphLabel.setText("");
    }

    private void showPendingState(AgencyAdminApplication app) {
        formContainer.setVisible(false);
        formContainer.setManaged(false);
        statusContainer.setVisible(true);
        statusContainer.setManaged(true);
        statusGlyphLabel.setText("\u23F0");
        statusGlyphLabel.setVisible(true);
        statusGlyphLabel.setManaged(true);
        statusTitleLabel.setText("Application pending review");
        statusMessageLabel.setText("Your agency proposal \"" + safe(app.getAgencyNameRequested()) + "\" is under review. Please wait for admin approval.");
        statusActionButton.setVisible(false);
        statusActionButton.setManaged(false);
        simulateApprovalButton.setVisible(true);
        simulateApprovalButton.setManaged(true);
    }

    private void showRejectedState(AgencyAdminApplication app) {
        formContainer.setVisible(false);
        formContainer.setManaged(false);
        statusContainer.setVisible(true);
        statusContainer.setManaged(true);
        hideStatusGlyph();
        statusTitleLabel.setText("Application rejected");
        String note = app.getReviewNote() == null || app.getReviewNote().isBlank()
                ? "No admin note."
                : app.getReviewNote();
        statusMessageLabel.setText("Your previous application was rejected.\nAdmin note: " + note + "\nYou can submit a new proposal now.");
        statusActionButton.setVisible(true);
        statusActionButton.setManaged(true);
        statusActionButton.setText("Submit a new proposal");
        statusActionButton.setOnAction(e -> showFormState());
        simulateApprovalButton.setVisible(false);
        simulateApprovalButton.setManaged(false);
    }

    private void showApprovedState() {
        statusContainer.setVisible(true);
        statusContainer.setManaged(true);
        formContainer.setVisible(false);
        formContainer.setManaged(false);
        statusTitleLabel.setText("Application approved");
        statusMessageLabel.setText("Your application is approved. You can now access your agency page.");
        statusActionButton.setVisible(true);
        statusActionButton.setManaged(true);
        statusActionButton.setText("Open agency page");
        statusActionButton.setOnAction(e -> NavigationManager.getInstance().showMyAgency());
        simulateApprovalButton.setVisible(false);
        simulateApprovalButton.setManaged(false);
    }

    private void showAlreadyAgencyState(AgencyAccount agency) {
        statusContainer.setVisible(true);
        statusContainer.setManaged(true);
        formContainer.setVisible(false);
        formContainer.setManaged(false);
        hideStatusGlyph();
        statusTitleLabel.setText("Agency already exists");
        statusMessageLabel.setText("You already manage \"" + safe(agency.getAgencyName()) + "\". Opening agency page.");
        statusActionButton.setVisible(true);
        statusActionButton.setManaged(true);
        statusActionButton.setText("Open agency page");
        statusActionButton.setOnAction(e -> NavigationManager.getInstance().showMyAgency());
        simulateApprovalButton.setVisible(false);
        simulateApprovalButton.setManaged(false);
    }

    @FXML
    private void onSubmitProposal() {
        feedbackLabel.setText("");
        Optional<Integer> currentUserId = NavigationManager.getInstance().sessionUser().map(u -> u.getId());
        if (currentUserId.isEmpty()) {
            NavigationManager.getInstance().showLogin();
            return;
        }

        String agencyName = agencyNameField.getText() == null ? "" : agencyNameField.getText().trim();
        String country = readSelectedCountryCode();
        String message = messageToAdminField.getText() == null ? "" : messageToAdminField.getText().trim();

        if (agencyName.isBlank()) {
            feedbackLabel.setText("Agency name is required.");
            return;
        }
        if (country == null || country.isBlank()) {
            feedbackLabel.setText("Country is required.");
            return;
        }

        AgencyAdminApplication app = new AgencyAdminApplication();
        app.setApplicantId(currentUserId.get());
        app.setAgencyNameRequested(agencyName);
        app.setCountry(country);
        app.setMessageToAdmin(message);
        try {
            applicationService.submit(app);
            feedbackLabel.setText("Application submitted successfully.");
            refreshState();
        } catch (SQLException | IllegalArgumentException e) {
            feedbackLabel.setText("Cannot submit application: " + e.getMessage());
        }
    }

    @FXML
    private void onSimulateApproval() {
        feedbackLabel.setText("");
        Optional<Integer> currentUserId = NavigationManager.getInstance().sessionUser().map(u -> u.getId());
        if (currentUserId.isEmpty()) {
            NavigationManager.getInstance().showLogin();
            return;
        }
        try {
            Optional<AgencyAdminApplication> latest = applicationService.findLatestByApplicant(currentUserId.get());
            AgencyAdminApplication app;
            if (latest.isPresent() && latest.get().getStatus() == AgencyApplicationStatus.PENDING) {
                app = latest.get();
            } else {
                app = new AgencyAdminApplication();
                app.setApplicantId(currentUserId.get());
                app.setAgencyNameRequested(defaultAgencyName());
                app.setCountry(Optional.ofNullable(readSelectedCountryCode()).orElse("TN"));
                app.setMessageToAdmin("Temporary proposal for approval flow testing.");
                applicationService.submit(app);
            }

            applicationService.approve(app.getId(), currentUserId.get());
            NavigationManager.getInstance().showMyAgency();
        } catch (SQLException | IllegalArgumentException e) {
            statusMessageLabel.setText("Test approval failed: " + e.getMessage());
            feedbackLabel.setText("Test approval failed.");
        }
    }

    private void loadCountriesAsync() {
        Thread loader = new Thread(() -> {
            List<CountryCatalog.CountryRow> rows = CountryCatalog.fetchAllOrEmpty();
            if (rows.isEmpty()) {
                Platform.runLater(this::applyFallbackCountries);
            } else {
                Platform.runLater(() -> applyCountries(rows));
            }
        }, "proposal-countries");
        loader.setDaemon(true);
        loader.start();
    }

    private void applyCountries(List<CountryCatalog.CountryRow> rows) {
        countryCombo.setItems(FXCollections.observableArrayList(rows));
    }

    private void applyFallbackCountries() {
        countryCombo.setItems(FXCollections.observableArrayList(new ArrayList<>(CountryCatalog.fallbackSample())));
    }

    private String readSelectedCountryCode() {
        CountryCatalog.CountryRow v = countryCombo.getValue();
        return v == null ? null : v.cca2();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String defaultAgencyName() {
        String typed = agencyNameField.getText() == null ? "" : agencyNameField.getText().trim();
        if (!typed.isBlank()) {
            return typed;
        }
        String username = NavigationManager.getInstance().sessionUser().map(u -> u.getUsername()).orElse("Smart");
        return username + " Agency";
    }

    @FXML
    private void onBackToAgencies() {
        NavigationManager.getInstance().showSignedInAgencies();
    }

    @FXML
    private void onThemeToggle() {
        NavigationManager.getInstance().toggleTheme();
    }

    @FXML
    private void onLogout() {
        NavigationManager.getInstance().logoutToGuest();
    }

    @FXML
    private void onHome() {
        NavigationManager.getInstance().showSignedInShell();
    }

    @FXML
    private void onOffres() {
        NavigationManager.getInstance().showSignedInShell();
    }

    @FXML
    private void onAgences() {
        NavigationManager.getInstance().showSignedInAgencies();
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
        NavigationManager.getInstance().showSignedInEvents();
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
        NavigationManager.getInstance().showSignedInShell();
    }
}
