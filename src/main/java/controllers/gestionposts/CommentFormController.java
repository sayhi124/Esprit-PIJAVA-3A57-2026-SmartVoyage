package controllers.gestionposts;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import models.gestionposts.Comment;
import services.gestionposts.CommentService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le formulaire d'ajout/modification de commentaire.
 */
public class CommentFormController implements Initializable {

    @FXML private Label titleLabel;
    @FXML private Label errorLabel;
    @FXML private TextArea contentArea;
    @FXML private Label counterLabel;

    private Comment comment;
    private final CommentService commentService;
    private Stage stage;
    private Runnable onSaveCallback;

    public CommentFormController() {
        this.commentService = new CommentService();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupCounter();
    }

    private void setupCounter() {
        contentArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal != null ? newVal.length() : 0;
            counterLabel.setText(len + "/1000");

            if (len < 5 || len > 1000) {
                counterLabel.setStyle("-fx-text-fill: #e74c3c;");
            } else {
                counterLabel.setStyle("-fx-text-fill: #27ae60;");
            }
        });
    }

    public void setComment(Comment comment) {
        this.comment = comment;
        if (comment != null) {
            titleLabel.setText("Modifier le commentaire");
            contentArea.setText(comment.getContent());
        } else {
            titleLabel.setText("Nouveau commentaire");
            contentArea.clear();
        }
        hideError();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnSave(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void onSave() {
        hideError();

        String content = contentArea.getText().trim();

        try {
            if (comment != null) {
                // Update existing
                comment.setContent(content);
                commentService.update(comment);
            }

            if (stage != null) {
                stage.close();
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Erreur base de données: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        if (stage != null) {
            stage.close();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
