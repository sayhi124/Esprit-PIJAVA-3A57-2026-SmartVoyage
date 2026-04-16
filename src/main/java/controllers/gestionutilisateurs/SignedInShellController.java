package controllers.gestionutilisateurs;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import utils.NavigationManager;

public class SignedInShellController {

    @FXML
    private Label userGreetingLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Button dashboardIaButton;
    @FXML
    private Button profileSidebarButton;

    @FXML
    private void initialize() {
        NavigationManager nav = NavigationManager.getInstance();
        if (!nav.canAccessSignedInShell()) {
            nav.showLogin();
            return;
        }

        var currentUser = nav.sessionUser().orElse(null);
        if (currentUser == null) {
            nav.showLogin();
            return;
        }

        String displayName = currentUser.getUsername() != null && !currentUser.getUsername().isBlank()
                ? currentUser.getUsername()
                : currentUser.getEmail();
        userGreetingLabel.setText("Welcome back, " + displayName);

        boolean agencyAdmin = nav.canAccessAgencyAdminFeatures();
        roleLabel.setText(agencyAdmin ? "Agency Admin session active" : "User session active");
        dashboardIaButton.setVisible(agencyAdmin);
        dashboardIaButton.setManaged(agencyAdmin);
    }

    @FXML
    private void onHome() {
        // Already on signed-in home shell.
    }

    @FXML
    private void onOffres() {
        NavigationManager.getInstance().showSignedInOffers();
    }

    @FXML
    private void onAgences() {
        NavigationManager.getInstance().showSignedInAgencies();
    }

    @FXML
    private void onMessagerie() {
        showPlaceholder("Messagerie", "Route Messagerie signed-in will open here.");
    }

    @FXML
    private void onRecommandation() {
        NavigationManager.getInstance().showSignedInPosts();
    }

    @FXML
    private void onEvenement() {
        NavigationManager.getInstance().showSignedInEvents();
    }

    @FXML
    private void onPremium() {
        showPlaceholder("Premium", "Route Premium signed-in will open here.");
    }

    @FXML
    private void onNotifications() {
        showPlaceholder("Notifications", "Route Notifications signed-in will open here.");
    }

    @FXML
    private void onProfile() {
        NavigationManager.getInstance().showUserProfile();
    }

    @FXML
    private void onDashboardIa() {
        if (!NavigationManager.getInstance().canAccessAgencyAdminFeatures()) {
            showPlaceholder("Access denied", "Dashboard IA is only available for agency admins.");
            return;
        }
        showPlaceholder("Dashboard IA", "Route Dashboard IA signed-in will open here.");
    }

    @FXML
    private void onThemeToggle() {
        NavigationManager.getInstance().toggleTheme();
    }

    @FXML
    private void onLogout() {
        NavigationManager.getInstance().logoutToGuest();
    }

    private void showPlaceholder(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
