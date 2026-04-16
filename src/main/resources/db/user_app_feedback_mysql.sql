-- User feedbacks on app experience (rating + note history shown in Mon Profil page).

CREATE TABLE IF NOT EXISTS `user_app_feedback` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `stars` TINYINT NOT NULL,
    `note` TEXT NOT NULL,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    KEY `idx_user_app_feedback_user` (`user_id`),
    CONSTRAINT `fk_user_app_feedback_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_user_app_feedback_stars`
        CHECK (`stars` >= 1 AND `stars` <= 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
