package utils;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import enums.gestionutilisateurs.UserRole;
import models.gestionposts.Post;
import models.gestionutilisateurs.User;
import services.gestionutilisateurs.UserService;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
public class NavigationManager {

    private static NavigationManager instance;

    private Stage stage;
    private Scene sharedScene;
    private boolean lightTheme;
    private UserService userService;
    private User sessionUser;
    private Post selectedPost;
    private Long selectedAgencyId;
    private Instant sessionIssuedAt;
    private static final java.time.Duration SESSION_TTL = java.time.Duration.ofDays(7);

    private static volatile boolean controlsFxCssBridgeInstalled;

    private static final Set<Window> controlsFxBridgedWindows =
            Collections.newSetFromMap(new WeakHashMap<>());

    private NavigationManager() {
    }

    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    public void configure(Stage primaryStage, UserService userSvc) {
        this.stage = primaryStage;
        this.userService = userSvc;
        this.stage.setMinWidth(1100);
        this.stage.setMinHeight(700);
        installControlsFxModenaBridge();
        var bridgeResEarly = NavigationManager.class.getResource("/css/controlsfx-modena-bridge.css");
        if (bridgeResEarly != null) {
            hookWindowSceneForControlsFxBridge(primaryStage, bridgeResEarly.toExternalForm());
        }
    }

    public UserService userService() {
        return userService;
    }

    public Optional<User> sessionUser() {
        if (!hasValidSession()) {
            clearSession();
            return Optional.empty();
        }
        return Optional.ofNullable(sessionUser);
    }

    public void setSessionUser(User source) {
        if (source == null) {
            sessionUser = null;
            return;
        }
        User copy = new User();
        copy.setId(source.getId());
        copy.setUsername(source.getUsername());
        copy.setEmail(source.getEmail());
        copy.setRoles(source.getRoles());
        copy.setRole(source.getRole());
        copy.setIsActive(source.getIsActive());
        copy.setProfileImageId(source.getProfileImageId());
        copy.setPhone(source.getPhone());
        copy.setEmailVerified(source.getEmailVerified());
        copy.setFaceVerified(source.getFaceVerified());
        sessionUser = copy;
        normalizeSessionRoles();
        sessionIssuedAt = Instant.now();
    }

    public void clearSession() {
        sessionUser = null;
        sessionIssuedAt = null;
        selectedAgencyId = null;
    }

    public boolean isAuthenticated() {
        return sessionUser().isPresent();
    }

    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        Optional<User> current = sessionUser();
        if (current.isEmpty()) {
            return false;
        }
        List<String> expanded = expandRoles(current.get().getRoles());
        return expanded.contains(role.trim().toUpperCase(Locale.ROOT));
    }

    public boolean canAccessSignedInShell() {
        Optional<User> current = sessionUser();
        if (current.isEmpty()) {
            return false;
        }
        Boolean isActive = current.get().getIsActive();
        if (isActive != null && !isActive) {
            return false;
        }
        return hasRole(UserRole.USER.getValue())
                || hasRole(UserRole.AGENCY_ADMIN.getValue())
                || hasRole(UserRole.ADMIN.getValue());
    }

    public boolean canAccessAgencyAdminFeatures() {
        return hasRole(UserRole.AGENCY_ADMIN.getValue());
    }

    public boolean canAccessAdminFeatures() {
        return hasRole(UserRole.ADMIN.getValue());
    }

    public void logoutToGuest() {
        clearSession();
        showWelcome();
    }

    public Optional<Long> selectedAgencyId() {
        return Optional.ofNullable(selectedAgencyId);
    }

    public void setSelectedAgencyId(Long agencyId) {
        this.selectedAgencyId = agencyId;
    }

    public Optional<Post> selectedPost() {
        return Optional.ofNullable(selectedPost);
    }

    public Post getSelectedPost() {
        return selectedPost;
    }

    public void setSelectedPost(Post post) {
        this.selectedPost = post;
    }

    public void showPostLoginHome() {
        if (!canAccessSignedInShell()) {
            clearSession();
            showLogin();
            return;
        }
        if (canAccessAdminFeatures()) {
            showAdminDashboard();
            return;
        }
        showSignedInShell();
    }

    public void showSignedInShell() {
        if (!canAccessSignedInShell()) {
            clearSession();
            showLogin();
            return;
        }
        loadScene("/fxml/user/signed_in_shell.fxml", "Smart Voyage - Home");
    }

    public void showSignedInAgencies() {
        if (!canAccessSignedInShell()) {
            clearSession();
            showLogin();
            return;
        }
        loadScene("/fxml/agency/agencies_signed_in.fxml", "Smart Voyage - Agencies");
    }

    public void showSignedInEvents() {
        if (!canAccessSignedInShell()) {
            clearSession();
            showLogin();
            return;
        }
        loadScene("/fxml/user/events_signed_in.fxml", "Smart Voyage - Evenements");
    }

    public void showSignedInOffers() {
        if (!canAccessSignedInShell()) {
            clearSession();
            showLogin();
            return;
        }
        loadScene("/fxml/user/offers_guest.fxml", "Smart Voyage - Offres");
    }

    public void showAgencyProposal() {
        if (!canAccessSignedInShell()) {
            clearSession();
            showLogin();
            return;
        }
        loadScene("/fxml/agency/agency_proposal.fxml", "Smart Voyage - Mon agence");
    }

    public void showMyAgency() {
        if (!canAccessAgencyAdminFeatures()) {
            showSignedInAgencies();
            return;
        }
        selectedAgencyId = null;
        loadScene("/fxml/agency/my_agency.fxml", "Smart Voyage - My Agency");
    }

    public void showAgencyProfile(Long agencyId) {
        if (!canAccessSignedInShell()) {
            clearSession();
            showLogin();
            return;
        }
        selectedAgencyId = agencyId;
        loadScene("/fxml/agency/my_agency.fxml", "Smart Voyage - Agency");
    }

    public void showAgencyPostCreate() {
        if (!canAccessSignedInShell()) {
            clearSession();
            showLogin();
            return;
        }
        loadScene("/fxml/agency/agency_post_create.fxml", "Smart Voyage - Add Agency Post");
    }

    public void showUserProfile() {
        if (!canAccessSignedInShell()) {
            clearSession();
            showLogin();
            return;
        }
        loadScene("/fxml/user/user_profile.fxml", "Smart Voyage - Mon Profil");
    }

    public void showWelcome() {
        loadScene("/fxml/user/welcome.fxml", "Smart Voyage");
    }

    public void showGuestOffers() {
        loadScene("/fxml/user/offers_guest.fxml", "Offers");
    }

    public void showGuestFeedbacks() {
        loadScene("/fxml/user/feedbacks_guest.fxml", "Feedbacks");
    }

    public void showGuestCrew() {
        loadScene("/fxml/user/crew_guest.fxml", "Crew");
    }

    public void showLogin() {
        loadScene("/fxml/user/login.fxml", "Sign in");
    }

    public void showRegister() {
        loadScene("/fxml/user/register.fxml", "Sign up");
    }

    public void showAdminLogin() {
        loadScene("/fxml/user/admin_login.fxml", "Admin Sign in");
    }

    public void showAdminDashboard() {
        if (!canAccessAdminFeatures()) {
            showPostLoginHome();
            return;
        }
        loadScene("/fxml/user/admin_dashboard.fxml", "Smart Voyage - Admin Dashboard");
    }

    public void showSignedInPosts() {
        if (!canAccessSignedInShell()) {
            clearSession();
            showLogin();
            return;
        }
        loadScene("/fxml/posts/posts_view.fxml", "Smart Voyage - Posts");
    }

    public void showPostDetail(Post post) {
        if (!canAccessSignedInShell()) {
            clearSession();
            showLogin();
            return;
        }
        this.selectedPost = post;
        loadScene("/fxml/posts/post_detail.fxml", "Smart Voyage - Post Detail");
    }

    private void loadScene(String resource, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(NavigationManager.class.getResource(resource)));
            Parent root = loader.load();
            applyScene(root, title);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger " + resource, e);
        }
    }

    private void applyScene(Parent root, String title) {
        applyThemeClass(root);
        if (sharedScene == null) {
            sharedScene = new Scene(root);
            attachAppStylesheets(sharedScene);
            stage.setScene(sharedScene);
        } else {
            sharedScene.setRoot(root);
        }
        stage.setTitle(title);
        if (!stage.isShowing()) {
            stage.show();
        }
        if (!stage.isMaximized()) {
            Platform.runLater(() -> {
                if (!stage.isMaximized()) {
                    stage.setMaximized(true);
                }
            });
        }
    }

    private static void attachAppStylesheets(Scene scene) {
        if (scene == null) {
            return;
        }
        var css = NavigationManager.class.getResource("/css/styles.css");
        if (css != null) {
            String form = css.toExternalForm();
            if (!scene.getStylesheets().contains(form)) {
                scene.getStylesheets().add(form);
            }
        }
        var postsCss = NavigationManager.class.getResource("/css/posts_styles.css");
        if (postsCss != null) {
            String postsForm = postsCss.toExternalForm();
            if (!scene.getStylesheets().contains(postsForm)) {
                scene.getStylesheets().add(postsForm);
            }
        }
        var bridge = NavigationManager.class.getResource("/css/controlsfx-modena-bridge.css");
        if (bridge != null) {
            String b = bridge.toExternalForm();
            if (!scene.getStylesheets().contains(b)) {
                scene.getStylesheets().add(0, b);
            }
        }
    }

    /**
     * ControlsFX notifications use a separate popup {@link Scene}; it does not inherit {@code styles.css}.
     * Define Modena paint names expected by {@code notificationpopup.css} so JavaFX 21 does not log Paint cast warnings.
     */
    private static void installControlsFxModenaBridge() {
        if (controlsFxCssBridgeInstalled) {
            return;
        }
        synchronized (NavigationManager.class) {
            if (controlsFxCssBridgeInstalled) {
                return;
            }
            var bridgeRes = NavigationManager.class.getResource("/css/controlsfx-modena-bridge.css");
            if (bridgeRes == null) {
                controlsFxCssBridgeInstalled = true;
                return;
            }
            final String bridgeUrl = bridgeRes.toExternalForm();
            for (Window w : Window.getWindows()) {
                hookWindowSceneForControlsFxBridge(w, bridgeUrl);
            }
            Window.getWindows().addListener((ListChangeListener<Window>) c -> {
                while (c.next()) {
                    for (Window w : c.getAddedSubList()) {
                        hookWindowSceneForControlsFxBridge(w, bridgeUrl);
                    }
                }
            });
            controlsFxCssBridgeInstalled = true;
        }
    }

    private static void hookWindowSceneForControlsFxBridge(Window window, String bridgeUrl) {
        if (window == null || bridgeUrl == null || bridgeUrl.isBlank()) {
            return;
        }
        synchronized (NavigationManager.class) {
            if (!controlsFxBridgedWindows.add(window)) {
                return;
            }
        }
        window.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                addControlsFxBridgeFirst(newScene, bridgeUrl);
            }
        });
        if (window.getScene() != null) {
            addControlsFxBridgeFirst(window.getScene(), bridgeUrl);
        }
    }

    private static void addControlsFxBridgeFirst(Scene scene, String bridgeUrl) {
        if (scene.getStylesheets().contains(bridgeUrl)) {
            return;
        }
        scene.getStylesheets().add(0, bridgeUrl);
    }

    public void toggleTheme() {
        if (sharedScene == null || sharedScene.getRoot() == null) {
            return;
        }
        Parent root = sharedScene.getRoot();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(130), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.86);
        fadeOut.setOnFinished(evt -> {
            lightTheme = !lightTheme;
            applyThemeClass(root);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(130), root);
            fadeIn.setFromValue(0.86);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void applyThemeClass(Parent root) {
        if (root == null) {
            return;
        }
        if (lightTheme) {
            if (!root.getStyleClass().contains("theme-light")) {
                root.getStyleClass().add("theme-light");
            }
        } else {
            root.getStyleClass().remove("theme-light");
        }
    }

    private boolean hasValidSession() {
        if (sessionUser == null || sessionIssuedAt == null) {
            return false;
        }
        return Instant.now().isBefore(sessionIssuedAt.plus(SESSION_TTL));
    }

    private void normalizeSessionRoles() {
        if (sessionUser == null) {
            return;
        }
        List<String> baseRoles = new ArrayList<>();
        if (sessionUser.getRoles() != null) {
            baseRoles.addAll(sessionUser.getRoles());
        }
        if (sessionUser.getRole() != null && !sessionUser.getRole().isBlank()) {
            baseRoles.add(sessionUser.getRole());
        }
        sessionUser.setRoles(expandRoles(baseRoles));
    }

    private List<String> expandRoles(List<String> rawRoles) {
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        if (rawRoles != null) {
            for (String role : rawRoles) {
                if (role == null || role.isBlank()) {
                    continue;
                }
                String normalized = normalizeRoleToken(role);
                expanded.add(normalized);
                if (UserRole.ADMIN.getValue().equals(normalized)) {
                    expanded.add(UserRole.AGENCY_ADMIN.getValue());
                    expanded.add(UserRole.USER.getValue());
                } else if (UserRole.AGENCY_ADMIN.getValue().equals(normalized)) {
                    expanded.add(UserRole.USER.getValue());
                }
            }
        }
        if (expanded.isEmpty()) {
            expanded.add(UserRole.USER.getValue());
        }
        return new ArrayList<>(expanded);
    }

    private String normalizeRoleToken(String rawRole) {
        String normalized = rawRole.trim().toUpperCase(Locale.ROOT);
        String token = normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
        return switch (token) {
            case "USER" -> UserRole.USER.getValue();
            case "ADMIN" -> UserRole.ADMIN.getValue();
            case "AGENCY_ADMIN", "AGENCYADMIN", "AGENCE_ADMIN" -> UserRole.AGENCY_ADMIN.getValue();
            default -> normalized;
        };
    }
}
