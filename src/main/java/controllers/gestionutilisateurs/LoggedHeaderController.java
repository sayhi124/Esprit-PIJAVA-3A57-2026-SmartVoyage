package controllers.gestionutilisateurs;

import utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Bandeau navigation utilisateur connecté (réf. integration base.html.twig).
 * Éditer {@code fxml/components/app_logged_header.fxml} dans Scene Builder.
 */
public class LoggedHeaderController {

    @FXML
    private Label userLabel;

    @FXML
    private Button adminButton;

    @FXML
    private void initialize() {
        NavigationManager.getInstance().sessionUser().ifPresentOrElse(
                u -> userLabel.setText(u.getUsername()),
                () -> userLabel.setText("?"));
        boolean admin = NavigationManager.getInstance().isAdmin();
        adminButton.setVisible(admin);
        adminButton.setManaged(admin);
    }

    @FXML
    private void onAgencies() {
        NavigationManager.getInstance().showAgencies();
    }

    @FXML
    private void onRequestAgency() {
        NavigationManager.getInstance().showAgencyRequest();
    }

    @FXML
    private void onAdmin() {
        NavigationManager.getInstance().showAdminAgencies();
    }

    @FXML
    private void onLogout() {
        NavigationManager.getInstance().clearSession();
        NavigationManager.getInstance().showWelcome();
    }
}
