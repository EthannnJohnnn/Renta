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

public class PropertyDetailController {

    @FXML private Label navPropertyName;
    @FXML private Label propertyNameLabel;
    @FXML private Label propertyAddressLabel;
    @FXML private Label propertyDescriptionLabel;
    @FXML private TableView<Room> roomsTable;
    @FXML private TableColumn<Room, String> roomNumberColumn;
    @FXML private TableColumn<Room, String> capacityColumn;
    @FXML private TableColumn<Room, String> priceColumn;
    @FXML private TableColumn<Room, String> availableColumn;
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
        roomNumberColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRoomNumber()));
        capacityColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCapacity() + " person(s)"));
        priceColumn.setCellValueFactory(data ->
                new SimpleStringProperty("₱" + String.format("%.2f", data.getValue().getPrice())));
        availableColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isAvailable() ? "Available" : "Occupied"));

        List<Room> rooms = roomDAO.getRoomsByPropertyId(currentProperty.getId());
        roomsTable.setItems(FXCollections.observableArrayList(rooms));

        roomsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            bookButton.setDisable(selected == null || !selected.isAvailable());
        });
    }

    @FXML
    public void handleBook() {
        Room selected = roomsTable.getSelectionModel().getSelectedItem();
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