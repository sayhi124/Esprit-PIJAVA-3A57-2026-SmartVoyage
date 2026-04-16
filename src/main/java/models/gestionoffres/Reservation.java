package models.gestionoffres;

import java.time.LocalDateTime;
import java.util.Objects;

public class Reservation {

    private int id;
    private TravelOffer offer;
    private Integer userId;
    private String contactInfo;
    private Integer reservedSeats;
    private LocalDateTime reservationDate;
    private String status;
    private Boolean isPaid;

    public Reservation() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TravelOffer getOffer() {
        return offer;
    }

    public void setOffer(TravelOffer offer) {
        this.offer = offer;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public Integer getReservedSeats() {
        return reservedSeats;
    }

    public void setReservedSeats(Integer reservedSeats) {
        this.reservedSeats = reservedSeats;
    }

    public LocalDateTime getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDateTime reservationDate) {
        this.reservationDate = reservationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean isPaid() {
        return isPaid;
    }

    public void setPaid(Boolean paid) {
        isPaid = paid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reservation that)) {
            return false;
        }
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", offerId=" + (offer != null ? offer.getId() : null) +
                ", userId=" + userId +
                ", reservedSeats=" + reservedSeats +
                ", reservationDate=" + reservationDate +
                ", status='" + status + '\'' +
                ", isPaid=" + isPaid +
                '}';
    }
}
