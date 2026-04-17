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
                        status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
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
                        status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PARTICIPATION',
                        requester_name VARCHAR(120) NULL,
                        contact_phone VARCHAR(30) NULL,
                        request_note TEXT NULL,
                        joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT uk_event_user UNIQUE (event_id, user_id),
                        CONSTRAINT fk_event_participation_event FOREIGN KEY (event_id) REFERENCES travel_event(id) ON DELETE CASCADE,
                        CONSTRAINT fk_event_participation_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS event_participant (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        event_id BIGINT NOT NULL,
                        user_id INT NOT NULL,
                        status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PARTICIPATION',
                        requester_name VARCHAR(120) NULL,
                        contact_phone VARCHAR(30) NULL,
                        request_note TEXT NULL,
                        joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT uk_event_participant_event_user UNIQUE (event_id, user_id)
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
                        statut VARCHAR(50) NOT NULL DEFAULT 'PENDING_SPONSOR',
                        is_paid TINYINT(1) NOT NULL DEFAULT 0,
                        sponsored_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        evenement_id BIGINT NOT NULL,
                        user_id INT NULL,
                        CONSTRAINT fk_event_sponsorship_event FOREIGN KEY (evenement_id) REFERENCES travel_event(id) ON DELETE CASCADE,
                        CONSTRAINT fk_event_sponsorship_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE SET NULL
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS user_app_feedback (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        stars TINYINT NOT NULL,
                        note TEXT NOT NULL,
                        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        CONSTRAINT fk_user_app_feedback_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
                        CONSTRAINT chk_user_app_feedback_stars CHECK (stars >= 1 AND stars <= 5)
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS agency_admin_application (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                        agency_name_requested VARCHAR(255) NOT NULL,
                        country VARCHAR(2) NULL,
                        message_to_admin TEXT NULL,
                        requested_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        applicant_id INT NOT NULL,
                        reviewed_by_id INT NULL,
                        reviewed_at DATETIME(6) NULL,
                        review_note TEXT NULL,
                        created_agency_account_id BIGINT NULL,
                        CONSTRAINT fk_agency_app_applicant FOREIGN KEY (applicant_id) REFERENCES `user`(id) ON DELETE CASCADE,
                        CONSTRAINT fk_agency_app_reviewer FOREIGN KEY (reviewed_by_id) REFERENCES `user`(id) ON DELETE SET NULL,
                        CONSTRAINT fk_agency_app_agency FOREIGN KEY (created_agency_account_id) REFERENCES agency_account(id) ON DELETE SET NULL
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS event_like (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        event_id BIGINT NOT NULL,
                        user_id INT NOT NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT uk_event_like UNIQUE (event_id, user_id),
                        CONSTRAINT fk_event_like_event FOREIGN KEY (event_id) REFERENCES travel_event(id) ON DELETE CASCADE,
                        CONSTRAINT fk_event_like_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS event_comment (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        event_id BIGINT NOT NULL,
                        user_id INT NOT NULL,
                        content TEXT NOT NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        is_deleted TINYINT(1) NOT NULL DEFAULT 0,
                        CONSTRAINT fk_event_comment_event FOREIGN KEY (event_id) REFERENCES travel_event(id) ON DELETE CASCADE,
                        CONSTRAINT fk_event_comment_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
                    )
                    """);

            // Migration-safe updates for existing schemas.
            try { st.execute("ALTER TABLE travel_event ADD COLUMN status VARCHAR(24) NOT NULL DEFAULT 'PENDING'"); } catch (SQLException ignored) { }
            try { st.execute("ALTER TABLE event_participation MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PARTICIPATION'"); } catch (SQLException ignored) { }
            try { st.execute("ALTER TABLE event_participation ADD COLUMN requester_name VARCHAR(120) NULL"); } catch (SQLException ignored) { }
            try { st.execute("ALTER TABLE event_participation ADD COLUMN contact_phone VARCHAR(30) NULL"); } catch (SQLException ignored) { }
            try { st.execute("ALTER TABLE event_participation ADD COLUMN request_note TEXT NULL"); } catch (SQLException ignored) { }
            try { st.execute("ALTER TABLE event_sponsorship MODIFY COLUMN statut VARCHAR(50) NOT NULL DEFAULT 'PENDING_SPONSOR'"); } catch (SQLException ignored) { }

            // Legacy mirror sync so phpMyAdmin legacy tables expose current app data.
            try {
                st.execute("""
                        INSERT INTO evenement (id, titre, description, date_debut, date_fin, lieu, country, image, nb_participants, statut, created_at, created_by_id)
                        SELECT te.id,
                               te.title,
                               COALESCE(te.description, ''),
                               te.event_date,
                               DATE_ADD(te.event_date, INTERVAL 2 HOUR),
                               te.location,
                               NULL,
                               te.image_path,
                               te.max_participants,
                               te.status,
                               te.created_at,
                               te.created_by_user_id
                        FROM travel_event te
                        ON DUPLICATE KEY UPDATE
                            titre = VALUES(titre),
                            description = VALUES(description),
                            date_debut = VALUES(date_debut),
                            date_fin = VALUES(date_fin),
                            lieu = VALUES(lieu),
                            image = VALUES(image),
                            nb_participants = VALUES(nb_participants),
                            statut = VALUES(statut),
                            created_by_id = VALUES(created_by_id)
                        """);
            } catch (SQLException ignored) { }

            try {
                st.execute("""
                        INSERT INTO event_participant (id, event_id, user_id, status, requester_name, contact_phone, request_note, joined_at, updated_at)
                        SELECT ep.id, ep.event_id, ep.user_id, ep.status, ep.requester_name, ep.contact_phone, ep.request_note, ep.joined_at, ep.updated_at
                        FROM event_participation ep
                        ON DUPLICATE KEY UPDATE
                            status = VALUES(status),
                            requester_name = VALUES(requester_name),
                            contact_phone = VALUES(contact_phone),
                            request_note = VALUES(request_note),
                            updated_at = VALUES(updated_at)
                        """);
            } catch (SQLException ignored) { }
        } catch (SQLException ignored) {
            // Keep startup resilient in dev environments with limited DB privileges.
        }
    }
}
