package controllers.messaging;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import models.messaging.Message;
import models.gestionutilisateurs.MessageConversation;
import services.messaging.MessageService;
import utils.NavigationManager;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MessagesController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int PREVIEW_MAX_LEN = 30;

    private final MessageService messageService = new MessageService();
    private final ObservableList<MessageConversation> allConversations = FXCollections.observableArrayList();
    private final ObservableList<MessageConversation> conversations = FXCollections.observableArrayList();
    private final ObservableList<Message> currentMessages = FXCollections.observableArrayList();

    private Integer currentUserId;
    private Integer selectedConversationUserId;
    private MessageConversation selectedConversation;
    private Runnable onUnreadChanged;
    private Timeline autoRefreshTimeline;
    private EventHandler<KeyEvent> escHandler;

    @FXML
    private ListView<MessageConversation> conversationsListView;
    @FXML
    private TextField conversationSearchField;
    @FXML
    private VBox messagesContainer;
    @FXML
    private ScrollPane messagesScrollPane;
    @FXML
    private TextArea messageInputArea;
    @FXML
    private Button sendButton;
    @FXML
    private Button backButton;
    @FXML
    private Label chatNameLabel;
    @FXML
    private Label chatRoleLabel;
    @FXML
    private Label chatAvatarLabel;
    @FXML
    private Label messageStatusLabel;

    @FXML
    protected void initialize() {
        escHandler = event -> {
            if (event.getCode() == KeyCode.ESCAPE && backButton != null && backButton.isVisible()) {
                event.consume();
                handleBack();
            }
        };

        conversationsListView.setItems(conversations);
        conversationsListView.setPlaceholder(new Label("No conversations yet"));
        conversationsListView.setCellFactory(list -> new ConversationCell());
        conversationsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedConversation = newVal;
            if (newVal != null) {
                selectedConversationUserId = newVal.getOtherUserId();
                updateChatHeader(newVal);
                loadConversation(selectedConversationUserId, true);
            } else {
                selectedConversationUserId = null;
                updateChatHeader(null);
            }
        });

        if (conversationSearchField != null) {
            conversationSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyConversationFilter());
        }

        sendButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            String value = messageInputArea.getText();
            boolean empty = value == null || value.trim().isEmpty();
            return empty || selectedConversationUserId == null;
        }, messageInputArea.textProperty(), conversationsListView.getSelectionModel().selectedItemProperty()));
        messageInputArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                onSendMessage();
            }
        });

        clearStatus();
        updateChatHeader(null);

        if (backButton != null) {
            backButton.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (oldScene != null) {
                    oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, escHandler);
                }
                if (newScene != null) {
                    newScene.addEventFilter(KeyEvent.KEY_PRESSED, escHandler);
                }
            });
        }

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
        loadConversations();
        startAutoRefresh();
    }

    public void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }

    @FXML
    protected void onSendMessage() {
        if (currentUserId == null || currentUserId <= 0) {
            showStatus("Invalid session.", true);
            return;
        }
        if (selectedConversation == null) {
            showStatus("Select a conversation first.", true);
            return;
        }
        String content = messageInputArea.getText() == null ? "" : messageInputArea.getText().trim();
        if (content.isBlank()) {
            return;
        }

        Message message = new Message();
        message.setSenderId(currentUserId);
        message.setReceiverId(selectedConversationUserId);
        message.setContent(content);
        message.setRead(false);

        try {
            messageService.sendMessage(message);
            messageInputArea.clear();
            loadConversation(selectedConversationUserId, false);
            loadConversations();
            clearStatus();
            if (onUnreadChanged != null) {
                onUnreadChanged.run();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showStatus("Failed to send message. Try again.", true);
        }
    }

    private void loadConversations() {
        if (currentUserId == null || currentUserId <= 0) {
            conversations.clear();
            return;
        }
        try {
            List<MessageConversation> source = messageService.getConversationsForUser(currentUserId);
            allConversations.setAll(source);
            applyConversationFilter();
        } catch (Exception ex) {
            ex.printStackTrace();
            showStatus("Unable to load conversations.", true);
        }
    }

    private void applyConversationFilter() {
        String query = conversationSearchField == null || conversationSearchField.getText() == null
            ? ""
            : conversationSearchField.getText().trim().toLowerCase();

        if (query.isBlank()) {
            conversations.setAll(allConversations);
        } else {
            conversations.setAll(allConversations.stream()
                .filter(c -> safe(c.getOtherUserName(), "").toLowerCase().contains(query))
                .toList());
        }

        if (selectedConversation == null && !conversations.isEmpty()) {
            conversationsListView.getSelectionModel().select(0);
            return;
        }

        if (selectedConversation == null && conversations.isEmpty()) {
            updateChatHeader(null);
            messagesContainer.getChildren().setAll(createPlaceholder("Start the conversation"));
            return;
        }

        if (selectedConversation != null) {
            conversations.stream()
                .filter(c -> c.getOtherUserId() == selectedConversation.getOtherUserId())
                .findFirst()
                .ifPresentOrElse(found -> {
                    selectedConversation = found;
                    conversationsListView.getSelectionModel().select(found);
                }, () -> {
                    if (!conversations.isEmpty()) {
                        conversationsListView.getSelectionModel().select(0);
                    } else {
                        selectedConversation = null;
                        selectedConversationUserId = null;
                        messagesContainer.getChildren().setAll(createPlaceholder("Start the conversation"));
                        updateChatHeader(null);
                    }
                });
        }
    }

    private void loadConversation(int otherUserId, boolean markRead) {
        if (currentUserId == null || currentUserId <= 0 || otherUserId <= 0) {
            return;
        }
        try {
            if (markRead) {
                messageService.markConversationAsRead(currentUserId, otherUserId);
                if (onUnreadChanged != null) {
                    onUnreadChanged.run();
                }
            }

            List<Message> conversation = messageService.getConversation(currentUserId, otherUserId);
            currentMessages.setAll(conversation);
            renderMessages();

        } catch (SQLException ex) {
            ex.printStackTrace();
            showStatus("Unable to load messages.", true);
        }
    }

    private void renderMessages() {
        messagesContainer.getChildren().clear();
        if (currentMessages.isEmpty()) {
            messagesContainer.getChildren().add(createPlaceholder("Start the conversation"));
            return;
        }

        for (Message message : currentMessages) {
            boolean mine = currentUserId != null && message.getSenderId() == currentUserId;

            HBox row = new HBox();
            row.setPadding(new Insets(2, 0, 2, 0));
            row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox bubble = new VBox(4);
            bubble.getStyleClass().addAll("message-bubble", mine ? "message-bubble-sent" : "message-bubble-received");

            Label content = new Label(message.getContent());
            content.setWrapText(true);
            content.getStyleClass().addAll("message-bubble-content", mine ? "message-sent" : "message-received");

            Label time = new Label(message.getCreatedAt() == null ? "" : TIME_FMT.format(message.getCreatedAt()));
            time.getStyleClass().add("message-bubble-time");

            bubble.getChildren().addAll(content, time);
            row.getChildren().add(bubble);
            messagesContainer.getChildren().add(row);

            FadeTransition fade = new FadeTransition(Duration.millis(180), row);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }

        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }

    @FXML
    protected void handleBack() {
        NavigationManager.getInstance().showSignedInOffers();
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            loadConversations();
            if (selectedConversationUserId != null) {
                loadConversation(selectedConversationUserId, true);
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void updateChatHeader(MessageConversation conversation) {
        if (chatNameLabel == null || chatRoleLabel == null || chatAvatarLabel == null) {
            return;
        }

        if (conversation == null) {
            chatNameLabel.setText("Select a conversation");
            chatRoleLabel.setText("-");
            chatRoleLabel.getStyleClass().removeAll("role-user", "role-agency");
            chatRoleLabel.getStyleClass().add("role-user");
            chatAvatarLabel.setText("?");
            return;
        }

        String role = normalizeRole(conversation.getOtherUserRole());
        String name = safe(conversation.getOtherUserName(), "Conversation");

        chatNameLabel.setText(name);
        chatRoleLabel.setText(role);
        chatRoleLabel.getStyleClass().removeAll("role-user", "role-agency");
        chatRoleLabel.getStyleClass().add("Agency".equals(role) ? "role-agency" : "role-user");
        chatAvatarLabel.setText(name.substring(0, 1).toUpperCase());
    }

    private void showStatus(String message, boolean error) {
        messageStatusLabel.setText(message == null ? "" : message);
        messageStatusLabel.getStyleClass().removeAll("status-error", "status-success");
        messageStatusLabel.getStyleClass().add(error ? "status-error" : "status-success");
        messageStatusLabel.setVisible(true);
        messageStatusLabel.setManaged(true);
    }

    private void clearStatus() {
        messageStatusLabel.setText("");
        messageStatusLabel.getStyleClass().removeAll("status-error", "status-success");
        messageStatusLabel.setVisible(false);
        messageStatusLabel.setManaged(false);
    }

    private final class ConversationCell extends ListCell<MessageConversation> {
        @Override
        protected void updateItem(MessageConversation item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            String role = normalizeRole(item.getOtherUserRole());

            Label name = new Label(safe(item.getOtherUserName(), "Conversation"));
            name.getStyleClass().add("conversation-name");

            Label roleBadge = new Label(role);
            roleBadge.getStyleClass().addAll("conversation-role-badge", roleStyleClass(role));

            HBox titleRow = new HBox(8, name, roleBadge);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            Label preview = new Label(truncatePreview(item.getLastMessage()));
            preview.getStyleClass().add("conversation-preview");
            preview.setWrapText(true);

            VBox textCol = new VBox(4, titleRow, preview);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            VBox rightCol = new VBox(6);
            rightCol.setAlignment(Pos.TOP_RIGHT);

            Label time = new Label(item.getLastMessageAt() == null ? "" : TIME_FMT.format(item.getLastMessageAt()));
            time.getStyleClass().add("conversation-time");

            Region unreadDot = new Region();
            unreadDot.getStyleClass().add("conversation-unread-dot");
            unreadDot.setVisible(item.getUnreadCount() > 0);
            unreadDot.setManaged(item.getUnreadCount() > 0);
            unreadDot.setMinSize(8, 8);
            unreadDot.setPrefSize(8, 8);
            unreadDot.setMaxSize(8, 8);

            rightCol.getChildren().addAll(time, unreadDot);

            HBox row = new HBox(10, textCol, spacer, rightCol);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("conversation-item");
            setGraphic(row);
        }
    }

    private Label createPlaceholder(String text) {
        Label placeholder = new Label(text);
        placeholder.getStyleClass().add("message-placeholder");
        return placeholder;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "User";
        }
        return "Agency".equalsIgnoreCase(role) ? "Agency" : "User";
    }

    private String roleStyleClass(String role) {
        return "Agency".equalsIgnoreCase(role) ? "role-agency" : "role-user";
    }

    private static String truncatePreview(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() <= PREVIEW_MAX_LEN) {
            return text;
        }
        return text.substring(0, PREVIEW_MAX_LEN - 1) + "...";
    }
}
