package controllers;

import app.MainApp;
import dao.BookingDAO;
import dao.PropertyDAO;
import dao.RoomDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import models.Booking;
import models.Property;
import models.Room;
import models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyBookingsController {

    @FXML private TableView<MyBookingRow> bookingsTable;
    @FXML private TableColumn<MyBookingRow, String> propertyColumn;
    @FXML private TableColumn<MyBookingRow, String> roomColumn;
    @FXML private TableColumn<MyBookingRow, String> priceColumn;
    @FXML private TableColumn<MyBookingRow, String> dateColumn;
    @FXML private TableColumn<MyBookingRow, String> statusColumn;
    @FXML private TableColumn<MyBookingRow, String> actionsColumn;
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

        propertyColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().propertyLabel));
        roomColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().roomLabel));
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().priceLabel));
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

        // Add the actions column cell factory logic
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("Cancel Request");

            {
                cancelBtn.getStyleClass().add("danger-button");
                cancelBtn.setOnAction(event -> {
                    MyBookingRow row = getTableView().getItems().get(getIndex());
                    if (row != null) {
                        bookingDAO.updateStatus(row.bookingId, "CANCELLED");
                        loadBookings(); 
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    MyBookingRow row = getTableView().getItems().get(getIndex());
                    if ("PENDING".equals(row.status)) {
                        setGraphic(cancelBtn);
                    } else {
                        setGraphic(new Label("-"));
                    }
                }
            }
        });

        loadBookings();
    }

    private String getStatusBadgeClass(String status) {
        if (status == null) return "badge-pending";
        return switch (status.toUpperCase()) {
            case "APPROVED" -> "badge-approved";
            case "REJECTED" -> "badge-rejected";
            default -> "badge-pending";
        };
    }

    private void loadBookings() {
        User user = SessionManager.getInstance().getCurrentUser();
        List<Booking> bookings = bookingDAO.getBookingsByTenantId(user.getId());

        List<Property> properties = propertyDAO.getAllProperties();
        Map<Integer, String> propertyNameById = new HashMap<>();
        for (Property p : properties) {
            propertyNameById.put(p.getId(), p.getName());
        }

        List<MyBookingRow> rows = new ArrayList<>();
        for (Booking booking : bookings) {
            String roomLabel = "Room #" + booking.getRoomId();
            String priceLabel = "—";
            String propertyLabel = "Property";

            // Requires RoomDAO.getRoomById(int) (backend task)
            // Room room = roomDAO.getRoomById(booking.getRoomId());
            // if (room != null) {
            //     roomLabel = room.getRoomNumber();
            //     priceLabel = "₱" + String.format("%.2f", room.getPrice());
            //     propertyLabel = propertyNameById.getOrDefault(
            //             room.getPropertyId(),
            //             "Property #" + room.getPropertyId()
            //     );
            // }

            rows.add(new MyBookingRow(
                    booking.getId(),
                    propertyLabel,
                    roomLabel,
                    priceLabel,
                    booking.getBookingDate(),
                    booking.getStatus()
            ));
        }

        bookingsTable.setItems(FXCollections.observableArrayList(rows));
        bookingCountLabel.setText(rows.size() + " bookings");
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

    private static class MyBookingRow {
        private final int bookingId;
        private final String propertyLabel;
        private final String roomLabel;
        private final String priceLabel;
        private final String bookingDate;
        private final String status;

        private MyBookingRow(int bookingId,
                             String propertyLabel,
                             String roomLabel,
                             String priceLabel,
                             String bookingDate,
                             String status) {
            this.bookingId = bookingId;
            this.propertyLabel = propertyLabel;
            this.roomLabel = roomLabel;
            this.priceLabel = priceLabel;
            this.bookingDate = bookingDate;
            this.status = status;
        }
    }
}