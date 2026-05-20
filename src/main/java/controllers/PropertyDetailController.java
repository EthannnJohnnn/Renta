package controllers;

import app.MainApp;
import dao.PropertyDAO;
import dao.RoomDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import models.Property;
import models.Room;
import dao.ReviewDAO;
import models.Review;
import models.User;

import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import java.io.IOException;
import java.util.List;

import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

public class PropertyDetailController {

    @FXML private Label navPropertyName;
    @FXML private Label propertyNameLabel;
    @FXML private Label propertyAddressLabel;
    @FXML private Label propertyDescriptionLabel;
    @FXML private ListView<Room> roomsListView;
    @FXML private Button bookButton;
    @FXML private ListView<String> reviewsListView;
    @FXML private ComboBox<Integer> ratingComboBox;
    @FXML private TextArea commentArea;
    @FXML private Label errorLabel;
    @FXML private Label reviewErrorLabel;
    @FXML private StackPane imageContainer;
    @FXML private ImageView propertyImageView;
    @FXML private Label imageErrorLabel;

    private final RoomDAO roomDAO = new RoomDAO();
    private Property currentProperty;

    public void setProperty(Property property) {
        this.currentProperty = property;

        navPropertyName.setText(property.getName());
        propertyNameLabel.setText(property.getName());
        propertyAddressLabel.setText(property.getAddress());

        String url = property.getImageUrl();
        if (url != null && !url.isBlank()) {
            try {
                Image img = new Image(url, true); // background load
                propertyImageView.setImage(img);
                imageContainer.setVisible(true);
                imageContainer.setManaged(true);
            } catch (Exception e) {
                imageErrorLabel.setVisible(true);
            }
        }

        propertyDescriptionLabel.setText(
                property.getDescription() != null ? property.getDescription() : "No description provided.");

        loadRooms();
        loadReviews();
        ratingComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
    }

    private void loadReviews() {
        ReviewDAO reviewDAO = new ReviewDAO();
        List<Review> reviews = reviewDAO.getReviewsByPropertyId(currentProperty.getId());
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Review r : reviews) {
            items.add("⭐ " + r.getRating() + "/5 — " + r.getComment());
        }
        reviewsListView.setItems(items);
    }

    private void loadRooms() {
        List<Room> rooms = roomDAO.getRoomsByPropertyId(currentProperty.getId());
        roomsListView.setItems(FXCollections.observableArrayList(rooms));

        roomsListView.setCellFactory(listView -> new ListCell<Room>() {
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox vbox = new VBox(6);
                    vbox.setPadding(new Insets(12, 16, 12, 16));
                    vbox.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-cursor: hand;");

                    Label roomLbl = new Label("🚪 Room " + room.getRoomNumber());
                    roomLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

                    Label capLbl = new Label("👥 Capacity: " + room.getCapacity() + " person(s)");
                    capLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");

                    Label priceLbl = new Label(String.format("💰 ₱%,.2f / month", room.getPrice()));
                    priceLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #16A34A; -fx-font-weight: bold;");

                    Label statusLbl = new Label(room.isAvailable() ? "✓ Available" : "🚫 Occupied");
                    statusLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 4px 0px; -fx-text-fill: " + (room.isAvailable() ? "#059669;" : "#DC2626;"));

                    vbox.getChildren().addAll(roomLbl, capLbl, priceLbl, statusLbl);
                    setGraphic(vbox);
                    setStyle("-fx-background-color: transparent; -fx-padding: 4px 8px;");
                }
            }
        });

        roomsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            bookButton.setDisable(selected == null || !selected.isAvailable());
        });
    }

    @FXML
    public void handleBook() {
        Room selected = roomsListView.getSelectionModel().getSelectedItem();
        if (selected == null || !selected.isAvailable()) {
            errorLabel.setText("Please select an available room.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/views/BookingForm.fxml"));
            Parent root = loader.load();
            BookingController controller = loader.getController();
            controller.setRoom(selected, currentProperty);
            MainApp.navigateTo(root);
        } catch (IOException e) {
            errorLabel.setText("Failed to open booking form.");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSubmitReview() {
        Integer rating = ratingComboBox.getValue();
        String comment = commentArea.getText().trim();

        if (rating == null || comment.isEmpty()) {
            reviewErrorLabel.setText("Please select a rating and write a comment.");
            return;
        }

        User tenant = SessionManager.getInstance().getCurrentUser();
        ReviewDAO reviewDAO = new ReviewDAO();

        Review review = new Review();
        review.setPropertyId(currentProperty.getId());
        review.setTenantId(tenant.getId());
        review.setRating(rating);
        review.setComment(comment);

        boolean success = reviewDAO.addReview(review);

        if (!success) {
            reviewErrorLabel.setText("Failed to submit review.");
            return;
        }

        reviewErrorLabel.setText("");
        commentArea.clear();
        ratingComboBox.setValue(null);
        loadReviews();
    }

    @FXML
    public void handleBack() {
        MainApp.switchTo("views/PropertyList.fxml");
    }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.switchTo("views/Login.fxml");
    }
}