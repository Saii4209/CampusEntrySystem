package com.campus;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static final String URL;
    private static final String USER;
    private static final String PASS;

    static {
        try (InputStream in =
                     DatabaseConnection.class.getResourceAsStream("/db.properties")) {

            if (in == null) {
                throw new RuntimeException("db.properties not found on classpath.");
            }

            Properties p = new Properties();
            p.load(in);

            String host = p.getProperty("db.host", "localhost").trim();
            String port = p.getProperty("db.port", "5432").trim();
            String name = p.getProperty("db.name", "postgres").trim();

            URL  = "jdbc:postgresql://" + host + ":" + port + "/" + name
                 + "?connectTimeout=10";
            USER = p.getProperty("db.user",     "postgres").trim();
            PASS = p.getProperty("db.password", "").trim();

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Database Connection Error");
                alert.setHeaderText("Failed to connect to the database");
                alert.setContentText(
                    "Could not connect to local PostgreSQL.\n\n" +
                    "Make sure:\n" +
                    "  1. PostgreSQL service is running\n" +
                    "     → Open Services (Win+R → services.msc)\n" +
                    "       and start 'postgresql-x64-xx'\n" +
                    "  2. Password is correct in db.properties\n\n" +
                    "Error: " + e.getMessage()
                );
                alert.getDialogPane().setMinWidth(450);
                alert.show();
            });
            throw e;
        }
    }
}
