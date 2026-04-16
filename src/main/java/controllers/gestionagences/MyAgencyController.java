package controllers.gestionagences;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import models.gestionagences.AgencyAccount;
import models.gestionagences.AgencyPost;
import models.gestionagences.AgencyPostComment;
import models.gestionagences.ImageAsset;
import org.controlsfx.control.Notifications;
import services.gestionagences.AgencyAccountService;
import services.gestionagences.AgencyAccountValidationResult;
import services.gestionagences.AgencyPostService;
import services.gestionagences.ImageAssetService;
import utils.NavigationManager;

import java.io.ByteArrayInputStream;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MyAgencyController {

    @FXML private Label agencyTitleLabel;
    @FXML private Label agencyVerifiedBadgeLabel;
    @FXML private Label agencyCountryLabel;
    @FXML private Label agencyAddressLabel;
    @FXML private Label agencyWebsiteLabel;
    @FXML private Label agencyPhoneLabel;
    @FXML private Label agencyDescriptionLabel;
    @FXML private HBox descriptionRow;
    @FXML private HBox addressRow;
    @FXML private HBox websiteRow;
    @FXML private HBox phoneRow;
    @FXML private HBox countryRow;
    @FXML private Label feedbackLabel;

    @FXML private Button postsTabButton;
    @FXML private Button aboutTabButton;
    @FXML private Button offersTabButton;
    @FXML private Button addPostButton;
    @FXML private Label placeholderTitleLabel;
    @FXML private Label placeholderSubtitleLabel;
    @FXML private ScrollPane postsScrollPane;
    @FXML private VBox postsListBox;
    @FXML private VBox mainContentPlaceholder;

    @FXML private TextField editAgencyNameField;
    @FXML private TextField editWebsiteField;
    @FXML private TextField editPhoneField;
    @FXML private TextField editAddressField;
    @FXML private TextField editCountryField;
    @FXML private TextArea editDescriptionField;
    @FXML private Label editAgencyNameErrorLabel;
    @FXML private Label editDescriptionErrorLabel;
    @FXML private Label editAddressErrorLabel;
    @FXML private Label editWebsiteErrorLabel;
    @FXML private Label editPhoneErrorLabel;
    @FXML private Label editCountryErrorLabel;
    @FXML private VBox editPanel;
    @FXML private Button editButton;
    @FXML private Button saveButton;
    @FXML private Button cancelEditButton;
    @FXML private Button editCoverButton;
    @FXML private Button editAvatarButton;
    @FXML private Button editNameFieldButton;
    @FXML private Button editDescriptionFieldButton;
    @FXML private Button editAddressFieldButton;
    @FXML private Button editWebsiteFieldButton;
    @FXML private Button editPhoneFieldButton;
    @FXML private Button editCountryFieldButton;
    @FXML private StackPane bannerStack;
    @FXML private ImageView coverImageView;
    @FXML private ImageView avatarImageView;
    @FXML private Label avatarFallbackLabel;

    private final AgencyAccountService agencyService = new AgencyAccountService();
    private final AgencyPostService agencyPostService = new AgencyPostService();
    private final ImageAssetService imageAssetService = new ImageAssetService();
    private final DateTimeFormatter postDateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private AgencyAccount currentAgency;
    private boolean canEditAgency;

    @FXML
    private void initialize() {
        NavigationManager nav = NavigationManager.getInstance();
        if (!nav.canAccessSignedInShell()) {
            nav.showSignedInAgencies();
            return;
        }
        loadAgencyForCurrentContext();
        configureImageNodes();
        showPostsTab();
        setEditMode(false);
    }

    private void loadAgencyForCurrentContext() {
        Optional<Integer> userIdOpt = NavigationManager.getInstance().sessionUser().map(u -> u.getId());
        if (userIdOpt.isEmpty()) {
            NavigationManager.getInstance().showLogin();
            return;
        }
        int userId = userIdOpt.get();
        Long selectedAgencyId = NavigationManager.getInstance().selectedAgencyId().orElse(null);
        try {
            Optional<AgencyAccount> target = selectedAgencyId != null
                    ? agencyService.get(selectedAgencyId)
                    : agencyService.findByResponsableId(userId);

            if (target.isEmpty()) {
                if (selectedAgencyId != null) {
                    feedbackLabel.setText("Agency not found.");
                    NavigationManager.getInstance().showSignedInAgencies();
                } else {
                    feedbackLabel.setText("No agency found for this account.");
                    NavigationManager.getInstance().showAgencyProposal();
                }
                return;
            }

            currentAgency = target.get();
            boolean isOwnerAdmin = currentAgency.getResponsableId() != null
                    && currentAgency.getResponsableId().equals(userId)
                    && NavigationManager.getInstance().canAccessAgencyAdminFeatures();
            boolean isPlatformAdmin = NavigationManager.getInstance().canAccessAdminFeatures();
            canEditAgency = isOwnerAdmin || isPlatformAdmin;
            bindAgencyToView();
            applyEditVisibility();
            if (!canEditAgency) {
                feedbackLabel.setText("");
            } else {
                feedbackLabel.setText("");
            }
        } catch (SQLException e) {
            feedbackLabel.setText("Unable to load agency: " + e.getMessage());
        }
    }

    private void bindAgencyToView() {
        if (currentAgency == null) {
            return;
        }
        agencyTitleLabel.setText(safe(currentAgency.getAgencyName(), "My Agency"));
        boolean verified = Boolean.TRUE.equals(currentAgency.getVerified());
        agencyVerifiedBadgeLabel.setVisible(verified);
        agencyVerifiedBadgeLabel.setManaged(verified);
        editCoverButton.setText(currentAgency.getCoverImageId() != null ? "✎ Edit cover" : "+ Add cover");
        editAvatarButton.setText(currentAgency.getAgencyProfileImageId() != null ? "✎" : "+");
        bindFieldRow(descriptionRow, agencyDescriptionLabel, currentAgency.getDescription(), "");
        bindFieldRow(addressRow, agencyAddressLabel, currentAgency.getAddress(), "Address");
        bindFieldRow(websiteRow, agencyWebsiteLabel, currentAgency.getWebsiteUrl(), "Website");
        bindFieldRow(phoneRow, agencyPhoneLabel, currentAgency.getPhone(), "Phone");
        bindFieldRow(countryRow, agencyCountryLabel, currentAgency.getCountry(), "Country");

        editAgencyNameField.setText(safe(currentAgency.getAgencyName(), ""));
        editWebsiteField.setText(safe(currentAgency.getWebsiteUrl(), ""));
        editPhoneField.setText(safe(currentAgency.getPhone(), ""));
        editAddressField.setText(safe(currentAgency.getAddress(), ""));
        editCountryField.setText(safe(currentAgency.getCountry(), ""));
        editDescriptionField.setText(safe(currentAgency.getDescription(), ""));
        clearEditFieldErrors();
        loadAgencyImages();
    }

    private void setEditMode(boolean enabled) {
        boolean showEditControls = canEditAgency && enabled;
        editPanel.setVisible(enabled && showEditControls);
        editPanel.setManaged(enabled && showEditControls);
        saveButton.setVisible(enabled && showEditControls);
        saveButton.setManaged(enabled && showEditControls);
        cancelEditButton.setVisible(enabled && showEditControls);
        cancelEditButton.setManaged(enabled && showEditControls);
        editButton.setVisible(!enabled && canEditAgency);
        editButton.setManaged(!enabled && canEditAgency);
    }

    private void applyEditVisibility() {
        List<Button> editControls = List.of(
                editCoverButton,
                editAvatarButton,
                editNameFieldButton,
                editDescriptionFieldButton,
                editAddressFieldButton,
                editWebsiteFieldButton,
                editPhoneFieldButton,
                editCountryFieldButton,
                editButton,
                addPostButton
        );
        for (Button b : editControls) {
            b.setVisible(canEditAgency);
            b.setManaged(canEditAgency);
        }
    }

    @FXML
    private void onEditAgency() {
        if (!canEditAgency) {
            feedbackLabel.setText("You are not allowed to edit this agency.");
            return;
        }
        setEditMode(true);
        showAboutTab();
    }

    @FXML
    private void onCancelEdit() {
        bindAgencyToView();
        setEditMode(false);
    }

    @FXML
    private void onSaveAgency() {
        feedbackLabel.setText("");
        clearEditFieldErrors();
        if (!canEditAgency) {
            feedbackLabel.setText("You are not allowed to update this agency.");
            return;
        }
        if (currentAgency == null) {
            feedbackLabel.setText("No agency loaded.");
            return;
        }
        AgencyAccount draft = new AgencyAccount();
        draft.setAgencyName(trim(editAgencyNameField.getText()));
        draft.setDescription(trim(editDescriptionField.getText()));
        draft.setWebsiteUrl(blankToNull(editWebsiteField.getText()));
        draft.setPhone(blankToNull(editPhoneField.getText()));
        draft.setAddress(blankToNull(editAddressField.getText()));
        String countryRaw = blankToNull(editCountryField.getText());
        draft.setCountry(countryRaw != null ? countryRaw.toUpperCase(Locale.ROOT) : null);
        agencyService.applyResolvedCountryIfMissing(draft);

        AgencyAccountValidationResult validation = agencyService.validateAgencyProfileFields(draft);
        if (!validation.isValid()) {
            applyEditFieldErrors(validation);
            feedbackLabel.setText("");
            return;
        }

        currentAgency.setAgencyName(draft.getAgencyName());
        currentAgency.setDescription(draft.getDescription());
        currentAgency.setPhone(draft.getPhone());
        currentAgency.setAddress(draft.getAddress());
        currentAgency.setCountry(draft.getCountry());
        currentAgency.setWebsiteUrl(normalizeWebsiteUrl(draft.getWebsiteUrl()));
        try {
            agencyService.update(currentAgency);
            bindAgencyToView();
            setEditMode(false);
            feedbackLabel.setText("Agency updated successfully.");
        } catch (SQLException | IllegalArgumentException e) {
            feedbackLabel.setText("Update failed: " + e.getMessage());
        }
    }

    private void clearEditFieldErrors() {
        setFieldError(editAgencyNameErrorLabel, null);
        setFieldError(editDescriptionErrorLabel, null);
        setFieldError(editAddressErrorLabel, null);
        setFieldError(editWebsiteErrorLabel, null);
        setFieldError(editPhoneErrorLabel, null);
        setFieldError(editCountryErrorLabel, null);
    }

    private void applyEditFieldErrors(AgencyAccountValidationResult r) {
        setFieldError(editAgencyNameErrorLabel, r.getError(AgencyAccountValidationResult.FIELD_AGENCY_NAME));
        setFieldError(editDescriptionErrorLabel, r.getError(AgencyAccountValidationResult.FIELD_DESCRIPTION));
        setFieldError(editAddressErrorLabel, r.getError(AgencyAccountValidationResult.FIELD_ADDRESS));
        setFieldError(editWebsiteErrorLabel, r.getError(AgencyAccountValidationResult.FIELD_WEBSITE_URL));
        setFieldError(editPhoneErrorLabel, r.getError(AgencyAccountValidationResult.FIELD_PHONE));
        setFieldError(editCountryErrorLabel, r.getError(AgencyAccountValidationResult.FIELD_COUNTRY));
    }

    private static void setFieldError(Label label, String message) {
        if (message == null || message.isBlank()) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
        } else {
            label.setText(message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    /** Adds https:// when missing so stored URLs are consistent after validation. */
    private static String normalizeWebsiteUrl(String urlOrNull) {
        if (urlOrNull == null || urlOrNull.isBlank()) {
            return null;
        }
        String s = urlOrNull.trim();
        if (s.matches("(?i)^https?://.*")) {
            return s;
        }
        return "https://" + s;
    }

    @FXML
    private void showPostsTab() {
        setActiveTab(postsTabButton);
        placeholderTitleLabel.setText("Posts");
        placeholderSubtitleLabel.setText("Share updates, offers, and travel moments from your agency.");
        togglePostsContent(true);
        renderPostsTab();
        animateTabSwitch();
    }

    @FXML
    private void showAboutTab() {
        setActiveTab(aboutTabButton);
        placeholderTitleLabel.setText("About");
        placeholderSubtitleLabel.setText("Agency details and profile sections will appear in this area.");
        togglePostsContent(false);
        animateTabSwitch();
    }

    @FXML
    private void showOffersTab() {
        setActiveTab(offersTabButton);
        placeholderTitleLabel.setText("Offers");
        placeholderSubtitleLabel.setText("Approved offers will be listed here with cards.");
        togglePostsContent(false);
        animateTabSwitch();
    }

    private void animateTabSwitch() {
        if (mainContentPlaceholder == null) {
            return;
        }
        mainContentPlaceholder.setOpacity(0.90);
        FadeTransition fade = new FadeTransition(Duration.millis(170), mainContentPlaceholder);
        fade.setFromValue(0.90);
        fade.setToValue(1.0);
        fade.play();
    }

    private void togglePostsContent(boolean visible) {
        if (postsScrollPane == null) {
            return;
        }
        postsScrollPane.setVisible(visible);
        postsScrollPane.setManaged(visible);
    }

    private void renderPostsTab() {
        if (postsListBox == null) {
            return;
        }
        postsListBox.getChildren().clear();
        if (currentAgency == null || currentAgency.getId() == null) {
            postsListBox.getChildren().add(buildPlaceholder("No agency selected."));
            return;
        }
        Integer viewerId = NavigationManager.getInstance().sessionUser().map(u -> u.getId()).orElse(null);
        String viewerName = NavigationManager.getInstance().sessionUser().map(u -> u.getUsername()).orElse("You");
        try {
            List<AgencyPost> posts = agencyPostService.listByAgency(currentAgency.getId(), viewerId);
            if (posts.isEmpty()) {
                postsListBox.getChildren().add(buildPlaceholder("No agency posts yet."));
                return;
            }
            for (AgencyPost post : posts) {
                postsListBox.getChildren().add(buildPostCard(post, viewerId, viewerName));
            }
        } catch (SQLException e) {
            postsListBox.getChildren().add(buildPlaceholder("Cannot load agency posts: " + e.getMessage()));
        }
    }

    private VBox buildPostCard(AgencyPost post, Integer viewerId, String viewerName) {
        VBox card = new VBox(10);
        card.getStyleClass().add("agency-post-card");

        HBox header = new HBox(8);
        StackPane avatarShell = new StackPane();
        avatarShell.getStyleClass().add("agency-post-avatar-shell");
        if (avatarImageView != null && avatarImageView.getImage() != null) {
            ImageView agencyAvatar = new ImageView(avatarImageView.getImage());
            agencyAvatar.setFitWidth(48);
            agencyAvatar.setFitHeight(48);
            agencyAvatar.setPreserveRatio(false);
            Circle clip = new Circle(24, 24, 24);
            agencyAvatar.setClip(clip);
            avatarShell.getChildren().add(agencyAvatar);
        } else {
            Label fallback = new Label("A");
            fallback.getStyleClass().add("agency-post-avatar-fallback");
            avatarShell.getChildren().add(fallback);
        }

        Label author = new Label(safe(currentAgency != null ? currentAgency.getAgencyName() : post.getAuthorUsername(), "Agency"));
        author.getStyleClass().add("agency-post-author");
        Label date = new Label(formatRelative(post.getCreatedAt()));
        date.getStyleClass().add("agency-post-date");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(avatarShell, author, spacer, date);

        Label title = new Label(safe(post.getTitle(), "Post"));
        title.getStyleClass().add("agency-post-title");
        title.setWrapText(true);

        Label content = new Label(safe(post.getContent(), ""));
        content.getStyleClass().add("agency-post-content");
        content.setWrapText(true);

        card.getChildren().addAll(header, title, content);

        if (!post.getImageAssetIds().isEmpty()) {
            StackPane media = buildPostMedia(post.getImageAssetIds());
            if (media != null) {
                card.getChildren().add(media);
            }
        } else if (coverImageView != null && coverImageView.getImage() != null) {
            StackPane media = buildAgencyCoverMedia();
            if (media != null) {
                card.getChildren().add(media);
            }
        }

        HBox actions = new HBox(10);
        actions.getStyleClass().add("agency-post-actions");
        Button likeButton = new Button(likeButtonText(post.isLikedByViewer(), post.getLikesCount()));
        likeButton.getStyleClass().add("agency-post-action-button");
        Button commentsToggleButton = new Button("💬 " + post.getCommentsCount());
        commentsToggleButton.getStyleClass().add("agency-post-action-button");
        actions.getChildren().addAll(likeButton, commentsToggleButton);
        card.getChildren().add(actions);

        VBox commentsBox = new VBox(8);
        commentsBox.getStyleClass().add("agency-post-comments-box");
        commentsBox.setVisible(false);
        commentsBox.setManaged(false);

        VBox commentsList = new VBox(6);
        commentsList.getStyleClass().add("agency-post-comments-list");
        for (AgencyPostComment comment : post.getComments()) {
            commentsList.getChildren().add(buildCommentNode(comment));
        }
        ScrollPane commentsScroll = new ScrollPane(commentsList);
        commentsScroll.setFitToWidth(true);
        commentsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        commentsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        commentsScroll.setPrefViewportHeight(148);
        commentsScroll.setMaxHeight(148);
        commentsScroll.getStyleClass().add("agency-comments-scroll");
        commentsBox.getChildren().add(commentsScroll);

        if (viewerId != null) {
            HBox addCommentRow = new HBox(8);
            TextField commentField = new TextField();
            commentField.setPromptText("Write a comment...");
            HBox.setHgrow(commentField, Priority.ALWAYS);
            Button sendButton = new Button("Send");
            sendButton.getStyleClass().add("agency-post-send-button");
            sendButton.setOnAction(e -> {
                String typed = trim(commentField.getText());
                if (typed.isBlank()) {
                    return;
                }
                try {
                    AgencyPostComment created = agencyPostService.addComment(post.getId(), viewerId, typed);
                    created.setAuthorUsername(viewerName);
                    post.getComments().add(created);
                    post.setCommentsCount(post.getCommentsCount() + 1);
                    commentsList.getChildren().add(buildCommentNode(created));
                    commentsToggleButton.setText("💬 " + post.getCommentsCount());
                    commentField.clear();
                } catch (SQLException | IllegalArgumentException ex) {
                    feedbackLabel.setText("Comment failed: " + ex.getMessage());
                }
            });
            addCommentRow.getChildren().addAll(commentField, sendButton);
            commentsBox.getChildren().add(addCommentRow);
        }

        commentsToggleButton.setOnAction(e -> {
            boolean show = !commentsBox.isVisible();
            commentsBox.setVisible(show);
            commentsBox.setManaged(show);
        });

        if (viewerId == null) {
            likeButton.setDisable(true);
        } else {
            likeButton.setOnAction(e -> {
                try {
                    AgencyPostService.LikeResult result = agencyPostService.toggleLike(post.getId(), viewerId);
                    post.setLikedByViewer(result.liked());
                    post.setLikesCount(result.likesCount());
                    likeButton.setText(likeButtonText(post.isLikedByViewer(), post.getLikesCount()));
                } catch (SQLException ex) {
                    feedbackLabel.setText("Like action failed: " + ex.getMessage());
                }
            });
        }

        card.getChildren().add(commentsBox);
        return card;
    }

    private VBox buildPlaceholder(String text) {
        VBox box = new VBox();
        box.getStyleClass().add("agency-post-card");
        Label label = new Label(text);
        label.getStyleClass().add("agency-post-content");
        label.setWrapText(true);
        box.getChildren().add(label);
        return box;
    }

    private VBox buildCommentNode(AgencyPostComment comment) {
        VBox node = new VBox();
        node.getStyleClass().add("agency-post-comment");
        VBox body = new VBox(2);
        body.getStyleClass().add("agency-post-comment-body");
        String who = safe(comment.getAuthorUsername(), "User");
        String when = formatRelative(comment.getCreatedAt());
        Label meta = new Label(who + " • " + when);
        meta.getStyleClass().add("agency-post-comment-meta");
        Label content = new Label(safe(comment.getContent(), ""));
        content.getStyleClass().add("agency-post-comment-content");
        content.setWrapText(true);
        body.getChildren().addAll(meta, content);

        StackPane avatarShell = new StackPane();
        avatarShell.getStyleClass().add("agency-post-comment-avatar-shell");
        Image avatar = loadImage(comment.getAuthorProfileImageId());
        if (avatar != null) {
            ImageView avatarView = new ImageView(avatar);
            avatarView.setFitWidth(30);
            avatarView.setFitHeight(30);
            avatarView.setPreserveRatio(false);
            avatarView.setClip(new Circle(15, 15, 15));
            avatarShell.getChildren().add(avatarView);
        } else {
            Label fallback = new Label(who.substring(0, 1).toUpperCase());
            fallback.getStyleClass().add("agency-post-comment-avatar-fallback");
            avatarShell.getChildren().add(fallback);
        }
        HBox row = new HBox(8, avatarShell, body);
        node.getChildren().add(row);
        return node;
    }

    private Image loadImage(Long imageId) {
        if (imageId == null) {
            return null;
        }
        try {
            Optional<ImageAsset> image = imageAssetService.get(imageId);
            if (image.isPresent() && image.get().getData() != null && image.get().getData().length > 0) {
                return new Image(new ByteArrayInputStream(image.get().getData()));
            }
        } catch (SQLException ignored) {
            // Ignore and fallback to initials.
        }
        return null;
    }

    private StackPane buildPostMedia(List<Long> imageAssetIds) {
        List<Image> images = new ArrayList<>();
        for (Long id : imageAssetIds) {
            try {
                Optional<ImageAsset> image = imageAssetService.get(id);
                if (image.isPresent() && image.get().getData() != null && image.get().getData().length > 0) {
                    images.add(new Image(new ByteArrayInputStream(image.get().getData())));
                }
            } catch (SQLException ignored) {
                // Keep carousel usable even if one asset fails.
            }
        }
        if (images.isEmpty()) {
            return null;
        }

        ImageView imageView = new ImageView(images.get(0));
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(260);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("agency-post-image");

        StackPane root = new StackPane(imageView);
        root.getStyleClass().add("agency-post-carousel");
        if (images.size() == 1) {
            return root;
        }

        int[] idx = {0};
        HBox dots = new HBox(6);
        dots.getStyleClass().add("agency-post-carousel-dots");
        List<Button> dotButtons = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            final int dotIndex = i;
            Button dot = new Button();
            dot.getStyleClass().add("agency-post-carousel-dot");
            dot.setOnAction(e -> {
                idx[0] = dotIndex;
                imageView.setImage(images.get(idx[0]));
                for (int j = 0; j < dotButtons.size(); j++) {
                    dotButtons.get(j).getStyleClass().remove("agency-post-carousel-dot-active");
                    if (j == idx[0]) {
                        dotButtons.get(j).getStyleClass().add("agency-post-carousel-dot-active");
                    }
                }
            });
            dotButtons.add(dot);
            dots.getChildren().add(dot);
        }
        dotButtons.get(0).getStyleClass().add("agency-post-carousel-dot-active");

        Button prev = new Button("◀");
        prev.getStyleClass().add("agency-post-carousel-nav");
        prev.setOnAction(e -> {
            idx[0] = (idx[0] - 1 + images.size()) % images.size();
            imageView.setImage(images.get(idx[0]));
            for (int j = 0; j < dotButtons.size(); j++) {
                dotButtons.get(j).getStyleClass().remove("agency-post-carousel-dot-active");
                if (j == idx[0]) {
                    dotButtons.get(j).getStyleClass().add("agency-post-carousel-dot-active");
                }
            }
        });

        Button next = new Button("▶");
        next.getStyleClass().add("agency-post-carousel-nav");
        next.setOnAction(e -> {
            idx[0] = (idx[0] + 1) % images.size();
            imageView.setImage(images.get(idx[0]));
            for (int j = 0; j < dotButtons.size(); j++) {
                dotButtons.get(j).getStyleClass().remove("agency-post-carousel-dot-active");
                if (j == idx[0]) {
                    dotButtons.get(j).getStyleClass().add("agency-post-carousel-dot-active");
                }
            }
        });

        BorderPane controls = new BorderPane();
        controls.getStyleClass().add("agency-post-carousel-controls");
        controls.setLeft(prev);
        controls.setRight(next);
        controls.setBottom(dots);
        BorderPane.setAlignment(dots, javafx.geometry.Pos.BOTTOM_CENTER);

        prev.setOpacity(0);
        next.setOpacity(0);
        root.setOnMouseEntered(e -> {
            prev.setOpacity(1);
            next.setOpacity(1);
        });
        root.setOnMouseExited(e -> {
            prev.setOpacity(0);
            next.setOpacity(0);
        });

        root.getChildren().add(controls);
        return root;
    }

    /**
     * Fallback media for posts without images: current agency cover.
     */
    private StackPane buildAgencyCoverMedia() {
        if (coverImageView == null || coverImageView.getImage() == null) {
            return null;
        }
        ImageView imageView = new ImageView(coverImageView.getImage());
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(260);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("agency-post-image");

        StackPane root = new StackPane(imageView);
        root.getStyleClass().add("agency-post-carousel");
        return root;
    }

    private String formatRelative(LocalDateTime ts) {
        if (ts == null) {
            return "now";
        }
        long seconds = java.time.Duration.between(ts, LocalDateTime.now()).getSeconds();
        if (seconds < 60) {
            return "just now";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h ago";
        }
        long days = hours / 24;
        if (days < 7) {
            return days + "d ago";
        }
        return postDateTimeFmt.format(ts);
    }

    private String likeButtonText(boolean liked, int count) {
        return (liked ? "♥ " : "♡ ") + count;
    }

    private void setActiveTab(Button active) {
        postsTabButton.getStyleClass().remove("agency-tab-pill-active");
        aboutTabButton.getStyleClass().remove("agency-tab-pill-active");
        offersTabButton.getStyleClass().remove("agency-tab-pill-active");
        if (!active.getStyleClass().contains("agency-tab-pill-active")) {
            active.getStyleClass().add("agency-tab-pill-active");
        }
    }

    private void configureImageNodes() {
        if (bannerStack != null && coverImageView != null) {
            coverImageView.fitWidthProperty().bind(bannerStack.widthProperty());
            coverImageView.fitHeightProperty().bind(bannerStack.heightProperty());
        }
        if (avatarImageView != null) {
            double radius = avatarImageView.getFitWidth() / 2.0;
            Circle clip = new Circle(radius, radius, radius);
            avatarImageView.setClip(clip);
        }
    }

    private void loadAgencyImages() {
        if (currentAgency == null || currentAgency.getId() == null) {
            return;
        }
        loadCoverImage();
        loadAvatarImage();
    }

    private void loadCoverImage() {
        try {
            Optional<ImageAsset> cover = agencyService.loadCoverImage(currentAgency.getId());
            if (cover.isPresent() && cover.get().getData() != null && cover.get().getData().length > 0) {
                Image image = new Image(new ByteArrayInputStream(cover.get().getData()));
                coverImageView.setImage(image);
                return;
            }
        } catch (SQLException ignored) {
            // fall through to clear image
        }
        coverImageView.setImage(null);
    }

    private void loadAvatarImage() {
        try {
            Optional<ImageAsset> avatar = agencyService.loadAgencyProfileImage(currentAgency.getId());
            if (avatar.isPresent() && avatar.get().getData() != null && avatar.get().getData().length > 0) {
                Image image = new Image(new ByteArrayInputStream(avatar.get().getData()));
                avatarImageView.setImage(image);
                avatarFallbackLabel.setVisible(false);
                avatarFallbackLabel.setManaged(false);
                return;
            }
        } catch (SQLException ignored) {
            // fall through to fallback label
        }
        avatarImageView.setImage(null);
        avatarFallbackLabel.setVisible(true);
        avatarFallbackLabel.setManaged(true);
    }

    @FXML private void onEditAvatar() { uploadAgencyImage(false); }
    @FXML private void onEditNameField() { setEditMode(true); editAgencyNameField.requestFocus(); }
    @FXML private void onEditDescriptionField() { setEditMode(true); editDescriptionField.requestFocus(); }
    @FXML private void onEditAddressField() { setEditMode(true); editAddressField.requestFocus(); }
    @FXML private void onEditWebsiteField() { setEditMode(true); editWebsiteField.requestFocus(); }
    @FXML private void onEditPhoneField() { setEditMode(true); editPhoneField.requestFocus(); }
    @FXML private void onEditCountryField() { setEditMode(true); editCountryField.requestFocus(); }

    @FXML private void onBackToAgencies() { NavigationManager.getInstance().showSignedInAgencies(); }
    @FXML private void onThemeToggle() { NavigationManager.getInstance().toggleTheme(); }
    @FXML private void onLogout() { NavigationManager.getInstance().logoutToGuest(); }
    @FXML private void onHome() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onOffres() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onAgences() { NavigationManager.getInstance().showSignedInAgencies(); }
    @FXML private void onMessagerie() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onRecommandation() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onEvenement() { NavigationManager.getInstance().showSignedInEvents(); }
    @FXML private void onPremium() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onNotifications() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onProfile() { NavigationManager.getInstance().showUserProfile(); }
    @FXML private void onDashboardIa() { NavigationManager.getInstance().showSignedInShell(); }

    @FXML
    private void onAddPost() {
        if (!canEditAgency || currentAgency == null || currentAgency.getId() == null) {
            feedbackLabel.setText("You are not allowed to add posts for this agency.");
            return;
        }
        NavigationManager.getInstance().showAgencyPostCreate();
    }

    private String safe(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v;
    }

    private String trim(String v) {
        return v == null ? "" : v.trim();
    }

    private String blankToNull(String v) {
        String t = trim(v);
        return t.isBlank() ? null : t;
    }

    private void bindFieldRow(HBox row, Label label, String value, String title) {
        if (value == null || value.isBlank()) {
            row.setVisible(false);
            row.setManaged(false);
            return;
        }
        row.setVisible(true);
        row.setManaged(true);
        label.setText(title.isBlank() ? value : (title + ": " + value));
    }

    private void showInfoToast(String text) {
        Notifications.create()
                .title("Agency Profile")
                .text(text)
                .hideAfter(Duration.seconds(2.1))
                .showInformation();
    }

    @FXML
    private void onEditCover() {
        uploadAgencyImage(true);
    }

    private void uploadAgencyImage(boolean cover) {
        if (!canEditAgency) {
            feedbackLabel.setText("You are not allowed to update images for this agency.");
            return;
        }
        if (currentAgency == null || currentAgency.getId() == null) {
            feedbackLabel.setText("No agency loaded.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(cover ? "Choose cover image" : "Choose profile image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File file = chooser.showOpenDialog(editCoverButton.getScene() != null ? editCoverButton.getScene().getWindow() : null);
        if (file == null) {
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String mime = detectMime(file);
            if (mime == null) {
                feedbackLabel.setText("Unsupported image format. Use PNG/JPG/GIF/WEBP.");
                return;
            }
            if (cover) {
                agencyService.replaceCoverImage(currentAgency.getId(), bytes, mime);
            } else {
                agencyService.replaceAgencyProfileImage(currentAgency.getId(), bytes, mime);
            }
            reloadCurrentAgency();
            if (cover) {
                refreshPostsUiAfterAgencyCoverChanged();
            } else {
                refreshPostsUiAfterAgencyAvatarChanged();
            }
            feedbackLabel.setText(cover ? "Cover image updated." : "Profile image updated.");
            showInfoToast(cover ? "Cover image updated." : "Profile image updated.");
        } catch (IOException e) {
            feedbackLabel.setText("Cannot read selected file: " + e.getMessage());
        } catch (SQLException | IllegalArgumentException e) {
            feedbackLabel.setText("Image update failed: " + e.getMessage());
        }
    }

    private void reloadCurrentAgency() throws SQLException {
        if (currentAgency == null || currentAgency.getId() == null) {
            return;
        }
        Optional<AgencyAccount> refreshed = agencyService.get(currentAgency.getId());
        if (refreshed.isPresent()) {
            currentAgency = refreshed.get();
            bindAgencyToView();
        }
    }

    /**
     * Post cards use the agency cover as fallback media when a post has no images.
     */
    private void refreshPostsUiAfterAgencyCoverChanged() {
        refreshPostsTabIfPostsPaneVisible();
    }

    /**
     * Post headers clone the main agency avatar {@link ImageView} image; rebuild cards so avatars match.
     */
    private void refreshPostsUiAfterAgencyAvatarChanged() {
        refreshPostsTabIfPostsPaneVisible();
    }

    private void refreshPostsTabIfPostsPaneVisible() {
        if (postsScrollPane != null && postsScrollPane.isVisible()) {
            renderPostsTab();
        }
    }

    private String detectMime(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        return null;
    }
}
