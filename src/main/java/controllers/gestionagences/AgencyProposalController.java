package controllers.gestionagences;

import enums.gestionagences.AgencyApplicationStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import models.gestionagences.AgencyAccount;
import models.gestionagences.AgencyAdminApplication;
import services.gestionagences.AgencyAccountService;
import services.gestionagences.AgencyAdminApplicationService;
import utils.NavigationManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AgencyProposalController {

    @FXML
    private VBox formContainer;
    @FXML
    private VBox statusContainer;
    @FXML
    private Label statusTitleLabel;
    @FXML
    private Label statusMessageLabel;
    @FXML
    private Button statusActionButton;
    @FXML
    private TextField agencyNameField;
    @FXML
    private ChoiceBox<String> countryChoice;
    @FXML
    private TextArea messageToAdminField;
    @FXML
    private Label feedbackLabel;
    @FXML
    private Button simulateApprovalButton;

    private final AgencyAccountService agencyService = new AgencyAccountService();
    private final AgencyAdminApplicationService applicationService = new AgencyAdminApplicationService();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final List<CountryOption> countryOptions = new ArrayList<>();

    @FXML
    private void initialize() {
        NavigationManager nav = NavigationManager.getInstance();
        if (!nav.canAccessSignedInShell()) {
            nav.showLogin();
            return;
        }

        loadCountriesAsync();
        refreshState();
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
    }

    private void showPendingState(AgencyAdminApplication app) {
        formContainer.setVisible(false);
        formContainer.setManaged(false);
        statusContainer.setVisible(true);
        statusContainer.setManaged(true);
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
        String country = readCountryCode(countryChoice.getValue());
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
                app.setCountry(readCountryCode(countryChoice.getValue()) != null ? readCountryCode(countryChoice.getValue()) : "TN");
                app.setMessageToAdmin("Temporary proposal for approval flow testing.");
                applicationService.submit(app);
            }

            // Temporary test path: reviewer is current session user.
            applicationService.approve(app.getId(), currentUserId.get());
            // Go directly to agency page after approval.
            NavigationManager.getInstance().showMyAgency();
        } catch (SQLException | IllegalArgumentException e) {
            statusMessageLabel.setText("Test approval failed: " + e.getMessage());
            feedbackLabel.setText("Test approval failed.");
        }
    }

    private void loadCountriesAsync() {
        Thread loader = new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://restcountries.com/v3.1/all?fields=name,cca2,flag"))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    List<CountryOption> parsed = parseCountries(response.body());
                    Platform.runLater(() -> applyCountries(parsed));
                    return;
                }
            } catch (IOException | InterruptedException ignored) {
                if (ignored instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
            Platform.runLater(this::applyFallbackCountries);
        }, "countries-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private List<CountryOption> parseCountries(String json) {
        List<CountryOption> list = new ArrayList<>();
        Pattern entryPattern = Pattern.compile("\\{[^\\{\\}]*\"flag\"\\s*:\\s*\"([^\"]+)\"[^\\{\\}]*\"name\"\\s*:\\s*\\{[^\\{\\}]*\"common\"\\s*:\\s*\"([^\"]+)\"[^\\{\\}]*\\}[^\\{\\}]*\"cca2\"\\s*:\\s*\"([A-Z]{2})\"[^\\{\\}]*\\}");
        Matcher matcher = entryPattern.matcher(json);
        while (matcher.find()) {
            String flag = matcher.group(1);
            String name = matcher.group(2);
            String code = matcher.group(3);
            list.add(new CountryOption(name, code, flag));
        }
        list.sort(Comparator.comparing(c -> c.name.toLowerCase(Locale.ROOT)));
        return list;
    }

    private void applyCountries(List<CountryOption> parsed) {
        countryOptions.clear();
        countryOptions.addAll(parsed);
        if (countryOptions.isEmpty()) {
            applyFallbackCountries();
            return;
        }
        countryChoice.getItems().setAll(countryOptions.stream().map(CountryOption::label).collect(Collectors.toList()));
    }

    private void applyFallbackCountries() {
        countryOptions.clear();
        countryOptions.add(new CountryOption("France", "FR", "🇫🇷"));
        countryOptions.add(new CountryOption("United Arab Emirates", "AE", "🇦🇪"));
        countryOptions.add(new CountryOption("Maldives", "MV", "🇲🇻"));
        countryOptions.add(new CountryOption("Tunisia", "TN", "🇹🇳"));
        countryChoice.getItems().setAll(countryOptions.stream().map(CountryOption::label).collect(Collectors.toList()));
    }

    private String readCountryCode(String label) {
        if (label == null) {
            return null;
        }
        return countryOptions.stream()
                .filter(c -> c.label().equals(label))
                .map(c -> c.code)
                .findFirst()
                .orElse(null);
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

    @FXML private void onBackToAgencies() { NavigationManager.getInstance().showSignedInAgencies(); }
    @FXML private void onThemeToggle() { NavigationManager.getInstance().toggleTheme(); }
    @FXML private void onLogout() { NavigationManager.getInstance().logoutToGuest(); }
    @FXML private void onHome() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onOffres() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onAgences() { NavigationManager.getInstance().showSignedInAgencies(); }
    @FXML private void onMessagerie() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onRecommandation() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onEvenement() { NavigationManager.getInstance().showSignedInEvents(); }
    @FXML private void onPremium() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onNotifications() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onProfile() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onDashboardIa() { NavigationManager.getInstance().showSignedInShell(); }

    private record CountryOption(String name, String code, String flag) {
        String label() {
            return flag + "  " + name + " (" + code + ")";
        }
    }
}
