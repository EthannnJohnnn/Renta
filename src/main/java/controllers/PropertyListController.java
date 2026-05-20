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

import dao.RoomDAO;
import models.Room;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PropertyListController {

    @FXML private ListView<Property> propertyListView;
    @FXML private TextField searchField;
    @FXML private Label resultCountLabel;
    @FXML private ComboBox<String> maxPriceFilter;
    @FXML private CheckBox availableOnlyFilter;

    private final PropertyDAO propertyDAO = new PropertyDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private List<Property> allProperties;
    private List<Property> filteredProperties;

    @FXML
    public void initialize() {
        allProperties = propertyDAO.getAllProperties();
        filteredProperties = allProperties;
        populateList(filteredProperties);
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
        propertyListView.setItems(FXCollections.observableArrayList(properties));
        resultCountLabel.setText("Showing " + properties.size() + " listing(s)");
        
        propertyListView.setCellFactory(listView -> new ListCell<Property>() {
            @Override
            protected void updateItem(Property property, boolean empty) {
                super.updateItem(property, empty);
                if (empty || property == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    List<Room> rooms = roomDAO.getRoomsByPropertyId(property.getId());
                    double minPrice = rooms.stream().mapToDouble(Room::getPrice).min().orElse(0.0);
                    double maxPrice = rooms.stream().mapToDouble(Room::getPrice).max().orElse(0.0);
                    
                    String priceRangeText;
                    if (rooms.isEmpty()) {
                        priceRangeText = "No rooms configured";
                    } else if (minPrice == maxPrice) {
                        priceRangeText = String.format("₱%,.2f / month", minPrice);
                    } else {
                        priceRangeText = String.format("₱%,.2f – ₱%,.2f / month", minPrice, maxPrice);
                    }

                    VBox vbox = new VBox(6);
                    vbox.setPadding(new Insets(14, 18, 14, 18));
                    vbox.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-cursor: hand;");
                    
                    Label nameLbl = new Label("⌂ " + property.getName());
                    nameLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
                    
                    Label addrLbl = new Label("📍 " + property.getAddress());
                    addrLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");
                    
                    Label descLbl = new Label(property.getDescription());
                    descLbl.setWrapText(true);
                    descLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569; -fx-padding: 4px 0px 0px 0px;");
                    
                    Label priceLbl = new Label(priceRangeText);
                    priceLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #16A34A; -fx-padding: 8px 0px 0px 0px;");
                    
                    vbox.getChildren().addAll(nameLbl, addrLbl, descLbl, priceLbl);
                    
                    setGraphic(vbox);
                    setStyle("-fx-background-color: transparent; -fx-padding: 6px 12px;");
                }
            }
        });
    }

    private void handleSearch(String keyword) {
        filteredProperties = allProperties.stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword.toLowerCase())
                        || p.getAddress().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
        populateList(filteredProperties);
    }

    @FXML
    public void handlePropertyClick() {
        Property selected = propertyListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            navigateToDetail(selected);
        }
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