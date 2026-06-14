package com.campus;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * FacultyLoginController
 *
 * Authenticates faculty against the faculty table in the database.
 * Passwords are stored as BCrypt hashes; plain-text comparison has been removed.
 */

public class FacultyLoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void login() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // ── Null/blank guard ─────────────────────────────────────────────────
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                    "Please enter your username and password.");
            return;
        }

        // ── DB authentication ────────────────────────────────────────────────
        try {
            if (authenticate(username, password)) {
                showAlert(Alert.AlertType.INFORMATION, "Login Successful",
                        "Welcome, " + username + "!");
                MainApp.switchScene("dashboard.fxml");
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed",
                        "Invalid username or password.");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not verify credentials: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void back() {
        try {
            MainApp.switchScene("role-selection.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // Computed once at class load — the whole point is to have a real hash
    // ready so the checkpw() call takes the same time as a real verification.
    private static final String DUMMY_HASH =
            BCrypt.hashpw("dummy-timing-guard", BCrypt.gensalt());

    /**
     * Fetch the stored BCrypt hash for the given username, then verify the
     * submitted password against it.  Returns false (never throws) if the
     * username does not exist, so timing differences do not leak user existence.
     */
    private boolean authenticate(String username,
                                 String password) throws SQLException {
        // Fetch only the stored hash — do NOT compare in SQL.
        String sql = "SELECT password FROM faculty WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // Username not found; run a dummy check to avoid timing oracle.
                    BCrypt.checkpw(password, DUMMY_HASH);
                    return false;
                }
                String storedHash = rs.getString("password");
                return BCrypt.checkpw(password, storedHash);
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
