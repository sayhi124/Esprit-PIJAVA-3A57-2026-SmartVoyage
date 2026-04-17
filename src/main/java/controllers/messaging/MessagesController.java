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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import models.gestionagences.AgencyAccount;
import models.messaging.Message;
import models.gestionutilisateurs.MessageConversation;
import models.gestionutilisateurs.User;
import services.gestionagences.AgencyAccountService;
import services.gestionutilisateurs.UserService;
import services.messaging.MessageService;
import utils.NavigationManager;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public class MessagesController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int PREVIEW_MAX_LEN = 30;
    private static final int MAX_SUGGESTIONS = 8;

    private final MessageService messageService = new MessageService();
    private final UserService userService = new UserService();
    private final AgencyAccountService agencyAccountService = new AgencyAccountService();
    private final ObservableList<MessageConversation> allConversations = FXCollections.observableArrayList();
    private final ObservableList<MessageConversation> conversations = FXCollections.observableArrayList();
    private final ObservableList<Message> currentMessages = FXCollections.observableArrayList();

    private Integer currentUserId;
    private Integer selectedConversationUserId;
    private int receiverId = -1;
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
    private ScrollPane scrollPane;
    @FXML
    private TextField inputField;
    @FXML
    private HBox composerRow;
    @FXML
    private Button sendBtn;
    @FXML
    private Button newGroupBtn;
    @FXML
    private Button backButton;
    @FXML
    private Label usernameLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label avatarLabel;
    @FXML
    private Label messageStatusLabel;
    @FXML
    private VBox suggestionsContainer;
    @FXML
    private Label suggestionsTitleLabel;

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
                toggleSuggestionState(false);
                selectedConversationUserId = newVal.getOtherUserId() > 0 ? newVal.getOtherUserId() : null;
                receiverId = selectedConversationUserId == null ? -1 : selectedConversationUserId;
                updateChatHeader(newVal);
                if (receiverId > 0) {
                    loadConversation(receiverId, true);
                }
            } else {
                selectedConversationUserId = null;
                if (receiverId > 0) {
                    updateHeader(receiverId);
                } else {
                    updateChatHeader(null);
                }
            }
            updateSendButtonState();
        });

        if (conversationSearchField != null) {
            conversationSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyConversationFilter());
        }

        inputField.textProperty().addListener((obs, oldVal, newVal) -> updateSendButtonState());
        updateSendButtonState();
        sendBtn.setOnAction(event -> sendMessage());
        if (newGroupBtn != null) {
            newGroupBtn.setVisible(false);
            newGroupBtn.setManaged(false);
        }
        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                sendMessage();
            }
        });

        clearStatus();
        updateChatHeader(null);
        toggleSuggestionState(false);

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
        if (receiverId > 0) {
            loadOrCreateConversation(receiverId);
        }
        startAutoRefresh();
    }

    public void setReceiverId(int id) {
        System.out.println("SET RECEIVER: " + id);
        this.receiverId = id;
        toggleSuggestionState(false);
        updateHeader(id);
        loadOrCreateConversation(id);
        updateSendButtonState();
    }

    public void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }

    @FXML
    protected void onSendMessage() {
        sendMessage();
    }

    private void sendMessage() {
        Integer senderId = NavigationManager.getInstance().sessionUser()
            .map(User::getId)
            .orElse(currentUserId);
        if (senderId == null || senderId <= 0) {
            showStatus("Invalid session.", true);
            return;
        }
        currentUserId = senderId;
        if (receiverId <= 0) {
            showStatus("Select a receiver first.", true);
            return;
        }
        String content = inputField.getText() == null ? "" : inputField.getText().trim();
        if (content.isBlank()) {
            return;
        }

        System.out.println("Sender: " + senderId);
        System.out.println("Receiver: " + receiverId);
        System.out.println("Content: " + content);

        try {
            messageService.sendMessageToUser(senderId, receiverId, content);
            inputField.clear();
            loadConversation(receiverId, true);
            loadConversations();
            toggleSuggestionState(false);
            clearStatus();
            if (onUnreadChanged != null) {
                onUnreadChanged.run();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            String reason = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Unknown error"
                : ex.getMessage();
            showStatus("Failed to send message: " + reason, true);
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
            if (source.isEmpty() && receiverId <= 0) {
                loadSuggestions();
                toggleSuggestionState(true);
            } else {
                toggleSuggestionState(false);
            }
            if (receiverId > 0) {
                selectConversationByReceiver(receiverId);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showStatus("Unable to load conversations: " + safe(ex.getMessage(), "Unknown error"), true);
        }
    }

    private void selectConversationByReceiver(int userId) {
        if (userId <= 0 || conversationsListView == null) {
            return;
        }
        conversations.stream()
            .filter(c -> c.getOtherUserId() == userId)
            .findFirst()
            .ifPresent(c -> conversationsListView.getSelectionModel().select(c));
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
            if (receiverId > 0) {
                toggleSuggestionState(false);
                messagesContainer.getChildren().setAll(createPlaceholder("Start the conversation"));
                if (receiverId > 0) {
                    updateHeader(receiverId);
                } else {
                    updateChatHeader(null);
                }
            } else {
                loadSuggestions();
                toggleSuggestionState(true);
                updateChatHeader(null);
            }
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
                        if (receiverId > 0) {
                            toggleSuggestionState(false);
                            messagesContainer.getChildren().setAll(createPlaceholder("Start the conversation"));
                            if (receiverId > 0) {
                                updateHeader(receiverId);
                            } else {
                                updateChatHeader(null);
                            }
                        } else {
                            loadSuggestions();
                            toggleSuggestionState(true);
                            updateChatHeader(null);
                        }
                    }
                });
        }
    }

    private void loadConversation(int targetUserId, boolean markRead) {
        if (currentUserId == null || currentUserId <= 0 || targetUserId <= 0) {
            return;
        }
        try {
            if (markRead) {
                messageService.markConversationAsRead(currentUserId, targetUserId);
                if (onUnreadChanged != null) {
                    onUnreadChanged.run();
                }
            }

            List<Message> conversation = messageService.getConversation(currentUserId, targetUserId);
            currentMessages.setAll(conversation);
            renderMessages();

        } catch (SQLException ex) {
            ex.printStackTrace();
            showStatus("Unable to load messages.", true);
        }
    }

    private void loadOrCreateConversation(int userId) {
        if (currentUserId == null || currentUserId <= 0 || userId <= 0) {
            return;
        }

        selectedConversationUserId = userId;
        receiverId = userId;

        selectConversationByReceiver(userId);

        try {
            List<Message> messages = messageService.getConversation(currentUserId, userId);
            currentMessages.setAll(messages);
            toggleSuggestionState(false);
            messagesContainer.getChildren().clear();

            if (messages.isEmpty()) {
                Label empty = new Label("Start the conversation");
                empty.getStyleClass().add("empty-chat-label");
                messagesContainer.getChildren().add(empty);
            } else {
                for (Message msg : messages) {
                    addMessageBubble(msg);
                }
            }
            updateHeader(userId);
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
            updateSendButtonState();
        } catch (SQLException ex) {
            ex.printStackTrace();
            showStatus("Unable to load messages.", true);
        }
    }

    private void updateHeader(int userId) {
        if (usernameLabel == null || statusLabel == null || avatarLabel == null) {
            return;
        }
        if (userId <= 0) {
            usernameLabel.setText("Select a conversation");
            statusLabel.setText("Choose a chat to start messaging");
            avatarLabel.setText("?");
            return;
        }
        try {
            User user = userService.get(userId).orElse(null);
            String name = user == null
                ? "Conversation"
                : safe(user.getUsername(), safe(user.getEmail(), "Conversation"));
            usernameLabel.setText(name);
            statusLabel.setText("Active now");
            avatarLabel.setText(name.substring(0, 1).toUpperCase());
        } catch (SQLException ex) {
            String fallback = "Conversation";
            usernameLabel.setText(fallback);
            statusLabel.setText("Active now");
            avatarLabel.setText("?");
        }
    }

    private void renderMessages() {
        messagesContainer.getChildren().clear();
        if (currentMessages.isEmpty()) {
            messagesContainer.getChildren().add(createPlaceholder("Start the conversation"));
            return;
        }

        for (Message message : currentMessages) {
            addMessageBubble(message);
        }
    }

    private void addMessageBubble(Message msg) {
        HBox wrapper = new HBox();
        wrapper.setPadding(new Insets(2, 0, 2, 0));

        Label text = new Label(msg.getContent());
        text.setWrapText(true);
        text.getStyleClass().add("message-bubble");

        boolean isMine = currentUserId != null && msg.getSenderId() == currentUserId;
        if (isMine) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            text.getStyleClass().add("message-sent");
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);
            text.getStyleClass().add("message-received");
        }

        Label time = new Label(msg.getCreatedAt() == null ? "" : TIME_FMT.format(msg.getCreatedAt()));
        time.getStyleClass().add("message-bubble-time");

        VBox bubble = new VBox(3, text, time);
        bubble.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        if (isMine) {
            HBox actionsRow = new HBox(6);
            actionsRow.setAlignment(Pos.CENTER_RIGHT);
            actionsRow.getStyleClass().add("message-actions-row");

            Button editBtn = new Button("Edit");
            editBtn.getStyleClass().addAll("event-action-secondary", "message-action-edit");
            editBtn.setOnAction(event -> editMessage(msg));

            Button deleteBtn = new Button("Delete");
            deleteBtn.getStyleClass().addAll("event-action-secondary", "message-action-delete");
            deleteBtn.setOnAction(event -> deleteMessage(msg));

            actionsRow.getChildren().addAll(editBtn, deleteBtn);
            bubble.getChildren().add(actionsRow);
        }

        wrapper.getChildren().add(bubble);
        messagesContainer.getChildren().add(wrapper);

        FadeTransition fade = new FadeTransition(Duration.millis(180), wrapper);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();

        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void editMessage(Message msg) {
        if (msg == null || msg.getId() <= 0 || currentUserId == null || currentUserId <= 0) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog(msg.getContent());
        dialog.setTitle("Edit Message");
        dialog.setHeaderText("Edit your message");
        dialog.setContentText("Message:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String updated = result.get() == null ? "" : result.get().trim();
        if (updated.isBlank()) {
            showStatus("Message cannot be empty.", true);
            return;
        }

        try {
            messageService.editMessage(msg.getId(), currentUserId, updated);
            if (receiverId > 0) {
                loadConversation(receiverId, false);
            }
            loadConversations();
            showStatus("Message updated.", false);
        } catch (SQLException ex) {
            ex.printStackTrace();
            showStatus("Unable to edit message: " + safe(ex.getMessage(), "Unknown error"), true);
        }
    }

    private void deleteMessage(Message msg) {
        if (msg == null || msg.getId() <= 0 || currentUserId == null || currentUserId <= 0) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Message");
        confirm.setHeaderText("Delete this message?");
        confirm.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            messageService.deleteMessage(msg.getId(), currentUserId);
            if (receiverId > 0) {
                loadConversation(receiverId, false);
            }
            loadConversations();
            showStatus("Message deleted.", false);
        } catch (SQLException ex) {
            ex.printStackTrace();
            showStatus("Unable to delete message: " + safe(ex.getMessage(), "Unknown error"), true);
        }
    }

    @FXML
    protected void handleBack() {
        NavigationManager.getInstance().showSignedInOffers();
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            loadConversations();
            if (receiverId > 0) {
                loadOrCreateConversation(receiverId);
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void updateChatHeader(MessageConversation conversation) {
        if (usernameLabel == null || statusLabel == null || avatarLabel == null) {
            return;
        }

        if (conversation == null) {
            usernameLabel.setText("Select a conversation");
            statusLabel.setText("Choose a chat to start messaging");
            avatarLabel.setText("?");
            return;
        }
        String name = safe(conversation.getOtherUserName(), "Conversation");

        usernameLabel.setText(name);
        statusLabel.setText("Active now");
        avatarLabel.setText(name.substring(0, 1).toUpperCase());
    }

    private void loadSuggestions() {
        if (suggestionsContainer == null) {
            return;
        }

        suggestionsContainer.getChildren().clear();

        Label title = suggestionsTitleLabel != null ? suggestionsTitleLabel : new Label("Start a conversation");
        title.getStyleClass().setAll("suggestions-title");

        Label subtitle = new Label("No conversations yet. Start one below.");
        subtitle.getStyleClass().add("suggestions-subtitle");
        subtitle.setWrapText(true);

        suggestionsContainer.getChildren().addAll(title, subtitle);

        List<User> items = new ArrayList<>();
        LinkedHashSet<Integer> seen = new LinkedHashSet<>();

        try {
            List<AgencyAccount> agencies = agencyAccountService.findAll();
            for (AgencyAccount agency : agencies) {
                Integer candidateId = agency.getResponsableId();
                if (candidateId == null || candidateId <= 0 || candidateId.equals(currentUserId) || !seen.add(candidateId)) {
                    continue;
                }
                userService.get(candidateId).ifPresent(user -> {
                    if (items.size() < MAX_SUGGESTIONS) {
                        items.add(user);
                    }
                });
                if (items.size() >= MAX_SUGGESTIONS) {
                    break;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showStatus("Unable to load suggestions: " + safe(ex.getMessage(), "Unknown error"), true);
        }

        if (items.isEmpty()) {
            Label empty = new Label("No suggested contacts available.");
            empty.getStyleClass().add("message-placeholder");
            suggestionsContainer.getChildren().add(empty);
            return;
        }

        for (User user : items) {
            renderSuggestion(user);
        }
    }

    private void renderSuggestion(User user) {
        if (suggestionsContainer == null || user == null || user.getId() == null || user.getId() <= 0) {
            return;
        }

        String name = safe(user.getUsername(), safe(user.getEmail(), "User"));
        Label avatar = new Label(name.substring(0, 1).toUpperCase());
        avatar.getStyleClass().add("suggestion-avatar");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("suggestion-name");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Button contactBtn = new Button("Contact");
        contactBtn.getStyleClass().addAll("event-action-secondary", "suggestion-contact-btn");
        contactBtn.setOnAction(event -> setReceiverId(user.getId()));

        HBox row = new HBox(10, avatar, nameLabel, contactBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("suggestion-card");
        suggestionsContainer.getChildren().add(row);
    }

    private void toggleSuggestionState(boolean showSuggestions) {
        if (suggestionsContainer != null) {
            suggestionsContainer.setVisible(showSuggestions);
            suggestionsContainer.setManaged(showSuggestions);
        }
        if (scrollPane != null) {
            scrollPane.setVisible(!showSuggestions);
            scrollPane.setManaged(!showSuggestions);
        }
        if (composerRow != null) {
            composerRow.setVisible(!showSuggestions);
            composerRow.setManaged(!showSuggestions);
        }
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
        if ("Group".equalsIgnoreCase(role)) {
            return "Group";
        }
        return "Agency".equalsIgnoreCase(role) ? "Agency" : "User";
    }

    private String roleStyleClass(String role) {
        if ("Group".equalsIgnoreCase(role)) {
            return "role-user";
        }
        return "Agency".equalsIgnoreCase(role) ? "role-agency" : "role-user";
    }

    private void updateSendButtonState() {
        if (sendBtn == null || inputField == null) {
            return;
        }
        String value = inputField.getText();
        boolean empty = value == null || value.trim().isEmpty();
        sendBtn.setDisable(empty || receiverId <= 0);
    }

    private static String truncatePreview(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() <= PREVIEW_MAX_LEN) {
            return text;
        }
        return text.substring(0, PREVIEW_MAX_LEN - 1) + "...";
    }
}
