package controllers.gestionutilisateurs;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import utils.NavigationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FeedbacksGuestController {

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

    @FXML
    private void initialize() {
        installLibraryTheme();
        mockFeedbacks();
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

    private void mockFeedbacks() {
        allFeedbacks.clear();
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
        FeedbackPreview f1 = list.size() > 0 ? list.get(0) : allFeedbacks.get(0);
        FeedbackPreview f2 = list.size() > 1 ? list.get(1) : allFeedbacks.get(1);
        FeedbackPreview f3 = list.size() > 2 ? list.get(2) : allFeedbacks.get(2);
        FeedbackPreview f4 = list.size() > 3 ? list.get(3) : allFeedbacks.get(3);
        FeedbackPreview f5 = list.size() > 4 ? list.get(4) : allFeedbacks.get(4);
        apply(f1, avatar1, name1, role1, stars1, opinion1);
        apply(f2, avatar2, name2, role2, stars2, opinion2);
        apply(f3, avatar3, name3, role3, stars3, opinion3);
        apply(f4, avatar4, name4, role4, stars4, opinion4);
        apply(f5, avatar5, name5, role5, stars5, opinion5);
    }

    private void apply(FeedbackPreview feedback, ImageView avatar, Label name, Label role, Label stars, Label opinion) {
        name.setText(feedback.name);
        role.setText(feedback.role);
        stars.setText("★".repeat(Math.max(1, Math.min(5, feedback.stars))));
        opinion.setText(feedback.opinion);
        avatar.setImage(new Image(feedback.avatarUrl, true));
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
}
