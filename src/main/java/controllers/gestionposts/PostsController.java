package controllers.gestionposts;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import models.gestionposts.Post;
import models.gestionutilisateurs.User;
import services.gestionposts.PostService;
import utils.Countries;
import utils.NavigationManager;

import java.net.URL;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Contrôleur principal pour la vue des posts de voyage.
 * Gère l'affichage en cartes, les filtres, la recherche et la pagination.
 */
public class PostsController implements Initializable {

    @FXML private Label userGreetingLabel;
    @FXML private Label roleLabel;
    @FXML private Label postsStatusLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> countryComboBox;
    @FXML private Button searchButton;
    @FXML private Button clearFiltersButton;

    @FXML private FlowPane postsFlowPane;
    @FXML private ScrollPane postsScrollPane;

    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;
    @FXML private Label pageLabel;
    @FXML private Label totalPostsLabel;

    @FXML private Button addPostButton;

    // Inline Form Fields
    @FXML private StackPane contentStack;
    @FXML private VBox listView;
    @FXML private VBox formView;
    @FXML private Label formTitleLabel;
    @FXML private Label formErrorLabel;
    @FXML private TextField formTitreField;
    @FXML private ComboBox<String> formLocationCombo;
    @FXML private TextField formImageUrlField;
    @FXML private ImageView formImagePreview;
    @FXML private TextArea formContenuArea;
    @FXML private Label titreCounter;
    @FXML private Label contenuCounter;

    private final PostService postService;
    private final ObservableList<String> countriesList;

    private int currentPage = 1;
    private int totalPages = 1;
    private int totalPosts = 0;
    private String currentSearch = "";
    private String currentCountry = null;
    private Post editingPost = null;

    public PostsController() {
        this.postService = new PostService();
        this.countriesList = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupUserInfo();
        setupCountriesComboBox();
        setupPostsGrid();
        setupFormCounters();
        setupImagePreview();
        loadPosts();
    }

    private void setupImagePreview() {
        formImageUrlField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (formImagePreview != null) {
                formImagePreview.setImage(resolveImage(newValue));
            }
        });
        if (formImagePreview != null) {
            formImagePreview.setImage(resolveImage(null));
        }
    }

    @FXML
    private void onBrowseFormImage() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File selected = chooser.showOpenDialog(formTitreField.getScene().getWindow());
        if (selected != null) {
            formImageUrlField.setText(selected.getAbsolutePath());
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
        var url = PostsController.class.getResource(path);
        if (url == null) {
            return null;
        }
        return new Image(url.toExternalForm(), 480, 270, true, true, true);
    }

    private void setupFormCounters() {
        formTitreField.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal != null ? newVal.length() : 0;
            titreCounter.setText(len + "/100");
            titreCounter.setStyle(len < 10 || len > 100 ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
        });

        formContenuArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal != null ? newVal.length() : 0;
            contenuCounter.setText(len + "/5000");
            contenuCounter.setStyle(len < 50 || len > 5000 ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
        });

        formLocationCombo.setItems(countriesList);
    }

    private void setupUserInfo() {
        Optional<User> user = NavigationManager.getInstance().sessionUser();
        if (user.isPresent()) {
            userGreetingLabel.setText("Bienvenue, " + user.get().getUsername());
            roleLabel.setText(user.get().getRole() != null ? user.get().getRole() : "Utilisateur");
        } else {
            userGreetingLabel.setText("Bienvenue");
            roleLabel.setText("Invité");
        }
    }

    private void setupCountriesComboBox() {
        // Ajouter l'option "Tous les pays" en premier
        countriesList.add("Tous les pays");
        countriesList.addAll(Countries.getAllCountries());

        // Ajouter aussi les locations existantes dans la BDD
        try {
            List<String> dbLocations = postService.findAllLocationsFromPosts();
            for (String loc : dbLocations) {
                if (!countriesList.contains(loc)) {
                    countriesList.add(loc);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement locations: " + e.getMessage());
        }

        Collections.sort(countriesList.subList(1, countriesList.size()));

        countryComboBox.setItems(countriesList);
        countryComboBox.getSelectionModel().selectFirst();

        countryComboBox.setOnAction(e -> {
            String selected = countryComboBox.getSelectionModel().getSelectedItem();
            currentCountry = "Tous les pays".equals(selected) ? null : selected;
            currentPage = 1;
            loadPosts();
        });
    }

    private void setupPostsGrid() {
        postsFlowPane.setHgap(20);
        postsFlowPane.setVgap(20);
        postsFlowPane.setPadding(new Insets(10));
        postsFlowPane.prefWidthProperty().bind(postsScrollPane.widthProperty().subtract(40));
    }

    /**
     * Charge les posts selon les filtres actuels.
     */
    private void loadPosts() {
        postsFlowPane.getChildren().clear();

        try {
            List<Post> posts;

            // Déterminer quelle requête exécuter selon les filtres
            if (currentCountry != null && !currentSearch.isEmpty()) {
                // Filtre combiné pays + recherche
                posts = postService.searchByLocationAndKeywordPaginated(currentCountry, currentSearch, currentPage);
                totalPosts = postService.countSearchByLocationAndKeyword(currentCountry, currentSearch);
            } else if (currentCountry != null) {
                // Filtre pays uniquement
                posts = postService.findByLocationPaginated(currentCountry, currentPage);
                totalPosts = postService.countByLocation(currentCountry);
            } else if (!currentSearch.isEmpty()) {
                // Recherche uniquement
                posts = postService.searchPaginated(currentSearch, currentPage);
                totalPosts = postService.countSearch(currentSearch);
            } else {
                // Aucun filtre
                posts = postService.findAllPaginated(currentPage);
                totalPosts = postService.countAll();
            }

            totalPages = Math.max(1, postService.getTotalPages(totalPosts));

            // Mettre à jour la pagination
            updatePagination();

            // Afficher les posts
            if (posts.isEmpty()) {
                showStatus("Aucun post trouvé.");
            } else {
                hideStatus();
                for (Post post : posts) {
                    addPostCard(post);
                }
            }

            totalPostsLabel.setText("Total: " + totalPosts + " post" + (totalPosts > 1 ? "s" : ""));

        } catch (SQLException e) {
            showStatus("Erreur lors du chargement des posts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addPostCard(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/posts/post_card.fxml"));
            Parent card = loader.load();

            PostCardController controller = loader.getController();
            controller.setPost(post);
            controller.setOnEdit(this::onEditPost);
            controller.setOnDelete(this::onDeletePost);

            // Animation d'apparition
            card.setOpacity(0);
            postsFlowPane.getChildren().add(card);

            FadeTransition fade = new FadeTransition(Duration.millis(300), card);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();

        } catch (IOException e) {
            System.err.println("Erreur chargement carte post: " + e.getMessage());
        }
    }

    private void updatePagination() {
        pageLabel.setText("Page " + currentPage + " / " + totalPages);
        prevPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);
    }

    @FXML
    private void onSearch() {
        currentSearch = searchField.getText().trim();
        currentPage = 1;
        loadPosts();
    }

    @FXML
    private void onClearFilters() {
        searchField.clear();
        countryComboBox.getSelectionModel().selectFirst();
        currentSearch = "";
        currentCountry = null;
        currentPage = 1;
        loadPosts();
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadPosts();
        }
    }

    @FXML
    private void onNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadPosts();
        }
    }

    @FXML
    private void onAddPost() {
        showInlineForm(null);
    }

    private void onEditPost(Post post) {
        Optional<User> currentUser = NavigationManager.getInstance().sessionUser();
        if (currentUser.isEmpty() || post.getUserId() == null || currentUser.get().getId().intValue() != post.getUserId()) {
            showStatus("Vous ne pouvez modifier que vos propres posts.");
            return;
        }
        showInlineForm(post);
    }

    private void showInlineForm(Post post) {
        editingPost = post;
        formErrorLabel.setVisible(false);
        formErrorLabel.setManaged(false);

        if (post != null) {
            formTitleLabel.setText("Modifier le Post");
            formTitreField.setText(post.getTitre());
            formContenuArea.setText(post.getContenu());
            formImageUrlField.setText(post.getImageUrl());
            if (formImagePreview != null) {
                formImagePreview.setImage(resolveImage(post.getImageUrl()));
            }
            formLocationCombo.setValue(post.getLocation());
        } else {
            formTitleLabel.setText("Nouveau Post");
            formTitreField.clear();
            formContenuArea.clear();
            formImageUrlField.clear();
            if (formImagePreview != null) {
                formImagePreview.setImage(resolveImage(null));
            }
            formLocationCombo.getSelectionModel().clearSelection();
        }

        // Show form, hide list
        listView.setVisible(false);
        listView.setManaged(false);
        formView.setVisible(true);
        formView.setManaged(true);
    }

    @FXML
    private void onBackToList() {
        listView.setVisible(true);
        listView.setManaged(true);
        formView.setVisible(false);
        formView.setManaged(false);
        editingPost = null;
    }

    @FXML
    private void onCancelForm() {
        onBackToList();
    }

    @FXML
    private void onSaveForm() {
        formErrorLabel.setVisible(false);
        formErrorLabel.setManaged(false);

        Post postToSave = editingPost != null ? editingPost : new Post();
        postToSave.setTitre(formTitreField.getText().trim());
        postToSave.setContenu(formContenuArea.getText().trim());
        postToSave.setLocation(formLocationCombo.getValue());
        postToSave.setImageUrl(formImageUrlField.getText().trim());

        if (editingPost == null) {
            Optional<User> user = NavigationManager.getInstance().sessionUser();
            postToSave.setUserId(user.isPresent() ? user.get().getId().intValue() : 1);
        }

        try {
            if (editingPost == null) {
                postService.create(postToSave);
            } else {
                Integer actorUserId = NavigationManager.getInstance().sessionUser().map(u -> u.getId().intValue()).orElse(null);
                postService.updateByAuthor(postToSave, actorUserId);
            }

            loadPosts();
            onBackToList();
            showStatus("Post enregistré avec succès !");
            Platform.runLater(() -> {
                FadeTransition fade = new FadeTransition(Duration.seconds(2), postsStatusLabel);
                fade.setFromValue(1);
                fade.setToValue(0);
                fade.setOnFinished(e -> hideStatus());
                fade.play();
            });

        } catch (IllegalArgumentException e) {
            formErrorLabel.setText(e.getMessage());
            formErrorLabel.setVisible(true);
            formErrorLabel.setManaged(true);
        } catch (SQLException e) {
            formErrorLabel.setText("Erreur base de données: " + e.getMessage());
            formErrorLabel.setVisible(true);
            formErrorLabel.setManaged(true);
        }
    }

    private void onDeletePost(Post post) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le post");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + post.getTitre() + "\" ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Integer actorUserId = NavigationManager.getInstance().sessionUser().map(u -> u.getId().intValue()).orElse(null);
                postService.deleteByAuthor(post.getId(), actorUserId);
                loadPosts();
                showStatus("Post supprimé avec succès !");
            } catch (SQLException | IllegalArgumentException e) {
                showStatus("Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    private void showStatus(String message) {
        postsStatusLabel.setText(message);
        postsStatusLabel.setVisible(true);
        postsStatusLabel.setManaged(true);
    }

    private void hideStatus() {
        postsStatusLabel.setVisible(false);
        postsStatusLabel.setManaged(false);
    }

    // ========== Navigation ==========

    @FXML private void onHome() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onOffres() { NavigationManager.getInstance().showGuestOffers(); }
    @FXML private void onAgences() { NavigationManager.getInstance().showSignedInAgencies(); }
    @FXML private void onMessagerie() { }
    @FXML private void onRecommandation() { }
    @FXML private void onEvenement() { NavigationManager.getInstance().showSignedInEvents(); }
    @FXML private void onPremium() { }
    @FXML private void onNotifications() { }
    @FXML private void onProfile() { }
    @FXML private void onDashboardIa() { }
    @FXML private void onLogout() { NavigationManager.getInstance().logoutToGuest(); }
    @FXML private void onThemeToggle() { NavigationManager.getInstance().toggleTheme(); }
}
