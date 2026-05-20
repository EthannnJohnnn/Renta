package dao;

import models.Property;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PropertyDAO {

    // CREATE
    public boolean addProperty(Property property) {
        // ADDED image_url and the 5th ?
        String sql = "INSERT INTO properties (landlord_id, name, address, description, image_url) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, property.getLandlordId());
            pstmt.setString(2, property.getName());
            pstmt.setString(3, property.getAddress());
            pstmt.setString(4, property.getDescription());
            pstmt.setString(5, property.getImageUrl()); // NEW LINE

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding property: " + e.getMessage());
            return false;
        }
    }

    // READ: Get absolutely everything (For Tenant Search Dashboard)
    public List<Property> getAllProperties() {
        List<Property> properties = new ArrayList<>();
        String sql = "SELECT * FROM properties";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                properties.add(extractPropertyFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching properties: " + e.getMessage());
        }
        return properties;
    }

    // READ: Get only the properties owned by a specific Landlord (For Landlord Dashboard)
    public List<Property> getPropertiesByLandlordId(int landlordId) {
        List<Property> properties = new ArrayList<>();
        String sql = "SELECT * FROM properties WHERE landlord_id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, landlordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    properties.add(extractPropertyFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching properties for landlord: " + e.getMessage());
        }
        return properties;
    }

    // UPDATE
    public boolean updateProperty(Property property) {
        // ADDED image_url = ?
        String sql = "UPDATE properties SET name = ?, address = ?, description = ?, image_url = ? WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, property.getName());
            pstmt.setString(2, property.getAddress());
            pstmt.setString(3, property.getDescription());
            pstmt.setString(4, property.getImageUrl()); // NEW LINE
            pstmt.setInt(5, property.getId());          // Moved ID to parameter 5

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating property: " + e.getMessage());
            return false;
        }
    }

    // DELETE (Note: Because of CASCADE in your schema, this will also delete all Rooms attached to it!)
    public boolean deleteProperty(int propertyId) {
        String sql = "DELETE FROM properties WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, propertyId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting property: " + e.getMessage());
            return false;
        }
    }

    // HELPER: Keeps code clean by converting SQL rows to Java Objects
    private Property extractPropertyFromResultSet(ResultSet rs) throws SQLException {
        Property prop = new Property();
        prop.setId(rs.getInt("id"));
        prop.setLandlordId(rs.getInt("landlord_id"));
        prop.setName(rs.getString("name"));
        prop.setAddress(rs.getString("address"));
        prop.setDescription(rs.getString("description"));
        prop.setImageUrl(rs.getString("image_url")); // NEW LINE
        return prop;
    }
}