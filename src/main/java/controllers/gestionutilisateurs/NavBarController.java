package controllers.gestionutilisateurs;

import utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Barre de navigation commune (integration Smart Voyage). Ouvrir {@code fxml/components/app_navbar.fxml}
 * seul dans Scene Builder pour l’éditer ; le parent appelle {@link #initForPage(Page)} dans {@code initialize()}.
 */
public class NavBarController {

    public enum Page {
        WELCOME,
        LOGIN,
        REGISTER
    }

    @FXML
    private Button btnConnexion;

    @FXML
    private Button btnInscription;

    /**
     * Met en avant Connexion (page login) ou Inscription (page register).
     */
    public void initForPage(Page page) {
        btnConnexion.getStyleClass().remove("nav-outline-active");
        btnInscription.getStyleClass().remove("nav-inscription-nav-active");
        switch (page) {
            case LOGIN -> btnConnexion.getStyleClass().add("nav-outline-active");
            case REGISTER -> btnInscription.getStyleClass().add("nav-inscription-nav-active");
            default -> {
            }
        }
    }

    @FXML
    private void onHome() {
        NavigationManager.getInstance().showWelcome();
    }

    @FXML
    private void onConnexion() {
        NavigationManager.getInstance().showLogin();
    }

    @FXML
    private void onInscription() {
        NavigationManager.getInstance().showRegister();
    }

    @FXML
    private void onPremium() {
        /* réservé métier premium */
    }

    @FXML
    private void onSettings() {
        /* réservé paramètres */
    }

    @FXML
    private void onOffres() {
        /* réservé navigation offres */
    }

    @FXML
    private void onFeedbacks() {
        /* réservé */
    }

    @FXML
    private void onCrew() {
        /* réservé */
    }
}
