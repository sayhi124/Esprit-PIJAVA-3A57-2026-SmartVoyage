package controllers.gestionagences;

import utils.NavigationManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import models.gestionagences.AgencyAdminApplication;

import java.sql.SQLException;

/**
 * Formulaire demande d'agrément (integration {@code agency/request.html.twig}).
 */
public class AgencyRequestController {

    @FXML
    private TextField agencyNameField;

    @FXML
    private ComboBox<String> countryCombo;

    @FXML
    private TextArea messageArea;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        countryCombo.setItems(FXCollections.observableArrayList(
                "TN", "FR", "IT", "MA", "DZ", "DE", "ES", "US", "CA", "GB"));
    }

    @FXML
    private void onSubmit() {
        messageLabel.setText("");
        messageLabel.getStyleClass().removeAll("error-message", "success-message");
        Integer uid = NavigationManager.getInstance().currentUserId();
        if (uid == null) {
            NavigationManager.getInstance().showLogin();
            return;
        }
        String name = agencyNameField.getText() != null ? agencyNameField.getText().trim() : "";
        if (name.isEmpty()) {
            messageLabel.getStyleClass().add("error-message");
            messageLabel.setText("Le nom de l'agence est obligatoire.");
            return;
        }
        AgencyAdminApplication app = new AgencyAdminApplication();
        app.setApplicantId(uid);
        app.setAgencyNameRequested(name);
        String country = countryCombo.getSelectionModel().getSelectedItem();
        app.setCountry(country);
        String msg = messageArea.getText() != null ? messageArea.getText().trim() : "";
        app.setMessageToAdmin(msg.isEmpty() ? null : msg);
        try {
            NavigationManager.getInstance().agencyAdminApplicationService().submit(app);
            messageLabel.getStyleClass().add("success-message");
            messageLabel.setText("Demande envoyée. Un administrateur l'examinera.");
            agencyNameField.clear();
            messageArea.clear();
            countryCombo.getSelectionModel().clearSelection();
        } catch (IllegalArgumentException e) {
            messageLabel.getStyleClass().add("error-message");
            messageLabel.setText(e.getMessage());
        } catch (SQLException e) {
            messageLabel.getStyleClass().add("error-message");
            messageLabel.setText("Erreur base : " + e.getMessage());
        }
    }

    @FXML
    private void onBack() {
        NavigationManager.getInstance().showAgencies();
    }
}
