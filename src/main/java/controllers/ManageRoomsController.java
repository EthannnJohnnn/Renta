package controllers;

import app.MainApp;
import dao.RoomDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.Property;
import models.Room;

import java.util.List;

public class ManageRoomsController {

    @FXML private Label navPropertyName;
    @FXML private Label propertyNameLabel;
    @FXML private Label roomCountLabel;
    @FXML private TableView<Room> roomsTable;
    @FXML private TableColumn<Room, String> roomNumberColumn;
    @FXML private TableColumn<Room, String> capacityColumn;
    @FXML private TableColumn<Room, String> priceColumn;
    @FXML private TableColumn<Room, String> statusColumn;
    @FXML private TableColumn<Room, String> actionsColumn;
    @FXML private Label messageLabel;
    @FXML private VBox roomFormCard;
    @FXML private Label formTitleLabel;
    @FXML private TextField roomNumberField;
    @FXML private TextField capacityField;
    @FXML private TextField priceField;
    @FXML private CheckBox availableCheckBox;
    @FXML private Label formErrorLabel;
    @FXML private Button formSubmitButton;

    private final RoomDAO roomDAO = new RoomDAO();
    private Property currentProperty;
    private Room editingRoom;

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            MainApp.switchTo("views/Login.fxml");
            return;
        }

        roomFormCard.setVisible(false);
        roomFormCard.setManaged(false);

        roomNumberColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRoomNumber()));
        capacityColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCapacity() + " person(s)"));
        priceColumn.setCellValueFactory(data ->
                new SimpleStringProperty("₱" + String.format("%.2f", data.getValue().getPrice())));
        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isAvailable() ? "Available" : "Occupied"));

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button("✎  Edit");
            private final Button deleteBtn = new Button("✕  Delete");
            private final HBox   box       = new HBox(8, editBtn, deleteBtn);

            {
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                box.setPadding(new javafx.geometry.Insets(4, 0, 4, 0));

                editBtn.setStyle(
                        "-fx-background-color: #EFF6FF;" +
                                "-fx-text-fill: #2563EB;" +
                                "-fx-font-size: 12px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-color: #BFDBFE;" +
                                "-fx-border-radius: 8;" +
                                "-fx-border-width: 1.5;" +
                                "-fx-padding: 5 14 5 14;" +
                                "-fx-cursor: hand;"
                );
                deleteBtn.setStyle(
                        "-fx-background-color: #FEF2F2;" +
                                "-fx-text-fill: #DC2626;" +
                                "-fx-font-size: 12px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-color: #FECACA;" +
                                "-fx-border-radius: 8;" +
                                "-fx-border-width: 1.5;" +
                                "-fx-padding: 5 14 5 14;" +
                                "-fx-cursor: hand;"
                );

                editBtn.setOnAction(e -> {
                    Room selected = getTableView().getItems().get(getIndex());
                    ManageRoomsController.this.startEdit(selected);
                });
                deleteBtn.setOnAction(e -> {
                    Room selected = getTableView().getItems().get(getIndex());
                    handleDelete(selected);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
                setStyle("-fx-alignment: CENTER-LEFT;");
            }
        });

        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label badge = new Label(status);
                if ("Available".equals(status)) {
                    badge.setStyle(
                            "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;" +
                                    "-fx-font-size: 11px; -fx-font-weight: bold;" +
                                    "-fx-padding: 3 10 3 10; -fx-background-radius: 99;"
                    );
                } else {
                    badge.setStyle(
                            "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;" +
                                    "-fx-font-size: 11px; -fx-font-weight: bold;" +
                                    "-fx-padding: 3 10 3 10; -fx-background-radius: 99;"
                    );
                }
                setGraphic(badge);
                setText(null);
                setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }
        });

    }

    public void setProperty(Property property) {
        this.currentProperty = property;
        navPropertyName.setText("Rooms — " + property.getName());
        propertyNameLabel.setText(property.getName() + " — Rooms");
        loadRooms();
    }

    private void loadRooms() {
        if (currentProperty == null) {
            return;
        }
        List<Room> rooms = roomDAO.getRoomsByPropertyId(currentProperty.getId());
        roomsTable.setItems(FXCollections.observableArrayList(rooms));
        roomCountLabel.setText(rooms.size() + " rooms");
    }

    @FXML
    public void handleAddRoom() {
        editingRoom = null;
        formTitleLabel.setText("Add New Room");
        formSubmitButton.setText("Add Room");
        roomNumberField.clear();
        capacityField.clear();
        priceField.clear();
        availableCheckBox.setSelected(true);
        formErrorLabel.setText("");
        messageLabel.setText("");

        roomFormCard.setVisible(true);
        roomFormCard.setManaged(true);
    }

    @FXML
    public void handleSubmitRoom() {
        if (currentProperty == null) {
            formErrorLabel.setText("No property selected.");
            return;
        }

        String roomNumber = roomNumberField.getText().trim();
        String capacityText = capacityField.getText().trim();
        String priceText = priceField.getText().trim();

        if (roomNumber.isEmpty() || capacityText.isEmpty() || priceText.isEmpty()) {
            formErrorLabel.setText("Please fill in all required fields.");
            return;
        }

        int capacity;
        double price;
        try {
            capacity = Integer.parseInt(capacityText);
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            formErrorLabel.setText("Capacity and price must be numeric.");
            return;
        }

        boolean success;
        if (editingRoom == null) {
            Room newRoom = new Room(
                    currentProperty.getId(),
                    roomNumber,
                    capacity,
                    price,
                    availableCheckBox.isSelected()
            );
            success = roomDAO.addRoom(newRoom);
        } else {
            editingRoom.setRoomNumber(roomNumber);
            editingRoom.setCapacity(capacity);
            editingRoom.setPrice(price);
            editingRoom.setAvailable(availableCheckBox.isSelected());
            success = roomDAO.updateRoom(editingRoom);
        }

        if (!success) {
            formErrorLabel.setText("Failed to save room. Please try again.");
            return;
        }

        formErrorLabel.setText("");
        messageLabel.setText("");
        roomFormCard.setVisible(false);
        roomFormCard.setManaged(false);
        loadRooms();
    }

    @FXML
    public void handleCancelForm() {
        formErrorLabel.setText("");
        roomFormCard.setVisible(false);
        roomFormCard.setManaged(false);
    }

    private void startEdit(Room room) {
        editingRoom = room;
        formTitleLabel.setText("Edit Room");
        formSubmitButton.setText("Save Changes");
        roomNumberField.setText(room.getRoomNumber());
        capacityField.setText(String.valueOf(room.getCapacity()));
        priceField.setText(String.valueOf(room.getPrice()));
        availableCheckBox.setSelected(room.isAvailable());
        formErrorLabel.setText("");
        messageLabel.setText("");

        roomFormCard.setVisible(true);
        roomFormCard.setManaged(true);
    }

    private void handleDelete(Room room) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Room");
        confirm.setHeaderText("Delete room " + room.getRoomNumber() + "?");
        confirm.setContentText("This cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = roomDAO.deleteRoom(room.getId());
                if (!success) {
                    messageLabel.setText("Failed to delete room.");
                    return;
                }
                messageLabel.setText("");
                loadRooms();
            }
        });
    }

    @FXML
    public void handleBack() {
        MainApp.switchTo("views/ManageListings.fxml");
    }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        MainApp.switchTo("views/Login.fxml");
    }
}