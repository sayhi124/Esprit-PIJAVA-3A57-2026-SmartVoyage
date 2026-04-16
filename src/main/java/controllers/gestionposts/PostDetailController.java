package controllers.gestionposts;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.gestionposts.Comment;
import models.gestionposts.Post;
import models.gestionutilisateurs.User;
import services.gestionposts.CommentService;
import services.gestionposts.PostService;
import utils.NavigationManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la fenêtre de détail d'un post.
 * Affiche le post complet avec ses commentaires et permet d'ajouter/modifier/supprimer des commentaires.
 */
public class PostDetailController implements Initializable {

    @FXML private Label titleLabel;
    @FXML private Label locationLabel;
    @FXML private Label dateLabel;
    @FXML private Label likesLabel;
    @FXML private Label commentsLabel;
    @FXML private ImageView postImage;
    @FXML private Label contentLabel;
    @FXML private Label commentsCountLabel;
    @FXML private Label commentErrorLabel;
    @FXML private TextArea newCommentArea;
    @FXML private Label commentCounter;
    @FXML private VBox commentsList;
    @FXML private Button editPostButton;
    @FXML private Button deletePostButton;

    private Post post;
    private final PostService postService;
    private final CommentService commentService;
    private Stage stage;
    private Runnable onPostUpdated;
    private Runnable onPostDeleted;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy à HH:mm");

    public PostDetailController() {
        this.postService = new PostService();
        this.commentService = new CommentService();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupCommentCounter();
        
        // Load post from NavigationManager
        this.post = NavigationManager.getInstance().getSelectedPost();
        if (this.post != null) {
            loadPostDetails();
            loadComments();
            updatePostActionButtonsVisibility();
        }
    }

    private void updatePostActionButtonsVisibility() {
        Optional<User> currentUser = NavigationManager.getInstance().sessionUser();
        boolean isAuthor = currentUser.isPresent() && 
                          post.getUserId() != null && 
                          currentUser.get().getId().intValue() == post.getUserId();

        if (editPostButton != null) {
            editPostButton.setVisible(isAuthor);
            editPostButton.setManaged(isAuthor);
        }
        if (deletePostButton != null) {
            deletePostButton.setVisible(isAuthor);
            deletePostButton.setManaged(isAuthor);
        }
    }

    private void setupCommentCounter() {
        newCommentArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal != null ? newVal.length() : 0;
            commentCounter.setText(len + "/1000");
            commentCounter.setStyle(len < 5 || len > 1000 ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
        });
    }

    public void setPost(Post post) {
        this.post = post;
        loadPostDetails();
        loadComments();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnPostUpdated(Runnable callback) {
        this.onPostUpdated = callback;
    }

    public void setOnPostDeleted(Runnable callback) {
        this.onPostDeleted = callback;
    }

    private void loadPostDetails() {
        titleLabel.setText(post.getTitre());
        locationLabel.setText("📍 " + (post.getLocation() != null ? post.getLocation() : "Non spécifié"));

        if (post.getCreatedAt() != null) {
            dateLabel.setText(post.getCreatedAt().format(DATE_FORMATTER));
        } else {
            dateLabel.setText("Date inconnue");
        }

        likesLabel.setText("❤️ " + post.getNbLikes());
        commentsLabel.setText("💬 " + post.getNbCommentaires());
        commentsCountLabel.setText("(" + post.getNbCommentaires() + ")");

        // Load image
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            try {
                String imageUrl = resolveImageUrl(post.getImageUrl());
                Image image = new Image(imageUrl, true);
                postImage.setImage(image);
            } catch (Exception e) {
                // Use default placeholder
                postImage.setImage(null);
            }
        }

        contentLabel.setText(post.getContenu());
    }

    private String resolveImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "";
        }

        String path = imagePath.trim();

        // If it's already a URL (http/https/file), use it as is
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("file:")) {
            return path;
        }

        // If it's a Windows path (C:\) or contains backslashes, convert to file URI
        if (path.startsWith("C:\\") || path.contains("\\")) {
            try {
                java.io.File file = new java.io.File(path);
                if (file.exists()) {
                    return file.toURI().toString();
                }
            } catch (Exception e) {
                // Return original and let it fail gracefully
                return path;
            }
        }

        // If it's a classpath resource (starts with /)
        if (path.startsWith("/")) {
            var url = getClass().getResource(path);
            if (url != null) {
                return url.toExternalForm();
            }
        }

        return path;
    }

    private void loadComments() {
        commentsList.getChildren().clear();

        try {
            List<Comment> comments = commentService.findByPostIdOrdered(post.getId());
            post.setNbCommentaires(comments.size());
            commentsCountLabel.setText("(" + comments.size() + ")");
            commentsLabel.setText("💬 " + comments.size());

            if (comments.isEmpty()) {
                Label noComments = new Label("Aucun commentaire. Soyez le premier à commenter !");
                noComments.setStyle("-fx-text-fill: #718096; -fx-font-style: italic;");
                commentsList.getChildren().add(noComments);
            } else {
                for (Comment comment : comments) {
                    addCommentCard(comment);
                }
            }
        } catch (SQLException e) {
            showError("Erreur lors du chargement des commentaires: " + e.getMessage());
        }
    }

    private void addCommentCard(Comment comment) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 12px; -fx-background-radius: 8px;");

        // Header with author and date
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        String authorName = comment.getAuthorUsername() != null ? comment.getAuthorUsername() : "Utilisateur " + comment.getUserId();
        Label authorLabel = new Label("👤 " + authorName);
        authorLabel.setStyle("-fx-text-fill: #1E88E5; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label dateLabel = new Label(comment.getCreatedAt() != null ?
                comment.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        dateLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(authorLabel, dateLabel, spacer);

        // Content
        Label contentLabel = new Label(comment.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px;");

        card.getChildren().addAll(header, contentLabel);

        // Action buttons (only for author)
        Optional<User> currentUser = NavigationManager.getInstance().sessionUser();
        HBox actions = null;
        if (currentUser.isPresent() && currentUser.get().getId().intValue() == comment.getUserId()) {
            actions = new HBox(10);
            actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            Button editBtn = new Button("✏️ Modifier");
            editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b9dc3; -fx-font-size: 11px; -fx-cursor: hand;");
            HBox finalActions = actions;
            editBtn.setOnAction(e -> onEditComment(comment, card, contentLabel, finalActions));

            Button deleteBtn = new Button("🗑️ Supprimer");
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-size: 11px; -fx-cursor: hand;");
            deleteBtn.setOnAction(e -> onDeleteComment(comment));

            actions.getChildren().addAll(editBtn, deleteBtn);
            card.getChildren().add(actions);
        }

        commentsList.getChildren().add(card);
    }

    @FXML
    private void onAddComment() {
        commentErrorLabel.setVisible(false);
        commentErrorLabel.setManaged(false);

        String content = newCommentArea.getText().trim();

        Optional<User> user = NavigationManager.getInstance().sessionUser();
        if (!user.isPresent()) {
            showCommentError("Vous devez être connecté pour commenter.");
            return;
        }

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setUserId(user.get().getId().intValue());
        comment.setPostId(post.getId());

        try {
            commentService.create(comment);
            newCommentArea.clear();
            loadComments();

            // Refresh post data
            if (onPostUpdated != null) {
                onPostUpdated.run();
            }
        } catch (IllegalArgumentException e) {
            showCommentError(e.getMessage());
        } catch (SQLException e) {
            showCommentError("Erreur lors de l'ajout du commentaire: " + e.getMessage());
        }
    }

    private void onEditComment(Comment comment, VBox card, Label contentLabel, HBox actionsBox) {
        // Hide original content and action buttons
        contentLabel.setVisible(false);
        contentLabel.setManaged(false);
        if (actionsBox != null) {
            actionsBox.setVisible(false);
            actionsBox.setManaged(false);
        }

        // Create inline edit form
        VBox editForm = new VBox(8);
        editForm.setStyle("-fx-background-color: rgba(30,136,229,0.1); -fx-padding: 10px; -fx-background-radius: 8px;");

        TextArea editArea = new TextArea(comment.getContent());
        editArea.setWrapText(true);
        editArea.setPrefRowCount(3);
        editArea.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: #e2e8f0; -fx-prompt-text-fill: #718096;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button saveBtn = new Button("💾 Enregistrer");
        saveBtn.setStyle("-fx-background-color: #1E88E5; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6 15; -fx-background-radius: 4; -fx-cursor: hand;");

        Button cancelBtn = new Button("❌ Annuler");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; -fx-font-size: 12px; -fx-padding: 6 15; -fx-border-color: #718096; -fx-border-radius: 4; -fx-cursor: hand;");

        btnBox.getChildren().addAll(cancelBtn, saveBtn);
        editForm.getChildren().addAll(editArea, errorLabel, btnBox);

        // Add edit form to card (after header, before actions)
        int insertIndex = card.getChildren().indexOf(contentLabel);
        card.getChildren().add(insertIndex + 1, editForm);

        // Save action
        saveBtn.setOnAction(e -> {
            String newContent = editArea.getText().trim();

            if (newContent.length() < 5) {
                errorLabel.setText("Le commentaire doit contenir au moins 5 caractères.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            if (newContent.length() > 1000) {
                errorLabel.setText("Le commentaire ne doit pas dépasser 1000 caractères.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            comment.setContent(newContent);
            try {
                commentService.update(comment);
                // Restore original view
                card.getChildren().remove(editForm);
                contentLabel.setText(newContent);
                contentLabel.setVisible(true);
                contentLabel.setManaged(true);
                if (actionsBox != null) {
                    actionsBox.setVisible(true);
                    actionsBox.setManaged(true);
                }
                loadComments();
                if (onPostUpdated != null) {
                    onPostUpdated.run();
                }
            } catch (IllegalArgumentException ex) {
                errorLabel.setText(ex.getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            } catch (SQLException ex) {
                errorLabel.setText("Erreur lors de la sauvegarde: " + ex.getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });

        // Cancel action
        cancelBtn.setOnAction(e -> {
            card.getChildren().remove(editForm);
            contentLabel.setVisible(true);
            contentLabel.setManaged(true);
            if (actionsBox != null) {
                actionsBox.setVisible(true);
                actionsBox.setManaged(true);
            }
        });
    }

    private void onDeleteComment(Comment comment) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le commentaire");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce commentaire ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                commentService.delete(comment.getId());
                loadComments();

                if (onPostUpdated != null) {
                    onPostUpdated.run();
                }
            } catch (SQLException e) {
                showError("Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onEditPost() {
        Optional<User> user = NavigationManager.getInstance().sessionUser();
        if (!user.isPresent() || user.get().getId().intValue() != post.getUserId()) {
            showError("Vous n'êtes pas autorisé à modifier ce post.");
            return;
        }

        if (stage != null) {
            stage.close();
        }

        if (onPostUpdated != null) {
            onPostUpdated.run();
        }
    }

    @FXML
    private void onDeletePost() {
        Optional<User> user = NavigationManager.getInstance().sessionUser();
        if (!user.isPresent() || user.get().getId().intValue() != post.getUserId()) {
            showError("Vous n'êtes pas autorisé à supprimer ce post.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le post");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + post.getTitre() + "\" ?\n\nCette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                postService.delete(post.getId());
                // Navigate back to posts list
                NavigationManager.getInstance().showSignedInPosts();
            } catch (SQLException e) {
                showError("Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onClose() {
        // Navigate back to posts list page
        NavigationManager.getInstance().showSignedInPosts();
    }

    private void showCommentError(String message) {
        commentErrorLabel.setText(message);
        commentErrorLabel.setVisible(true);
        commentErrorLabel.setManaged(true);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
