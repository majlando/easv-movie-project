package dk.easv.movie.dal;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages database connections to MS SQL Server.
 * Reads configuration from config.properties file.
 */
public class DatabaseConnector {

    private final Properties props;
    private final SQLServerDataSource dataSource;

    private final String server;
    private final int port;
    private final String username;
    private final String password;
    private final boolean trustServerCertificate;
    private final int loginTimeout;

    /**
     * Creates a new DatabaseConnector and initializes the data source
     * @throws IOException If config file cannot be read
     */
    public DatabaseConnector() throws IOException {
        props = new Properties();

        // Load from classpath
        InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties");
        if (input == null) {
            throw new IOException(
                "Configuration file 'config.properties' not found in classpath. " +
                "Ensure config.properties is in the resources directory.");
        }

        try (InputStream is = input) {
            props.load(is);
        }

        // Parse configuration values
        this.server = props.getProperty("db.server", "localhost");
        this.port = parseIntSafe(props.getProperty("db.port"), 1433);
        this.username = props.getProperty("db.username", "");
        this.password = props.getProperty("db.password", "");
        this.trustServerCertificate = Boolean.parseBoolean(props.getProperty("db.trustServerCertificate", "true"));
        this.loginTimeout = parseIntSafe(props.getProperty("db.timeout"), 10);

        // Initialize the data source
        dataSource = new SQLServerDataSource();
        dataSource.setServerName(server);
        dataSource.setPortNumber(port);
        dataSource.setDatabaseName(props.getProperty("db.database", "movie"));
        dataSource.setUser(username);
        dataSource.setPassword(password);
        dataSource.setTrustServerCertificate(trustServerCertificate);
        dataSource.setLoginTimeout(loginTimeout);
    }

    /**
     * Safely parses an integer with a default fallback
     */
    private int parseIntSafe(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid integer value: '" + value + "', using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Gets the configured database name.
     */
    public String getDatabaseName() {
        return props.getProperty("db.database");
    }

    /**
     * Whether the app should automatically create the database if it does not exist.
     */
    public boolean isAutoCreateDatabaseEnabled() {
        return Boolean.parseBoolean(props.getProperty("db.autoCreate", "false"));
    }

    /**
     * Creates a new connection using the configured database.
     */
    public Connection getConnection() throws SQLServerException {
        return dataSource.getConnection();
    }

    /**
     * Creates a new connection to a specific database (e.g. "master").
     */
    public Connection getConnection(String databaseName) throws SQLServerException {
        SQLServerDataSource ds = new SQLServerDataSource();
        ds.setServerName(server);
        ds.setPortNumber(port);
        ds.setDatabaseName(databaseName);
        ds.setUser(username);
        ds.setPassword(password);
        ds.setTrustServerCertificate(trustServerCertificate);
        ds.setLoginTimeout(loginTimeout);
        return ds.getConnection();
    }

    /**
     * Tests the database connection
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }
}
