package dao;

import models.Booking;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

    // CREATE: Add a new booking (Tenant action)
    // NOTE: A "PENDING" status does NOT lock the room.
    // The room is only marked as unavailable when the landlord explicitly APPROVES the booking.
    public boolean addBooking(Booking booking) {
        // ADDED: booking_date to the SQL string and the 4th parameter (?)
        String sql = "INSERT INTO bookings (tenant_id, room_id, status, booking_date) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, booking.getTenantId());
            pstmt.setInt(2, booking.getRoomId());
            pstmt.setString(3, booking.getStatus()); // Usually "PENDING"
            pstmt.setString(4, booking.getBookingDate()); // ADDED: Actually bind the date!

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding booking: " + e.getMessage());
            return false;
        }
    }

    // READ: Find by Tenant (For Tenant Dashboard)
    public List<Booking> getBookingsByTenantId(int tenantId) {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT * FROM bookings WHERE tenant_id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, tenantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bookings.add(extractBookingFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching tenant bookings: " + e.getMessage());
        }
        return bookings;
    }

    // READ: Find by Property (Uses a JOIN to link Rooms to Properties)
    public List<Booking> getBookingsByPropertyId(int propertyId) {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT b.* FROM bookings b JOIN rooms r ON b.room_id = r.id WHERE r.property_id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, propertyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bookings.add(extractBookingFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching property bookings: " + e.getMessage());
        }
        return bookings;
    }

    // UPDATE: Change status to APPROVED or REJECTED (Landlord action)
    public boolean updateStatus(int bookingId, String status) {
        String sql = "UPDATE bookings SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, bookingId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating booking status: " + e.getMessage());
            return false;
        }
    }

    // READ: Get the total count of PENDING requests for a specific landlord's dashboard
    public int countPendingByLandlordId(int landlordId) {
        String sql = "SELECT COUNT(b.id) FROM bookings b " +
                "JOIN rooms r ON b.room_id = r.id " +
                "JOIN properties p ON r.property_id = p.id " +
                "WHERE p.landlord_id = ? AND b.status = 'PENDING'";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, landlordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1); // Returns the actual count number
                }
            }
        } catch (SQLException e) {
            System.err.println("Error counting pending bookings: " + e.getMessage());
        }
        return 0; // Return 0 if there's an error or no bookings
    }

    // HELPER: Convert SQL row to Java Object
    private Booking extractBookingFromResultSet(ResultSet rs) throws SQLException {
        Booking booking = new Booking();
        booking.setId(rs.getInt("id"));
        booking.setTenantId(rs.getInt("tenant_id"));
        booking.setRoomId(rs.getInt("room_id"));
        booking.setStatus(rs.getString("status"));
        booking.setBookingDate(rs.getString("booking_date"));
        return booking;
    }
    // NEW HELPER: Re-opens a room if a landlord rejects/cancels a booking
    public boolean restoreRoomAvailability(int roomId) {
        String sql = "UPDATE rooms SET is_available = 1 WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error restoring room availability: " + e.getMessage());
            return false;
        }
    }
}