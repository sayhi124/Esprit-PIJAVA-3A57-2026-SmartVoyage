package controllers.gestionutilisateurs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ButtonType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import models.gestionagences.AgencyAdminApplication;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import services.gestionagences.AgencyAccountService;
import services.gestionagences.AgencyAdminApplicationService;
import services.gestionutilisateurs.UserService;
import utils.DbConnexion;
import utils.NavigationManager;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.util.Duration;

public class AdminDashboardController implements Initializable {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML private Label revenueValue;
    @FXML private Label ordersValue;
    @FXML private Label usersValue;
    @FXML private Label growthValue;
    @FXML private LineChart<String, Number> revenueChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private TableView<CustomerData> customersTable;
    @FXML private TableColumn<CustomerData, String> nameColumn;
    @FXML private TableColumn<CustomerData, String> dealsColumn;
    @FXML private TableColumn<CustomerData, String> valueColumn;
    @FXML private TableColumn<CustomerData, CustomerData> userActionsColumn;

    @FXML private VBox dashboardOverviewPane;
    @FXML private VBox agenciesPane;
    @FXML private Label sectionTitleLabel;
    @FXML private Label agenciesCountValue;
    @FXML private Label postsCountValue;
    @FXML private Label interactionsRateValue;
    @FXML private Label pendingCountValue;
    @FXML private TableView<AgencyProposalRow> proposalsTable;
    @FXML private TableColumn<AgencyProposalRow, String> applicantColumn;
    @FXML private TableColumn<AgencyProposalRow, String> agencyColumn;
    @FXML private TableColumn<AgencyProposalRow, String> countryColumn;
    @FXML private TableColumn<AgencyProposalRow, String> requestedAtColumn;
    @FXML private TableColumn<AgencyProposalRow, String> statusColumn;
    @FXML private TableColumn<AgencyProposalRow, String> messageColumn;
    @FXML private TableColumn<AgencyProposalRow, AgencyProposalRow> proposalActionsColumn;
    @FXML private Label agenciesStatusLabel;
    @FXML private Button dashboardNavButton;
    @FXML private Button agencesNavButton;
    @FXML private PieChart agenciesStatusChart;
    @FXML private BarChart<String, Number> agenciesPostsChart;
    @FXML private CategoryAxis agencyPostsXAxis;
    @FXML private NumberAxis agencyPostsYAxis;
    @FXML private TableView<AgencySummaryRow> agenciesTable;
    @FXML private TableColumn<AgencySummaryRow, String> agencyIdColumn;
    @FXML private TableColumn<AgencySummaryRow, String> agencyNameColumn;
    @FXML private TableColumn<AgencySummaryRow, String> agencyOwnerColumn;
    @FXML private TableColumn<AgencySummaryRow, String> agencyVerifiedColumn;
    @FXML private TableColumn<AgencySummaryRow, String> agencyCreatedColumn;
    @FXML private TableColumn<AgencySummaryRow, String> agencyUpdatedColumn;
    @FXML private TableColumn<AgencySummaryRow, String> agencyPostsColumn;
    @FXML private TableColumn<AgencySummaryRow, String> agencyInteractionsColumn;
    @FXML private TableColumn<AgencySummaryRow, AgencySummaryRow> agencyActionsColumn;

    private final ObservableList<CustomerData> customerData = FXCollections.observableArrayList();
    private final ObservableList<AgencyProposalRow> proposalRows = FXCollections.observableArrayList();
    private final ObservableList<AgencySummaryRow> agencySummaryRows = FXCollections.observableArrayList();
    private final AgencyAdminApplicationService applicationService = new AgencyAdminApplicationService();
    private final AgencyAccountService agencyAccountService = new AgencyAccountService();
    private final UserService userService = new UserService();
    private Timeline liveRefreshTimeline;
    private String lastAgenciesDataSignature;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!NavigationManager.getInstance().canAccessAdminFeatures()) {
            NavigationManager.getInstance().showAdminLogin();
            return;
        }
        initializeStats();
        initializeChart();
        initializeTable();
        initializeAgenciesSection();
        setupLiveRefresh();
        showDashboardSection();
    }

    private void setupLiveRefresh() {
        liveRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), evt -> {
            if (agenciesPane != null && agenciesPane.isVisible()) {
                refreshAgencyAdminDataIfChanged();
            }
        }));
        liveRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        liveRefreshTimeline.play();
    }

    private void initializeStats() {
        refreshOverviewMetrics();
    }

    private void animateValue(Label label, String finalValue) {
        label.setText(finalValue);
    }

    private void initializeChart() {
        xAxis.setLabel("Month");
        yAxis.setLabel("Feedback count");
        refreshOverviewChart();
    }

    private void initializeTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        dealsColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        userActionsColumn.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cell.getValue()));
        userActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");
            {
                deleteBtn.getStyleClass().addAll("action-button", "secondary");
                deleteBtn.setOnAction(evt -> {
                    CustomerData row = getItem();
                    if (row != null) {
                        deleteUser(row);
                    }
                });
            }

            @Override
            protected void updateItem(CustomerData item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : deleteBtn);
            }
        });
        customersTable.setItems(customerData);
        customersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        loadTopUsers();
    }

    private void refreshOverviewMetrics() {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM `user`) AS users_count,
                    (SELECT COUNT(*) FROM agency_account) AS agencies_count,
                    (SELECT COUNT(*) FROM travel_event) AS events_count,
                    (SELECT COUNT(*) FROM user_app_feedback) AS feedback_count
                """;
        try {
            Connection c = DbConnexion.getInstance().getConnection();
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    animateValue(revenueValue, String.valueOf(rs.getLong("users_count")));
                    animateValue(ordersValue, String.valueOf(rs.getLong("agencies_count")));
                    animateValue(usersValue, String.valueOf(rs.getLong("events_count")));
                    animateValue(growthValue, String.valueOf(rs.getLong("feedback_count")));
                }
            }
        } catch (SQLException e) {
            setAgenciesStatus("Unable to load overview metrics: " + e.getMessage());
        }
    }

    private void refreshOverviewChart() {
        String sql = """
                SELECT DATE_FORMAT(created_at, '%b') AS month_label, COUNT(*) AS total
                FROM user_app_feedback
                WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
                GROUP BY DATE_FORMAT(created_at, '%Y-%m'), DATE_FORMAT(created_at, '%b')
                ORDER BY DATE_FORMAT(created_at, '%Y-%m')
                """;
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Monthly Feedbacks");
        try {
            Connection c = DbConnexion.getInstance().getConnection();
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    series.getData().add(new XYChart.Data<>(rs.getString("month_label"), rs.getLong("total")));
                }
            }
        } catch (SQLException e) {
            setAgenciesStatus("Unable to load chart data: " + e.getMessage());
        }
        revenueChart.getData().clear();
        revenueChart.getData().add(series);
        if (series.getNode() != null) {
            series.getNode().setStyle("-fx-stroke: linear-gradient(to right, #eab308, #22d3ee); -fx-stroke-width: 3px;");
        }
    }

    private void loadTopUsers() {
        customerData.clear();
        String sql = """
                SELECT id, username, email, role
                FROM `user`
                ORDER BY id DESC
                LIMIT 50
                """;
        try {
            Connection c = DbConnexion.getInstance().getConnection();
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    customerData.add(new CustomerData(
                            rs.getInt("id"),
                            safeCell(rs.getString("username")),
                            safeCell(rs.getString("email")),
                            safeCell(rs.getString("role"))
                    ));
                }
            }
        } catch (SQLException e) {
            setAgenciesStatus("Unable to load users: " + e.getMessage());
        }
    }

    private void deleteUser(CustomerData row) {
        Integer currentUserId = NavigationManager.getInstance().sessionUser().map(u -> u.getId()).orElse(null);
        if (currentUserId != null && row.getId() == currentUserId) {
            setAgenciesStatus("You cannot delete your own account.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Delete user");
        confirm.setContentText("Delete user '" + row.getName() + "'?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            userService.delete(row.getId());
            setAgenciesStatus("User deleted: " + row.getName());
            loadTopUsers();
            refreshOverviewMetrics();
        } catch (SQLException | IllegalArgumentException e) {
            setAgenciesStatus("Unable to delete user: " + e.getMessage());
        }
    }

    private void initializeAgenciesSection() {
        applicantColumn.setCellValueFactory(new PropertyValueFactory<>("applicant"));
        agencyColumn.setCellValueFactory(new PropertyValueFactory<>("agency"));
        countryColumn.setCellValueFactory(new PropertyValueFactory<>("country"));
        requestedAtColumn.setCellValueFactory(new PropertyValueFactory<>("requestedAt"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        proposalActionsColumn.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cell.getValue()));
        proposalActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button acceptBtn = new Button("✔");
            private final Button rejectBtn = new Button("🗑");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(8, acceptBtn, rejectBtn);
            {
                acceptBtn.getStyleClass().addAll("action-button", "primary");
                rejectBtn.getStyleClass().addAll("action-button", "secondary");
                acceptBtn.setOnAction(evt -> {
                    AgencyProposalRow row = getItem();
                    if (row != null) {
                        approveProposal(row);
                    }
                });
                rejectBtn.setOnAction(evt -> {
                    AgencyProposalRow row = getItem();
                    if (row != null) {
                        rejectProposal(row);
                    }
                });
            }

            @Override
            protected void updateItem(AgencyProposalRow item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : box);
            }
        });
        proposalsTable.setItems(proposalRows);
        proposalsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        agencyIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        agencyNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        agencyOwnerColumn.setCellValueFactory(new PropertyValueFactory<>("owner"));
        agencyVerifiedColumn.setCellValueFactory(new PropertyValueFactory<>("verified"));
        agencyCreatedColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        agencyUpdatedColumn.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
        agencyPostsColumn.setCellValueFactory(new PropertyValueFactory<>("posts"));
        agencyInteractionsColumn.setCellValueFactory(new PropertyValueFactory<>("interactions"));
        agencyActionsColumn.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cell.getValue()));
        agencyActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑 Supprimer");
            {
                deleteBtn.getStyleClass().addAll("action-button", "secondary");
                deleteBtn.setOnAction(evt -> {
                    AgencySummaryRow row = getItem();
                    if (row != null) {
                        deleteAgency(row);
                    }
                });
            }

            @Override
            protected void updateItem(AgencySummaryRow item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : deleteBtn);
            }
        });
        agenciesTable.setItems(agencySummaryRows);
        agenciesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        refreshAgencyAdminData();
    }

    private void showDashboardSection() {
        dashboardOverviewPane.setVisible(true);
        dashboardOverviewPane.setManaged(true);
        agenciesPane.setVisible(false);
        agenciesPane.setManaged(false);
        sectionTitleLabel.setText("Admin Dashboard");
        setSidebarActive(dashboardNavButton, agencesNavButton);
        setAgenciesStatus("");
        refreshOverviewMetrics();
        refreshOverviewChart();
        loadTopUsers();
    }

    private void showAgenciesSection() {
        dashboardOverviewPane.setVisible(false);
        dashboardOverviewPane.setManaged(false);
        agenciesPane.setVisible(true);
        agenciesPane.setManaged(true);
        sectionTitleLabel.setText("Agencies - Validation");
        setSidebarActive(agencesNavButton, dashboardNavButton);
        refreshAgencyAdminData();
    }

    private void setSidebarActive(Button active, Button inactive) {
        if (inactive != null) {
            inactive.getStyleClass().remove("sidebar-nav-item-active");
        }
        if (active != null && !active.getStyleClass().contains("sidebar-nav-item-active")) {
            active.getStyleClass().add("sidebar-nav-item-active");
        }
    }

    private void refreshAgencyAdminData() {
        try {
            List<AgencyAdminApplication> pending = applicationService.findPending();
            proposalRows.setAll(pending.stream().map(AgencyProposalRow::from).toList());
            pendingCountValue.setText(String.valueOf(pending.size()));
            List<AgencySummaryRow> rows = loadAgenciesSummaries();
            agencySummaryRows.setAll(rows);
            agenciesCountValue.setText(String.valueOf(rows.size()));
            long posts = rows.stream().mapToLong(r -> r.postsRaw).sum();
            postsCountValue.setText(String.valueOf(posts));
            long likes = rows.stream().mapToLong(r -> r.likesRaw).sum();
            long comments = rows.stream().mapToLong(r -> r.commentsRaw).sum();
            if (posts > 0) {
                double rate = ((double) (likes + comments)) / posts;
                interactionsRateValue.setText(String.format("%.2f / post", rate));
            } else {
                interactionsRateValue.setText("0.00 / post");
            }
            refreshAgencyCharts(rows);
            lastAgenciesDataSignature = readAgenciesDataSignature();
            setAgenciesStatus("");
        } catch (SQLException e) {
            setAgenciesStatus("Impossible de charger les donnees agences: " + e.getMessage());
        }
    }

    private void refreshAgencyAdminDataIfChanged() {
        try {
            String signature = readAgenciesDataSignature();
            if (signature != null && signature.equals(lastAgenciesDataSignature)) {
                return;
            }
            refreshAgencyAdminData();
        } catch (SQLException ignored) {
            // Silent during background polling.
        }
    }

    private String readAgenciesDataSignature() throws SQLException {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM agency_admin_application WHERE status='PENDING') AS pending_count,
                    (SELECT COALESCE(UNIX_TIMESTAMP(MAX(requested_at)), 0) FROM agency_admin_application) AS pending_max,
                    (SELECT COUNT(*) FROM agency_account) AS agency_count,
                    (SELECT COALESCE(UNIX_TIMESTAMP(MAX(updated_at)), 0) FROM agency_account) AS agency_max,
                    (SELECT COUNT(*) FROM agency_post WHERE is_deleted = 0) AS post_count,
                    (SELECT COALESCE(UNIX_TIMESTAMP(MAX(updated_at)), 0) FROM agency_post WHERE is_deleted = 0) AS post_max,
                    (SELECT COUNT(*) FROM agency_post_like) AS like_count,
                    (SELECT COALESCE(UNIX_TIMESTAMP(MAX(created_at)), 0) FROM agency_post_like) AS like_max,
                    (SELECT COUNT(*) FROM agency_post_comment WHERE is_deleted = 0) AS comment_count,
                    (SELECT COALESCE(UNIX_TIMESTAMP(MAX(created_at)), 0) FROM agency_post_comment WHERE is_deleted = 0) AS comment_max
                """;
        Connection c = DbConnexion.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("pending_count") + "|" + rs.getLong("pending_max") + "|" +
                        rs.getLong("agency_count") + "|" + rs.getLong("agency_max") + "|" +
                        rs.getLong("post_count") + "|" + rs.getLong("post_max") + "|" +
                        rs.getLong("like_count") + "|" + rs.getLong("like_max") + "|" +
                        rs.getLong("comment_count") + "|" + rs.getLong("comment_max");
            }
        }
        return null;
    }

    private List<AgencySummaryRow> loadAgenciesSummaries() throws SQLException {
        String sql = """
                SELECT a.id,
                       a.agency_name,
                       COALESCE(u.username, '-') AS owner_username,
                       a.verified,
                       a.created_at,
                       a.updated_at,
                       (SELECT COUNT(*) FROM agency_post p WHERE p.agency_id = a.id AND p.is_deleted = 0) AS posts_count,
                       (SELECT COUNT(*)
                          FROM agency_post_like l
                          JOIN agency_post p2 ON p2.id = l.agency_post_id
                         WHERE p2.agency_id = a.id AND p2.is_deleted = 0) AS likes_count,
                       (SELECT COUNT(*)
                          FROM agency_post_comment c
                          JOIN agency_post p3 ON p3.id = c.agency_post_id
                         WHERE p3.agency_id = a.id AND p3.is_deleted = 0 AND c.is_deleted = 0) AS comments_count
                FROM agency_account a
                LEFT JOIN `user` u ON u.id = a.responsable_id
                ORDER BY a.id DESC
                """;
        Connection c = DbConnexion.getInstance().getConnection();
        List<AgencySummaryRow> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(AgencySummaryRow.from(rs));
            }
        }
        return rows;
    }

    private void refreshAgencyCharts(List<AgencySummaryRow> rows) {
        long verified = rows.stream().filter(r -> "Verifiee".equals(r.verified)).count();
        long pending = rows.size() - verified;
        agenciesStatusChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Verifiees", verified),
                new PieChart.Data("En attente", pending)
        ));
        agenciesStatusChart.setLegendVisible(true);
        agenciesStatusChart.setLabelsVisible(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Posts");
        rows.stream().limit(8).forEach(r -> series.getData().add(new XYChart.Data<>(r.name, r.postsRaw)));
        agenciesPostsChart.getData().clear();
        agenciesPostsChart.getData().add(series);
        agencyPostsXAxis.setLabel("Agencies");
        agencyPostsYAxis.setLabel("Posts");
    }

    @FXML
    private void onBackToHome() {
        NavigationManager.getInstance().showWelcome();
    }

    @FXML
    private void onRefresh() {
        initializeStats();
        refreshAgencyAdminData();
    }

    @FXML
    private void onExport() {
        if (dashboardOverviewPane == null || dashboardOverviewPane.getScene() == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter dashboard en PDF");
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("admin-dashboard-export.pdf");
        File target = chooser.showSaveDialog(dashboardOverviewPane.getScene().getWindow());
        if (target == null) {
            return;
        }
        if (!target.getName().toLowerCase().endsWith(".pdf")) {
            target = new File(target.getParentFile(), target.getName() + ".pdf");
        }
        try {
            refreshAgencyAdminData();
            writeProfessionalPdfReport(target);
            setAgenciesStatus("Export PDF genere: " + target.getName());
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("PDF export failed");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            setAgenciesStatus("PDF export failed: " + e.getMessage());
        }
    }

    private void writeProfessionalPdfReport(File outputFile) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float margin = 32f;
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float pageWidth = page.getMediaBox().getWidth() - (margin * 2);
            float y = page.getMediaBox().getHeight() - margin;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                y = drawText(cs, titleFont, 18, margin, y, "SmartVoyage Admin Report");
                y = drawText(cs, bodyFont, 10, margin, y - 2, "Generated: " + java.time.LocalDateTime.now().format(DATE_FMT));
                y -= 14;

                float boxGap = 10f;
                float boxW = (pageWidth - (boxGap * 3)) / 4f;
                y = drawKpiBox(cs, margin, y, boxW, "Agencies", agenciesCountValue.getText());
                y = drawKpiBox(cs, margin + boxW + boxGap, y + 0, boxW, "Posts", postsCountValue.getText());
                y = drawKpiBox(cs, margin + (boxW + boxGap) * 2, y + 0, boxW, "Interactions", interactionsRateValue.getText());
                y = drawKpiBox(cs, margin + (boxW + boxGap) * 3, y + 0, boxW, "Requests", pendingCountValue.getText());
                y -= 24;
            }

            byte[] pieBytes = snapshotNodeAsPngBytes(agenciesStatusChart);
            byte[] barBytes = snapshotNodeAsPngBytes(agenciesPostsChart);
            y = drawImageOnCurrentPage(doc, page, pieBytes, margin, y, pageWidth / 2f - 8, 170);
            y = drawImageOnCurrentPage(doc, page, barBytes, margin + pageWidth / 2f + 8, y + 170, pageWidth / 2f - 8, 170);
            y -= 20;

            page = drawTableWithPagination(doc, page, margin, y, "Pending agency requests",
                    new String[]{"Applicant", "Requested agency", "Country", "Date", "Status", "Message"},
                    proposalRows.stream().map(r -> new String[]{r.getApplicant(), r.getAgency(), r.getCountry(), r.getRequestedAt(), r.getStatus(), r.getMessage()}).toList());

            float nextY = page.getMediaBox().getHeight() - margin;
            drawTableWithPagination(doc, page, margin, nextY - 14, "Toutes les agences",
                    new String[]{"#", "Nom", "Responsable", "Verifiee", "Creee le", "Mise a jour", "Posts", "Interactions"},
                    agencySummaryRows.stream().map(r -> new String[]{r.getId(), r.getName(), r.getOwner(), r.getVerified(), r.getCreatedAt(), r.getUpdatedAt(), r.getPosts(), r.getInteractions()}).toList());

            doc.save(outputFile);
        }
    }

    private float drawText(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
        return y - (size + 4f);
    }

    private float drawKpiBox(PDPageContentStream cs, float x, float y, float w, String label, String value) throws IOException {
        float h = 44f;
        cs.setNonStrokingColor(37f / 255f, 51f / 255f, 79f / 255f);
        cs.addRect(x, y - h, w, h);
        cs.fill();
        cs.setStrokingColor(75f / 255f, 95f / 255f, 130f / 255f);
        cs.addRect(x, y - h, w, h);
        cs.stroke();
        drawText(cs, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9, x + 8, y - 14, label);
        drawText(cs, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11, x + 8, y - 30, value == null ? "-" : value);
        return y - h;
    }

    private byte[] snapshotNodeAsPngBytes(javafx.scene.Node node) throws IOException {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage snap = node.snapshot(params, null);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(SwingFXUtils.fromFXImage(snap, null), "png", png);
        return png.toByteArray();
    }

    private float drawImageOnCurrentPage(PDDocument doc, PDPage page, byte[] pngBytes, float x, float y, float w, float h) throws IOException {
        PDImageXObject img = PDImageXObject.createFromByteArray(doc, pngBytes, "chart");
        try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            cs.drawImage(img, x, y - h, w, h);
        }
        return y - h;
    }

    private PDPage drawTableWithPagination(PDDocument doc, PDPage currentPage, float margin, float startY, String title,
                                           String[] headers, List<String[]> rows) throws IOException {
        float y = startY;
        float rowH = 18f;
        float pageW = currentPage.getMediaBox().getWidth() - (margin * 2);
        float[] colW = new float[headers.length];
        float w = pageW / headers.length;
        for (int i = 0; i < headers.length; i++) colW[i] = w;

        PDType1Font body = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        try (PDPageContentStream cs = new PDPageContentStream(doc, currentPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
            y = drawText(cs, bold, 12, margin, y, title) - 6;
        }
        y = drawTableHeader(doc, currentPage, margin, y, headers, colW, rowH);

        for (String[] row : rows) {
            if (y - rowH < margin) {
                currentPage = new PDPage(PDRectangle.A4);
                doc.addPage(currentPage);
                y = currentPage.getMediaBox().getHeight() - margin;
                y = drawTableHeader(doc, currentPage, margin, y, headers, colW, rowH);
            }
            try (PDPageContentStream cs = new PDPageContentStream(doc, currentPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
                float x = margin;
                for (int i = 0; i < headers.length; i++) {
                    cs.setStrokingColor(65f / 255f, 82f / 255f, 112f / 255f);
                    cs.addRect(x, y - rowH, colW[i], rowH);
                    cs.stroke();
                    String cell = i < row.length ? safeCell(row[i]) : "";
                    cs.beginText();
                    cs.setFont(body, 8f);
                    cs.newLineAtOffset(x + 4, y - 12);
                    cs.showText(cell);
                    cs.endText();
                    x += colW[i];
                }
            }
            y -= rowH;
        }
        return currentPage;
    }

    private float drawTableHeader(PDDocument doc, PDPage page, float margin, float y, String[] headers, float[] colW, float rowH) throws IOException {
        try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            float x = margin;
            for (int i = 0; i < headers.length; i++) {
                cs.setNonStrokingColor(35f / 255f, 49f / 255f, 77f / 255f);
                cs.addRect(x, y - rowH, colW[i], rowH);
                cs.fill();
                cs.setStrokingColor(80f / 255f, 98f / 255f, 128f / 255f);
                cs.addRect(x, y - rowH, colW[i], rowH);
                cs.stroke();
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 8f);
                cs.newLineAtOffset(x + 4, y - 12);
                cs.showText(safeCell(headers[i]));
                cs.endText();
                x += colW[i];
            }
        }
        return y - rowH;
    }

    private static String safeCell(String text) {
        if (text == null) return "";
        String t = text.replace('\n', ' ').replace('\r', ' ');
        return t.length() > 42 ? t.substring(0, 39) + "..." : t;
    }

    @FXML
    private void onDashboard() {
        animateValue(revenueValue, "$3,131,021");
        animateValue(ordersValue, "18,221");
        animateValue(usersValue, "8,562");
        animateValue(growthValue, "71%");
        updateChartData("Monthly Revenue", new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun"}, new double[]{45000, 52000, 48000, 61000, 58000, 72000});
        showDashboardSection();
    }

    @FXML
    private void onAgences() {
        showAgenciesSection();
    }

    @FXML
    private void onOffres() {
        animateValue(revenueValue, "$1,245,800");
        animateValue(ordersValue, "6,850");
        animateValue(usersValue, "2,340");
        animateValue(growthValue, "42%");
        updateChartData("Offers Revenue", new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun"}, new double[]{18000, 22000, 20000, 26000, 24000, 31000});
        showDashboardSection();
    }

    @FXML
    private void onMessagerie() {
        animateValue(revenueValue, "$0");
        animateValue(ordersValue, "12,450");
        animateValue(usersValue, "5,230");
        animateValue(growthValue, "18%");
        updateChartData("Messages Sent", new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun"}, new double[]{1800, 2200, 2000, 2600, 2400, 3100});
        showDashboardSection();
    }

    @FXML
    private void onEvenement() {
        animateValue(revenueValue, "$456,230");
        animateValue(ordersValue, "2,180");
        animateValue(usersValue, "890");
        animateValue(growthValue, "56%");
        updateChartData("Event Revenue", new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun"}, new double[]{5000, 7000, 6000, 9000, 8000, 12000});
        showDashboardSection();
    }

    @FXML
    private void onRecommandation() {
        animateValue(revenueValue, "$536,541");
        animateValue(ordersValue, "3,621");
        animateValue(usersValue, "1,852");
        animateValue(growthValue, "34%");
        updateChartData("Recommendations", new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun"}, new double[]{8000, 9000, 8500, 11000, 10500, 13000});
        showDashboardSection();
    }

    private void approveProposal(AgencyProposalRow row) {
        Integer reviewer = NavigationManager.getInstance().sessionUser().map(u -> u.getId()).orElse(null);
        if (reviewer == null) {
            setAgenciesStatus("Invalid admin session.");
            return;
        }
        try {
            applicationService.approve(row.id, reviewer);
            setAgenciesStatus("Request approved successfully.");
            refreshAgencyAdminData();
        } catch (SQLException | IllegalArgumentException e) {
            setAgenciesStatus("Approval failed: " + e.getMessage());
        }
    }

    private void rejectProposal(AgencyProposalRow row) {
        Integer reviewer = NavigationManager.getInstance().sessionUser().map(u -> u.getId()).orElse(null);
        if (reviewer == null) {
            setAgenciesStatus("Invalid admin session.");
            return;
        }
        try {
            applicationService.reject(row.id, reviewer, null);
            setAgenciesStatus("Request rejected.");
            refreshAgencyAdminData();
        } catch (SQLException | IllegalArgumentException e) {
            setAgenciesStatus("Rejection failed: " + e.getMessage());
        }
    }

    private void deleteAgency(AgencySummaryRow row) {
        try {
            agencyAccountService.delete(row.idRaw);
            setAgenciesStatus("Agency deleted: " + row.name);
            refreshAgencyAdminData();
        } catch (SQLException | IllegalArgumentException e) {
            setAgenciesStatus("Delete failed: " + e.getMessage());
        }
    }

    private void setAgenciesStatus(String message) {
        if (message == null || message.isBlank()) {
            agenciesStatusLabel.setManaged(false);
            agenciesStatusLabel.setVisible(false);
            agenciesStatusLabel.setText("");
        } else {
            agenciesStatusLabel.setManaged(true);
            agenciesStatusLabel.setVisible(true);
            agenciesStatusLabel.setText(message);
        }
    }

    private void updateChartData(String seriesName, String[] months, double[] values) {
        revenueChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(seriesName);
        for (int i = 0; i < months.length; i++) {
            series.getData().add(new XYChart.Data<>(months[i], values[i]));
        }
        revenueChart.getData().add(series);
        series.getNode().setStyle("-fx-stroke: linear-gradient(to right, #eab308, #22d3ee); -fx-stroke-width: 3px;");
        for (XYChart.Data<String, Number> data : series.getData()) {
            data.getNode().setStyle("-fx-background-color: #eab308, white; -fx-background-radius: 5px; -fx-padding: 5px;");
        }
    }

    public static class CustomerData {
        private final int id;
        private final String name;
        private final String email;
        private final String value;

        public CustomerData(int id, String name, String email, String value) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.value = value;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getValue() { return value; }
    }

    public static class AgencyProposalRow {
        private final Long id;
        private final String applicant;
        private final String agency;
        private final String country;
        private final String requestedAt;
        private final String status;
        private final String message;

        private AgencyProposalRow(Long id, String applicant, String agency, String country, String requestedAt, String status, String message) {
            this.id = id;
            this.applicant = applicant;
            this.agency = agency;
            this.country = country;
            this.requestedAt = requestedAt;
            this.status = status;
            this.message = message;
        }

        public static AgencyProposalRow from(AgencyAdminApplication app) {
            String when = app.getRequestedAt() == null ? "-" : DATE_FMT.format(app.getRequestedAt());
            String applicantLabel = app.getApplicantId() == null ? "-" : "User #" + app.getApplicantId();
            return new AgencyProposalRow(
                    app.getId(),
                    applicantLabel,
                    safe(app.getAgencyNameRequested()),
                    safe(app.getCountry()),
                    when,
                    app.getStatus() == null ? "-" : app.getStatus().name(),
                    safe(app.getMessageToAdmin())
            );
        }

        public String getApplicant() { return applicant; }
        public String getAgency() { return agency; }
        public String getCountry() { return country; }
        public String getRequestedAt() { return requestedAt; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }

        private static String safe(String v) {
            return v == null || v.isBlank() ? "-" : v;
        }
    }

    public static class AgencySummaryRow {
        private final long idRaw;
        private final String id;
        private final String name;
        private final String owner;
        private final String verified;
        private final String createdAt;
        private final String updatedAt;
        private final String posts;
        private final String interactions;
        private final long postsRaw;
        private final long likesRaw;
        private final long commentsRaw;

        private AgencySummaryRow(long idRaw, String id, String name, String owner, String verified, String createdAt, String updatedAt,
                                 String posts, String interactions, long postsRaw, long likesRaw, long commentsRaw) {
            this.idRaw = idRaw;
            this.id = id;
            this.name = name;
            this.owner = owner;
            this.verified = verified;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.posts = posts;
            this.interactions = interactions;
            this.postsRaw = postsRaw;
            this.likesRaw = likesRaw;
            this.commentsRaw = commentsRaw;
        }

        public static AgencySummaryRow from(ResultSet rs) throws SQLException {
            long id = rs.getLong("id");
            String name = rs.getString("agency_name");
            String owner = rs.getString("owner_username");
            boolean verified = rs.getBoolean("verified");
            Timestamp createdTs = rs.getTimestamp("created_at");
            Timestamp updatedTs = rs.getTimestamp("updated_at");
            long posts = rs.getLong("posts_count");
            long likes = rs.getLong("likes_count");
            long comments = rs.getLong("comments_count");
            String created = formatDate(createdTs);
            String updated = formatDate(updatedTs);
            return new AgencySummaryRow(
                    id,
                    String.valueOf(id),
                    safe(name),
                    safe(owner),
                    verified ? "Verifiee" : "En attente",
                    created,
                    updated,
                    String.valueOf(posts),
                    String.valueOf(likes + comments),
                    posts, likes, comments
            );
        }

        private static String formatDate(Timestamp ts) {
            LocalDateTime dt = ts == null ? null : ts.toLocalDateTime();
            return dt == null ? "-" : DATE_FMT.format(dt);
        }

        private static String safe(String v) {
            return v == null || v.isBlank() ? "-" : v;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getOwner() { return owner; }
        public String getVerified() { return verified; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public String getPosts() { return posts; }
        public String getInteractions() { return interactions; }
    }
}
