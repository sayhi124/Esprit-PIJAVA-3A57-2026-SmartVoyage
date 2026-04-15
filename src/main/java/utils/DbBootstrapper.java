package utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Minimal bootstrap for feature tables that are managed by this JavaFX app.
 */
public final class DbBootstrapper {

    private DbBootstrapper() {
    }

    public static void ensureSchema(Connection connection) {
        if (connection == null) {
            return;
        }
        try (Statement st = connection.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS travel_event (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        title VARCHAR(180) NOT NULL,
                        description TEXT NULL,
                        location VARCHAR(180) NOT NULL,
                        event_date DATETIME NOT NULL,
                        max_participants INT NOT NULL DEFAULT 100,
                        image_path VARCHAR(512) NULL,
                        created_by_user_id INT NOT NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT fk_travel_event_user FOREIGN KEY (created_by_user_id) REFERENCES `user`(id) ON DELETE CASCADE
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS event_participation (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        event_id BIGINT NOT NULL,
                        user_id INT NOT NULL,
                        status VARCHAR(24) NOT NULL DEFAULT 'PARTICIPATING',
                        joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT uk_event_user UNIQUE (event_id, user_id),
                        CONSTRAINT fk_event_participation_event FOREIGN KEY (event_id) REFERENCES travel_event(id) ON DELETE CASCADE,
                        CONSTRAINT fk_event_participation_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS event_sponsorship (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        nom VARCHAR(255) NOT NULL,
                        email VARCHAR(255) NOT NULL,
                        telephone VARCHAR(255) NULL,
                        montant_contribution DECIMAL(10,3) NOT NULL,
                        message LONGTEXT NULL,
                        statut VARCHAR(50) NOT NULL DEFAULT 'en_attente',
                        is_paid TINYINT(1) NOT NULL DEFAULT 0,
                        sponsored_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        evenement_id BIGINT NOT NULL,
                        user_id INT NULL,
                        CONSTRAINT fk_event_sponsorship_event FOREIGN KEY (evenement_id) REFERENCES travel_event(id) ON DELETE CASCADE,
                        CONSTRAINT fk_event_sponsorship_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE SET NULL
                    )
                    """);
        } catch (SQLException ignored) {
            // Keep startup resilient in dev environments with limited DB privileges.
        }
    }
}
