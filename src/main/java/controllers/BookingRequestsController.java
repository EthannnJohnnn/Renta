package controllers;

import app.MainApp;
import dao.BookingDAO;
import dao.PropertyDAO;
import dao.RoomDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import models.Booking;
import models.Property;
import models.Room;
import models.User;

import java.util.ArrayList;
import java.util.List;

public class BookingRequestsController {

    @FXML private TableView<BookingRequestRow> bookingsTable;
    @FXML private TableColumn<BookingRequestRow, String> tenantColumn;
    @FXML private TableColumn<BookingRequestRow, String> propertyColumn;
    @FXML private TableColumn<BookingRequestRow, String> roomColumn;
    @FXML private TableColumn<BookingRequestRow, String> dateColumn;
    @FXML private TableColumn<BookingRequestRow, String> statusColumn;
    @FXML private TableColumn<BookingRequestRow, String> actionsColumn;
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

        tenantColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().tenantLabel));
        propertyColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().propertyLabel));
        roomColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().roomLabel));
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().bookingDate));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status));

        // ✅ Status badge renderer
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(status.toUpperCase());
                    badge.getStyleClass().add(getStatusBadgeClass(status));
                    setGraphic(badge);
                    setText(null);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button approveBtn = new Button("Approve");
            private final Button rejectBtn = new Button("Reject");
            private final Button completeBtn = new Button("🏁 End Tenancy");
            private final HBox pendingBox = new HBox(8, approveBtn, rejectBtn);

            {
                approveBtn.getStyleClass().add("success-button");
                rejectBtn.getStyleClass().add("danger-button");

                approveBtn.setOnAction(e -> {
                    BookingRequestRow row = getTableView().getItems().get(getIndex());
                    updateStatus(row, "APPROVED");
                });

                rejectBtn.setOnAction(e -> {
                    BookingRequestRow row = getTableView().getItems().get(getIndex());
                    updateStatus(row, "REJECTED");
                });

                completeBtn.setOnAction(e -> {
                    BookingRequestRow row = getTableView().getItems().get(getIndex());
                    updateStatus(row, "COMPLETED");
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    BookingRequestRow row = getTableView().getItems().get(getIndex());
                    if ("PENDING".equals(row.status)) {
                        setGraphic(pendingBox);
                    } else if ("APPROVED".equals(row.status)) {
                        setGraphic(completeBtn);
                    } else {
                        setGraphic(new Label("-"));
                    }
                }
            }
        });

        loadRequests();
    }

    private String getStatusBadgeClass(String status) {
        if (status == null) return "badge-pending";
        return switch (status.toUpperCase()) {
            case "APPROVED" -> "badge-approved";
            case "REJECTED" -> "badge-rejected";
            default -> "badge-pending";
        };
    }

    private void loadRequests() {
        User user = SessionManager.getInstance().getCurrentUser();
        List<Property> properties = propertyDAO.getPropertiesByLandlordId(user.getId());
        List<BookingRequestRow> rows = new ArrayList<>();

        for (Property property : properties) {
            List<Booking> bookings = bookingDAO.getBookingsByPropertyId(property.getId());
            for (Booking booking : bookings) {
                String tenantLabel = "Tenant #" + booking.getTenantId();
                String roomLabel = "Room #" + booking.getRoomId();

                // Requires RoomDAO.getRoomById(int) (backend task)
                // Room room = roomDAO.getRoomById(booking.getRoomId());
                // if (room != null) {
                //     roomLabel = room.getRoomNumber();
                // }

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

        bookingsTable.setItems(FXCollections.observableArrayList(rows));

        long pendingCount = rows.stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.status))
                .count();
        requestCountLabel.setText(pendingCount + " pending requests");
    }

    private void updateStatus(BookingRequestRow row, String status) {
        boolean success = bookingDAO.updateStatus(row.booking.getId(), status);
        if (!success) {
            messageLabel.setText("Failed to update booking status.");
            return;
        }

        // NEW: Auto-restore room availability if the landlord rejects the booking
        if ("REJECTED".equals(status)) {
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