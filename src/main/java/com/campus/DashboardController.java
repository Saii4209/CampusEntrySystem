package com.campus;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import java.util.LinkedHashMap;
import java.util.Map;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DashboardController
 *
 * Faculty view: live today's attendance table, real stat counters,
 * CSV export via LogExporter, and student registration navigation.
 */
public class DashboardController {

    // ── Table ─────────────────────────────────────────────────────────────────
    @FXML private TableView<StudentDayRecord> entryTableView;
    @FXML private TableColumn<StudentDayRecord, String> colStudentNumber;
    @FXML private TableColumn<StudentDayRecord, String> colStudentName;
    @FXML private TableColumn<StudentDayRecord, String> colStatus;
    @FXML private TableColumn<StudentDayRecord, String> colTimeIn;
    @FXML private TableColumn<StudentDayRecord, String> colTimeOut;
    @FXML private TableColumn<StudentDayRecord, String> colDate;

    // ── Stat labels ───────────────────────────────────────────────────────────
    @FXML private Label totalEntriesLabel;
    @FXML private Label timeInLabel;
    @FXML private Label timeOutLabel;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final StudentDAO    studentDAO    = new StudentDAO();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Constrain the table so it doesn't stretch beyond the window
        entryTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        entryTableView.setMaxWidth(Double.MAX_VALUE);   // lets it fill but not overflow

        setupColumns();
        loadTodayData();
    }

    // ── Column setup ──────────────────────────────────────────────────────────

    private void setupColumns() {

        colStudentNumber.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().studentId));

        colStudentName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().studentName));

        colStatus.setCellValueFactory(c -> {
            StudentDayRecord r = c.getValue();
            String status;
            if (r.timeIn != null && r.timeOut != null) status = "Complete";
            else if (r.timeIn != null)                 status = "Timed In";
            else                                        status = "Incomplete";
            return new SimpleStringProperty(status);
        });

        colTimeIn.setCellValueFactory(c -> {
            LocalDateTime t = c.getValue().timeIn;
            return new SimpleStringProperty(t != null ? t.format(TIME_FMT) : "—");
        });

        colTimeOut.setCellValueFactory(c -> {
            LocalDateTime t = c.getValue().timeOut;
            return new SimpleStringProperty(t != null ? t.format(TIME_FMT) : "—");
        });

        colDate.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().date.format(DATE_FMT)));
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadTodayData() {
        try {
            List<AttendanceRecord> records = attendanceDAO.findAllToday();

            // Group TIME_IN and TIME_OUT into paired rows by student
            Map<String, StudentDayRecord> grouped = new LinkedHashMap<>();

            for (AttendanceRecord r : records) {
                String key = r.getStudentId();

                if (!grouped.containsKey(key)) {
                    // Resolve name once
                    String name = studentDAO.findById(key)
                            .map(Student::getFullName)
                            .orElse("Unknown");
                    grouped.put(key, new StudentDayRecord(
                            key, name, r.getTimestamp().toLocalDate()));
                }

                StudentDayRecord row = grouped.get(key);
                if ("TIME_IN".equals(r.getAction())  && row.timeIn  == null)
                    row.timeIn  = r.getTimestamp();
                if ("TIME_OUT".equals(r.getAction()) && row.timeOut == null)
                    row.timeOut = r.getTimestamp();
            }

            ObservableList<StudentDayRecord> data =
                    FXCollections.observableArrayList(grouped.values());
            entryTableView.setItems(data);

            // Stats count unique students, not raw records
            long ins  = data.stream().filter(r -> r.timeIn  != null).count();
            long outs = data.stream().filter(r -> r.timeOut != null).count();

            totalEntriesLabel.setText(String.valueOf(data.size()));
            timeInLabel.setText(String.valueOf(ins));
            timeOutLabel.setText(String.valueOf(outs));

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not load today's records: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Navigate to Add Student form. */
    @FXML
    private void addStudent() {
        try {
            MainApp.switchScene("add-student.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Export today's attendance to a CSV file chosen by the user. */
    @FXML
    private void exportCSV() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Export Folder");
        File dir = chooser.showDialog(MainApp.getStage());

        if (dir == null) return; // user-cancelled

        try {
            File csv = LogExporter.exportToday(Path.of(dir.toURI()));

            showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                    "CSV saved to:\n" + csv.getAbsolutePath());

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export Failed", e.getMessage());
            e.printStackTrace();
        }
    }

    /** Refresh the table (useful as a manual refresh button if added). */
    @FXML
    private void refreshTable() {
        loadTodayData();
    }

    /** Logout and return to role selection. */
    @FXML
    private void logout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to logout?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                MainApp.setCurrentStudentId(null);   // ← clear on logout
                try { MainApp.switchScene("role-selection.fxml"); }
                catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    // ── Inner class for paired display rows ───────────────────────────────────
    private static class StudentDayRecord {
        String studentId;
        String studentName;
        LocalDateTime timeIn;
        LocalDateTime timeOut;
        LocalDate date;

        StudentDayRecord(String studentId, String studentName, LocalDate date) {
            this.studentId   = studentId;
            this.studentName = studentName;
            this.date        = date;
        }
    }
}
