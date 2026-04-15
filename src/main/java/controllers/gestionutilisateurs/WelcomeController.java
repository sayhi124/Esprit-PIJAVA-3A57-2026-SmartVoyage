package controllers.gestionutilisateurs;

import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import utils.NavigationManager;

/** Immersive welcome landing; auth via {@link NavigationManager}. Images: {@code /images/welcome/*.jpg} (Unsplash). */
public class WelcomeController {

    private static final String HERO_IMAGE = "/images/welcome/hero-aerial-lagoon.jpg";
    private static final String FEATURED_CARD_IMAGE = "/images/welcome/featured-maldives-beach.jpg";
    private static final String FEATURED_BACK_IMAGE = "/images/welcome/featured-paris-eiffel.jpg";

    @FXML
    private StackPane heroMediaStack;

    @FXML
    private ImageView heroBackgroundImage;

    @FXML
    private ImageView featuredCardImage;

    @FXML
    private ImageView featuredCardBackImage;
    @FXML
    private JFXButton getStartedButton;
    @FXML
    private JFXButton seeOffersButton;
    @FXML
    private JFXButton viewDetailsButton;
    @FXML
    private void initialize() {
        if (heroBackgroundImage != null && heroMediaStack != null) {
            heroBackgroundImage.fitWidthProperty().bind(heroMediaStack.widthProperty());
            heroBackgroundImage.fitHeightProperty().bind(heroMediaStack.heightProperty());
        }
        loadBundledImage(heroBackgroundImage, HERO_IMAGE, 1920, 1080, false);
        loadBundledImage(featuredCardImage, FEATURED_CARD_IMAGE, 1200, 1600, false);
        loadBundledImage(featuredCardBackImage, FEATURED_BACK_IMAGE, 900, 1200, false);
        bindCardImage(featuredCardImage);
        bindCardImage(featuredCardBackImage);
        installHeroParallax();
        wireCriticalButtons();
    }

    /** Subtle mouse parallax on hero photo (scaled slightly so edges stay covered when shifted). */
    private void installHeroParallax() {
        if (heroMediaStack == null || heroBackgroundImage == null) {
            return;
        }
        var clip = new Rectangle();
        clip.widthProperty().bind(heroMediaStack.widthProperty());
        clip.heightProperty().bind(heroMediaStack.heightProperty());
        heroMediaStack.setClip(clip);
        heroBackgroundImage.setScaleX(1.06);
        heroBackgroundImage.setScaleY(1.06);
        heroMediaStack.setOnMouseMoved(e -> {
            double w = Math.max(heroMediaStack.getWidth(), 1);
            double h = Math.max(heroMediaStack.getHeight(), 1);
            double nx = (e.getX() / w - 0.5) * 14;
            double ny = (e.getY() / h - 0.5) * 9;
            heroBackgroundImage.setTranslateX(nx);
            heroBackgroundImage.setTranslateY(ny);
        });
    }

    private static void bindCardImage(ImageView view) {
        if (view == null) {
            return;
        }
        var p = view.getParent();
        if (!(p instanceof Region region)) {
            return;
        }
        view.fitWidthProperty().bind(region.widthProperty());
        view.fitHeightProperty().bind(region.heightProperty());
    }

    private static void loadBundledImage(ImageView view, String classpathPath, double w, double h, boolean preserveRatio) {
        if (view == null || classpathPath == null || classpathPath.isBlank()) {
            return;
        }
        var url = WelcomeController.class.getResource(classpathPath);
        if (url == null) {
            return;
        }
        Image img = new Image(url.toExternalForm(), w, h, preserveRatio, true, true);
        view.setImage(img);
        view.setSmooth(true);
        view.setCache(true);
    }

    private void wireCriticalButtons() {
        if (getStartedButton != null) {
            getStartedButton.toFront();
            getStartedButton.setDisable(false);
            getStartedButton.setMouseTransparent(false);
            getStartedButton.setOnAction(e -> onGetStarted());
            getStartedButton.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> onGetStarted());
            getStartedButton.setOnMouseEntered(e -> {
                getStartedButton.setScaleX(1.05);
                getStartedButton.setScaleY(1.05);
            });
            getStartedButton.setOnMouseExited(e -> {
                getStartedButton.setScaleX(1.0);
                getStartedButton.setScaleY(1.0);
            });
        }
        if (seeOffersButton != null) {
            seeOffersButton.toFront();
            seeOffersButton.setDisable(false);
            seeOffersButton.setMouseTransparent(false);
            seeOffersButton.setOnAction(e -> onSeeOffers());
            seeOffersButton.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> onSeeOffers());
            seeOffersButton.setOnMouseEntered(e -> {
                seeOffersButton.setScaleX(1.05);
                seeOffersButton.setScaleY(1.05);
            });
            seeOffersButton.setOnMouseExited(e -> {
                seeOffersButton.setScaleX(1.0);
                seeOffersButton.setScaleY(1.0);
            });
        }
        if (viewDetailsButton != null) {
            viewDetailsButton.toFront();
            viewDetailsButton.setDisable(false);
            viewDetailsButton.setMouseTransparent(false);
            viewDetailsButton.setOnAction(e -> onFeaturedDetails());
            viewDetailsButton.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> onFeaturedDetails());
        }
    }

    @FXML
    private void onHome() {
        // Guest home is this immersive landing screen.
    }

    @FXML
    private void onOffres() {
        NavigationManager.getInstance().showGuestOffers();
    }

    @FXML
    private void onFeedbacks() {
        NavigationManager.getInstance().showGuestFeedbacks();
    }

    @FXML
    private void onCrew() {
        NavigationManager.getInstance().showGuestCrew();
    }

    @FXML
    private void onPremium() {
        // Placeholder-safe for upcoming premium route.
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

    @FXML
    private void onGetStarted() {
        NavigationManager.getInstance().showRegister();
    }

    @FXML
    private void onSeeOffers() {
        NavigationManager.getInstance().showGuestOffers();
    }

    @FXML
    private void onCategoryPopular() {
        // Visual placeholder.
    }

    @FXML
    private void onCategoryRecommended() {
        // Visual placeholder.
    }

    @FXML
    private void onCategoryLuxury() {
        // Visual placeholder.
    }

    @FXML
    private void onCategoryMoments() {
        // Visual placeholder.
    }

    @FXML
    private void onFeaturedDetails() {
        NavigationManager.getInstance().showLogin();
    }

    @FXML
    private void onFabFavorite() {
        // Visual placeholder.
    }

    @FXML
    private void onFabBookmark() {
        // Visual placeholder.
    }
}
