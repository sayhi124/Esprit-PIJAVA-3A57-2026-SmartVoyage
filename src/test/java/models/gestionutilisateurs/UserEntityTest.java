package models.gestionutilisateurs;

import enums.gestionutilisateurs.UserRole;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import services.gestionutilisateurs.UserService;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserEntityTest {

    private static final UserService service = new UserService();
    private static Integer id;
    private static final String seed = String.valueOf(System.currentTimeMillis());
    private static String email = "user.entity." + seed + "@smartvoyage.com";
    private static String username = "user_entity_" + seed;

    @Test
    @Order(1)
    void ajouter() {
        try {
            User created = service.signUp(username, email, "Secret12345!", UserRole.USER);
            id = created.getId();
            assertTrue(id != null && id > 0);
        } catch (SQLException | IllegalArgumentException error) {
            System.err.println(error);
            assertTrue(false);
        }
    }

    @Test
    @Order(2)
    void afficher() {
        try {
            Optional<User> loaded = service.get(id);
            assertTrue(loaded.isPresent() && loaded.get().getEmail().equals(email));
        } catch (SQLException error) {
            System.err.println(error);
            assertTrue(false);
        }
    }

    @Test
    @Order(3)
    void modifier() {
        try {
            Optional<User> loaded = service.get(id);
            User u = loaded.orElseThrow();
            email = "user.entity.updated." + seed + "@smartvoyage.com";
            username = "user_entity_upd_" + seed;
            u.setEmail(email);
            u.setUsername(username);
            service.update(u);
            Optional<User> refreshed = service.get(id);
            assertTrue(refreshed.isPresent()
                    && refreshed.get().getEmail().equals(email)
                    && refreshed.get().getUsername().equals(username));
        } catch (Exception error) {
            System.err.println(error);
            assertTrue(false);
        }
    }

    @Test
    @Order(4)
    void supprimer() {
        try {
            service.delete(id);
            assertTrue(service.get(id).isEmpty());
        } catch (SQLException error) {
            System.err.println(error);
            assertTrue(false);
        }
    }
}
