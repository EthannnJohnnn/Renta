package dao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {

    private static final String DB_URL = "jdbc:sqlite:renta.db";
    private static Connection connection;

    private DatabaseHelper() {}

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            System.err.println("Failed to connect to the database: " + e.getMessage());
        }
        return connection;
    }

    // --- NEW METHOD: Run SQL files on startup ---
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Build the tables (schema.sql)
            executeSqlScript(stmt, "/database/schema.sql");

            // 2. Check if the users table already has data
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM users");
            int userCount = 0;
            if (rs.next()) {
                userCount = rs.getInt("count");
            }

            // 3. If empty, inject the dummy data (seed.sql)
            if (userCount == 0) {
                executeSqlScript(stmt, "/database/seed.sql");
                System.out.println("Database seeded with initial data.");
            } else {
                System.out.println("Database already contains data. Skipping seed.");
            }

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    // --- HELPER METHOD: Reads .sql text files and runs them ---
    private static void executeSqlScript(Statement stmt, String filePath) {
        try (InputStream is = DatabaseHelper.class.getResourceAsStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            if (is == null) {
                System.err.println("Could not find file: " + filePath);
                return;
            }

            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sqlBuilder.append(line).append("\n");
                // Also ignore lines that are purely comments when checking for semicolon to be safe, though not strictly necessary here.
                if (line.trim().endsWith(";")) {
                    stmt.execute(sqlBuilder.toString());
                    sqlBuilder.setLength(0); // Clear for the next command
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading SQL file " + filePath + ": " + e.getMessage());
        }
    }
}