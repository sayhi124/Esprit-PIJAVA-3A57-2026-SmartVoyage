package utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Admin login dialog for accessing the admin dashboard.
 * Validates username: admin, password: admin123
 */
public class AdminLoginDialog {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private Stage dialogStage;
    private boolean authenticated = false;

    public boolean show() {
        dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.UNDECORATED);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Admin Login");

        VBox root = createContent();
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        // Load CSS if available
        var cssUrl = getClass().getResource("/css/admin_dialog.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            // Apply inline styles
            root.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 12; -fx-padding: 30;");
        }

        dialogStage.setScene(scene);
        dialogStage.showAndWait();

        return authenticated;
    }

    private VBox createContent() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setPrefWidth(350);
        root.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 12;");

        // Drop shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.5));
        shadow.setRadius(20);
        root.setEffect(shadow);

        // Title
        Label titleLabel = new Label("Admin Access");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        // Subtitle
        Label subtitleLabel = new Label("Enter your credentials to access the dashboard");
        subtitleLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        subtitleLabel.setWrapText(true);

        // Username field
        Label userLabel = new Label("Username");
        userLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.setStyle(
            "-fx-background-color: #1e293b;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #64748b;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 12;" +
            "-fx-border-color: #334155;" +
            "-fx-border-radius: 8;"
        );

        // Password field
        Label passLabel = new Label("Password");
        passLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setStyle(
            "-fx-background-color: #1e293b;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #64748b;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 12;" +
            "-fx-border-color: #334155;" +
            "-fx-border-radius: 8;"
        );

        // Error label (hidden by default)
        Label errorLabel = new Label("Invalid username or password");
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        // Buttons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #94a3b8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 12 24;" +
            "-fx-border-color: #334155;" +
            "-fx-border-radius: 8;" +
            "-fx-cursor: hand;"
        );
        cancelButton.setOnAction(e -> dialogStage.close());

        Button loginButton = new Button("Login");
        loginButton.setStyle(
            "-fx-background-color: linear-gradient(to right, #eab308, #ca8a04);" +
            "-fx-text-fill: #000000;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 12 24;" +
            "-fx-cursor: hand;"
        );
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password)) {
                authenticated = true;
                dialogStage.close();
            } else {
                errorLabel.setVisible(true);
                usernameField.setStyle(
                    "-fx-background-color: #1e293b;" +
                    "-fx-text-fill: white;" +
                    "-fx-prompt-text-fill: #64748b;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 12;" +
                    "-fx-border-color: #ef4444;" +
                    "-fx-border-radius: 8;"
                );
                passwordField.setStyle(
                    "-fx-background-color: #1e293b;" +
                    "-fx-text-fill: white;" +
                    "-fx-prompt-text-fill: #64748b;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 12;" +
                    "-fx-border-color: #ef4444;" +
                    "-fx-border-radius: 8;"
                );
            }
        });

        buttonBox.getChildren().addAll(cancelButton, loginButton);

        root.getChildren().addAll(
            titleLabel,
            subtitleLabel,
            userLabel,
            usernameField,
            passLabel,
            passwordField,
            errorLabel,
            buttonBox
        );

        return root;
    }
}
