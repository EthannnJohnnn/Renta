package controllers;

import app.MainApp;
import dao.BookingDAO;
import dao.PropertyDAO;
import dao.RoomDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import models.Booking;
import models.Property;
import models.Room;
import models.User;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class TenantDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private ListView<Property> propertyListView;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> maxPriceFilter;
    @FXML private CheckBox availableOnlyFilter;
    @FXML private Label statPropertiesCount;
    @FXML private Label statBookingsCount;
    @FXML private Label statReviewsCount;

    private final PropertyDAO propertyDAO = new PropertyDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
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

        // Stat counters
        statPropertiesCount.setText(String.valueOf(allProperties.size()));

        if (user != null) {
            List<Booking> myBookings = bookingDAO.getBookingsByTenantId(user.getId());
            statBookingsCount.setText(String.valueOf(myBookings.size()));
        } else {
            statBookingsCount.setText("0");
        }
        statReviewsCount.setText("★");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch(newVal));
        maxPriceFilter.setItems(FXCollections.observableArrayList(
                "₱3,000", "₱5,000", "₱8,000", "₱10,000", "₱15,000"
        ));
    }

    @FXML
    public void handleClearFilters() {
        searchField.clear();
        maxPriceFilter.setValue(null);
        availableOnlyFilter.setSelected(false);
        filteredProperties = allProperties;
        populateList(filteredProperties);
    }

    @FXML
    public void handleSearch() {
        handleSearch(searchField.getText());
    }

    private void handleSearch(String keyword) {
        filteredProperties = allProperties.stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword.toLowerCase())
                        || p.getAddress().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
        populateList(filteredProperties);
    }

    private void populateList(List<Property> properties) {
        ObservableList<Property> items = FXCollections.observableArrayList(properties);
        propertyListView.setItems(items);

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

                    javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(4);
                    vbox.setPadding(new Insets(12, 16, 12, 16));
                    vbox.setStyle("-fx-background-color: white; -fx-background-radius: 8px;" +
                            "-fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-cursor: hand;");

                    Label nameLbl = new Label("⌂ " + property.getName());
                    nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

                    Label addrLbl = new Label("📍 " + property.getAddress());
                    addrLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

                    Label priceLbl = new Label(priceRangeText);
                    priceLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;" +
                            "-fx-text-fill: #16A34A; -fx-padding: 4px 0px 0px 0px;");

                    vbox.getChildren().addAll(nameLbl, addrLbl, priceLbl);
                    setGraphic(vbox);
                    setStyle("-fx-background-color: transparent; -fx-padding: 4px 8px;");
                }
            }
        });
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
        Property selected = propertyListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
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