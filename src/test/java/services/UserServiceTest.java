package services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    @Test
    void passwordHasherMatchesAfterHash() {
        String hash = utils.PasswordHasher.hash("secret12345");
        assertTrue(utils.PasswordHasher.matches("secret12345", hash));
        assertFalse(utils.PasswordHasher.matches("wrong", hash));
    }
}
