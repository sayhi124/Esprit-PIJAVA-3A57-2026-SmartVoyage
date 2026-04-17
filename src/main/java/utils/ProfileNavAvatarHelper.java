package utils;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import models.gestionagences.ImageAsset;
import services.gestionutilisateurs.UserService;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Renders a circular user avatar inside sidebar profile buttons.
 */
public final class ProfileNavAvatarHelper {

    private ProfileNavAvatarHelper() {
    }

    public static void install(Button targetButton, Integer userId, UserService userService) {
        if (targetButton == null || userId == null || userService == null) {
            return;
        }
        targetButton.setGraphic(null);
        Thread t = new Thread(() -> {
            try {
                Optional<ImageAsset> asset = userService.loadProfileImage(userId);
                if (asset.isPresent() && asset.get().getData() != null && asset.get().getData().length > 0) {
                    Image img = new Image(new ByteArrayInputStream(asset.get().getData()));
                    Platform.runLater(() -> applyGraphic(targetButton, img));
                }
            } catch (SQLException ignored) {
                // Keep default icon when image is unavailable.
            }
        }, "profile-nav-avatar");
        t.setDaemon(true);
        t.start();
    }

    private static void applyGraphic(Button targetButton, Image image) {
        ImageView view = new ImageView(image);
        view.setFitWidth(18);
        view.setFitHeight(18);
        view.setPreserveRatio(false);
        view.setClip(new Circle(9, 9, 9));
        StackPane shell = new StackPane(view);
        shell.setMinSize(18, 18);
        shell.setPrefSize(18, 18);
        shell.setAlignment(Pos.CENTER);
        targetButton.setGraphic(shell);
    }
}
