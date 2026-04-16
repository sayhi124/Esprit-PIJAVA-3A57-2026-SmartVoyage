package models.gestionagences;

import enums.gestionutilisateurs.UserRole;
import models.gestionutilisateurs.User;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import services.gestionagences.AgencyAccountService;
import services.gestionagences.AgencyPostService;
import services.gestionutilisateurs.UserService;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AgencyPostEntityTest {

    private static final UserService userService = new UserService();
    private static final AgencyAccountService agencyService = new AgencyAccountService();
    private static final AgencyPostService postService = new AgencyPostService();
    private static final String seed = String.valueOf(System.currentTimeMillis());
    private static Integer authorId;
    private static Long agencyId;
    private static Long postId;

    @Test
    @Order(1)
    void ajouter() {
        try {
            User author = userService.signUp("post_user_" + seed, "post.user." + seed + "@smartvoyage.com", "Secret12345!", UserRole.AGENCY_ADMIN);
            authorId = author.getId();
            AgencyAccount agency = new AgencyAccount();
            agency.setAgencyName("Post Agency " + seed);
            agency.setDescription("Agency for posts test.");
            agency.setWebsiteUrl("https://example.com");
            agency.setCountry("TN");
            agency.setResponsableId(authorId);
            agencyService.insert(agency);
            agencyId = agency.getId();
            postId = postService.createPost(agencyId, authorId, "Post title " + seed, "Post content for integration test.");
            assertTrue(postId != null && postId > 0);
        } catch (SQLException | IllegalArgumentException error) {
            System.err.println(error);
            assertTrue(false);
        }
    }

    @Test
    @Order(2)
    void afficher() {
        try {
            List<AgencyPost> posts = postService.listByAgency(agencyId, authorId);
            boolean found = posts.stream().anyMatch(p -> p.getId().equals(postId));
            assertTrue(found);
        } catch (SQLException error) {
            System.err.println(error);
            assertTrue(false);
        }
    }

    @Test
    @Order(3)
    void modifier() {
        try {
            Connection c = DbConnexion.getInstance().getConnection();
            try (PreparedStatement ps = c.prepareStatement("UPDATE agency_post SET title = ? WHERE id = ?")) {
                ps.setString(1, "Post title updated " + seed);
                ps.setLong(2, postId);
                ps.executeUpdate();
            }
            List<AgencyPost> posts = postService.listByAgency(agencyId, authorId);
            boolean foundUpdated = posts.stream().anyMatch(p -> p.getId().equals(postId) && p.getTitle().contains("updated"));
            assertTrue(foundUpdated);
        } catch (SQLException error) {
            System.err.println(error);
            assertTrue(false);
        }
    }

    @Test
    @Order(4)
    void supprimer() {
        try {
            Connection c = DbConnexion.getInstance().getConnection();
            try (PreparedStatement ps = c.prepareStatement("UPDATE agency_post SET is_deleted = 1 WHERE id = ?")) {
                ps.setLong(1, postId);
                ps.executeUpdate();
            }
            agencyService.delete(agencyId);
            userService.delete(authorId);
            List<AgencyPost> posts = postService.listByAgency(agencyId, authorId);
            boolean found = posts.stream().anyMatch(p -> p.getId().equals(postId));
            assertTrue(!found);
        } catch (SQLException error) {
            System.err.println(error);
            assertTrue(false);
        }
    }
}
