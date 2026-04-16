package models.gestionutilisateurs;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserFeedbackEntityTest {

    @Test
    void userFeedbackGettersAndSettersShouldWork() {
        UserFeedback feedback = new UserFeedback();
        LocalDateTime now = LocalDateTime.now();

        feedback.setId(55L);
        feedback.setUserId(8);
        feedback.setStars(4);
        feedback.setNote("Great app and smooth experience.");
        feedback.setCreatedAt(now);

        assertEquals(55L, feedback.getId());
        assertEquals(8, feedback.getUserId());
        assertEquals(4, feedback.getStars());
        assertEquals("Great app and smooth experience.", feedback.getNote());
        assertEquals(now, feedback.getCreatedAt());
    }

    @Test
    void userFeedbackDefaultValuesShouldBeNull() {
        UserFeedback feedback = new UserFeedback();
        assertNull(feedback.getId());
        assertNull(feedback.getUserId());
        assertNull(feedback.getStars());
        assertNull(feedback.getNote());
        assertNull(feedback.getCreatedAt());
    }
}
