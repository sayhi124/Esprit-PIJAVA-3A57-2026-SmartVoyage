package services.gestionposts;

import models.gestionposts.Post;
import services.CRUD;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Service métier pour la gestion des posts.
 * Implémente CRUD et gère les validations serveur avec messages en français.
 */
public class PostService implements CRUD<Post, Long> {

    private final PostDAO postDAO;
    private static final int POSTS_PER_PAGE = 12;

    public PostService() {
        this.postDAO = new PostDAOImpl();
    }

    @Override
    public void create(Post entity) throws SQLException {
        validate(entity, true);
        postDAO.create(entity);
    }

    @Override
    public void insert(Post entity) throws SQLException {
        create(entity);
    }

    @Override
    public void update(Post entity) throws SQLException {
        validate(entity, false);
        postDAO.update(entity);
    }

    @Override
    public void delete(Long id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("L'identifiant du post est obligatoire pour la suppression.");
        }
        postDAO.delete(id);
    }

    public Optional<Post> findById(Long id) throws SQLException {
        return postDAO.findById(id);
    }

    /**
     * Récupère les posts avec pagination.
     */
    public List<Post> findAllPaginated(int page) throws SQLException {
        int offset = (page - 1) * POSTS_PER_PAGE;
        return postDAO.findAll(offset, POSTS_PER_PAGE);
    }

    public int countAll() throws SQLException {
        return postDAO.countAll();
    }

    /**
     * Calcule le nombre total de pages.
     */
    public int getTotalPages(int totalCount) {
        return (int) Math.ceil((double) totalCount / POSTS_PER_PAGE);
    }

    /**
     * Filtre par pays avec pagination.
     */
    public List<Post> findByLocationPaginated(String location, int page) throws SQLException {
        int offset = (page - 1) * POSTS_PER_PAGE;
        return postDAO.findByLocation(location, offset, POSTS_PER_PAGE);
    }

    public int countByLocation(String location) throws SQLException {
        return postDAO.countByLocation(location);
    }

    /**
     * Recherche par mot-clé avec pagination.
     */
    public List<Post> searchPaginated(String keyword, int page) throws SQLException {
        int offset = (page - 1) * POSTS_PER_PAGE;
        return postDAO.search(keyword, offset, POSTS_PER_PAGE);
    }

    public int countSearch(String keyword) throws SQLException {
        return postDAO.countSearch(keyword);
    }

    /**
     * Recherche combinée pays + mot-clé avec pagination.
     */
    public List<Post> searchByLocationAndKeywordPaginated(String location, String keyword, int page) throws SQLException {
        int offset = (page - 1) * POSTS_PER_PAGE;
        return postDAO.searchByLocationAndKeyword(location, keyword, offset, POSTS_PER_PAGE);
    }

    public int countSearchByLocationAndKeyword(String location, String keyword) throws SQLException {
        return postDAO.countSearchByLocationAndKeyword(location, keyword);
    }

    /**
     * Récupère toutes les locations distinctes depuis les posts.
     */
    public List<String> findAllLocationsFromPosts() throws SQLException {
        return postDAO.findAllLocations();
    }

    /**
     * Validation serveur complète avec messages en français.
     */
    public void validate(Post post, boolean isCreate) {
        List<String> errors = new ArrayList<>();

        if (post == null) {
            throw new IllegalArgumentException("Le post est obligatoire.");
        }

        // Validation ID pour update
        if (!isCreate && post.getId() == null) {
            errors.add("L'identifiant du post est obligatoire pour la modification.");
        }

        // Validation titre (min 10, max 100)
        if (post.getTitre() == null || post.getTitre().trim().isEmpty()) {
            errors.add("Le titre est obligatoire.");
        } else {
            String titre = post.getTitre().trim();
            if (titre.length() < 10) {
                errors.add("Le titre doit contenir au moins 10 caractères (actuellement : " + titre.length() + ").");
            }
            if (titre.length() > 100) {
                errors.add("Le titre ne doit pas dépasser 100 caractères (actuellement : " + titre.length() + ").");
            }
        }

        // Validation contenu (min 50, max 5000)
        if (post.getContenu() == null || post.getContenu().trim().isEmpty()) {
            errors.add("Le contenu est obligatoire.");
        } else {
            String contenu = post.getContenu().trim();
            if (contenu.length() < 50) {
                errors.add("Le contenu doit contenir au moins 50 caractères (actuellement : " + contenu.length() + ").");
            }
            if (contenu.length() > 5000) {
                errors.add("Le contenu ne doit pas dépasser 5000 caractères (actuellement : " + contenu.length() + ").");
            }
        }

        // Validation location (non vide)
        if (post.getLocation() == null || post.getLocation().trim().isEmpty()) {
            errors.add("La localisation est obligatoire.");
        }

        // Validation URL image (optionnel mais doit être valide si fourni)
        if (post.getImageUrl() != null && !post.getImageUrl().trim().isEmpty()) {
            if (!isValidImageUrl(post.getImageUrl().trim())) {
                errors.add("L'URL de l'image n'est pas valide (doit être une URL HTTP/HTTPS pointant vers une image JPG, PNG ou GIF).");
            }
        }

        // Validation user_id pour création
        if (isCreate && post.getUserId() == null) {
            errors.add("L'utilisateur créateur est obligatoire.");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }
    }

    /**
     * Vérifie si l'URL est une URL d'image valide (HTTP/HTTPS) ou un chemin local.
     */
    private boolean isValidImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return true; // Empty is valid (optional field)
        }

        String trimmed = url.trim();

        // Check if it's a local file path (Windows C:\ or absolute path /)
        if (trimmed.startsWith("C:\\") || trimmed.startsWith("/") || trimmed.contains("\\")) {
            return hasValidImageExtension(trimmed);
        }

        // Check if it's a URL
        try {
            URL u = new URL(trimmed);
            String protocol = u.getProtocol();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                return false;
            }
            return hasValidImageExtension(u.getPath());
        } catch (Exception e) {
            // Not a valid URL, might still be a relative path
            return hasValidImageExtension(trimmed);
        }
    }

    private boolean hasValidImageExtension(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".gif") ||
               lower.endsWith(".webp") || lower.endsWith(".bmp");
    }

    public int getPostsPerPage() {
        return POSTS_PER_PAGE;
    }
}
