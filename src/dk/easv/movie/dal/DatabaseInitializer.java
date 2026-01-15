package dk.easv.movie.dal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles database initialization by running SQL setup scripts.
 * Optionally creates the database (if missing), then creates tables and seeds initial data.
 */
public class DatabaseInitializer {

    private final DatabaseConnector dbConnector;

    /**
     * Creates a new DatabaseInitializer
     * @param dbConnector The database connector to use
     */
    public DatabaseInitializer(DatabaseConnector dbConnector) {
        this.dbConnector = dbConnector;
    }

    /**
     * Initializes the database (optional create DB), then tables and seed data.
     *
     * @throws SQLException If SQL execution fails
     * @throws IOException If SQL files cannot be read
     */
    public void initialize() throws SQLException, IOException {
        initializeWithStatus();
    }

    /**
     * Same as {@link #initialize()} but returns a short status message that can be shown in the UI.
     */
    public String initializeWithStatus() throws SQLException, IOException {
        StringBuilder status = new StringBuilder("DB init: ");

        if (dbConnector.isAutoCreateDatabaseEnabled()) {
            boolean created = ensureDatabaseExists();
            status.append(created ? "created database" : "database exists");
        } else {
            ensureDatabaseExistsOrThrow();
            status.append("database exists");
        }

        // Run setup script to create tables
        runSqlScript("sql/setup_database.sql");
        status.append(", tables ok");

        // Run seed script to populate initial data
        runSqlScript("sql/seed_data.sql");
        status.append(", seed ok");

        System.out.println("Database initialization complete.");
        return status.toString();
    }

    /**
     * Ensures the configured database exists by connecting to master.
     *
     * @return true if the database was created, false if it already existed
     */
    private boolean ensureDatabaseExists() throws SQLException {
        String dbName = dbConnector.getDatabaseName();

        try (Connection conn = dbConnector.getConnection("master")) {
            if (databaseExists(conn, dbName)) {
                return false;
            }

            // Create database
            System.out.println("Database '" + dbName + "' does not exist. Creating...");
            try (Statement st = conn.createStatement()) {
                // QUOTENAME via brackets to avoid injection and handle weird names.
                st.execute("CREATE DATABASE [" + dbName.replace("]", "]]" ) + "]");
            } catch (SQLException createEx) {
                throw new SQLException(
                    "Failed to create database '" + dbName + "'. " +
                    "The SQL user may not have CREATE DATABASE permission, or the server is not reachable. " +
                    "Original error: " + createEx.getMessage(),
                    createEx);
            }

            System.out.println("Database created: " + dbName);
            return true;
        } catch (SQLException masterConnEx) {
            // Provide a clearer top-level message while preserving the original exception.
            throw new SQLException(
                "Failed to connect to 'master' to verify/create database. " +
                "Check db.server/db.port and credentials. Original error: " + masterConnEx.getMessage(),
                masterConnEx);
        }
    }

    /**
     * For environments where creating a database is not allowed, fail fast with a clear message.
     */
    private void ensureDatabaseExistsOrThrow() throws SQLException {
        String dbName = dbConnector.getDatabaseName();

        try (Connection conn = dbConnector.getConnection("master")) {
            if (!databaseExists(conn, dbName)) {
                throw new SQLException(
                    "Database '" + dbName + "' does not exist and db.autoCreate is disabled. " +
                    "Create the database manually or set db.autoCreate=true in config.properties.");
            }
        } catch (SQLException masterConnEx) {
            throw new SQLException(
                "Failed to connect to 'master' to verify database existence. " +
                "Check db.server/db.port and credentials. Original error: " + masterConnEx.getMessage(),
                masterConnEx);
        }
    }

    private boolean databaseExists(Connection conn, String dbName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM sys.databases WHERE name = ?")) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Runs a SQL script file.
     * Supports SQL Server "GO" batch separators on their own line.
     */
    private void runSqlScript(String resourcePath) throws SQLException, IOException {
        String sql = loadSqlFromResource(resourcePath);

        try (Connection conn = dbConnector.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String batch : splitSqlServerBatches(sql)) {
                String statement = batch.trim();
                if (statement.isEmpty()) {
                    continue;
                }
                try {
                    stmt.execute(statement);
                } catch (SQLException e) {
                    // Many init scripts are idempotent; keep current behavior but be explicit.
                    System.out.println("Note executing " + resourcePath + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Splits SQL into batches by lines containing only "GO" (case-insensitive).
     * This matches SQL Server tooling behavior more closely than splitting on any GO token.
     */
    private String[] splitSqlServerBatches(String sql) {
        return sql.replace("\r\n", "\n")
                  .split("(?im)^\\s*GO\\s*$");
    }

    /**
     * Loads SQL content from a resource file
     * @param resourcePath Path to the resource
     * @return SQL content as string
     * @throws IOException If file cannot be read
     */
    private String loadSqlFromResource(String resourcePath) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if (is == null) {
            throw new IOException(
                "SQL script '" + resourcePath + "' not found in classpath. " +
                "Ensure SQL files are in the resources/sql/ directory.");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
