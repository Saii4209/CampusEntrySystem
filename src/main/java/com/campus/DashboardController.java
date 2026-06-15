package com.campus;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.campus.Student;
import com.campus.AttendanceRecord;

/**
 * DashboardController
 */
public class DashboardController {

    // ── Table ────────────────────────────────────────────────────────────────
    @FXML private TableView<StudentDayRecord> entryTableView;
    @FXML private TableColumn<StudentDayRecord, String> colStudentNumber;
    @FXML private TableColumn<StudentDayRecord, String> colStudentName;
    @FXML private TableColumn<StudentDayRecord, String> colStatus;
    @FXML private TableColumn<StudentDayRecord, String> colTimeIn;
    @FXML private TableColumn<StudentDayRecord, String> colTimeOut;
    @FXML private TableColumn<StudentDayRecord, String> colDate;

    // ── CRUD action buttons ──────────────────────────────────────────────────
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    // ── Stats ────────────────────────────────────────────────────────────────
    @FXML private Label totalEntriesLabel;
    @FXML private Label timeInLabel;
    @FXML private Label timeOutLabel;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    @FXML
    public void initialize() {
        entryTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        entryTableView.setMaxWidth(Double.MAX_VALUE);

        setupColumns();
        setupSelectionActions();
        loadTodayData();
    }

    // ── Columns ──────────────────────────────────────────────────────────────
    private void setupColumns() {

        colStudentNumber.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().studentId));

        colStudentName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().studentName));

        colStatus.setCellValueFactory(c -> {
            StudentDayRecord r = c.getValue();
            String status =
                    (r.timeIn != null && r.timeOut != null) ? "Complete"
                            : (r.timeIn != null) ? "Timed In"
                              : "Incomplete";
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

        // ✅ FIXED (null-safe)
        colDate.setCellValueFactory(c -> {
            LocalDate d = c.getValue().date;
            return new SimpleStringProperty(d != null ? d.format(DATE_FMT) : "—");
        });
    }

    // ── Selection-driven CRUD actions ────────────────────────────────────────
    private void setupSelectionActions() {
        editButton.setDisable(true);
        deleteButton.setDisable(true);

        entryTableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) -> {
                    boolean hasSelection = newSelection != null;
                    editButton.setDisable(!hasSelection);
                    deleteButton.setDisable(!hasSelection);
                });
    }

    // ── Handlers ─────────────────────────────────────────────────────────────
    @FXML
    private void handleEditSelected() {
        StudentDayRecord selected = entryTableView.getSelectionModel().getSelectedItem();
        if (selected != null) handleEdit(selected);
    }

    @FXML
    private void handleDeleteSelected() {
        StudentDayRecord selected = entryTableView.getSelectionModel().getSelectedItem();
        if (selected != null) handleDelete(selected);
    }

    private void handleEdit(StudentDayRecord record) {
        try {
            MainApp.setCurrentStudentId(record.studentId);
            MainApp.switchScene("edit-student.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
    }

    private void handleDelete(StudentDayRecord record) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Delete record for " + record.studentName + "?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    attendanceDAO.deleteTodayByStudentId(record.studentId);
                    loadTodayData();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Delete Failed", e.getMessage());
                }
            }
        });
    }

    // ── Load Data ────────────────────────────────────────────────────────────
    private void loadTodayData() {
        try {
            List<AttendanceRecord> records = attendanceDAO.findAllToday();

            Map<String, StudentDayRecord> grouped = new LinkedHashMap<>();

            for (AttendanceRecord r : records) {
                String key = r.getStudentId();

                grouped.putIfAbsent(key, new StudentDayRecord(
                        key,
                        studentDAO.findById(key)
                                .map(Student::getFullName)
                                .orElse("Unknown"),
                        r.getTimestamp().toLocalDate()
                ));

                StudentDayRecord row = grouped.get(key);

                if ("TIME_IN".equals(r.getAction()) && row.timeIn == null)
                    row.timeIn = r.getTimestamp();

                if ("TIME_OUT".equals(r.getAction()) && row.timeOut == null)
                    row.timeOut = r.getTimestamp();
            }

            ObservableList<StudentDayRecord> data =
                    FXCollections.observableArrayList(grouped.values());

            entryTableView.setItems(data);

            long ins = data.stream().filter(r -> r.timeIn != null).count();
            long outs = data.stream().filter(r -> r.timeOut != null).count();

            totalEntriesLabel.setText(String.valueOf(data.size()));
            timeInLabel.setText(String.valueOf(ins));
            timeOutLabel.setText(String.valueOf(outs));

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────────
    @FXML private void exportCSV() {
        javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("Choose Export Folder");
        java.io.File dir = chooser.showDialog(MainApp.getStage());

        if (dir == null) return;

        try {
            java.io.File csv = LogExporter.exportToday(java.nio.file.Path.of(dir.toURI()));
            showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                    "CSV saved to:\n" + csv.getAbsolutePath());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export Failed", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void addStudent() throws Exception {
        MainApp.switchScene("add-student.fxml");
    }

    @FXML private void viewRegisteredStudents() throws Exception {
        MainApp.switchScene("student-list.fxml");
    }

    @FXML private void refreshTable() {
        loadTodayData();
    }

    @FXML private void logout() {
        try {
            MainApp.setCurrentStudentId(null);
            MainApp.switchScene("role-selection.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ── Inner Class ──────────────────────────────────────────────────────────
    private static class StudentDayRecord {
        String studentId;
        String studentName;
        LocalDateTime timeIn;
        LocalDateTime timeOut;
        LocalDate date;

        StudentDayRecord(String id, String name, LocalDate date) {
            this.studentId = id;
            this.studentName = name;
            this.date = date;
        }
    }
}
