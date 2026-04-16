package controllers.gestionutilisateurs;

import atlantafx.base.theme.PrimerDark;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import enums.gestionutilisateurs.UserRole;
import models.gestionutilisateurs.User;
import utils.NavigationManager;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class RegisterController {
    private static final String HERO_IMAGE = "/images/welcome/hero-aerial-lagoon.jpg";
    private static final Pattern EMAIL_SIMPLE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @FXML
    private BorderPane authRoot;
    @FXML
    private StackPane authMediaStack;
    @FXML
    private ImageView authBackgroundImage;
    @FXML
    private Region parallaxOrbBack;
    @FXML
    private Region parallaxOrbFront;
    @FXML
    private VBox parallaxHeroContent;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordVisibleField;
    @FXML
    private Button togglePasswordButton;
    @FXML
    private ComboBox<String> accountTypeCombo;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        installLibraryTheme();
        if (authBackgroundImage != null && authMediaStack != null) {
            authBackgroundImage.fitWidthProperty().bind(authMediaStack.widthProperty());
            authBackgroundImage.fitHeightProperty().bind(authMediaStack.heightProperty());
            loadBundledImage(authBackgroundImage, HERO_IMAGE, 1920, 1080);
        }
        if (passwordField != null && passwordVisibleField != null) {
            passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        }
        if (accountTypeCombo != null) {
            accountTypeCombo.getItems().setAll("Traveler", "Agency Admin");
            accountTypeCombo.getSelectionModel().select("Traveler");
        }
        if (authRoot != null) {
            authRoot.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(320), authRoot);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
            authRoot.setOnMouseMoved(event -> {
                double width = Math.max(authRoot.getWidth(), 1);
                double height = Math.max(authRoot.getHeight(), 1);
                double offsetX = (event.getSceneX() / width) - 0.5;
                double offsetY = (event.getSceneY() / height) - 0.5;
                if (parallaxOrbBack != null) {
                    parallaxOrbBack.setTranslateX(offsetX * 30);
                    parallaxOrbBack.setTranslateY(offsetY * 22);
                }
                if (parallaxOrbFront != null) {
                    parallaxOrbFront.setTranslateX(offsetX * -18);
                    parallaxOrbFront.setTranslateY(offsetY * -12);
                }
                if (parallaxHeroContent != null) {
                    parallaxHeroContent.setTranslateX(offsetX * 10);
                    parallaxHeroContent.setTranslateY(offsetY * 7);
                }
            });
            authRoot.setOnMouseExited(event -> resetParallax());
        }
    }

    private static void loadBundledImage(ImageView view, String classpathPath, double w, double h) {
        if (view == null || classpathPath == null || classpathPath.isBlank()) {
            return;
        }
        var url = RegisterController.class.getResource(classpathPath);
        if (url == null) {
            return;
        }
        Image img = new Image(url.toExternalForm(), w, h, false, true, true);
        view.setImage(img);
        view.setSmooth(true);
        view.setCache(true);
    }

    private void installLibraryTheme() {
        String current = Application.getUserAgentStylesheet();
        String primer = new PrimerDark().getUserAgentStylesheet();
        if (current == null || current.isBlank() || !current.equals(primer)) {
            Application.setUserAgentStylesheet(primer);
        }
    }

    private void resetParallax() {
        if (parallaxOrbBack != null) {
            parallaxOrbBack.setTranslateX(0);
            parallaxOrbBack.setTranslateY(0);
        }
        if (parallaxOrbFront != null) {
            parallaxOrbFront.setTranslateX(0);
            parallaxOrbFront.setTranslateY(0);
        }
        if (parallaxHeroContent != null) {
            parallaxHeroContent.setTranslateX(0);
            parallaxHeroContent.setTranslateY(0);
        }
    }

    @FXML
    private void onRegister() {
        messageLabel.getStyleClass().removeAll("message-error", "message-success");
        messageLabel.setText("");
        String username = usernameField == null ? "" : usernameField.getText().trim();
        String email = emailField == null ? "" : emailField.getText().trim();
        String password = getPasswordInput() == null ? "" : getPasswordInput().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            messageLabel.getStyleClass().add("message-error");
            if (username.isEmpty() && email.isEmpty() && password.isEmpty()) {
                messageLabel.setText("Username, email, and password are required.");
            } else if (username.isEmpty()) {
                messageLabel.setText("Username is required.");
            } else if (email.isEmpty()) {
                messageLabel.setText("Email is required.");
            } else {
                messageLabel.setText("Password is required.");
            }
            return;
        }
        if (!EMAIL_SIMPLE.matcher(email).matches()) {
            messageLabel.getStyleClass().add("message-error");
            messageLabel.setText("Invalid email format.");
            return;
        }

        try {
            UserRole selectedRole = "Agency Admin".equals(accountTypeCombo.getValue())
                    ? UserRole.AGENCY_ADMIN
                    : UserRole.USER;
            User created = NavigationManager.getInstance().userService().signUp(
                    username,
                    email,
                    password,
                    selectedRole
            );
            messageLabel.getStyleClass().add("message-success");
            messageLabel.setText("Account created successfully (id = " + created.getId() + ").");
            NavigationManager.getInstance().showLogin();
        } catch (IllegalArgumentException e) {
            messageLabel.getStyleClass().add("message-error");
            messageLabel.setText(e.getMessage());
        } catch (SQLException e) {
            messageLabel.getStyleClass().add("message-error");
            messageLabel.setText("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void onBack() {
        NavigationManager.getInstance().showWelcome();
    }

    @FXML
    private void onGoLogin() {
        NavigationManager.getInstance().showLogin();
    }

    @FXML
    private void onHome() {
        NavigationManager.getInstance().showWelcome();
    }

    @FXML
    private void onOffres() {
        NavigationManager.getInstance().showGuestOffers();
    }

    @FXML
    private void onFeedbacks() {
        NavigationManager.getInstance().showGuestFeedbacks();
    }

    @FXML
    private void onCrew() {
        NavigationManager.getInstance().showGuestCrew();
    }

    @FXML
    private void onPremium() {
        // Placeholder-safe in guest mode.
    }

    @FXML
    private void onThemeToggle() {
        NavigationManager.getInstance().toggleTheme();
    }

    @FXML
    private void onSignIn() {
        NavigationManager.getInstance().showLogin();
    }

    @FXML
    private void onSignUp() {
        // Already on sign up.
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
}
