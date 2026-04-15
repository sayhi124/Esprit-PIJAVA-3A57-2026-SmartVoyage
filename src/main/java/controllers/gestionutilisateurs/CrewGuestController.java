package controllers.gestionutilisateurs;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import utils.NavigationManager;

import java.nio.file.Path;

public class CrewGuestController {

    @FXML
    private ImageView crewAvatar1;
    @FXML
    private ImageView crewAvatar2;
    @FXML
    private ImageView crewAvatar3;
    @FXML
    private ImageView crewAvatar4;
    @FXML
    private ImageView crewAvatar5;

    @FXML
    private Label crewName1;
    @FXML
    private Label crewName2;
    @FXML
    private Label crewName3;
    @FXML
    private Label crewName4;
    @FXML
    private Label crewName5;

    @FXML
    private Label crewRole1;
    @FXML
    private Label crewRole2;
    @FXML
    private Label crewRole3;
    @FXML
    private Label crewRole4;
    @FXML
    private Label crewRole5;

    @FXML
    private void initialize() {
        installLibraryTheme();
        applyCrew(crewAvatar1, crewName1, crewRole1, "Safa Gharbi", "Design & Tech",
                localImage("C:\\Users\\21650\\.cursor\\projects\\c-Users-21650-Downloads-DayFlow\\assets\\c__Users_21650_AppData_Roaming_Cursor_User_workspaceStorage_ad6a6de55e625a0cf132e51c9b38a177_images_Capture_d__cran_2026-04-14_143936-f0a260ea-096b-4626-9ebe-d5f42d9db38c.png"));
        applyCrew(crewAvatar2, crewName2, crewRole2, "Rayen Swayah", "Growth & Operations",
                localImage("C:\\Users\\21650\\.cursor\\projects\\c-Users-21650-Downloads-DayFlow\\assets\\c__Users_21650_AppData_Roaming_Cursor_User_workspaceStorage_ad6a6de55e625a0cf132e51c9b38a177_images_Capture_d__cran_2026-04-14_143834-838c117c-281a-435f-898b-9bf123ee943e.png"));
        applyCrew(crewAvatar3, crewName3, crewRole3, "Mohamed Dhia Rached", "Engineering",
                localImage("C:\\Users\\21650\\.cursor\\projects\\c-Users-21650-Downloads-DayFlow\\assets\\c__Users_21650_AppData_Roaming_Cursor_User_workspaceStorage_ad6a6de55e625a0cf132e51c9b38a177_images_Capture_d__cran_2026-04-14_143951-76981550-8527-4588-b799-d6f942970914.png"));
        applyCrew(crewAvatar4, crewName4, crewRole4, "Manel Ouni", "Product & Experience",
                localImage("C:\\Users\\21650\\.cursor\\projects\\c-Users-21650-Downloads-DayFlow\\assets\\c__Users_21650_AppData_Roaming_Cursor_User_workspaceStorage_ad6a6de55e625a0cf132e51c9b38a177_images_Capture_d__cran_2026-04-14_143904-f7a8a78d-9a77-4c70-8f8d-0988e99abcf8.png"));
        applyCrew(crewAvatar5, crewName5, crewRole5, "Mohamed Aziz Sayhi", "Platform & Support",
                localImage("C:\\Users\\21650\\.cursor\\projects\\c-Users-21650-Downloads-DayFlow\\assets\\c__Users_21650_AppData_Roaming_Cursor_User_workspaceStorage_ad6a6de55e625a0cf132e51c9b38a177_images_Capture_d__cran_2026-04-14_143848-c57d922c-bbb2-4b25-bdbe-416974118db7.png"));
    }

    private void installLibraryTheme() {
        String current = Application.getUserAgentStylesheet();
        String primer = new PrimerDark().getUserAgentStylesheet();
        if (current == null || current.isBlank() || !current.equals(primer)) {
            Application.setUserAgentStylesheet(primer);
        }
    }

    private void applyCrew(ImageView avatar, Label name, Label role, String nameText, String roleText, String avatarUrl) {
        Image image = new Image(avatarUrl, 1200, 1200, true, true, true);
        avatar.setImage(image);
        avatar.setSmooth(true);
        avatar.setCache(true);
        avatar.setClip(new Circle(60, 60, 60));
        name.setText(nameText);
        role.setText(roleText);
    }

    private String localImage(String path) {
        return Path.of(path).toUri().toString();
    }

    @FXML
    private void onHome() {
        NavigationManager.getInstance().showWelcome();
    }

    @FXML
    private void onOffres() {
        NavigationManager.getInstance().showGuestOffers();
    }

    @FXML
    private void onFeedbacks() {
        NavigationManager.getInstance().showGuestFeedbacks();
    }

    @FXML
    private void onCrew() {
        // Already on crew.
    }

    @FXML
    private void onPremium() {
        // Placeholder-safe in guest mode.
    }

    @FXML
    private void onThemeToggle() {
        NavigationManager.getInstance().toggleTheme();
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
