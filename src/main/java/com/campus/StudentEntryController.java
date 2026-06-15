package com.campus;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * StudentEntryController
 *
 * Handles Time-In / Time-Out submission.
 *
 * Business rules (enforced by AttendanceDAO):
 *  • Student must be registered before logging.
 *  • Only one TIME_IN and one TIME_OUT per calendar day.
 *  • Cannot TIME_OUT without a matching TIME_IN.
 */
public class StudentEntryController {

    // FXML components
    @FXML private TextField studentNumberField;
    @FXML private Button    timeInButton;
    @FXML private Button    timeOutButton;
    @FXML private Label     selectionStatusLabel;

    // State
    private enum AttendanceAction { NONE, TIME_IN, TIME_OUT }
    private AttendanceAction currentSelection = AttendanceAction.NONE;

    // CSS class constants
    private static final String CSS_SELECTED_IN  = "selected-time-in";
    private static final String CSS_SELECTED_OUT = "selected-time-out";
    private static final String CSS_STATUS_IN    = "status-time-in";
    private static final String CSS_STATUS_OUT   = "status-time-out";
    private static final String CSS_STATUS_IDLE  = "status-idle";

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();

    // ── Button handlers ───────────────────────────────────────────────────────

    @FXML
    private void selectTimeIn() {
        currentSelection = AttendanceAction.TIME_IN;
        applySelectionStyles();
    }
    
    @FXML
    private void selectTimeOut() {
        currentSelection = AttendanceAction.TIME_OUT;
        applySelectionStyles();
    }

    @FXML
    private void submitEntry() {
        String studentId = studentNumberField.getText().trim();

        // ── Client-side validation ───────────────────────────────────────────
        if (studentId.isEmpty()) {
            showAlert(AlertType.WARNING, "Missing Information",
                    "Please enter your Student ID.");
            return;
        }

        if (currentSelection == AttendanceAction.NONE) {
            showAlert(AlertType.WARNING, "No Action Selected",
                    "Please select Time In or Time Out before submitting.");
            return;
        }

        // ── Persist via DAO (validates student existence + rules) ─────────────
        try {
            LocalDateTime recorded = LocalDateTime.now();

            if (currentSelection == AttendanceAction.TIME_IN) {
                attendanceDAO.logTimeIn(studentId);
                showAlert(AlertType.INFORMATION, "Time In Recorded",
                        "Time In logged at " + fmt(recorded) + ".");
            } else {
                attendanceDAO.logTimeOut(studentId);
                showAlert(AlertType.INFORMATION, "Time Out Recorded",
                        "Time Out logged at " + fmt(recorded) + ".");
            }

            // Save ID for history view
            MainApp.setCurrentStudentId(studentId);
            resetForm();

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Business rule violation — if timeout attempted without timein, switch to timein mode
            if (currentSelection == AttendanceAction.TIME_OUT &&
                    e.getMessage() != null && e.getMessage().contains("time in")) {
                showAlert(AlertType.WARNING, "Time In Required",
                    e.getMessage() + "\nSwitching to Time In — please submit again.");
                currentSelection = AttendanceAction.TIME_IN;
                applySelectionStyles();
            } else {
                showAlert(AlertType.WARNING, "Cannot Log Entry", e.getMessage());
            }

        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Database Error",
                    "Failed to save attendance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        MainApp.setCurrentStudentId(null);   // ← clear session on exit
        navigate("role-selection.fxml", "Role Selection");
    }

    @FXML
    private void viewHistory() {
        String studentId = studentNumberField.getText().trim();
        if (studentId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING,
                    "No Student Selected",
                    "Please enter your Student ID first.");
            return;
        }
        StudentDAO studentDAO = new StudentDAO();

        try {
            if (!studentDAO.exists(studentId)) {
                showAlert(Alert.AlertType.WARNING,
                        "Student Not Found",
                        "No registered student exists with ID: " + studentId);
                return;
            }

            MainApp.setCurrentStudentId(studentId);
            MainApp.switchScene("student-history.fxml");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Style helpers ─────────────────────────────────────────────────────────

    private void applySelectionStyles() {
        if (currentSelection == AttendanceAction.TIME_IN) {
            timeInButton.getStyleClass().add(CSS_SELECTED_IN);
            timeOutButton.getStyleClass().remove(CSS_SELECTED_OUT);
            selectionStatusLabel.setText("✔ Time In selected — click Submit to record.");
            setStatusStyle(CSS_STATUS_IN);
        } else {
            timeOutButton.getStyleClass().add(CSS_SELECTED_OUT);
            timeInButton.getStyleClass().remove(CSS_SELECTED_IN);
            selectionStatusLabel.setText("✔ Time Out selected — click Submit to record.");
            setStatusStyle(CSS_STATUS_OUT);
        }
    }

    private void resetForm() {
        studentNumberField.clear();
        currentSelection = AttendanceAction.NONE;
        timeInButton.getStyleClass().remove(CSS_SELECTED_IN);
        timeOutButton.getStyleClass().remove(CSS_SELECTED_OUT);
        selectionStatusLabel.setText("");
        setStatusStyle(CSS_STATUS_IDLE);
    }

    private void setStatusStyle(String newClass) {
        selectionStatusLabel.getStyleClass()
                .removeAll(CSS_STATUS_IN, CSS_STATUS_OUT, CSS_STATUS_IDLE);
        selectionStatusLabel.getStyleClass().add(newClass);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private void navigate(String fxml, String screenName) {
        try {
            MainApp.switchScene(fxml);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Navigation Error",
                    "Unable to open " + screenName + " screen.");
        }
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String fmt(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("hh:mm a, MMMM d, yyyy"));
    }
}
