package services.gestionposts;

import models.gestionposts.Comment;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des commentaires avec validation.
 */
public class CommentService {

    private final CommentDAO commentDAO;

    public CommentService() {
        this.commentDAO = new CommentDAOImpl();
    }

    /**
     * Crée un nouveau commentaire avec validation.
     *
     * @param comment Le commentaire à créer
     * @throws IllegalArgumentException Si les données sont invalides
     * @throws SQLException En cas d'erreur base de données
     */
    public void create(Comment comment) throws IllegalArgumentException, SQLException {
        validate(comment);
        commentDAO.create(comment);
    }

    /**
     * Met à jour un commentaire avec validation.
     *
     * @param comment Le commentaire à mettre à jour
     * @throws IllegalArgumentException Si les données sont invalides
     * @throws SQLException En cas d'erreur base de données
     */
    public void update(Comment comment) throws IllegalArgumentException, SQLException {
        validate(comment);
        commentDAO.update(comment);
    }

    public void updateByAuthor(Comment comment, Integer authorUserId) throws IllegalArgumentException, SQLException {
        validate(comment);
        if (authorUserId == null) {
            throw new IllegalArgumentException("Utilisateur invalide.");
        }
        if (comment.getId() == null) {
            throw new IllegalArgumentException("Commentaire introuvable.");
        }
        if (!commentDAO.isAuthor(comment.getId(), authorUserId)) {
            throw new IllegalArgumentException("Vous ne pouvez modifier que vos propres commentaires.");
        }
        commentDAO.update(comment);
    }

    /**
     * Supprime un commentaire.
     *
     * @param id L'ID du commentaire
     * @throws SQLException En cas d'erreur base de données
     */
    public void delete(Long id) throws SQLException {
        commentDAO.delete(id);
    }

    public void deleteByAuthor(Long id, Integer authorUserId) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("Commentaire introuvable.");
        }
        if (authorUserId == null) {
            throw new IllegalArgumentException("Utilisateur invalide.");
        }
        if (!commentDAO.isAuthor(id, authorUserId)) {
            throw new IllegalArgumentException("Vous ne pouvez supprimer que vos propres commentaires.");
        }
        commentDAO.delete(id);
    }

    public Optional<Comment> findById(Long id) throws SQLException {
        return commentDAO.findById(id);
    }

    /**
     * Vérifie si l'utilisateur est l'auteur du commentaire.
     *
     * @param commentId L'ID du commentaire
     * @param userId L'ID de l'utilisateur
     * @return true si l'utilisateur est l'auteur
     * @throws SQLException En cas d'erreur base de données
     */
    public boolean isAuthor(Long commentId, Integer userId) throws SQLException {
        return commentDAO.isAuthor(commentId, userId);
    }

    /**
     * Récupère les commentaires d'un post triés par date décroissante.
     *
     * @param postId L'ID du post
     * @return La liste des commentaires
     * @throws SQLException En cas d'erreur base de données
     */
    public List<Comment> findByPostIdOrdered(Long postId) throws SQLException {
        return commentDAO.findByPostIdOrdered(postId);
    }

    /**
     * Compte les commentaires d'un post.
     *
     * @param postId L'ID du post
     * @return Le nombre de commentaires
     * @throws SQLException En cas d'erreur base de données
     */
    public int countByPostId(Long postId) throws SQLException {
        return commentDAO.countByPostId(postId);
    }

    /**
     * Valide un commentaire.
     *
     * @param comment Le commentaire à valider
     * @throws IllegalArgumentException Si les données sont invalides
     */
    private void validate(Comment comment) throws IllegalArgumentException {
        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu du commentaire est obligatoire.");
        }

        String content = comment.getContent().trim();

        if (content.length() < 5) {
            throw new IllegalArgumentException("Le commentaire doit contenir au moins 5 caractères.");
        }

        if (content.length() > 1000) {
            throw new IllegalArgumentException("Le commentaire ne doit pas dépasser 1000 caractères.");
        }

        if (comment.getUserId() == null) {
            throw new IllegalArgumentException("L'utilisateur est obligatoire.");
        }

        if (comment.getPostId() == null) {
            throw new IllegalArgumentException("Le post est obligatoire.");
        }
    }
}
