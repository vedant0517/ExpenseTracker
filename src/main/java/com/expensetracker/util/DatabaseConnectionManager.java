package com.expensetracker.util;

import com.expensetracker.exception.StorageException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Dedicated database connection and lifecycle utility.
 *
 * Responsibilities:
 * - Loads credentials outside source code (Env vars -> db.properties).
 * - Masks credentials to prevent accidental credential leakage in logs or exceptions.
 * - Provides active JDBC Connection instances.
 * - Automatically ensures tables exist on startup.
 */
public class DatabaseConnectionManager {

    private static final String DEFAULT_CONFIG_FILE = "db.properties";
    private final String url;
    private final String user;
    private final String password;

    public DatabaseConnectionManager() throws StorageException {
        Properties props = loadConfiguration();
        this.url = props.getProperty("db.url");
        this.user = props.getProperty("db.user", "");
        this.password = props.getProperty("db.password", "");

        if (this.url == null || this.url.trim().isEmpty()) {
            throw new StorageException(
                    "Database URL is not configured. Please define DB_URL in environment variables or 'db.url' in db.properties.");
        }
    }

    public DatabaseConnectionManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Resolves configuration with priority:
     * 1. Environment variables (DB_URL, DB_USER, DB_PASSWORD)
     * 2. Local db.properties file
     */
    private static Properties loadConfiguration() throws StorageException {
        Properties props = new Properties();

        // 1. Check environment variables
        String envUrl = System.getenv("DB_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            props.setProperty("db.url", envUrl);
            props.setProperty("db.user", System.getenv().getOrDefault("DB_USER", ""));
            props.setProperty("db.password", System.getenv().getOrDefault("DB_PASSWORD", ""));
            return props;
        }

        // 2. Check local db.properties file
        Path configPath = Path.of(DEFAULT_CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (InputStream in = new FileInputStream(configPath.toFile())) {
                props.load(in);
                return props;
            } catch (IOException e) {
                throw new StorageException("Failed to read configuration file: " + DEFAULT_CONFIG_FILE, e);
            }
        }

        throw new StorageException(
                "No database configuration found! Please create 'db.properties' (see db.properties.example) or set DB_URL, DB_USER, DB_PASSWORD environment variables.");
    }

    /**
     * Creates and returns a new JDBC Connection.
     * Callers must close it using try-with-resources.
     */
    public Connection getConnection() throws SQLException {
        try {
            if (user != null && !user.isEmpty()) {
                return DriverManager.getConnection(url, user, password);
            } else {
                return DriverManager.getConnection(url);
            }
        } catch (SQLException e) {
            // Sanitize exception message to never expose password
            String sanitizedMsg = String.format("Failed to connect to database at '%s' (User: %s): %s",
                    url, user, e.getMessage());
            throw new SQLException(sanitizedMsg, e.getSQLState(), e.getErrorCode());
        }
    }

    /**
     * Automatically initializes the 'transactions' table if it does not exist.
     */
    public void initializeSchema() throws StorageException {
        // Table DDL compatible with MySQL, PostgreSQL, and H2
        String ddl = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                amount DECIMAL(12, 2) NOT NULL,
                category VARCHAR(100) NOT NULL,
                type VARCHAR(20) NOT NULL,
                date DATE NOT NULL,
                description TEXT
            );
            """;

        // For PostgreSQL fallback syntax if AUTO_INCREMENT is not supported
        if (url.contains("postgresql")) {
            ddl = """
                CREATE TABLE IF NOT EXISTS transactions (
                    id SERIAL PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    amount DECIMAL(12, 2) NOT NULL,
                    category VARCHAR(100) NOT NULL,
                    type VARCHAR(20) NOT NULL,
                    date DATE NOT NULL,
                    description TEXT
                );
                """;
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(ddl);
        } catch (SQLException e) {
            throw new StorageException("Failed to initialize database schema: " + e.getMessage(), e);
        }
    }

    public String getSanitizedUrl() {
        return url;
    }
}
