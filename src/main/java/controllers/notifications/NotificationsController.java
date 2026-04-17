package controllers.notifications;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.notifications.Notification;
import services.notifications.NotificationService;
import utils.NavigationManager;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationsController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationService notificationService = new NotificationService();

    private Integer currentUserId;
    private Runnable onUnreadChanged;

    @FXML
    private VBox notificationsListContainer;
    @FXML
    private Label notificationStatusLabel;
    @FXML
    private Label unreadCountLabel;
    @FXML
    private Button markAllReadButton;

    @FXML
    private void initialize() {
        Integer sessionUserId = NavigationManager.getInstance().sessionUser()
            .map(models.gestionutilisateurs.User::getId)
            .orElse(null);
        if (sessionUserId != null && sessionUserId > 0) {
            prepare(sessionUserId, null);
        }
    }

    public void prepare(Integer currentUserId, Runnable onUnreadChanged) {
        this.currentUserId = currentUserId;
        this.onUnreadChanged = onUnreadChanged;
        refreshNotifications();
    }

    @FXML
    private void onMarkAllAsRead() {
        if (currentUserId == null || currentUserId <= 0) {
            showStatus("Invalid session.", true);
            return;
        }
        try {
            notificationService.markAllAsRead(currentUserId);
            refreshNotifications();
            if (onUnreadChanged != null) {
                onUnreadChanged.run();
            }
            showStatus("All notifications marked as read.", false);
        } catch (Exception ex) {
            ex.printStackTrace();
            showStatus("Unable to update notifications.", true);
        }
    }

    private void refreshNotifications() {
        notificationsListContainer.getChildren().clear();
        if (currentUserId == null || currentUserId <= 0) {
            unreadCountLabel.setText("0 unread");
            return;
        }
        try {
            List<Notification> notifications = notificationService.getUserNotifications(currentUserId);
            int unread = 0;

            if (notifications.isEmpty()) {
                Label empty = new Label("No notifications yet.");
                empty.getStyleClass().add("message-placeholder");
                notificationsListContainer.getChildren().add(empty);
            } else {
                for (Notification notification : notifications) {
                    if (!notification.isRead()) {
                        unread++;
                    }
                    notificationsListContainer.getChildren().add(buildNotificationItem(notification));
                }
            }

            unreadCountLabel.setText(unread + " unread");
            markAllReadButton.setDisable(unread == 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            showStatus("Unable to load notifications.", true);
        }
    }

    private HBox buildNotificationItem(Notification notification) {
        HBox row = new HBox(10);
        row.getStyleClass().addAll("notification-item", notification.isRead() ? "notification-read" : "notification-unread");
        row.setAlignment(Pos.CENTER_LEFT);

        Region dot = new Region();
        dot.getStyleClass().add("notification-dot");
        dot.setVisible(!notification.isRead());
        dot.setManaged(!notification.isRead());
        dot.setMinSize(10, 10);
        dot.setPrefSize(10, 10);
        dot.setMaxSize(10, 10);

        VBox textCol = new VBox(3);
        Label content = new Label(notification.getContent());
        content.getStyleClass().add("notification-content");
        content.setWrapText(true);

        Label time = new Label(notification.getCreatedAt() == null ? "" : TIME_FMT.format(notification.getCreatedAt()));
        time.getStyleClass().add("notification-time");
        textCol.getChildren().addAll(content, time);

        row.getChildren().addAll(dot, textCol);
        row.setOnMouseClicked(e -> {
            if (notification.isRead()) {
                return;
            }
            try {
                notificationService.markAsRead(notification.getId());
                refreshNotifications();
                if (onUnreadChanged != null) {
                    onUnreadChanged.run();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                showStatus("Unable to mark notification as read.", true);
            }
        });

        return row;
    }

    private void showStatus(String message, boolean error) {
        notificationStatusLabel.setText(message == null ? "" : message);
        notificationStatusLabel.getStyleClass().removeAll("status-error", "status-success");
        notificationStatusLabel.getStyleClass().add(error ? "status-error" : "status-success");
        notificationStatusLabel.setVisible(true);
        notificationStatusLabel.setManaged(true);
    }
}
