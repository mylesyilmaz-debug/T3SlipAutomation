package ca.empire.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {

    private Connection connection;
    private Properties properties;

    public DatabaseManager() {
        // Load the existing 'pilot.env' file
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream("environments/pilot.env")) {
            properties.load(fis);
        } catch (IOException e) {
            System.err.println("[WARN] Could not load pilot.env. Falling back to System Environment Variables.");
        }
    }

    public void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            // 1. Try reading from pilot.env
            String dbUrl  = properties.getProperty("DB_URL");
            String dbUser = properties.getProperty("DB_USER");
            String dbPass = properties.getProperty("DB_PASS");

            // 2. Fallback: If not in file, check System Env (Useful for Jenkins/CI)
            if (dbUrl == null)  dbUrl  = System.getenv("DB_URL");
            if (dbUser == null) dbUser = System.getenv("DB_USER");
            if (dbPass == null) dbPass = System.getenv("DB_PASS");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                throw new RuntimeException("[ERROR] Database credentials missing! Please add DB_URL, DB_USER, and DB_PASS to pilot.env");
            }

            connection = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            System.out.println("[INFO] Database Connection Established.");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[INFO] Database Connection Closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}