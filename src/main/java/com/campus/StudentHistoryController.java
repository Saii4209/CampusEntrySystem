package com.campus;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * StudentHistoryController
 *
 * Displays a student's personal attendance log, grouped by date.
 * Uses AttendanceDAO to pull records from the database.
 */
public class StudentHistoryController {

    @FXML private VBox  historyRowsContainer;
    @FXML private Label summaryLabel;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("hh:mm a");

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();

    @FXML
    private void initialize() {
        loadHistory();
    }

    @FXML
    private void goBack() {
        try {
            MainApp.switchScene("student-entry.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Navigation Error",
                    "Unable to return to the Entry screen.");
        }
    }

    // ── Load data ─────────────────────────────────────────────────────────────

    private void loadHistory() {
        historyRowsContainer.getChildren().clear();
        summaryLabel.setText("");

        String studentId = MainApp.getCurrentStudentId();

        // ← If no ID is set, send them back instead of showing blank or wrong data
        if (studentId == null || studentId.isBlank()) {
            showAlert(AlertType.WARNING, "No Student Selected",
                    "Please enter your Student ID on the entry screen first.");
            try { MainApp.switchScene("student-entry.fxml"); }
            catch (Exception e) { e.printStackTrace(); }
            return;
        }


        try {
            List<AttendanceRecord> records =
                    attendanceDAO.findByStudent(studentId);

            if (records.isEmpty()) {
                addEmptyLabel("No attendance records found.");
                summaryLabel.setText("0 records");
                return;
            }

            // Group by date (newest → oldest, already sorted by DAO)
            Map<LocalDate, DayPair> dayMap = new LinkedHashMap<>();
            boolean hasDuplicates = false;    // tracks anomalies

            for (AttendanceRecord r : records) {
                LocalDate date = r.getTimestamp().toLocalDate();
                DayPair pair = dayMap.computeIfAbsent(date, d -> new DayPair());
                if ("TIME_IN".equals(r.getAction())) {
                    if (pair.timeIn == null) {
                        pair.timeIn = r.getTimestamp();
                    } else {
                        hasDuplicates = true;   // ← duplicate detected, don't silently skip
                    }
                }
                if ("TIME_OUT".equals(r.getAction())) {
                    if (pair.timeOut == null) {
                        pair.timeOut = r.getTimestamp();
                    } else {
                        hasDuplicates = true;
                    }
                }
            }

            int rowIndex = 0;
            for (Map.Entry<LocalDate, DayPair> e : dayMap.entrySet()) {
                historyRowsContainer.getChildren().add(
                        buildRow(e.getKey(), e.getValue(), rowIndex++));
            }

            summaryLabel.setText(rowIndex + " record" + (rowIndex == 1 ? "" : "s"));

            // Inform the user if data anomalies were found
            if (hasDuplicates) {
                showAlert(AlertType.WARNING, "Data Anomaly",
                        "Duplicate attendance entries were detected for one or more days. " +
                                "Only the first entry per day is shown. Please contact your administrator.");
            }

        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Database Error",
                    "Failed to load history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Row builder ───────────────────────────────────────────────────────────

    private GridPane buildRow(LocalDate date, DayPair pair, int index) {
        GridPane row = new GridPane();
        row.setMaxWidth(Double.MAX_VALUE);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            cc.setHgrow(Priority.ALWAYS);
            row.getColumnConstraints().add(cc);
        }

        String bg = (index % 2 == 0) ? "#ffffff" : "#f8fafc";
        String cell = "-fx-font-size: 13px; -fx-text-fill: #1e293b;" +
                      "-fx-padding: 10 14 10 14;" +
                      "-fx-background-color: " + bg + ";" +
                      "-fx-border-color: transparent transparent #f1f5f9 transparent;" +
                      "-fx-border-width: 0 0 1 0;";

        Label dateLabel    = styledLabel(date.format(DATE_FMT), cell);
        Label timeInLabel  = styledLabel(pair.timeIn  != null
                                         ? pair.timeIn.format(TIME_FMT)  : "—", cell);
        Label timeOutLabel = styledLabel(pair.timeOut != null
                                         ? pair.timeOut.format(TIME_FMT) : "—", cell);

        boolean complete = pair.timeIn != null && pair.timeOut != null;
        boolean onlyIn   = pair.timeIn != null && pair.timeOut == null;

        String badgeBg, badgeFg, badgeText;
        if (complete)     { badgeBg = "#dcfce7"; badgeFg = "#15803d"; badgeText = "Complete"; }
        else if (onlyIn)  { badgeBg = "#fef9c3"; badgeFg = "#854d0e"; badgeText = "Ongoing";  }
        else              { badgeBg = "#fee2e2"; badgeFg = "#b91c1c"; badgeText = "Incomplete"; }

        Label badge = new Label(badgeText);
        badge.setStyle("-fx-background-color: " + badgeBg + ";" +
                       "-fx-text-fill: " + badgeFg + ";" +
                       "-fx-font-size: 11px; -fx-font-weight: bold;" +
                       "-fx-background-radius: 4; -fx-padding: 3 10 3 10;");

        HBox badgeBox = new HBox(badge);
        badgeBox.setMaxWidth(Double.MAX_VALUE);
        badgeBox.setAlignment(Pos.CENTER_LEFT);
        badgeBox.setStyle("-fx-padding: 7 14 7 14; -fx-background-color: " + bg + ";" +
                          "-fx-border-color: transparent transparent #f1f5f9 transparent;" +
                          "-fx-border-width: 0 0 1 0;");

        row.add(dateLabel,    0, 0);
        row.add(timeInLabel,  1, 0);
        row.add(timeOutLabel, 2, 0);
        row.add(badgeBox,     3, 0);
        return row;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Label styledLabel(String text, String style) {
        Label l = new Label(text);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle(style);
        return l;
    }

    private void addEmptyLabel(String text) {
        Label empty = new Label(text);
        empty.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8; -fx-padding: 40 0 0 0;");
        empty.setMaxWidth(Double.MAX_VALUE);
        empty.setAlignment(Pos.CENTER);
        historyRowsContainer.getChildren().add(empty);
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    private static class DayPair {
        LocalDateTime timeIn;
        LocalDateTime timeOut;
    }
}
