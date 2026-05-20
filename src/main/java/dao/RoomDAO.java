package dao;

import models.Room;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO {

    // CREATE
    public boolean addRoom(Room room) {
        String sql = "INSERT INTO rooms (property_id, room_number, capacity, price, is_available) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, room.getPropertyId());
            pstmt.setString(2, room.getRoomNumber());
            pstmt.setInt(3, room.getCapacity());
            pstmt.setDouble(4, room.getPrice());
            pstmt.setBoolean(5, room.isAvailable()); // 1 for true, 0 for false handled by SQLite driver

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding room: " + e.getMessage());
            return false;
        }
    }

    // READ: Get all rooms that belong to a specific Property
    public List<Room> getRoomsByPropertyId(int propertyId) {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM rooms WHERE property_id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, propertyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rooms.add(extractRoomFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching rooms: " + e.getMessage());
        }
        return rooms;
    }

    // UPDATE: Fix typos, change rent price, or mark as unavailable
    public boolean updateRoom(Room room) {
        String sql = "UPDATE rooms SET room_number = ?, capacity = ?, price = ?, is_available = ? WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, room.getRoomNumber());
            pstmt.setInt(2, room.getCapacity());
            pstmt.setDouble(3, room.getPrice());
            pstmt.setBoolean(4, room.isAvailable());
            pstmt.setInt(5, room.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating room: " + e.getMessage());
            return false;
        }
    }

    // DELETE
    public boolean deleteRoom(int roomId) {
        String sql = "DELETE FROM rooms WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting room: " + e.getMessage());
            return false;
        }
    }

    // READ: Get a single specific room by its ID (Required for Booking Data tables)
    public Room getRoomById(int roomId) {
        String sql = "SELECT * FROM rooms WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Uses your existing helper method to return the clean Java object!
                    return extractRoomFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching room by ID: " + e.getMessage());
        }
        return null; // Return null if the room doesn't exist
    }

    // HELPER: Keeps code clean by converting SQL rows to Java Objects
    private Room extractRoomFromResultSet(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setId(rs.getInt("id"));
        room.setPropertyId(rs.getInt("property_id"));
        room.setRoomNumber(rs.getString("room_number"));
        room.setCapacity(rs.getInt("capacity"));
        room.setPrice(rs.getDouble("price"));
        room.setAvailable(rs.getBoolean("is_available"));
        return room;
    }
}