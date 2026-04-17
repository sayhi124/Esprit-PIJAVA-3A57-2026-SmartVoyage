package models.gestionmessages;

/**
 * Entité intermédiaire {@code group_member} pour la relation many-to-many entre ChatGroup et User.
 * L'{@code id} est généré par la base (AUTO_INCREMENT) : ne pas le passer au constructeur ;
 * après {@code INSERT}, le service pose l'id via {@link #setId(Integer)}.
 */
public class GroupMember {

    private Integer id;
    private Integer groupId;
    private Integer userId;

    public GroupMember() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
