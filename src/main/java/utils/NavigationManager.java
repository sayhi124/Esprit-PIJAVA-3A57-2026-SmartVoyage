package utils;

import enums.gestionutilisateurs.UserRole;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.gestionutilisateurs.User;
import services.gestionagences.AgencyAccountService;
import services.gestionagences.AgencyAdminApplicationService;
import services.gestionutilisateurs.UserService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/**
 * Point central de navigation FXML + session + services (équivalent pratique d'un {@code NavigationManager} type PIDEV).
 */
public class NavigationManager {

    private static NavigationManager instance;

    private Stage stage;
    private UserService userService;
    private AgencyAccountService agencyAccountService;
    private AgencyAdminApplicationService agencyAdminApplicationService;
    private User sessionUser;
    private Long routedAgencyId;

    private NavigationManager() {
    }

    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    /**
     * À appeler une fois depuis {@link app.Main} après création des services.
     */
    public void configure(Stage primaryStage,
                          UserService userSvc,
                          AgencyAccountService agencySvc,
                          AgencyAdminApplicationService agencyAppSvc) {
        this.stage = primaryStage;
        this.userService = userSvc;
        this.agencyAccountService = agencySvc;
        this.agencyAdminApplicationService = agencyAppSvc;
    }

    public UserService userService() {
        return userService;
    }

    public AgencyAccountService agencyAccountService() {
        return agencyAccountService;
    }

    public AgencyAdminApplicationService agencyAdminApplicationService() {
        return agencyAdminApplicationService;
    }

    public Optional<User> sessionUser() {
        return Optional.ofNullable(sessionUser);
    }

    public void setSessionUser(User source) {
        if (source == null) {
            sessionUser = null;
            return;
        }
        User copy = new User();
        copy.setId(source.getId());
        copy.setUsername(source.getUsername());
        copy.setEmail(source.getEmail());
        copy.setRoles(source.getRoles());
        copy.setRole(source.getRole());
        copy.setIsActive(source.getIsActive());
        sessionUser = copy;
    }

    public void clearSession() {
        sessionUser = null;
        routedAgencyId = null;
    }

    public boolean isAdmin() {
        return sessionUser != null
                && sessionUser.getRoles() != null
                && sessionUser.getRoles().contains(UserRole.ADMIN.getValue());
    }

    public boolean isAgencyAdmin() {
        return sessionUser != null
                && sessionUser.getRoles() != null
                && sessionUser.getRoles().contains(UserRole.AGENCY_ADMIN.getValue());
    }

    public Integer currentUserId() {
        return sessionUser != null ? sessionUser.getId() : null;
    }

    public void setRoutedAgencyId(Long id) {
        routedAgencyId = id;
    }

    public Long consumeRoutedAgencyId() {
        Long v = routedAgencyId;
        routedAgencyId = null;
        return v;
    }

    public void showWelcome() {
        loadScene("/fxml/user/welcome.fxml", "Smart Voyage");
    }

    public void showLogin() {
        loadScene("/fxml/user/login.fxml", "Connexion");
    }

    public void showRegister() {
        loadScene("/fxml/user/register.fxml", "Inscription");
    }

    public void showAgencies() {
        if (sessionUser == null) {
            showLogin();
            return;
        }
        loadScene("/fxml/agency/agencies_list.fxml", "Agences | Smart Voyage");
    }

    public void showAgencyDetail(long agencyId) {
        if (sessionUser == null) {
            showLogin();
            return;
        }
        routedAgencyId = agencyId;
        loadScene("/fxml/agency/agency_detail.fxml", "Agence | Smart Voyage");
    }

    public void showAgencyRequest() {
        if (sessionUser == null) {
            showLogin();
            return;
        }
        loadScene("/fxml/agency/agency_request.fxml", "Demande d'agrément agence");
    }

    public void showAdminAgencies() {
        if (sessionUser == null) {
            showLogin();
            return;
        }
        if (!isAdmin()) {
            showAgencies();
            return;
        }
        loadScene("/fxml/admin/admin_agencies.fxml", "Demandes d'agence | Admin");
    }

    public void showMyAgency() {
        if (sessionUser == null) {
            showLogin();
            return;
        }
        try {
            var opt = agencyAccountService.findByResponsableId(sessionUser.getId());
            if (opt.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setHeaderText(null);
                a.setContentText("Aucune agence n'est encore associée à votre compte.");
                a.showAndWait();
                return;
            }
            showAgencyDetail(opt.get().getId());
        } catch (SQLException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }

    private void loadScene(String resource, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(NavigationManager.class.getResource(resource)));
            Parent root = loader.load();
            applyScene(root, title);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger " + resource, e);
        }
    }

    private void applyScene(Parent root, String title) {
        final boolean wasMaximized = stage.isMaximized();
        final double w = stage.getWidth();
        final double h = stage.getHeight();

        Scene scene = new Scene(root);
        var css = NavigationManager.class.getResource("/css/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.setTitle(title);
        if (wasMaximized) {
            // Replacing the scene often drops maximized on Windows; restore after attach.
            Platform.runLater(() -> stage.setMaximized(true));
        } else if (w > 0 && h > 0) {
            stage.setWidth(w);
            stage.setHeight(h);
        }
        stage.show();
    }
}
