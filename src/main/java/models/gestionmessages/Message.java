package models.gestionmessages;

import java.time.LocalDateTime;

/**
 * Entité {@code message}. Représente un message privé ou de groupe.
 * L'{@code id} est généré par la base (AUTO_INCREMENT) : ne pas le passer au constructeur ;
 * après {@code INSERT}, le service pose l'id via {@link #setId(Integer)}.
 */
public class Message {

    private Integer id;
    private String contenu;
    private LocalDateTime dateEnvoi;
    private String statut;
    private Integer expediteurId;
    private Integer destinataireId;
    private Integer groupId;

    public Message() {
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

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Integer getExpediteurId() {
        return expediteurId;
    }

    public void setExpediteurId(Integer expediteurId) {
        this.expediteurId = expediteurId;
    }

    public Integer getDestinatataireId() {
        return destinataireId;
    }

    public void setDestinatataireId(Integer destinataireId) {
        this.destinataireId = destinataireId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }
}
