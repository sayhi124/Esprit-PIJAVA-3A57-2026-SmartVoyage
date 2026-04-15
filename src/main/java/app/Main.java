package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.NavigationManager;
import services.gestionagences.AgencyAccountService;
import services.gestionagences.AgencyAdminApplicationService;
import services.gestionutilisateurs.UserService;
import utils.DbConnexion;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        UserService userService = new UserService();
        AgencyAccountService agencyAccountService = new AgencyAccountService();
        AgencyAdminApplicationService agencyAdminApplicationService =
                new AgencyAdminApplicationService(userService, agencyAccountService);
        NavigationManager.getInstance().configure(stage, userService, agencyAccountService, agencyAdminApplicationService);

        var welcomeUrl = Main.class.getResource("/fxml/user/welcome.fxml");
        Objects.requireNonNull(welcomeUrl,
                "Missing /fxml/user/welcome.fxml — run Maven Compile so resources are in target/classes, or fix IDE output path.");
        Parent root = FXMLLoader.load(welcomeUrl);
        Scene scene = new Scene(root);
        var css = Main.class.getResource("/css/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.setTitle("Smart Voyage");
        stage.setMaximized(true);
        stage.setMinWidth(960);
        stage.setMinHeight(600);
        stage.setOnCloseRequest(e -> DbConnexion.shutdown());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
