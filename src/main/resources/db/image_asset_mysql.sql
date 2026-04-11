-- Table image_asset + photo de profil utilisateur (profile_image_id), comme l'integration Symfony.
-- Les colonnes agency_account.cover_image_id et agency_profile_image_id existent deja : elles peuvent
-- pointer vers image_asset une fois cette table creee. Contraintes FK agence optionnelles (donnees propres).

CREATE TABLE IF NOT EXISTS `image_asset` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `mime_type` VARCHAR(127) NOT NULL,
    `data` LONGBLOB NOT NULL,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Si la colonne existe deja, ignorer l'erreur MySQL 1060.
ALTER TABLE `user` ADD COLUMN `profile_image_id` BIGINT DEFAULT NULL;

-- Si la contrainte existe deja, ignorer l'erreur 1826 / 1061.
ALTER TABLE `user`
    ADD CONSTRAINT `fk_user_profile_image_asset`
    FOREIGN KEY (`profile_image_id`) REFERENCES `image_asset` (`id`) ON DELETE SET NULL;
