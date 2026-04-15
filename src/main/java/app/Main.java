package app;

import javafx.application.Application;
import javafx.stage.Stage;
import utils.NavigationManager;
import services.gestionutilisateurs.UserService;
import utils.DbConnexion;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        UserService userService = new UserService();
        NavigationManager.getInstance().configure(stage, userService);
        stage.setOnCloseRequest(e -> DbConnexion.shutdown());
        NavigationManager.getInstance().showWelcome();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
