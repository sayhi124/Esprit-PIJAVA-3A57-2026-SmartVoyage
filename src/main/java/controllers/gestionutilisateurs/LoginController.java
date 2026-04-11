package controllers.gestionutilisateurs;

import utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import models.gestionutilisateurs.User;

import java.sql.SQLException;
import java.util.Optional;

public class LoginController {

    @FXML
    private NavBarController appNavbarController;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        appNavbarController.initForPage(NavBarController.Page.LOGIN);
    }

    @FXML
    private void onLogin() {
        messageLabel.setText("");
        messageLabel.getStyleClass().removeAll("error-message", "success-message");
        try {
            Optional<User> user = NavigationManager.getInstance().userService().login(
                    emailField.getText(),
                    passwordField.getText()
            );
            if (user.isPresent()) {
                User u = user.get();
                NavigationManager.getInstance().setSessionUser(u);
                NavigationManager.getInstance().showAgencies();
            } else {
                messageLabel.getStyleClass().add("error-message");
                messageLabel.setText("Email ou mot de passe incorrect.");
            }
        } catch (SQLException e) {
            messageLabel.getStyleClass().add("error-message");
            messageLabel.setText("Erreur base de donnees : " + e.getMessage());
        }
    }

    @FXML
    private void onBack() {
        NavigationManager.getInstance().showWelcome();
    }

    @FXML
    private void onGoRegister() {
        NavigationManager.getInstance().showRegister();
    }
}
