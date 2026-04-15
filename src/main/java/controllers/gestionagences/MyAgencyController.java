package controllers.gestionagences;

import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import models.gestionagences.AgencyAccount;
import models.gestionagences.ImageAsset;
import org.controlsfx.control.Notifications;
import services.gestionagences.AgencyAccountService;
import utils.NavigationManager;

import java.io.ByteArrayInputStream;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.sql.SQLException;
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

    @FXML private JFXButton postsTabButton;
    @FXML private JFXButton aboutTabButton;
    @FXML private JFXButton offersTabButton;
    @FXML private Label placeholderTitleLabel;
    @FXML private Label placeholderSubtitleLabel;

    @FXML private TextField editAgencyNameField;
    @FXML private TextField editWebsiteField;
    @FXML private TextField editPhoneField;
    @FXML private TextField editAddressField;
    @FXML private TextField editCountryField;
    @FXML private TextArea editDescriptionField;
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
                feedbackLabel.setText("Read-only profile. Only agency owner admin can update this page.");
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
                editButton
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
        if (!canEditAgency) {
            feedbackLabel.setText("You are not allowed to update this agency.");
            return;
        }
        if (currentAgency == null) {
            feedbackLabel.setText("No agency loaded.");
            return;
        }
        String agencyName = trim(editAgencyNameField.getText());
        String description = trim(editDescriptionField.getText());
        if (agencyName.isBlank() || description.isBlank()) {
            feedbackLabel.setText("Agency name and description are required.");
            return;
        }
        currentAgency.setAgencyName(agencyName);
        currentAgency.setDescription(description);
        currentAgency.setWebsiteUrl(blankToNull(editWebsiteField.getText()));
        currentAgency.setPhone(blankToNull(editPhoneField.getText()));
        currentAgency.setAddress(blankToNull(editAddressField.getText()));
        currentAgency.setCountry(blankToNull(editCountryField.getText()));
        try {
            agencyService.update(currentAgency);
            bindAgencyToView();
            setEditMode(false);
            feedbackLabel.setText("Agency updated successfully.");
        } catch (SQLException | IllegalArgumentException e) {
            feedbackLabel.setText("Update failed: " + e.getMessage());
        }
    }

    @FXML
    private void showPostsTab() {
        setActiveTab(postsTabButton);
        placeholderTitleLabel.setText("Posts");
        placeholderSubtitleLabel.setText("Main feed area is ready for posts and interactions.");
    }

    @FXML
    private void showAboutTab() {
        setActiveTab(aboutTabButton);
        placeholderTitleLabel.setText("About");
        placeholderSubtitleLabel.setText("Agency details and profile sections will appear in this area.");
    }

    @FXML
    private void showOffersTab() {
        setActiveTab(offersTabButton);
        placeholderTitleLabel.setText("Offers");
        placeholderSubtitleLabel.setText("Approved offers will be listed here with cards.");
    }

    private void setActiveTab(JFXButton active) {
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
            Circle clip = new Circle(60, 60, 60);
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
    @FXML private void onEvenement() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onPremium() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onNotifications() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onProfile() { NavigationManager.getInstance().showSignedInShell(); }
    @FXML private void onDashboardIa() { NavigationManager.getInstance().showSignedInShell(); }

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

    private String detectMime(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        return null;
    }
}
