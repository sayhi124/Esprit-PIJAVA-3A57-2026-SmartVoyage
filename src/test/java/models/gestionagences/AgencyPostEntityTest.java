package models.gestionagences;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgencyPostEntityTest {

    @Test
    void agencyPostGettersAndSettersShouldWork() {
        AgencyPost post = new AgencyPost();
        LocalDateTime now = LocalDateTime.now();

        post.setId(90L);
        post.setAgencyId(12L);
        post.setAuthorId(3);
        post.setAuthorUsername("agency_admin");
        post.setTitle("New Summer Offer");
        post.setContent("Discover premium beach destinations.");
        post.setCreatedAt(now);
        post.setLikesCount(25);
        post.setCommentsCount(7);
        post.setLikedByViewer(true);

        post.getImageAssetIds().add(1001L);
        post.getImageAssetIds().add(1002L);

        AgencyPostComment comment = new AgencyPostComment();
        comment.setId(1L);
        comment.setAgencyPostId(90L);
        post.getComments().add(comment);

        assertEquals(90L, post.getId());
        assertEquals(12L, post.getAgencyId());
        assertEquals(3, post.getAuthorId());
        assertEquals("agency_admin", post.getAuthorUsername());
        assertEquals("New Summer Offer", post.getTitle());
        assertEquals("Discover premium beach destinations.", post.getContent());
        assertEquals(now, post.getCreatedAt());
        assertEquals(25, post.getLikesCount());
        assertEquals(7, post.getCommentsCount());
        assertTrue(post.isLikedByViewer());
        assertEquals(2, post.getImageAssetIds().size());
        assertEquals(1, post.getComments().size());
        assertEquals(1001L, post.getImageAssetIds().get(0));
        assertEquals(1L, post.getComments().get(0).getId());
    }

    @Test
    void listFieldsShouldBeInitializedByDefault() {
        AgencyPost post = new AgencyPost();
        assertNotNull(post.getImageAssetIds());
        assertNotNull(post.getComments());
        assertTrue(post.getImageAssetIds().isEmpty());
        assertTrue(post.getComments().isEmpty());
        assertFalse(post.isLikedByViewer());
    }
}
