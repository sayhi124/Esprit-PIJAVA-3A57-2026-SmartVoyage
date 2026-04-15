-- Demandes d'agrément admin (aligné integration / AgencyAdminApplication).
-- Exécuter sur smart_voyage après `user` et `agency_account` existants.

CREATE TABLE IF NOT EXISTS `agency_admin_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    `agency_name_requested` VARCHAR(255) NOT NULL,
    `country` VARCHAR(2) DEFAULT NULL,
    `message_to_admin` TEXT,
    `requested_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `applicant_id` INT NOT NULL,
    `reviewed_by_id` INT DEFAULT NULL,
    `reviewed_at` DATETIME(6) DEFAULT NULL,
    `review_note` TEXT,
    `created_agency_account_id` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_agency_app_status` (`status`),
    KEY `idx_agency_app_applicant` (`applicant_id`),
    CONSTRAINT `fk_agency_app_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_agency_app_reviewer` FOREIGN KEY (`reviewed_by_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_agency_app_agency` FOREIGN KEY (`created_agency_account_id`) REFERENCES `agency_account` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
