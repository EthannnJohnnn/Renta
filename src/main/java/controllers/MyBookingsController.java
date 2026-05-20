package controllers;

import app.MainApp;
import dao.BookingDAO;
import dao.PropertyDAO;
import dao.RoomDAO;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import models.Booking;
import models.Property;
import models.Room;
import models.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyBookingsController {

    @FXML private VBox bookingsListContainer;
    @FXML private Label bookingCountLabel;

    private final BookingDAO bookingDAO = new BookingDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final PropertyDAO propertyDAO = new PropertyDAO();

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            MainApp.switchTo("views/Login.fxml");
            return;
        }

        loadBookings();
    }

    private String getStatusBadgeClass(String status) {
        if (status == null) return "badge-pending";
        return switch (status.toUpperCase()) {
            case "APPROVED"  -> "badge-approved";
            case "REJECTED"  -> "badge-rejected";
            case "CANCELLED" -> "badge-cancelled";
            case "COMPLETED" -> "badge-completed";
            default          -> "badge-pending";
        };
    }

    private void loadBookings() {
        bookingsListContainer.getChildren().clear();
        User user = SessionManager.getInstance().getCurrentUser();
        List<Booking> bookings = bookingDAO.getBookingsByTenantId(user.getId());

        List<Property> properties = propertyDAO.getAllProperties();
        Map<Integer, String> propertyNameByRoomPropertyId = new HashMap<>();
        for (Property p : properties) {
            propertyNameByRoomPropertyId.put(p.getId(), p.getName());
        }

        bookingCountLabel.setText(bookings.size() + " booking(s)");

        if (bookings.isEmpty()) {
            VBox emptyState = new VBox(10);
            emptyState.setAlignment(Pos.CENTER);
            Label emptyTitle = new Label("No bookings yet");
            emptyTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #1E293B; -fx-font-weight: bold;");
            Label emptySub = new Label("Browse properties and book a room to get started.");
            emptySub.setStyle("-fx-text-fill: #64748B;");
            emptyState.getChildren().addAll(emptyTitle, emptySub);
            bookingsListContainer.getChildren().add(emptyState);
            return;
        }

        for (Booking booking : bookings) {
            String roomLabel    = "Room #" + booking.getRoomId();
            String priceLabel   = "—";
            String propertyLabel = "Property";

            Room room = roomDAO.getRoomById(booking.getRoomId());
            if (room != null) {
                roomLabel     = room.getRoomNumber();
                priceLabel    = "₱" + String.format("%.2f", room.getPrice());
                propertyLabel = propertyNameByRoomPropertyId.getOrDefault(
                        room.getPropertyId(),
                        "Property #" + room.getPropertyId()
                );
            }

            bookingsListContainer.getChildren().add(createBookingCard(booking, propertyLabel, roomLabel, priceLabel));
        }
    }

    private HBox createBookingCard(Booking booking, String propertyName, String roomName, String price) {
        HBox card = new HBox(16);
        card.getStyleClass().add("card-flat");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox leftDetails = new VBox(4);
        Label propLabel = new Label(propertyName);
        propLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label roomSubLabel = new Label("Room " + roomName + "  •  " + price + " / Month");
        roomSubLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
        Label dateLabel = new Label("Booked on: " + booking.getBookingDate());
        dateLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        leftDetails.getChildren().addAll(propLabel, roomSubLabel, dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox rightDetails = new VBox(8);
        rightDetails.setAlignment(Pos.CENTER_RIGHT);

        Label statusBadge = new Label(booking.getStatus().toUpperCase());
        statusBadge.getStyleClass().add(getStatusBadgeClass(booking.getStatus()));
        
        rightDetails.getChildren().add(statusBadge);

        if ("PENDING".equals(booking.getStatus())) {
            Button cancelBtn = new Button("Cancel Request");
            cancelBtn.getStyleClass().add("danger-button");
            cancelBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
            cancelBtn.setOnAction(e -> {
                bookingDAO.updateStatus(booking.getId(), "CANCELLED");
                loadBookings();
            });
            rightDetails.getChildren().add(cancelBtn);
        }

        card.getChildren().addAll(leftDetails, spacer, rightDetails);
        return card;
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
