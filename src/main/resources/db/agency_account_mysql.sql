-- Table agency_account (alignee sur l'entite Symfony AgencyAccount / integration).
-- Executer sur la base smart_voyage si la table n'existe pas encore.

CREATE TABLE IF NOT EXISTS `agency_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `agency_name` VARCHAR(255) NOT NULL,
    `description` TEXT NOT NULL,
    `website_url` VARCHAR(500) DEFAULT NULL,
    `phone` VARCHAR(50) DEFAULT NULL,
    `address` VARCHAR(500) DEFAULT NULL,
    `country` VARCHAR(2) DEFAULT NULL,
    `latitude` DOUBLE DEFAULT NULL,
    `longitude` DOUBLE DEFAULT NULL,
    `verified` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    `responsable_id` INT NOT NULL,
    `cover_image_id` BIGINT DEFAULT NULL,
    `agency_profile_image_id` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_agency_responsable` (`responsable_id`),
    CONSTRAINT `fk_agency_account_user` FOREIGN KEY (`responsable_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
