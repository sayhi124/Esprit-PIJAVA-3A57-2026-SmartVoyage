package models.gestionagences;

import enums.gestionutilisateurs.UserRole;
import models.gestionutilisateurs.User;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import services.gestionagences.AgencyAccountService;
import services.gestionutilisateurs.UserService;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AgencyAccountEntityTest {

    private static final UserService userService = new UserService();
    private static final AgencyAccountService service = new AgencyAccountService();
    private static final String seed = String.valueOf(System.currentTimeMillis());
    private static Integer ownerId;
    private static Long agencyId;

    @Test
    @Order(1)
    void ajouter() {
        try {
            User owner = userService.signUp(
                    "agency_owner_" + seed,
                    "agency.owner." + seed + "@smartvoyage.com",
                    "Secret12345!",
                    UserRole.AGENCY_ADMIN
            );
            ownerId = owner.getId();
            AgencyAccount agency = new AgencyAccount();
            agency.setAgencyName("Agency Test " + seed);
            agency.setDescription("Agency description for test.");
            agency.setWebsiteUrl("https://example.com");
            agency.setPhone("+21670000000");
            agency.setAddress("Tunis");
            agency.setCountry("TN");
            agency.setResponsableId(ownerId);
            service.insert(agency);
            agencyId = agency.getId();
            assertTrue(agencyId != null && agencyId > 0);
        } catch (SQLException | IllegalArgumentException error) {
            System.err.println(error);
            assertTrue(false);
        }
    }

    @Test
    @Order(2)
    void afficher() {
        try {
            Optional<AgencyAccount> loaded = service.get(agencyId);
            assertTrue(loaded.isPresent() && loaded.get().getResponsableId().equals(ownerId));
        } catch (SQLException error) {
            System.err.println(error);
            assertTrue(false);
        }
    }

    @Test
    @Order(3)
    void modifier() {
        try {
            AgencyAccount agency = service.get(agencyId).orElseThrow();
            agency.setAgencyName("Agency Updated " + seed);
            agency.setDescription("Updated description.");
            service.update(agency);
            Optional<AgencyAccount> refreshed = service.get(agencyId);
            assertTrue(refreshed.isPresent() && refreshed.get().getAgencyName().contains("Updated"));
        } catch (Exception error) {
            System.err.println(error);
            assertTrue(false);
        }
    }

    @Test
    @Order(4)
    void supprimer() {
        try {
            service.delete(agencyId);
            userService.delete(ownerId);
            assertTrue(service.get(agencyId).isEmpty());
        } catch (SQLException error) {
            System.err.println(error);
            assertTrue(false);
        }
    }
}
