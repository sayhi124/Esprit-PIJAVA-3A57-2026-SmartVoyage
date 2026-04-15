package controllers.gestionutilisateurs;

import utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import models.gestionutilisateurs.User;

import java.sql.SQLException;

public class RegisterController {

    @FXML
    private NavBarController appNavbarController;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        appNavbarController.initForPage(NavBarController.Page.REGISTER);
    }

    @FXML
    private void onRegister() {
        messageLabel.setText("");
        messageLabel.getStyleClass().removeAll("error-message", "success-message");
        try {
            User created = NavigationManager.getInstance().userService().signUp(
                    usernameField.getText(),
                    emailField.getText(),
                    passwordField.getText()
            );
            messageLabel.getStyleClass().add("success-message");
            messageLabel.setText("Compte cree (id = " + created.getId() + "). Vous pouvez vous connecter.");
        } catch (IllegalArgumentException e) {
            messageLabel.getStyleClass().add("error-message");
            messageLabel.setText(e.getMessage());
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
    private void onGoLogin() {
        NavigationManager.getInstance().showLogin();
    }
}
