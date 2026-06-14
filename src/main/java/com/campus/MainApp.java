package com.campus;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage  stage;
    private static String currentStudentId;

    public static String getCurrentStudentId()         { return currentStudentId; }
    public static void   setCurrentStudentId(String s) { currentStudentId = s; }

    @Override
    public void start(Stage primaryStage) {
        try {
            stage = primaryStage;
            stage.setTitle("Campus Entry & Exit Monitoring System");
            stage.setResizable(true);
            stage.setMinWidth(400);
            stage.setMinHeight(300);

            // ── Initialize DB tables (shows its own error dialog on failure) ─
            try {
                DatabaseInitializer.initialize();
            } catch (Exception dbEx) {
                // DatabaseConnection already showed an alert; just log & continue
                // so the UI still opens (read-only mode gracefully degrades).
                System.err.println("[Startup] DB init failed: " + dbEx.getMessage());
            }

            switchScene("role-selection.fxml");
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Startup Error");
                alert.setHeaderText("The application failed to start");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            });
        }
    }

    public static void switchScene(String fxml) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/campus/" + fxml));
        Parent root = loader.load();
        Scene scene;

        if (stage.getScene() != null) {
            scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
        } else {
            scene = new Scene(root, 900, 600);
        }

        scene.getStylesheets().add(
                MainApp.class.getResource("/com/campus/style.css").toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    public static Stage getStage() { return stage; }

    public static void main(String[] args) { launch(args); }
}
