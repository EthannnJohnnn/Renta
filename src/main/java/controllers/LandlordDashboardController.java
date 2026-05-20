package controllers;

import app.MainApp;
import dao.BookingDAO;
import dao.PropertyDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import models.Property;
import models.User;

import java.util.List;

public class LandlordDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private ListView<String> listingsView;
    @FXML private HBox pendingBanner;
    @FXML private Label pendingBannerTitle;
    @FXML private Label pendingBannerSub;
    @FXML private Label pendingCountStat;

    private final PropertyDAO propertyDAO = new PropertyDAO();
    private final BookingDAO bookingDAO = new BookingDAO();

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();

        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getUsername());
            loadListings(user.getId());

            // Get total pending bookings using the newly added backend method
            int pendingCount = bookingDAO.countPendingByLandlordId(user.getId());
            pendingCountStat.setText(String.valueOf(pendingCount));

            // Update UI Based on Pending Count
            if (pendingCount > 0) {
                pendingBannerTitle.setText("⚠️ You have " + pendingCount + " pending booking request(s)!");
                pendingBannerSub.setText("Review and respond to keep your tenants informed.");
                pendingBanner.setVisible(true);
                pendingBanner.setManaged(true);
            } else {
                // If 0, hide the banner entirely
                pendingBanner.setVisible(false);
                pendingBanner.setManaged(false);
            }
        }
    }

    private void loadListings(int landlordId) {
        List<Property> properties = propertyDAO.getPropertiesByLandlordId(landlordId);
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Property p : properties) {
            items.add(p.getName() + " — " + p.getAddress());
        }
        listingsView.setItems(items);
    }

    @FXML
    public void handleAddProperty() {
        MainApp.switchTo("views/AddEditProperty.fxml");
    }

    @FXML
    public void handleManageListings() {
        MainApp.switchTo("views/ManageListings.fxml");
    }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.switchTo("views/Login.fxml");
    }

    @FXML
    public void handleBookingRequests() {
        MainApp.switchTo("views/BookingRequests.fxml");
    }
}