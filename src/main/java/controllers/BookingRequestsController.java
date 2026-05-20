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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.Booking;
import models.Property;
import models.Room;
import models.User;

import java.util.ArrayList;
import java.util.List;

public class BookingRequestsController {

    @FXML private VBox bookingsContainer;
    @FXML private Label requestCountLabel;
    @FXML private Label messageLabel;

    private final BookingDAO bookingDAO = new BookingDAO();
    private final PropertyDAO propertyDAO = new PropertyDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            MainApp.switchTo("views/Login.fxml");
            return;
        }
        loadRequests();
    }

    private void loadRequests() {
        if(bookingsContainer == null) return;
        bookingsContainer.getChildren().clear();

        User user = SessionManager.getInstance().getCurrentUser();
        List<Property> properties = propertyDAO.getPropertiesByLandlordId(user.getId());
        List<BookingRequestRow> rows = new ArrayList<>();

        for (Property property : properties) {
            List<Booking> bookings = bookingDAO.getBookingsByPropertyId(property.getId());
            for (Booking booking : bookings) {
                String tenantLabel = "Tenant #" + booking.getTenantId();
                String roomLabel = "Room #" + booking.getRoomId();

                rows.add(new BookingRequestRow(
                        booking,
                        tenantLabel,
                        property.getName(),
                        roomLabel,
                        booking.getBookingDate(),
                        booking.getStatus()
                ));
            }
        }

        long pendingCount = rows.stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.status))
                .count();
        requestCountLabel.setText(pendingCount + " pending request(s)");

        if (rows.isEmpty()) {
            VBox emptyState = new VBox(10);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setStyle("-fx-padding: 40;");
            Label emptyIcon = new Label("✉");
            emptyIcon.setStyle("-fx-font-size: 32px; -fx-text-fill: #94A3B8;");
            Label emptyTitle = new Label("No booking requests yet");
            emptyTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #1E293B; -fx-font-weight: bold;");
            Label emptySub = new Label("When tenants submit bookings for your properties, they will appear here.");
            emptySub.setStyle("-fx-text-fill: #64748B;");
            emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptySub);
            bookingsContainer.getChildren().add(emptyState);
            return;
        }

        for (BookingRequestRow row : rows) {
            bookingsContainer.getChildren().add(createRequestCard(row));
        }
    }

    private HBox createRequestCard(BookingRequestRow row) {
        HBox card = new HBox(16);
        card.getStyleClass().add("card-flat");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon based on status
        Label icon = new Label("✉");
        icon.setStyle("-fx-font-size: 28px; -fx-text-fill: #D97706; -fx-background-color: #FEF3C7; -fx-background-radius: 8; -fx-padding: 10 16;");
        if ("APPROVED".equals(row.status)) {
            icon.setStyle("-fx-font-size: 28px; -fx-text-fill: #16A34A; -fx-background-color: #DCFCE7; -fx-background-radius: 8; -fx-padding: 10 16;");
        } else if ("REJECTED".equals(row.status)) {
            icon.setStyle("-fx-font-size: 28px; -fx-text-fill: #DC2626; -fx-background-color: #FEE2E2; -fx-background-radius: 8; -fx-padding: 10 16;");
        }

        // Details
        VBox details = new VBox(4);
        
        HBox topInfo = new HBox(8);
        topInfo.setAlignment(Pos.CENTER_LEFT);
        Label propertyLabel = new Label(row.propertyLabel + " — " + row.roomLabel);
        propertyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        
        Label statusBadge = new Label(row.status.toUpperCase());
        statusBadge.getStyleClass().add(getStatusBadgeClass(row.status));
        topInfo.getChildren().addAll(propertyLabel, statusBadge);

        Label tenantLabel = new Label("Requested by: " + row.tenantLabel);
        tenantLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px;");

        Label dateLabel = new Label("Booked on: " + row.bookingDate);
        dateLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");

        details.getChildren().addAll(topInfo, tenantLabel, dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Actions
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);

        if ("PENDING".equals(row.status)) {
            Button approveBtn = new Button("✓ Approve");
            approveBtn.getStyleClass().add("success-button");
            approveBtn.setStyle("-fx-padding: 8 16;");
            approveBtn.setOnAction(e -> updateStatus(row, "APPROVED"));

            Button rejectBtn = new Button("✕ Reject");
            rejectBtn.getStyleClass().add("danger-button");
            rejectBtn.setStyle("-fx-padding: 8 16;");
            rejectBtn.setOnAction(e -> updateStatus(row, "REJECTED"));

            actions.getChildren().addAll(approveBtn, rejectBtn);
        } else if ("APPROVED".equals(row.status)) {
            Button completeBtn = new Button("⚐ End Tenancy");
            completeBtn.getStyleClass().add("secondary-button");
            completeBtn.setStyle("-fx-padding: 8 16; -fx-text-fill: #0284C7; -fx-border-color: #0284C7;");
            completeBtn.setOnAction(e -> updateStatus(row, "COMPLETED"));
            actions.getChildren().add(completeBtn);
        } else {
             Label resolvedLabel = new Label("Resolved");
             resolvedLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
             actions.getChildren().add(resolvedLabel);
        }

        card.getChildren().addAll(icon, details, spacer, actions);
        return card;
    }

    private String getStatusBadgeClass(String status) {
        if (status == null) return "badge-pending";
        return switch (status.toUpperCase()) {
            case "APPROVED" -> "badge-approved";
            case "REJECTED" -> "badge-rejected";
            default -> "badge-pending";
        };
    }

    private void updateStatus(BookingRequestRow row, String status) {
        boolean success = bookingDAO.updateStatus(row.booking.getId(), status);
        if (!success) {
            messageLabel.setText("Failed to update booking status.");
            return;
        }

        if ("REJECTED".equals(status) || "COMPLETED".equals(status)) {
            bookingDAO.restoreRoomAvailability(row.booking.getRoomId());
        }

        messageLabel.setText("");
        loadRequests();
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

    private static class BookingRequestRow {
        private final Booking booking;
        private final String tenantLabel;
        private final String propertyLabel;
        private final String roomLabel;
        private final String bookingDate;
        private final String status;

        private BookingRequestRow(Booking booking,
                                  String tenantLabel,
                                  String propertyLabel,
                                  String roomLabel,
                                  String bookingDate,
                                  String status) {
            this.booking = booking;
            this.tenantLabel = tenantLabel;
            this.propertyLabel = propertyLabel;
            this.roomLabel = roomLabel;
            this.bookingDate = bookingDate;
            this.status = status;
        }
    }
}
