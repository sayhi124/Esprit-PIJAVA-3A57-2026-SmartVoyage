package controllers.gestionutilisateurs;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import services.gestionutilisateurs.UserService;
import services.gestionutilisateurs.UserFeedbackService;
import utils.NavigationManager;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class FeedbacksGuestController {
    private static final String DEFAULT_AVATAR = "https://i.pravatar.cc/200?img=13";
    private static final int MAX_FEEDBACKS_IN_MEMORY = 30;

    @FXML
    private TextField searchField;

    @FXML
    private ImageView avatar1;
    @FXML
    private ImageView avatar2;
    @FXML
    private ImageView avatar3;
    @FXML
    private ImageView avatar4;
    @FXML
    private ImageView avatar5;

    @FXML
    private Label name1;
    @FXML
    private Label name2;
    @FXML
    private Label name3;
    @FXML
    private Label name4;
    @FXML
    private Label name5;

    @FXML
    private Label role1;
    @FXML
    private Label role2;
    @FXML
    private Label role3;
    @FXML
    private Label role4;
    @FXML
    private Label role5;

    @FXML
    private Label stars1;
    @FXML
    private Label stars2;
    @FXML
    private Label stars3;
    @FXML
    private Label stars4;
    @FXML
    private Label stars5;

    @FXML
    private Label opinion1;
    @FXML
    private Label opinion2;
    @FXML
    private Label opinion3;
    @FXML
    private Label opinion4;
    @FXML
    private Label opinion5;

    private final List<FeedbackPreview> allFeedbacks = new ArrayList<>();
    private final UserFeedbackService userFeedbackService = new UserFeedbackService();
    private final UserService userService = new UserService();
    private final Map<Integer, Image> profileAvatarCache = new HashMap<>();

    @FXML
    private void initialize() {
        installLibraryTheme();
        loadFeedbacksFromBackend();
        clipAvatar(avatar1);
        clipAvatar(avatar2);
        clipAvatar(avatar3);
        clipAvatar(avatar4);
        clipAvatar(avatar5);
        render(allFeedbacks);
    }

    private void installLibraryTheme() {
        String current = Application.getUserAgentStylesheet();
        String primer = new PrimerDark().getUserAgentStylesheet();
        if (current == null || current.isBlank() || !current.equals(primer)) {
            Application.setUserAgentStylesheet(primer);
        }
    }

    private void loadFeedbacksFromBackend() {
        allFeedbacks.clear();
        try {
            List<UserFeedbackService.PublicFeedbackView> rows = userFeedbackService.findRecentPublic(MAX_FEEDBACKS_IN_MEMORY);
            for (UserFeedbackService.PublicFeedbackView row : rows) {
                allFeedbacks.add(new FeedbackPreview(
                        shortenName(row.getUsername(), row.getUserId()),
                        mapRoleLabel(row.getRole()),
                        row.getStars() == null ? 5 : row.getStars(),
                        quoteText(row.getNote()),
                        resolveAvatarForUser(row.getUserId())
                ));
            }
        } catch (SQLException ignored) {
            // Fall back to static samples if DB is not reachable.
        }
        if (!allFeedbacks.isEmpty()) {
            return;
        }
        addMockFeedbacks();
    }

    private void addMockFeedbacks() {
        allFeedbacks.add(new FeedbackPreview("Aziz S.", "Traveller", 5,
                "\"SmartVoyage is a game-changer for travel lovers. Finding trips is easy, the interface is clean, and I love the agency profiles and offers.\"",
                "https://i.pravatar.cc/200?img=13"));
        allFeedbacks.add(new FeedbackPreview("Mouti H.", "Traveller", 5,
                "\"I did not expect to love it this much. It is my go-to for discovering local agencies and great deals from beach getaways to city breaks.\"",
                "https://i.pravatar.cc/200?img=47"));
        allFeedbacks.add(new FeedbackPreview("Raouf K.", "Agency Admin", 5,
                "\"Excellent platform for our agency. We get more visibility and engagement, and it is a modern place to connect with travelers.\"",
                "https://i.pravatar.cc/200?img=33"));
        allFeedbacks.add(new FeedbackPreview("Nour L.", "Traveller", 5,
                "\"The offer previews are beautiful and easy to compare. I can decide faster before signing in.\"",
                "https://i.pravatar.cc/200?img=26"));
        allFeedbacks.add(new FeedbackPreview("Karim T.", "Traveller", 4,
                "\"I love the premium desktop design and smooth navigation. The experience feels polished and trustworthy.\"",
                "https://i.pravatar.cc/200?img=16"));
    }

    private void render(List<FeedbackPreview> list) {
        FeedbackPreview f1 = pickFeedback(list, 0);
        FeedbackPreview f2 = pickFeedback(list, 1);
        FeedbackPreview f3 = pickFeedback(list, 2);
        FeedbackPreview f4 = pickFeedback(list, 3);
        FeedbackPreview f5 = pickFeedback(list, 4);
        apply(f1, avatar1, name1, role1, stars1, opinion1);
        apply(f2, avatar2, name2, role2, stars2, opinion2);
        apply(f3, avatar3, name3, role3, stars3, opinion3);
        apply(f4, avatar4, name4, role4, stars4, opinion4);
        apply(f5, avatar5, name5, role5, stars5, opinion5);
    }

    private void apply(FeedbackPreview feedback, ImageView avatar, Label name, Label role, Label stars, Label opinion) {
        name.setText(feedback.name);
        role.setText("\"" + feedback.role + "\"");
        stars.setText("★".repeat(Math.max(1, Math.min(5, feedback.stars))));
        opinion.setText(feedback.opinion);
        String avatarRef = feedback.avatarUrl == null || feedback.avatarUrl.isBlank() ? DEFAULT_AVATAR : feedback.avatarUrl;
        if (avatarRef.startsWith("cache:")) {
            try {
                Integer userId = Integer.parseInt(avatarRef.substring("cache:".length()));
                Image cached = profileAvatarCache.get(userId);
                if (cached != null) {
                    avatar.setImage(cached);
                    return;
                }
            } catch (NumberFormatException ignored) {
                // Continue with default URL loading.
            }
        }
        avatar.setImage(new Image(avatarRef, true));
    }

    private void clipAvatar(ImageView avatar) {
        Circle clip = new Circle(56, 56, 56);
        avatar.setClip(clip);
    }

    @FXML
    private void onSearchInput() {
        String query = (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase(Locale.ROOT);
        List<FeedbackPreview> filtered = allFeedbacks.stream()
                .filter(f -> query.isEmpty()
                        || f.name.toLowerCase(Locale.ROOT).contains(query)
                        || f.role.toLowerCase(Locale.ROOT).contains(query)
                        || f.opinion.toLowerCase(Locale.ROOT).contains(query))
                .collect(Collectors.toList());
        render(filtered);
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
        // Already on feedbacks page.
    }

    @FXML
    private void onCrew() {
        NavigationManager.getInstance().showGuestCrew();
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

    private static final class FeedbackPreview {
        private final String name;
        private final String role;
        private final int stars;
        private final String opinion;
        private final String avatarUrl;

        private FeedbackPreview(String name, String role, int stars, String opinion, String avatarUrl) {
            this.name = name;
            this.role = role;
            this.stars = stars;
            this.opinion = opinion;
            this.avatarUrl = avatarUrl;
        }
    }

    private FeedbackPreview pickFeedback(List<FeedbackPreview> source, int index) {
        if (source != null && source.size() > index) {
            return source.get(index);
        }
        if (allFeedbacks.size() > index) {
            return allFeedbacks.get(index);
        }
        if (!allFeedbacks.isEmpty()) {
            return allFeedbacks.get(allFeedbacks.size() - 1);
        }
        return new FeedbackPreview(
                "SmartVoyage User",
                "Traveller",
                5,
                "\"Great experience on SmartVoyage.\"",
                DEFAULT_AVATAR
        );
    }

    private static String shortenName(String username, Integer userId) {
        String base = username == null || username.isBlank() ? "User " + (userId == null ? "" : "#" + userId) : username.trim();
        if (base.length() <= 20) {
            return base;
        }
        return base.substring(0, 17) + "...";
    }

    private static String mapRoleLabel(String role) {
        if (role == null || role.isBlank()) {
            return "Traveller";
        }
        String r = role.trim().toUpperCase(Locale.ROOT);
        if (r.contains("AGENCY")) {
            return "Agency Admin";
        }
        if (r.contains("ADMIN")) {
            return "Admin";
        }
        return "Traveller";
    }

    private static String quoteText(String note) {
        String text = note == null ? "" : note.trim();
        if (text.isEmpty()) {
            return "\"Great experience on SmartVoyage.\"";
        }
        return "\"" + text + "\"";
    }

    private static String avatarByUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            return DEFAULT_AVATAR;
        }
        int mapped = (Math.abs(userId) % 70) + 1;
        return "https://i.pravatar.cc/200?img=" + mapped;
    }

    private String resolveAvatarForUser(Integer userId) {
        if (userId == null || userId <= 0) {
            return DEFAULT_AVATAR;
        }
        if (profileAvatarCache.containsKey(userId)) {
            return "cache:" + userId;
        }
        try {
            var imgOpt = userService.loadProfileImage(userId);
            if (imgOpt.isPresent() && imgOpt.get().getData() != null && imgOpt.get().getData().length > 0) {
                Image img = new Image(new ByteArrayInputStream(imgOpt.get().getData()));
                profileAvatarCache.put(userId, img);
                return "cache:" + userId;
            }
        } catch (SQLException ignored) {
            // Keep fallback.
        }
        return avatarByUserId(userId);
    }
}
