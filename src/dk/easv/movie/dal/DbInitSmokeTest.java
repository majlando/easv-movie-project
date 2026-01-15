package dk.easv.movie.dal;

/**
 * Tiny smoke test to verify DB initialization logic without starting JavaFX.
 *
 * Usage (IntelliJ): Run this main() and check console output.
 */
public class DbInitSmokeTest {
    public static void main(String[] args) {
        try {
            DatabaseConnector connector = new DatabaseConnector();
            DatabaseInitializer initializer = new DatabaseInitializer(connector);
            String status = initializer.initializeWithStatus();
            System.out.println(status);
        } catch (Exception e) {
            System.err.println("DB init smoke test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
