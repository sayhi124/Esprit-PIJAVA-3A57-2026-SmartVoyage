package controllers.gestionposts;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.gestionposts.Post;
import models.gestionutilisateurs.User;
import services.gestionposts.PostService;
import utils.NavigationManager;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le formulaire d'ajout/édition de post.
 * Gère la validation et l'enregistrement.
 */
public class PostFormController implements Initializable {

    @FXML private Label formTitleLabel;
    @FXML private TextField titreField;
    @FXML private ComboBox<String> locationComboBox;
    @FXML private TextField imageUrlField;
    @FXML private ImageView imagePreview;
    @FXML private TextArea contenuArea;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    @FXML private Label titreCounter;
    @FXML private Label contenuCounter;

    private Post post;
    private PostService postService;
    private Stage stage;
    private Runnable onSave;

    private static final int TITRE_MIN = 10;
    private static final int TITRE_MAX = 100;
    private static final int CONTENU_MIN = 50;
    private static final int CONTENU_MAX = 5000;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        postService = new PostService();
        setupCharacterCounters();
        setupImagePreview();
        clearError();
    }

    private void setupImagePreview() {
        imageUrlField.textProperty().addListener((obs, oldValue, newValue) -> imagePreview.setImage(resolveImage(newValue)));
        imagePreview.setImage(resolveImage(null));
    }

    @FXML
    private void onBrowseImage() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File selected = chooser.showOpenDialog(titreField.getScene().getWindow());
        if (selected != null) {
            imageUrlField.setText(selected.getAbsolutePath());
        }
    }

    private Image resolveImage(String imagePath) {
        Image fallback = loadFromClasspath("/images/welcome/featured-paris-eiffel.jpg");
        if (imagePath == null || imagePath.isBlank()) {
            return fallback;
        }

        String path = imagePath.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return new Image(path, 480, 270, true, true, true);
        }

        if (path.startsWith("/")) {
            Image classpathImage = loadFromClasspath(path);
            if (classpathImage != null) {
                return classpathImage;
            }
        }

        try {
            Path p = Path.of(path);
            if (Files.exists(p)) {
                return new Image(p.toUri().toString(), 480, 270, true, true, true);
            }
        } catch (Exception ignored) {
        }

        return fallback;
    }

    private Image loadFromClasspath(String path) {
        var url = PostFormController.class.getResource(path);
        if (url == null) {
            return null;
        }
        return new Image(url.toExternalForm(), 480, 270, true, true, true);
    }

    private void setupCharacterCounters() {
        // Compteur pour le titre
        titreField.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal != null ? newVal.length() : 0;
            titreCounter.setText(len + "/" + TITRE_MAX);
            if (len < TITRE_MIN || len > TITRE_MAX) {
                titreCounter.setStyle("-fx-text-fill: #e74c3c;");
            } else {
                titreCounter.setStyle("-fx-text-fill: #27ae60;");
            }
        });

        // Compteur pour le contenu
        contenuArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal != null ? newVal.length() : 0;
            contenuCounter.setText(len + "/" + CONTENU_MAX);
            if (len < CONTENU_MIN || len > CONTENU_MAX) {
                contenuCounter.setStyle("-fx-text-fill: #e74c3c;");
            } else {
                contenuCounter.setStyle("-fx-text-fill: #27ae60;");
            }
        });
    }

    public void setPost(Post post) {
        this.post = post;
        if (post != null) {
            formTitleLabel.setText("Modifier le Post");
            loadPostData();
        } else {
            formTitleLabel.setText("Nouveau Post");
        }
        clearError();
    }

    public void setCountriesList(ObservableList<String> countries) {
        locationComboBox.setItems(countries);
        if (post != null && post.getLocation() != null && !countries.contains(post.getLocation())) {
            locationComboBox.getItems().add(post.getLocation());
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnSave(Runnable onSave) {
        this.onSave = onSave;
    }

    private void loadPostData() {
        titreField.setText(post.getTitre());
        contenuArea.setText(post.getContenu());
        imageUrlField.setText(post.getImageUrl());
        imagePreview.setImage(resolveImage(post.getImageUrl()));

        if (post.getLocation() != null) {
            locationComboBox.setValue(post.getLocation());
        }
    }

    @FXML
    private void onSave() {
        clearError();

        // Créer ou mettre à jour le post
        Post postToSave = this.post != null ? this.post : new Post();

        postToSave.setTitre(titreField.getText().trim());
        postToSave.setContenu(contenuArea.getText().trim());
        postToSave.setLocation(locationComboBox.getValue());
        postToSave.setImageUrl(imageUrlField.getText().trim());

        // Définir l'utilisateur si c'est une création
        if (this.post == null) {
            Optional<User> user = NavigationManager.getInstance().sessionUser();
            if (user.isPresent()) {
                postToSave.setUserId(user.get().getId().intValue());
            } else {
                postToSave.setUserId(1); // Valeur par défaut
            }
        }

        // Validation et sauvegarde
        try {
            if (this.post == null) {
                postService.create(postToSave);
            } else {
                postService.update(postToSave);
            }

            if (onSave != null) {
                onSave.run();
            }

            closeForm();

        } catch (IllegalArgumentException e) {
            // Erreurs de validation
            showError(e.getMessage());
        } catch (SQLException e) {
            // Erreurs SQL
            showError("Erreur base de données: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        closeForm();
    }

    private void closeForm() {
        if (stage != null) {
            stage.close();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
