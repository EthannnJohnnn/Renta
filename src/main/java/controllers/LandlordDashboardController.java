package controllers;

import app.MainApp;
import dao.BookingDAO;
import dao.PropertyDAO;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.Property;
import models.User;

import java.util.List;

public class LandlordDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private VBox listingsContainer;
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

            int pendingCount = bookingDAO.countPendingByLandlordId(user.getId());
            pendingCountStat.setText(String.valueOf(pendingCount));

            if (pendingCount > 0) {
                pendingBannerTitle.setText("[!] You have " + pendingCount + " pending booking request(s)!");
                pendingBannerSub.setText("Review and respond to keep your tenants informed.");
                pendingBanner.setVisible(true);
                pendingBanner.setManaged(true);
            } else {
                pendingBanner.setVisible(false);
                pendingBanner.setManaged(false);
            }
        }
    }

    private void loadListings(int landlordId) {
        listingsContainer.getChildren().clear();
        List<Property> properties = propertyDAO.getPropertiesByLandlordId(landlordId);

        if (properties.isEmpty()) {
            VBox emptyState = new VBox(10);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setStyle("-fx-padding: 40;");
            Label emptyIcon = new Label("⌂");
            emptyIcon.setStyle("-fx-font-size: 32px; -fx-text-fill: #94A3B8;");
            Label emptyTitle = new Label("No properties listed yet");
            emptyTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #1E293B; -fx-font-weight: bold;");
            Label emptySub = new Label("Click '+ Add Property' above to list your first property.");
            emptySub.setStyle("-fx-text-fill: #64748B;");
            emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptySub);
            listingsContainer.getChildren().add(emptyState);
            return;
        }

        // Show a max preview of 3 properties nicely formatted
        int limit = Math.min(properties.size(), 3);
        for (int i = 0; i < limit; i++) {
            Property p = properties.get(i);
            
            HBox card = new HBox(16);
            card.getStyleClass().add("card-flat");
            card.setAlignment(Pos.CENTER_LEFT);

            VBox details = new VBox(4);
            Label nameLabel = new Label(p.getName());
            nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
            
            Label addressLabel = new Label(p.getAddress());
            addressLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
            
            details.getChildren().addAll(nameLabel, addressLabel);
            card.getChildren().add(details);
            
            listingsContainer.getChildren().add(card);
        }
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
