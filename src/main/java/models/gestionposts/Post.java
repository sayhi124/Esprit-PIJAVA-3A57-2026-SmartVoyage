package models.gestionposts;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modèle représentant un post de voyage dans la base de données.
 * Correspond à la table 'post' créée par Symfony.
 */
public class Post {

    private Long id;
    private String titre;
    private String contenu;
    private String location;
    private String imageUrl;
    private Integer userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer nbLikes;
    private Integer nbCommentaires;

    public Post() {
        this.nbLikes = 0;
        this.nbCommentaires = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getNbLikes() {
        return nbLikes;
    }

    public void setNbLikes(Integer nbLikes) {
        this.nbLikes = nbLikes != null ? nbLikes : 0;
    }

    public Integer getNbCommentaires() {
        return nbCommentaires;
    }

    public void setNbCommentaires(Integer nbCommentaires) {
        this.nbCommentaires = nbCommentaires != null ? nbCommentaires : 0;
    }

    /**
     * Retourne un résumé du contenu (pour l'affichage dans les cartes)
     */
    public String getContenuResume(int maxLength) {
        if (contenu == null || contenu.length() <= maxLength) {
            return contenu;
        }
        return contenu.substring(0, maxLength) + "...";
    }

    public void validateForPersistence(boolean createMode) {
        List<String> errors = new ArrayList<>();
        String t = titre == null ? "" : titre.trim();
        String c = contenu == null ? "" : contenu.trim();
        String l = location == null ? "" : location.trim();

        if (!createMode && id == null) {
            errors.add("L'identifiant du post est obligatoire.");
        }
        if (t.isEmpty() || t.length() < 10 || t.length() > 100) {
            errors.add("Le titre doit contenir entre 10 et 100 caractères.");
        }
        if (c.isEmpty() || c.length() < 50 || c.length() > 5000) {
            errors.add("Le contenu doit contenir entre 50 et 5000 caractères.");
        }
        if (l.isEmpty() || l.length() > 100) {
            errors.add("La localisation est obligatoire et ne doit pas dépasser 100 caractères.");
        }
        if (createMode && userId == null) {
            errors.add("L'utilisateur créateur est obligatoire.");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }
    }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", location='" + location + '\'' +
                ", nbLikes=" + nbLikes +
                ", nbCommentaires=" + nbCommentaires +
                '}';
    }
}
