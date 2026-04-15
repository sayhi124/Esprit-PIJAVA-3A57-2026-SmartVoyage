package controllers.gestionagences;

import utils.NavigationManager;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import models.gestionagences.AgencyAccount;
import models.gestionagences.AgencyAdminApplication;
import models.gestionutilisateurs.User;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Admin : demandes d'agrément + liste des agences (integration {@code admin/agency_apps.html.twig}).
 * Données affichées directement depuis les entités ; libellés / aperçus calculés côté UI.
 */
public class AdminAgenciesController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Résolu au refresh pour éviter des accès SQL dans chaque cellule. */
    private final Map<Integer, String> userDisplayById = new HashMap<>();

    @FXML
    private TableView<AgencyAdminApplication> pendingTable;

    @FXML
    private TableView<AgencyAccount> agenciesTable;

    @FXML
    private void initialize() {
        if (!NavigationManager.getInstance().isAdmin()) {
            NavigationManager.getInstance().showAgencies();
            return;
        }
        buildPendingColumns();
        buildAgenciesColumns();
        refreshAll();
    }

    private void buildPendingColumns() {
        pendingTable.getColumns().clear();
        TableColumn<AgencyAdminApplication, Long> cId = new TableColumn<>("#");
        cId.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getId()));
        cId.setPrefWidth(50);
        TableColumn<AgencyAdminApplication, String> cName = new TableColumn<>("Nom de l'agence");
        cName.setCellValueFactory(cd -> {
            String n = cd.getValue().getAgencyNameRequested();
            return new ReadOnlyStringWrapper(n != null && !n.isBlank() ? n : "—");
        });
        cName.setPrefWidth(160);
        TableColumn<AgencyAdminApplication, String> cReq = new TableColumn<>("Demandeur");
        cReq.setCellValueFactory(cd -> {
            Integer uid = cd.getValue().getApplicantId();
            String label = uid == null ? "—" : userDisplayById.getOrDefault(uid, "#" + uid);
            return new ReadOnlyStringWrapper(label);
        });
        cReq.setPrefWidth(120);
        TableColumn<AgencyAdminApplication, String> cDate = new TableColumn<>("Date de demande");
        cDate.setCellValueFactory(cd -> new ReadOnlyStringWrapper(fmt(cd.getValue().getRequestedAt())));
        cDate.setPrefWidth(140);
        TableColumn<AgencyAdminApplication, String> cMsg = new TableColumn<>("Message");
        cMsg.setCellValueFactory(cd -> new ReadOnlyStringWrapper(messagePreview(cd.getValue().getMessageToAdmin())));
        cMsg.setPrefWidth(220);
        TableColumn<AgencyAdminApplication, Void> cAct = new TableColumn<>("Actions");
        cAct.setPrefWidth(220);
        cAct.setCellFactory(col -> new TableCell<>() {
            private final Button approve = new Button("✓ Approuver");
            private final Button reject = new Button("✕ Rejeter");
            private final HBox box = new HBox(8, approve, reject);

            {
                approve.getStyleClass().add("admin-btn-approve");
                reject.getStyleClass().add("admin-btn-reject");
                approve.setOnAction(e -> {
                    AgencyAdminApplication row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row != null && row.getId() != null) {
                        confirmApprove(row.getId());
                    }
                });
                reject.setOnAction(e -> {
                    AgencyAdminApplication row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row != null && row.getId() != null) {
                        confirmReject(row.getId());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
        pendingTable.getColumns().addAll(cId, cName, cReq, cDate, cMsg, cAct);
    }

    private void buildAgenciesColumns() {
        agenciesTable.getColumns().clear();
        TableColumn<AgencyAccount, Long> cId = new TableColumn<>("#");
        cId.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getId()));
        cId.setPrefWidth(50);
        TableColumn<AgencyAccount, String> cName = new TableColumn<>("Nom");
        cName.setCellValueFactory(cd -> {
            String n = cd.getValue().getAgencyName();
            return new ReadOnlyStringWrapper(n != null && !n.isBlank() ? n : "—");
        });
        cName.setPrefWidth(160);
        TableColumn<AgencyAccount, String> cResp = new TableColumn<>("Responsable");
        cResp.setCellValueFactory(cd -> {
            Integer uid = cd.getValue().getResponsableId();
            String label = uid == null ? "—" : userDisplayById.getOrDefault(uid, "#" + uid);
            return new ReadOnlyStringWrapper(label);
        });
        cResp.setPrefWidth(120);
        TableColumn<AgencyAccount, String> cVer = new TableColumn<>("Vérifiée");
        cVer.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                Boolean.TRUE.equals(cd.getValue().getVerified()) ? "Vérifiée" : "En attente"));
        cVer.setPrefWidth(100);
        TableColumn<AgencyAccount, String> cCr = new TableColumn<>("Créée le");
        cCr.setCellValueFactory(cd -> new ReadOnlyStringWrapper(fmt(cd.getValue().getCreatedAt())));
        cCr.setPrefWidth(130);
        TableColumn<AgencyAccount, String> cUp = new TableColumn<>("Mis à jour");
        cUp.setCellValueFactory(cd -> new ReadOnlyStringWrapper(fmt(cd.getValue().getUpdatedAt())));
        cUp.setPrefWidth(130);
        TableColumn<AgencyAccount, Void> cAct = new TableColumn<>("Actions");
        cAct.setPrefWidth(120);
        cAct.setCellFactory(col -> new TableCell<>() {
            private final Button del = new Button("✕ Supprimer");

            {
                del.getStyleClass().add("admin-btn-reject");
                del.setOnAction(e -> {
                    AgencyAccount row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row != null && row.getId() != null) {
                        confirmDeleteAgency(row.getId());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(del);
                }
            }
        });
        agenciesTable.getColumns().addAll(cId, cName, cResp, cVer, cCr, cUp, cAct);
    }

    private void refreshAll() {
        try {
            List<AgencyAdminApplication> pending = NavigationManager.getInstance().agencyAdminApplicationService().findPending();
            List<AgencyAccount> agencies = NavigationManager.getInstance().agencyAccountService().findAll();
            rebuildUserDisplayCache(pending, agencies);
            pendingTable.setItems(FXCollections.observableArrayList(pending));
            agenciesTable.setItems(FXCollections.observableArrayList(agencies));
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void rebuildUserDisplayCache(List<AgencyAdminApplication> pending, List<AgencyAccount> agencies)
            throws SQLException {
        userDisplayById.clear();
        Set<Integer> ids = new HashSet<>();
        for (AgencyAdminApplication app : pending) {
            if (app.getApplicantId() != null) {
                ids.add(app.getApplicantId());
            }
        }
        for (AgencyAccount a : agencies) {
            if (a.getResponsableId() != null) {
                ids.add(a.getResponsableId());
            }
        }
        for (Integer id : ids) {
            userDisplayById.put(id, resolveUsername(id));
        }
    }

    private String resolveUsername(Integer userId) throws SQLException {
        if (userId == null) {
            return "—";
        }
        Optional<User> u = NavigationManager.getInstance().userService().get(userId);
        return u.map(User::getUsername).orElse("#" + userId);
    }

    private static String messagePreview(String msg) {
        if (msg == null || msg.isBlank()) {
            return "–";
        }
        return msg.length() > 60 ? msg.substring(0, 60) + "…" : msg;
    }

    private String fmt(java.time.LocalDateTime t) {
        return t == null ? "–" : t.format(DT);
    }

    private void confirmApprove(long applicationId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText("Approuver cette demande d'agence ?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }
        Integer adminId = NavigationManager.getInstance().currentUserId();
        try {
            NavigationManager.getInstance().agencyAdminApplicationService().approve(applicationId, adminId);
            refreshAll();
            new Alert(Alert.AlertType.INFORMATION, "Demande approuvée.").showAndWait();
        } catch (SQLException | IllegalArgumentException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void confirmReject(long applicationId) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rejet");
        dialog.setHeaderText("Motif du rejet (optionnel)");
        dialog.setContentText("Note :");
        Optional<String> note = dialog.showAndWait();
        if (note.isEmpty()) {
            return;
        }
        Integer adminId = NavigationManager.getInstance().currentUserId();
        try {
            String n = note.get().trim();
            NavigationManager.getInstance().agencyAdminApplicationService().reject(applicationId, adminId, n.isEmpty() ? null : n);
            refreshAll();
            new Alert(Alert.AlertType.INFORMATION, "Demande rejetée.").showAndWait();
        } catch (SQLException | IllegalArgumentException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void confirmDeleteAgency(long agencyId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer cette agence ?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }
        try {
            NavigationManager.getInstance().agencyAccountService().delete(agencyId);
            refreshAll();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onNavAgencies() {
        NavigationManager.getInstance().showAgencies();
    }

    @FXML
    private void onLogout() {
        NavigationManager.getInstance().clearSession();
        NavigationManager.getInstance().showWelcome();
    }
}
