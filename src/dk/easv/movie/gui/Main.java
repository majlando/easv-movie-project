package dk.easv.movie.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the Private Movie Collection application.
 * Initializes the JavaFX application and loads the main view.
 */
public class Main extends Application {

    /**
     * Starts the JavaFX application
     * @param primaryStage The primary stage for the application
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the main view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainView.fxml"));
            Parent root = loader.load();

            // Create and configure the scene
            Scene scene = new Scene(root, 1280, 780);

            // Configure the stage
            primaryStage.setTitle("🎬 Private Movie Collection - v1.0.0");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(650);

            // Center the window on screen
            primaryStage.centerOnScreen();

            // Show the stage
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("Failed to start application: " + e.getMessage());
            e.printStackTrace();
            showErrorAndExit("Application Error",
                "Failed to start the application.\n\nError: " + e.getMessage());
        }
    }

    /**
     * Shows an error dialog and exits the application
     */
    private void showErrorAndExit(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        System.exit(1);
    }

    /**
     * Main method - launches the JavaFX application
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}

