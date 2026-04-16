package models.gestionagences;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgencyAccountEntityTest {

    @Test
    void agencyAccountGettersAndSettersShouldWork() {
        AgencyAccount agency = new AgencyAccount();
        LocalDateTime now = LocalDateTime.now();

        agency.setId(10L);
        agency.setAgencyName("Smart Voyage Tunisia");
        agency.setDescription("Premium travel agency");
        agency.setWebsiteUrl("https://smartvoyage.com");
        agency.setPhone("+21670000000");
        agency.setAddress("Tunis Centre");
        agency.setCountry("Tunisia");
        agency.setLatitude(36.8065);
        agency.setLongitude(10.1815);
        agency.setVerified(true);
        agency.setCreatedAt(now.minusDays(3));
        agency.setUpdatedAt(now);
        agency.setResponsableId(99);
        agency.setCoverImageId(300L);
        agency.setAgencyProfileImageId(301L);

        assertEquals(10L, agency.getId());
        assertEquals("Smart Voyage Tunisia", agency.getAgencyName());
        assertEquals("Premium travel agency", agency.getDescription());
        assertEquals("https://smartvoyage.com", agency.getWebsiteUrl());
        assertEquals("+21670000000", agency.getPhone());
        assertEquals("Tunis Centre", agency.getAddress());
        assertEquals("Tunisia", agency.getCountry());
        assertEquals(36.8065, agency.getLatitude());
        assertEquals(10.1815, agency.getLongitude());
        assertTrue(agency.getVerified());
        assertEquals(now.minusDays(3), agency.getCreatedAt());
        assertEquals(now, agency.getUpdatedAt());
        assertEquals(99, agency.getResponsableId());
        assertEquals(300L, agency.getCoverImageId());
        assertEquals(301L, agency.getAgencyProfileImageId());
    }
}
