package controllers;

import app.MainApp;
import dao.RoomDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.Property;
import models.Room;

import java.util.List;

public class ManageRoomsController {

    // ── Navbar / header ──────────────────────────────────────────────────────
    @FXML private Label navPropertyName;
    @FXML private Label propertyNameLabel;
    @FXML private Label roomCountLabel;

    // ── Hidden TableView — kept so fx:id bindings resolve without error ───────
    @FXML private TableView<Room>          roomsTable;
    @FXML private TableColumn<Room,String> roomNumberColumn;
    @FXML private TableColumn<Room,String> capacityColumn;
    @FXML private TableColumn<Room,String> priceColumn;
    @FXML private TableColumn<Room,String> statusColumn;
    @FXML private TableColumn<Room,String> actionsColumn;

    // ── Card list (rendered dynamically) ────────────────────────────────────
    // Injected from the parent ScrollPane's VBox — we'll locate it at runtime.
    private VBox roomCardsContainer;

    // ── Inline add/edit form ─────────────────────────────────────────────────
    @FXML private VBox     roomFormCard;
    @FXML private Label    formTitleLabel;
    @FXML private TextField roomNumberField;
    @FXML private TextField capacityField;
    @FXML private TextField priceField;
    @FXML private CheckBox  availableCheckBox;
    @FXML private Label    formErrorLabel;
    @FXML private Button   formSubmitButton;

    // ── Misc ─────────────────────────────────────────────────────────────────
    @FXML private Label messageLabel;

    private final RoomDAO roomDAO = new RoomDAO();
    private Property currentProperty;
    private Room     editingRoom;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            MainApp.switchTo("views/Login.fxml");
            return;
        }

        // Hide the legacy table; we never populate it visually
        if (roomsTable != null) {
            roomsTable.setVisible(false);
            roomsTable.setManaged(false);
        }

        // Wire up minimal cell-value factories so no NPE is thrown internally
        if (roomNumberColumn != null)
            roomNumberColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRoomNumber()));
        if (capacityColumn != null)
            capacityColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCapacity() + " person(s)"));
        if (priceColumn != null)
            priceColumn.setCellValueFactory(d -> new SimpleStringProperty("₱" + String.format("%.2f", d.getValue().getPrice())));
        if (statusColumn != null)
            statusColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isAvailable() ? "Available" : "Occupied"));

        // Build the cards VBox and insert it right before the form card in the parent
        roomCardsContainer = new VBox(16);
        VBox parent = (VBox) roomFormCard.getParent();
        int formIdx = parent.getChildren().indexOf(roomFormCard);
        parent.getChildren().add(formIdx, roomCardsContainer);

        // Hide form until "Add Room" or "Edit" is clicked
        roomFormCard.setVisible(false);
        roomFormCard.setManaged(false);
    }

    // ── Called after setProperty() from ManageListingsController ─────────────
    public void setProperty(Property property) {
        this.currentProperty = property;
        navPropertyName.setText("Rooms — " + property.getName());
        propertyNameLabel.setText(property.getName() + " — Rooms");
        loadRooms();
    }

    // ── Load & render room cards ──────────────────────────────────────────────
    private void loadRooms() {
        if (roomCardsContainer == null || currentProperty == null) return;
        roomCardsContainer.getChildren().clear();

        List<Room> rooms = roomDAO.getRoomsByPropertyId(currentProperty.getId());
        roomCountLabel.setText(rooms.size() + " room(s)");

        if (rooms.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-padding: 40;");
            Label icon  = new Label("⊞");
            icon.setStyle("-fx-font-size: 32px; -fx-text-fill: #CBD5E1;");
            Label title = new Label("No rooms added yet");
            title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
            Label sub   = new Label("Click '+ Add Room' above to add your first room.");
            sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");
            empty.getChildren().addAll(icon, title, sub);
            roomCardsContainer.getChildren().add(empty);
            return;
        }

        for (Room room : rooms) {
            roomCardsContainer.getChildren().add(createRoomCard(room));
        }

        // Also keep the hidden table in sync (no visual effect, but keeps model consistent)
        roomsTable.setItems(FXCollections.observableArrayList(rooms));
    }

    // ── Card builder ──────────────────────────────────────────────────────────
    private HBox createRoomCard(Room room) {
        HBox card = new HBox(16);
        card.getStyleClass().add("card-flat");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon box — colour changes with availability
        Label icon = new Label("⊞");
        if (room.isAvailable()) {
            icon.setStyle("-fx-font-size: 26px; -fx-text-fill: #16A34A;" +
                    " -fx-background-color: #D1FAE5; -fx-background-radius: 8; -fx-padding: 10 16;");
        } else {
            icon.setStyle("-fx-font-size: 26px; -fx-text-fill: #DC2626;" +
                    " -fx-background-color: #FEE2E2; -fx-background-radius: 8; -fx-padding: 10 16;");
        }

        // Details column
        VBox details = new VBox(4);

        // Top row: room number + status badge
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label roomLbl = new Label("Room " + room.getRoomNumber());
        roomLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label statusBadge = new Label(room.isAvailable() ? "Available" : "Occupied");
        statusBadge.getStyleClass().add(room.isAvailable() ? "badge-approved" : "badge-rejected");
        topRow.getChildren().addAll(roomLbl, statusBadge);

        // Secondary info
        Label infoLbl = new Label(
                "👥 Capacity: " + room.getCapacity() + " person(s)" +
                        "    •    💰 ₱" + String.format("%,.2f", room.getPrice()) + " / month"
        );
        infoLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        details.getChildren().addAll(topRow, infoLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Action buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);

        Button editBtn = new Button("✎  Edit");
        editBtn.getStyleClass().add("primary-button");
        editBtn.setStyle("-fx-padding: 8 16; -fx-font-size: 12px;");
        editBtn.setOnAction(e -> startEdit(room));

        Button deleteBtn = new Button("✕  Delete");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setStyle("-fx-padding: 8 16; -fx-font-size: 12px;");
        deleteBtn.setOnAction(e -> handleDelete(room));

        actions.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(icon, details, spacer, actions);
        return card;
    }

    // ── Form visibility helpers ───────────────────────────────────────────────
    private void showForm() {
        roomFormCard.setVisible(true);
        roomFormCard.setManaged(true);
    }

    private void hideForm() {
        roomFormCard.setVisible(false);
        roomFormCard.setManaged(false);
    }

    // ── FXML actions ─────────────────────────────────────────────────────────
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
        if (messageLabel != null) messageLabel.setText("");
        showForm();
    }

    @FXML
    public void handleSubmitRoom() {
        if (currentProperty == null) {
            formErrorLabel.setText("No property selected.");
            return;
        }

        String roomNumber   = roomNumberField.getText().trim();
        String capacityText = capacityField.getText().trim();
        String priceText    = priceField.getText().trim();

        if (roomNumber.isEmpty() || capacityText.isEmpty() || priceText.isEmpty()) {
            formErrorLabel.setText("Please fill in all required fields.");
            return;
        }

        int    capacity;
        double price;
        try {
            capacity = Integer.parseInt(capacityText);
            price    = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            formErrorLabel.setText("Capacity and price must be numeric.");
            return;
        }

        boolean success;
        if (editingRoom == null) {
            Room newRoom = new Room(currentProperty.getId(), roomNumber, capacity, price,
                    availableCheckBox.isSelected());
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

        hideForm();
        loadRooms();
    }

    @FXML
    public void handleCancelForm() {
        formErrorLabel.setText("");
        hideForm();
    }

    // ── Triggered by Edit button on a card ───────────────────────────────────
    private void startEdit(Room room) {
        editingRoom = room;
        formTitleLabel.setText("Edit Room");
        formSubmitButton.setText("Save Changes");
        roomNumberField.setText(room.getRoomNumber());
        capacityField.setText(String.valueOf(room.getCapacity()));
        priceField.setText(String.valueOf(room.getPrice()));
        availableCheckBox.setSelected(room.isAvailable());
        formErrorLabel.setText("");
        if (messageLabel != null) messageLabel.setText("");
        showForm();
    }

    // ── Triggered by Delete button on a card ─────────────────────────────────
    private void handleDelete(Room room) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Room");
        confirm.setHeaderText("Delete room " + room.getRoomNumber() + "?");
        confirm.setContentText("This cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = roomDAO.deleteRoom(room.getId());
                if (!success) {
                    if (messageLabel != null)
                        messageLabel.setText("Failed to delete room.");
                    return;
                }
                if (messageLabel != null) messageLabel.setText("");
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