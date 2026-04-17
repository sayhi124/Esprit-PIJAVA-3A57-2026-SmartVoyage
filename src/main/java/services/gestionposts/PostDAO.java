package services.gestionposts;

import models.gestionposts.Post;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Interface DAO pour la gestion des posts de voyage.
 * Définit les opérations CRUD et les recherches avancées.
 */
public interface PostDAO {

    /**
     * Crée un nouveau post dans la base de données.
     */
    void create(Post post) throws SQLException;

    /**
     * Met à jour un post existant.
     */
    void update(Post post) throws SQLException;

    /**
     * Supprime un post par son ID.
     */
    void delete(Long id) throws SQLException;

    /**
     * Recherche un post par son ID.
     */
    Optional<Post> findById(Long id) throws SQLException;

    /**
     * Récupère tous les posts avec pagination.
     */
    List<Post> findAll(int offset, int limit) throws SQLException;

    /**
     * Compte le nombre total de posts.
     */
    int countAll() throws SQLException;

    /**
     * Filtre les posts par pays/location.
     */
    List<Post> findByLocation(String location, int offset, int limit) throws SQLException;

    /**
     * Récupère les posts d'un utilisateur avec pagination.
     */
    List<Post> findByUserId(Integer userId, int offset, int limit) throws SQLException;

    /**
     * Compte les posts par pays.
     */
    int countByLocation(String location) throws SQLException;

    /**
     * Compte les posts d'un utilisateur.
     */
    int countByUserId(Integer userId) throws SQLException;

    /**
     * Recherche des posts par mot-clé (titre, contenu ou location).
     */
    List<Post> search(String keyword, int offset, int limit) throws SQLException;

    /**
     * Compte les résultats de recherche.
     */
    int countSearch(String keyword) throws SQLException;

    /**
     * Recherche combinée : pays + mot-clé.
     */
    List<Post> searchByLocationAndKeyword(String location, String keyword, int offset, int limit) throws SQLException;

    /**
     * Compte les résultats combinés.
     */
    int countSearchByLocationAndKeyword(String location, String keyword) throws SQLException;

    /**
     * Récupère toutes les locations distinctes (pour le filtre pays).
     */
    List<String> findAllLocations() throws SQLException;
}
