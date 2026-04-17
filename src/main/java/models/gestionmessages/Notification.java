package models.gestionmessages;

import java.time.LocalDateTime;

/**
 * Entité {@code notification}. Représente une notification utilisateur.
 * L'{@code id} est généré par la base (AUTO_INCREMENT) : ne pas le passer au constructeur ;
 * après {@code INSERT}, le service pose l'id via {@link #setId(Integer)}.
 */
public class Notification {

    private Integer id;
    private String contenu;
    private LocalDateTime dateNotification;
    private String statut;
    private Integer userId;

    public Notification() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateNotification() {
        return dateNotification;
    }

    public void setDateNotification(LocalDateTime dateNotification) {
        this.dateNotification = dateNotification;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
