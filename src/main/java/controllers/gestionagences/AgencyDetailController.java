package controllers.gestionagences;

import utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import models.gestionagences.AgencyAccount;

import java.sql.SQLException;

/**
 * Détail agence simplifié (integration {@code agency/show.html.twig}).
 */
public class AgencyDetailController {

    @FXML
    private Label nameLabel;

    @FXML
    private Label verifiedLabel;

    @FXML
    private Label descriptionLabel;

    @FXML
    private void initialize() {
        Long id = NavigationManager.getInstance().consumeRoutedAgencyId();
        if (id == null) {
            NavigationManager.getInstance().showAgencies();
            return;
        }
        try {
            var opt = NavigationManager.getInstance().agencyAccountService().get(id);
            if (opt.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Agence introuvable.").showAndWait();
                NavigationManager.getInstance().showAgencies();
                return;
            }
            AgencyAccount a = opt.get();
            nameLabel.setText(a.getAgencyName());
            descriptionLabel.setText(a.getDescription() != null ? a.getDescription() : "");
            if (Boolean.TRUE.equals(a.getVerified())) {
                verifiedLabel.setText("✓ Verified Agency");
                verifiedLabel.getStyleClass().setAll("agency-verified", "agency-verified-on");
            } else {
                verifiedLabel.setText("En attente de vérification");
                verifiedLabel.getStyleClass().setAll("agency-verified", "agency-verified-off");
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            NavigationManager.getInstance().showAgencies();
        }
    }

    @FXML
    private void onBack() {
        NavigationManager.getInstance().showAgencies();
    }
}
