package controllers;

import app.MainApp;
import dao.PropertyDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import models.Property;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PropertyListController {

    @FXML private TableView<Property> propertyTable;
    @FXML private TableColumn<Property, String> nameColumn;
    @FXML private TableColumn<Property, String> addressColumn;
    @FXML private TableColumn<Property, String> descriptionColumn;
    @FXML private TableColumn<Property, String> viewColumn;
    @FXML private TextField searchField;
    @FXML private Label resultCountLabel;
    @FXML private ComboBox<String> maxPriceFilter;
    @FXML private CheckBox availableOnlyFilter;
    @FXML private ListView<String> propertyListView;

    private final PropertyDAO propertyDAO = new PropertyDAO();
    private List<Property> allProperties;
    private List<Property> filteredProperties;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));
        addressColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAddress()));
        descriptionColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDescription()));

        viewColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View Details");

            {
                viewBtn.setStyle("-fx-background-color: #4A9EFF; -fx-text-fill: white; " +
                        "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;");
                viewBtn.setOnAction(e -> {
                    Property selected = getTableView().getItems().get(getIndex());
                    navigateToDetail(selected);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewBtn);
            }
        });

        allProperties = propertyDAO.getAllProperties();
        populateTable(allProperties);
        maxPriceFilter.setItems(FXCollections.observableArrayList(
                "₱3,000", "₱5,000", "₱8,000", "₱10,000", "₱15,000"
        ));

        searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch(newVal));
    }

    // handleClearFilters():
    @FXML public void handleClearFilters() {
        searchField.clear();
        maxPriceFilter.setValue(null);
        availableOnlyFilter.setSelected(false);
        filteredProperties = allProperties;
        populateList(filteredProperties);
    }

    private void populateList(List<Property> properties) {
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Property p : properties) {
            items.add(p.getName() + " — " + p.getAddress());
        }
        propertyListView.setItems(items);
    }

    private void handleSearch(String keyword) {
        List<Property> filtered = allProperties.stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword.toLowerCase())
                        || p.getAddress().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
        populateTable(filtered);
    }

    private void populateTable(List<Property> properties) {
        propertyTable.setItems(FXCollections.observableArrayList(properties));
        resultCountLabel.setText("Showing " + properties.size() + " listing(s)");
    }

    private void navigateToDetail(Property property) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/views/PropertyDetail.fxml"));
            Parent root = loader.load();
            PropertyDetailController controller = loader.getController();
            controller.setProperty(property);
            MainApp.navigateTo(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleBackToDashboard() {
        MainApp.switchTo("views/TenantDashboard.fxml");
    }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.switchTo("views/Login.fxml");
    }


}