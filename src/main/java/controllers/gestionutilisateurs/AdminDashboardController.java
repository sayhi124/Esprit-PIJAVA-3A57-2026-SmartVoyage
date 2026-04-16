package controllers.gestionutilisateurs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import utils.NavigationManager;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Admin Dashboard Controller - Modern analytics dashboard with:
 * - Statistics cards (Revenue, Orders, Users, Growth)
 * - Chart placeholder for data visualization
 * - Customer table with sample data
 */
public class AdminDashboardController implements Initializable {

    @FXML
    private Label revenueValue;
    @FXML
    private Label ordersValue;
    @FXML
    private Label usersValue;
    @FXML
    private Label growthValue;

    @FXML
    private LineChart<String, Number> revenueChart;
    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;

    @FXML
    private TableView<CustomerData> customersTable;
    @FXML
    private TableColumn<CustomerData, String> nameColumn;
    @FXML
    private TableColumn<CustomerData, Integer> dealsColumn;
    @FXML
    private TableColumn<CustomerData, String> valueColumn;

    private final ObservableList<CustomerData> customerData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeStats();
        initializeChart();
        initializeTable();
    }

    private void initializeStats() {
        // Set animated values for statistics cards
        animateValue(revenueValue, "$3,131,021");
        animateValue(ordersValue, "18,221");
        animateValue(usersValue, "8,562");
        animateValue(growthValue, "71%");
    }

    private void animateValue(Label label, String finalValue) {
        label.setText(finalValue);
    }

    private void initializeChart() {
        // Set axis labels
        xAxis.setLabel("Month");
        yAxis.setLabel("Revenue ($)");

        // Configure chart with sample data
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Monthly Revenue");

        // Sample data points
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
        double[] values = {45000, 52000, 48000, 61000, 58000, 72000};

        for (int i = 0; i < months.length; i++) {
            series.getData().add(new XYChart.Data<>(months[i], values[i]));
        }

        revenueChart.getData().add(series);

        // Style the chart line
        series.getNode().setStyle("-fx-stroke: linear-gradient(to right, #eab308, #22d3ee); -fx-stroke-width: 3px;");

        // Style data points
        for (XYChart.Data<String, Number> data : series.getData()) {
            data.getNode().setStyle("-fx-background-color: #eab308, white; -fx-background-radius: 5px; -fx-padding: 5px;");
        }
    }

    private void initializeTable() {
        // Configure table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        dealsColumn.setCellValueFactory(new PropertyValueFactory<>("deals"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

        // Add sample customer data
        customerData.addAll(
            new CustomerData("Sarah Johnson", 12, "$45,230"),
            new CustomerData("Michael Chen", 8, "$32,150"),
            new CustomerData("Emma Rodriguez", 15, "$58,420"),
            new CustomerData("James Wilson", 6, "$21,890"),
            new CustomerData("Lisa Anderson", 10, "$39,750"),
            new CustomerData("David Kim", 14, "$52,180"),
            new CustomerData("Maria Garcia", 9, "$28,940"),
            new CustomerData("Robert Taylor", 11, "$41,560")
        );

        customersTable.setItems(customerData);
    }

    @FXML
    private void onBackToHome() {
        NavigationManager.getInstance().showWelcome();
    }

    @FXML
    private void onRefresh() {
        // Refresh dashboard data
        initializeStats();
    }

    @FXML
    private void onExport() {
        // Placeholder for export functionality
        System.out.println("Exporting dashboard data...");
    }

    @FXML
    private void onDashboard() {
        // Main dashboard - general statistics
        animateValue(revenueValue, "$3,131,021");
        animateValue(ordersValue, "18,221");
        animateValue(usersValue, "8,562");
        animateValue(growthValue, "71%");
        updateChartData("Monthly Revenue", new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun"}, new double[]{45000, 52000, 48000, 61000, 58000, 72000});
        System.out.println("Dashboard: General Statistics");
    }

    @FXML
    private void onAgences() {
        // Agencies section statistics
        animateValue(revenueValue, "$892,450");
        animateValue(ordersValue, "3,420");
        animateValue(usersValue, "1,250");
        animateValue(growthValue, "24%");
        updateChartData("Agency Revenue", new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun"}, new double[]{12000, 15000, 13000, 18000, 16000, 22000});
        System.out.println("Dashboard: Agencies Statistics");
    }

    @FXML
    private void onOffres() {
        // Offers section statistics
        animateValue(revenueValue, "$1,245,800");
        animateValue(ordersValue, "6,850");
        animateValue(usersValue, "2,340");
        animateValue(growthValue, "42%");
        updateChartData("Offers Revenue", new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun"}, new double[]{18000, 22000, 20000, 26000, 24000, 31000});
        System.out.println("Dashboard: Offers Statistics");
    }

    @FXML
    private void onMessagerie() {
        // Messaging section statistics
        animateValue(revenueValue, "$0");
        animateValue(ordersValue, "12,450");
        animateValue(usersValue, "5,230");
        animateValue(growthValue, "18%");
        updateChartData("Messages Sent", new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun"}, new double[]{1800, 2200, 2000, 2600, 2400, 3100});
        System.out.println("Dashboard: Messaging Statistics");
    }

    @FXML
    private void onEvenement() {
        // Events section statistics
        animateValue(revenueValue, "$456,230");
        animateValue(ordersValue, "2,180");
        animateValue(usersValue, "890");
        animateValue(growthValue, "56%");
        updateChartData("Event Revenue", new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun"}, new double[]{5000, 7000, 6000, 9000, 8000, 12000});
        System.out.println("Dashboard: Events Statistics");
    }

    @FXML
    private void onRecommandation() {
        NavigationManager.getInstance().showSignedInPosts();
        animateValue(usersValue, "1,852");
        animateValue(growthValue, "34%");
        updateChartData("Recommendations", new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun"}, new double[]{8000, 9000, 8500, 11000, 10500, 13000});
        System.out.println("Dashboard: Recommendations Statistics");
    }

    private void updateChartData(String seriesName, String[] months, double[] values) {
        revenueChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(seriesName);

        for (int i = 0; i < months.length; i++) {
            series.getData().add(new XYChart.Data<>(months[i], values[i]));
        }

        revenueChart.getData().add(series);

        // Style the chart line
        series.getNode().setStyle("-fx-stroke: linear-gradient(to right, #eab308, #22d3ee); -fx-stroke-width: 3px;");

        // Style data points
        for (XYChart.Data<String, Number> data : series.getData()) {
            data.getNode().setStyle("-fx-background-color: #eab308, white; -fx-background-radius: 5px; -fx-padding: 5px;");
        }
    }

    /**
     * Customer data model for the table
     */
    public static class CustomerData {
        private final String name;
        private final int deals;
        private final String value;

        public CustomerData(String name, int deals, String value) {
            this.name = name;
            this.deals = deals;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getDeals() {
            return deals;
        }

        public String getValue() {
            return value;
        }
    }
}
