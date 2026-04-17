-- Messaging and Notification Module Database Schema

-- Create chat_group table
CREATE TABLE IF NOT EXISTS chat_group (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nom VARCHAR(255) NOT NULL,
  date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_nom (nom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create message table
CREATE TABLE IF NOT EXISTS message (
  id INT AUTO_INCREMENT PRIMARY KEY,
  contenu LONGTEXT NOT NULL,
  date_envoi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  statut VARCHAR(50) DEFAULT 'non lu',
  expediteur_id INT NOT NULL,
  destinataire_id INT,
  group_id INT,
  FOREIGN KEY (expediteur_id) REFERENCES user(id) ON DELETE CASCADE,
  FOREIGN KEY (destinataire_id) REFERENCES user(id) ON DELETE CASCADE,
  FOREIGN KEY (group_id) REFERENCES chat_group(id) ON DELETE CASCADE,
  INDEX idx_expediteur_id (expediteur_id),
  INDEX idx_destinataire_id (destinataire_id),
  INDEX idx_group_id (group_id),
  INDEX idx_date_envoi (date_envoi),
  INDEX idx_statut (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create group_member table (intermediate table for many-to-many relationship)
CREATE TABLE IF NOT EXISTS group_member (
  id INT AUTO_INCREMENT PRIMARY KEY,
  group_id INT NOT NULL,
  user_id INT NOT NULL,
  FOREIGN KEY (group_id) REFERENCES chat_group(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
  UNIQUE KEY uk_group_user (group_id, user_id),
  INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create notification table
CREATE TABLE IF NOT EXISTS notification (
  id INT AUTO_INCREMENT PRIMARY KEY,
  contenu LONGTEXT NOT NULL,
  date_notification TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  statut VARCHAR(50) DEFAULT 'non lu',
  user_id INT NOT NULL,
  FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
  INDEX idx_user_id (user_id),
  INDEX idx_statut (statut),
  INDEX idx_date_notification (date_notification)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
