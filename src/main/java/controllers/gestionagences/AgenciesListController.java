package controllers.gestionagences;

import utils.NavigationManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.gestionagences.AgencyAccount;

import java.sql.SQLException;

/**
 * Liste des agences (integration {@code agency/list.html.twig}).
 */
public class AgenciesListController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> countryFilter;

    @FXML
    private FlowPane cardsPane;

    @FXML
    private Label countLabel;

    @FXML
    private void initialize() {
        countryFilter.setItems(FXCollections.observableArrayList(
                "Tous les pays", "TN", "FR", "IT", "MA", "DZ", "DE", "ES"));
        countryFilter.getSelectionModel().selectFirst();
        reload();
        searchField.textProperty().addListener((o, a, b) -> reload());
        countryFilter.valueProperty().addListener((o, a, b) -> reload());
    }

    private void reload() {
        try {
            String q = searchField.getText();
            String cc = countryFilter.getValue();
            if (cc == null || cc.equals("Tous les pays")) {
                cc = null;
            }
            var list = NavigationManager.getInstance().agencyAccountService().findFiltered(q, cc);
            cardsPane.getChildren().clear();
            for (AgencyAccount a : list) {
                cardsPane.getChildren().add(buildCard(a));
            }
            countLabel.setText(list.size() + " AGENCES TROUVÉES");
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private VBox buildCard(AgencyAccount a) {
        VBox card = new VBox(12);
        card.getStyleClass().add("agency-card");
        card.setAlignment(Pos.TOP_LEFT);

        StackPane img = new StackPane();
        img.getStyleClass().add("agency-card-image");
        img.setPrefHeight(160);
        String name = a.getAgencyName() != null ? a.getAgencyName() : "";
        String initial = !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "?";
        Label av = new Label(initial);
        av.getStyleClass().add("agency-card-initial");
        img.getChildren().add(av);

        Label title = new Label(name);
        title.getStyleClass().add("agency-card-title");
        Label desc = new Label(truncate(a.getDescription(), 160));
        desc.setWrapText(true);
        desc.getStyleClass().add("agency-card-meta");

        Button go = new Button("Voir le profil →");
        go.getStyleClass().add("agency-card-btn");
        go.setMaxWidth(Double.MAX_VALUE);
        go.setOnAction(e -> NavigationManager.getInstance().showAgencyDetail(a.getId()));

        card.getChildren().addAll(img, title, desc, go);
        return card;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    @FXML
    private void onMapStub() {
        new Alert(Alert.AlertType.INFORMATION, "Carte : prochaine étape (équivalent agency_map).").showAndWait();
    }

    @FXML
    private void onMyAgency() {
        NavigationManager.getInstance().showMyAgency();
    }
}
