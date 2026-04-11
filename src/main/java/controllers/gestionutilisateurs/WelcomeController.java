package controllers.gestionutilisateurs;

import utils.NavigationManager;
import javafx.fxml.FXML;

public class WelcomeController {

    @FXML
    private NavBarController appNavbarController;

    @FXML
    private void initialize() {
        appNavbarController.initForPage(NavBarController.Page.WELCOME);
    }

    @FXML
    private void onSignIn() {
        NavigationManager.getInstance().showLogin();
    }

    @FXML
    private void onSignUp() {
        NavigationManager.getInstance().showRegister();
    }
}
