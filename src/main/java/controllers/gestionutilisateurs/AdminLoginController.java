package controllers.gestionutilisateurs;

import atlantafx.base.theme.PrimerDark;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import enums.gestionutilisateurs.UserRole;
import models.gestionutilisateurs.User;
import utils.NavigationManager;
import utils.PasswordHasher;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class AdminLoginController {
    private static final String ADMIN_IMAGE = "/images/admin/admin.jpg";
    private static final String LEGACY_ADMIN_USERNAME = "admin";
    private static final String LEGACY_ADMIN_PASSWORD = "admin123";
    private static final Pattern EMAIL_SIMPLE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @FXML
    private BorderPane authRoot;
    @FXML
    private StackPane adminImagePanel;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordVisibleField;
    @FXML
    private Button togglePasswordButton;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        installLibraryTheme();
        applyAdminPanelImage();
        if (passwordField != null && passwordVisibleField != null) {
            passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        }
        if (authRoot != null) {
            authRoot.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(320), authRoot);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        }
    }

    private void installLibraryTheme() {
        String current = Application.getUserAgentStylesheet();
        String primer = new PrimerDark().getUserAgentStylesheet();
        if (current == null || current.isBlank() || !current.equals(primer)) {
            Application.setUserAgentStylesheet(primer);
        }
    }

    private void applyAdminPanelImage() {
        if (adminImagePanel == null) {
            return;
        }
        var imageUrl = AdminLoginController.class.getResource(ADMIN_IMAGE);
        if (imageUrl == null) {
            return;
        }
        adminImagePanel.setStyle(
                "-fx-background-image: url('" + imageUrl.toExternalForm() + "');"
                        + "-fx-background-size: cover;"
                        + "-fx-background-repeat: no-repeat;"
                        + "-fx-background-position: center center;"
        );
    }

    @FXML
    private void onLogin() {
        messageLabel.getStyleClass().removeAll("message-error", "message-success");
        messageLabel.setText("");

        String username = usernameField != null ? usernameField.getText() : "";
        String password = getPasswordInput();
        String identifier = username.trim();

        if (identifier.isEmpty() || password.trim().isEmpty()) {
            messageLabel.getStyleClass().add("message-error");
            messageLabel.setText("Please enter both username and password.");
            return;
        }

        Optional<User> adminUser;
        try {
            adminUser = authenticateAdmin(identifier, password);
        } catch (SQLException e) {
            messageLabel.getStyleClass().add("message-error");
            messageLabel.setText("Database error: " + e.getMessage());
            return;
        }

        if (adminUser.isEmpty()) {
            messageLabel.getStyleClass().add("message-error");
            messageLabel.setText("Invalid admin credentials.");
            return;
        }

        NavigationManager.getInstance().setSessionUser(adminUser.get());
        messageLabel.getStyleClass().add("message-success");
        messageLabel.setText("Admin sign-in successful.");
        NavigationManager.getInstance().showAdminDashboard();
    }

    @FXML
    private void onBack() {
        NavigationManager.getInstance().showWelcome();
    }

    @FXML
    private void onHome() {
        NavigationManager.getInstance().showWelcome();
    }

    @FXML
    private void onTogglePassword() {
        boolean show = passwordVisibleField != null && !passwordVisibleField.isVisible();
        if (passwordVisibleField != null && passwordField != null) {
            passwordVisibleField.setVisible(show);
            passwordVisibleField.setManaged(show);
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);
        }
        if (togglePasswordButton != null) {
            togglePasswordButton.setText(show ? "🙈" : "👁");
        }
    }

    private String getPasswordInput() {
        if (passwordVisibleField != null && passwordVisibleField.isVisible()) {
            return passwordVisibleField.getText();
        }
        return passwordField != null ? passwordField.getText() : "";
    }

    private Optional<User> authenticateAdmin(String identifier, String rawPassword) throws SQLException {
        var userService = NavigationManager.getInstance().userService();
        Optional<User> candidate = EMAIL_SIMPLE.matcher(identifier).matches()
                ? userService.findByEmail(identifier.toLowerCase(Locale.ROOT))
                : userService.findByUsername(identifier);

        if (candidate.isPresent() && isAdminAccount(candidate.get())
                && PasswordHasher.matches(rawPassword, candidate.get().getPassword())) {
            return userService.login(candidate.get().getEmail(), rawPassword)
                    .filter(this::isAdminAccount);
        }

        if (LEGACY_ADMIN_USERNAME.equals(identifier) && LEGACY_ADMIN_PASSWORD.equals(rawPassword)) {
            return Optional.of(buildLegacyAdminUser());
        }
        return Optional.empty();
    }

    private boolean isAdminAccount(User user) {
        if (user == null) {
            return false;
        }
        if (isRoleAdminToken(user.getRole())) {
            return true;
        }
        List<String> roles = user.getRoles();
        if (roles == null) {
            return false;
        }
        for (String role : roles) {
            if (isRoleAdminToken(role)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRoleAdminToken(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return false;
        }
        String normalized = rawRole.trim().toUpperCase(Locale.ROOT);
        return normalized.equals(UserRole.ADMIN.getValue()) || normalized.equals("ADMIN");
    }

    private User buildLegacyAdminUser() {
        User user = new User();
        user.setUsername(LEGACY_ADMIN_USERNAME);
        user.setEmail("admin@smartvoyage.local");
        user.setRole(UserRole.ADMIN.getValue());
        user.setRoles(List.of(UserRole.ADMIN.getValue(), UserRole.AGENCY_ADMIN.getValue(), UserRole.USER.getValue()));
        user.setIsActive(true);
        return user;
    }
}
