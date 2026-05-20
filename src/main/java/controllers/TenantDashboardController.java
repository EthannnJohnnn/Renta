package controllers;

import app.MainApp;
import dao.PropertyDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.Property;
import models.User;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class TenantDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private ListView<String> propertyListView;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> maxPriceFilter;
    @FXML private CheckBox availableOnlyFilter;

    private final PropertyDAO propertyDAO = new PropertyDAO();
    private List<Property> allProperties;
    private List<Property> filteredProperties;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getUsername());
        }

        allProperties = propertyDAO.getAllProperties();
        filteredProperties = allProperties;
        populateList(filteredProperties);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch(newVal));
        maxPriceFilter.setItems(FXCollections.observableArrayList(
                "₱3,000", "₱5,000", "₱8,000", "₱10,000", "₱15,000"
        ));
    }

    @FXML public void handleClearFilters() {
        searchField.clear();
        maxPriceFilter.setValue(null);
        availableOnlyFilter.setSelected(false);
        filteredProperties = allProperties;
        populateList(filteredProperties);
    }

    // Update handleSearch() to also apply price and availability filters.
    private void handleSearch(String keyword) {
        filteredProperties = allProperties.stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword.toLowerCase())
                        || p.getAddress().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
        populateList(filteredProperties);
    }

    private void populateList(List<Property> properties) {
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Property p : properties) {
            items.add(p.getName() + " — " + p.getAddress());
        }
        propertyListView.setItems(items);
    }

    @FXML
    public void handleBrowseProperties() {
        MainApp.switchTo("views/PropertyList.fxml");
    }

    @FXML
    public void handleMyBookings() {
        MainApp.switchTo("views/MyBookings.fxml");
    }

    @FXML
    public void handlePropertyClick() {
        int index = propertyListView.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            Property selected = filteredProperties.get(index);
            try {
                FXMLLoader loader = new FXMLLoader(
                        MainApp.class.getResource("/views/PropertyDetail.fxml"));
                Parent root = loader.load();
                PropertyDetailController controller = loader.getController();
                controller.setProperty(selected);
                MainApp.navigateTo(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.switchTo("views/Login.fxml");
    }
}