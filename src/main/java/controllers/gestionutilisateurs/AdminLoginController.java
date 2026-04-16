package controllers.gestionutilisateurs;

import atlantafx.base.theme.PrimerDark;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import utils.NavigationManager;

public class AdminLoginController {
    private static final String ADMIN_IMAGE = "/images/admin/admin.jpg";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    @FXML
    private BorderPane authRoot;
    @FXML
    private StackPane authMediaStack;
    @FXML
    private ImageView authBackgroundImage;
    @FXML
    private ImageView adminImageView;

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
        if (authBackgroundImage != null && authMediaStack != null) {
            authBackgroundImage.fitWidthProperty().bind(authMediaStack.widthProperty());
            authBackgroundImage.fitHeightProperty().bind(authMediaStack.heightProperty());
            loadBundledImage(authBackgroundImage, ADMIN_IMAGE, 1920, 1080);
        }
        if (passwordField != null && passwordVisibleField != null) {
            passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        }
        // Load admin side image
        if (adminImageView != null) {
            loadBundledImage(adminImageView, "/images/admin/admin.jpg", 600, 800);
        }
        if (authRoot != null) {
            authRoot.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(320), authRoot);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        }
    }

    private static void loadBundledImage(ImageView view, String classpathPath, double w, double h) {
        if (view == null || classpathPath == null || classpathPath.isBlank()) {
            return;
        }
        var url = AdminLoginController.class.getResource(classpathPath);
        if (url == null) {
            System.err.println("ERROR: Could not find image: " + classpathPath);
            return;
        }
        // Load image without background loading to ensure it's ready immediately
        Image img = new Image(url.toExternalForm(), w, h, false, false, false);
        if (img.isError()) {
            System.err.println("ERROR loading image: " + img.getException().getMessage());
            return;
        }
        view.setImage(img);
        view.setSmooth(true);
        view.setCache(false); // Disable caching to ensure fresh load
        System.out.println("Loaded admin image: " + img.getWidth() + "x" + img.getHeight());
    }

    private void installLibraryTheme() {
        String current = Application.getUserAgentStylesheet();
        String primer = new PrimerDark().getUserAgentStylesheet();
        if (current == null || current.isBlank() || !current.equals(primer)) {
            Application.setUserAgentStylesheet(primer);
        }
    }

    @FXML
    private void onLogin() {
        messageLabel.getStyleClass().removeAll("message-error", "message-success");
        messageLabel.setText("");

        String username = usernameField != null ? usernameField.getText() : "";
        String password = getPasswordInput();

        if (username.trim().isEmpty() || password.trim().isEmpty()) {
            messageLabel.getStyleClass().add("message-error");
            messageLabel.setText("Please enter both username and password.");
            return;
        }

        if (ADMIN_USERNAME.equals(username.trim()) && ADMIN_PASSWORD.equals(password)) {
            messageLabel.getStyleClass().add("message-success");
            messageLabel.setText("Login successful! Redirecting...");
            NavigationManager.getInstance().showAdminDashboard();
        } else {
            messageLabel.getStyleClass().add("message-error");
            messageLabel.setText("Invalid admin credentials.");
        }
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
}
