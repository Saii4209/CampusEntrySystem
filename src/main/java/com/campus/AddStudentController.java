package com.campus;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

/**
 * AddStudentController
 *
 * Handles student registration with full validation before persisting to DB.
 * Uses StudentDAO for all database operations.
 */
public class AddStudentController {

    @FXML private TextField nameField;
    @FXML private TextField studentIdField;
    @FXML private TextField courseField;
    @FXML private TextField yearLevelField;
    @FXML private TextField contactField;
    @FXML private TextField emailField;

    private final StudentDAO studentDAO = new StudentDAO();

    /** Register a new student. */
    @FXML
    private void registerStudent() {

        // ── Null / blank guard ───────────────────────────────────────────────
        if (isAnyBlank(nameField, studentIdField, courseField,
                       yearLevelField, contactField, emailField)) {
            showAlert(Alert.AlertType.ERROR,
                      "Validation Error",
                      "All fields are required. Please complete the form.");
            return;
        }

        Student student = new Student(
                studentIdField.getText().trim(),
                nameField.getText().trim(),
                courseField.getText().trim(),
                yearLevelField.getText().trim(),
                contactField.getText().trim(),
                emailField.getText().trim()
        );
        // ── Persist ─────────────────────────────────────────────────────────
        try {
            studentDAO.insert(student);

            showAlert(Alert.AlertType.INFORMATION,
                      "Registration Successful",
                      "Student " + student.getFullName() + " has been registered.");
            clearForm();

        } catch (IllegalArgumentException e) {
            // business-rule violation (duplicate ID, bad email/contact, …)
            showAlert(Alert.AlertType.ERROR, "Validation Error", e.getMessage());

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR,
                      "Database Error",
                      "Could not save student: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Clear all form fields. */
    @FXML
    private void clearForm() {
        nameField.clear();
        studentIdField.clear();
        courseField.clear();
        yearLevelField.clear();
        contactField.clear();
        emailField.clear();
    }

    /** Return to Faculty Dashboard. */
    @FXML
    private void goBack() {
        try {
            MainApp.switchScene("dashboard.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isAnyBlank(TextField... fields) {
        for (TextField f : fields) {
            if (f.getText() == null || f.getText().isBlank()) return true;
        }
        return false;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
