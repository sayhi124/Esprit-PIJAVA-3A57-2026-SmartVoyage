package models.gestionmessages;

import java.time.LocalDateTime;

/**
 * Entité {@code chat_group}. Représente un groupe de discussion.
 * L'{@code id} est généré par la base (AUTO_INCREMENT) : ne pas le passer au constructeur ;
 * après {@code INSERT}, le service pose l'id via {@link #setId(Integer)}.
 */
public class ChatGroup {

    private Integer id;
    private String nom;
    private LocalDateTime dateCreation;

    public ChatGroup() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
