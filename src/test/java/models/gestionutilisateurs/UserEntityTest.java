package models.gestionutilisateurs;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserEntityTest {

    @Test
    void userGettersAndSettersShouldWork() {
        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        user.setId(7);
        user.setUsername("ahmed");
        user.setEmail("ahmed@smartvoyage.com");
        user.setPassword("hashed-password");
        user.setProfilePicture("avatar.png");
        user.setProfileImageId(42L);
        user.setRoles(List.of("ROLE_USER", "ROLE_ADMIN"));
        user.setRole("ROLE_ADMIN");
        user.setIsActive(true);
        user.setFaceVerified(true);
        user.setFaceVerifiedAt(now);
        user.setEmailVerified(true);
        user.setPhone("+21612345678");
        user.setResetToken("token-123");
        user.setResetTokenExpiresAt(now.plusHours(1));
        user.setPoints(150);

        assertEquals(7, user.getId());
        assertEquals("ahmed", user.getUsername());
        assertEquals("ahmed@smartvoyage.com", user.getEmail());
        assertEquals("hashed-password", user.getPassword());
        assertEquals("avatar.png", user.getProfilePicture());
        assertEquals(42L, user.getProfileImageId());
        assertEquals(List.of("ROLE_USER", "ROLE_ADMIN"), user.getRoles());
        assertEquals("ROLE_ADMIN", user.getRole());
        assertTrue(user.getIsActive());
        assertTrue(user.getFaceVerified());
        assertEquals(now, user.getFaceVerifiedAt());
        assertTrue(user.getEmailVerified());
        assertEquals("+21612345678", user.getPhone());
        assertEquals("token-123", user.getResetToken());
        assertEquals(now.plusHours(1), user.getResetTokenExpiresAt());
        assertEquals(150, user.getPoints());
    }

    @Test
    void setRolesNullShouldFallbackToEmptyList() {
        User user = new User();
        user.setRoles(null);

        assertTrue(user.getRoles().isEmpty());
        assertFalse(user.getRoles() == null);
    }

    @Test
    void defaultOptionalFieldsShouldBeNull() {
        User user = new User();
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getEmail());
        assertNull(user.getProfileImageId());
    }
}
