package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Renta");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);

        // --- BACKEND LOGIC: Initialize the SQLite database before the UI loads ---
        dao.DatabaseHelper.initializeDatabase();

        // --- FRONTEND LOGIC: Load the Login screen ---
        switchTo("views/Login.fxml");
        
        // Start maximizing the window for a modern, immersive desktop experience
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    // A helper method the UI team wrote to easily swap between screens
    public static void switchTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/" + fxmlPath)
            );
            Parent root = loader.load();
            if (primaryStage.getScene() != null) {
                // If scene exists, preserve window size/fullscreen state by just swapping the root
                primaryStage.getScene().setRoot(root);
            } else {
                // Initial launch with modern 16:9 standard resolution HD
                Scene scene = new Scene(root, 1280, 720);
                primaryStage.setScene(scene);
            }
        } catch (IOException e) {
            System.err.println("Could not load: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static void navigateTo(Parent root) {
        primaryStage.getScene().setRoot(root);
    }

    public static Stage getStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}