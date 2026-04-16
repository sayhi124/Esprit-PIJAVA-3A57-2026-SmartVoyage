package models.gestionutilisateurs;

import enums.gestionutilisateurs.UserRole;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import services.gestionutilisateurs.UserFeedbackService;
import services.gestionutilisateurs.UserService;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserFeedbackEntityTest {

    private static final UserService userService = new UserService();
    private static final UserFeedbackService service = new UserFeedbackService();
    private static final String seed = String.valueOf(System.currentTimeMillis());
    private static Integer userId;
    private static Long feedbackId;

    @Test
    @Order(1)
    void ajouter() {
        try {
            User user = userService.signUp("feedback_user_" + seed, "feedback.user." + seed + "@smartvoyage.com", "Secret12345!", UserRole.USER);
            userId = user.getId();
            UserFeedback feedback = new UserFeedback();
            feedback.setUserId(userId);
            feedback.setStars(5);
            feedback.setNote("Excellent test feedback");
            service.insert(feedback);
            feedbackId = feedback.getId();
            assertTrue(feedbackId != null && feedbackId > 0);
        } catch (SQLException | IllegalArgumentException error) {
            System.err.println(error);
            assertTrue(false);
        }
    }

    @Test
    @Order(2)
    void afficher() {
        try {
            List<UserFeedback> list = service.findByUser(userId);
            boolean found = list.stream().anyMatch(f -> f.getId().equals(feedbackId));
            assertTrue(found);
        } catch (SQLException error) {
            System.err.println(error);
            assertTrue(false);
        }
    }

    @Test
    @Order(3)
    void supprimer() {
        try {
            Connection c = DbConnexion.getInstance().getConnection();
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM user_app_feedback WHERE id = ?")) {
                ps.setLong(1, feedbackId);
                ps.executeUpdate();
            }
            userService.delete(userId);
            List<UserFeedback> list = service.findByUser(userId);
            boolean found = list.stream().anyMatch(f -> f.getId().equals(feedbackId));
            assertTrue(!found);
        } catch (SQLException error) {
            System.err.println(error);
            assertTrue(false);
        }
    }
}
