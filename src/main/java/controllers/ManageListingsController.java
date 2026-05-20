package controllers;

import app.MainApp;
import dao.PropertyDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.Property;
import models.User;

import java.io.IOException;
import java.util.List;

public class ManageListingsController {

    @FXML private VBox listingsContainer;
    @FXML private Label listingCountLabel;
    @FXML private Label messageLabel;

    private final PropertyDAO propertyDAO = new PropertyDAO();

    @FXML
    public void initialize() {
        loadListings();
    }

    private void loadListings() {
        if(listingsContainer == null) return;
        listingsContainer.getChildren().clear();
        User user = SessionManager.getInstance().getCurrentUser();
        List<Property> properties = propertyDAO.getPropertiesByLandlordId(user.getId());
        
        listingCountLabel.setText(properties.size() + " properties listed");

        if (properties.isEmpty()) {
            VBox emptyState = new VBox(10);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setStyle("-fx-padding: 40;");
            Label emptyIcon = new Label("⌂");
            emptyIcon.setStyle("-fx-font-size: 32px; -fx-text-fill: #94A3B8;");
            Label emptyTitle = new Label("No properties listed yet");
            emptyTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #1E293B; -fx-font-weight: bold;");
            Label emptySub = new Label("Click '+ Add New Property' to list your first property.");
            emptySub.setStyle("-fx-text-fill: #64748B;");
            emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptySub);
            listingsContainer.getChildren().add(emptyState);
            return;
        }

        for (Property p : properties) {
            listingsContainer.getChildren().add(createPropertyCard(p));
        }
    }

    private HBox createPropertyCard(Property p) {
        HBox card = new HBox(16);
        card.getStyleClass().add("card-flat");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon
        Label icon = new Label("⌂");
        icon.setStyle("-fx-font-size: 28px; -fx-text-fill: #3B82F6; -fx-background-color: #DBEAFE; -fx-background-radius: 8; -fx-padding: 10 16;");

        // Details
        VBox details = new VBox(4);
        Label nameLabel = new Label(p.getName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label addrLabel = new Label(p.getAddress());
        addrLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px;");
        Label descLabel = new Label(p.getDescription());
        descLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");
        details.getChildren().addAll(nameLabel, addrLabel, descLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Actions
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        
        Button editBtn = new Button("✎ Edit");
        editBtn.getStyleClass().add("primary-button");
        editBtn.setStyle("-fx-padding: 8 16;");
        editBtn.setOnAction(e -> navigateToEdit(p));

        Button roomsBtn = new Button("⊞ Rooms");
        roomsBtn.getStyleClass().add("secondary-button");
        roomsBtn.setStyle("-fx-padding: 8 16;");
        roomsBtn.setOnAction(e -> navigateToRooms(p));

        Button deleteBtn = new Button("✕ Delete");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setStyle("-fx-padding: 8 16;");
        deleteBtn.setOnAction(e -> handleDelete(p));

        actions.getChildren().addAll(roomsBtn, editBtn, deleteBtn);

        card.getChildren().addAll(icon, details, spacer, actions);
        return card;
    }

    private void navigateToEdit(Property property) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/views/AddEditProperty.fxml"));
            Parent root = loader.load();
            AddEditPropertyController controller = loader.getController();
            controller.setProperty(property);
            MainApp.navigateTo(root);
        } catch (IOException e) {
            messageLabel.setText("Failed to open edit screen.");
            e.printStackTrace();
        }
    }

    private void navigateToRooms(Property property) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/views/ManageRooms.fxml"));
            Parent root = loader.load();
            ManageRoomsController controller = loader.getController();
            controller.setProperty(property);
            MainApp.navigateTo(root);
        } catch (IOException e) {
            messageLabel.setText("Failed to open room management screen.");
            e.printStackTrace();
        }
    }

    private void handleDelete(Property property) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Property");
        confirm.setHeaderText("Delete \"" + property.getName() + "\"?");
        confirm.setContentText("This will also delete all rooms under this property. This cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = propertyDAO.deleteProperty(property.getId());
                if (success) {
                    messageLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                    messageLabel.setText("Property deleted successfully.");
                    loadListings();
                } else {
                    messageLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    messageLabel.setText("Failed to delete property.");
                }
            }
        });
    }

    @FXML
    public void handleAddProperty() {
        MainApp.switchTo("views/AddEditProperty.fxml");
    }

    @FXML
    public void handleBackToDashboard() {
        MainApp.switchTo("views/LandlordDashboard.fxml");
    }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.switchTo("views/Login.fxml");
    }
}
