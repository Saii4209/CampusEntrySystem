package com.campus;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * StudentListController
 *
 * Displays all registered students in a searchable table.
 * Edit and Delete are CRUD action buttons below the table that
 * operate on the currently selected row.
 */
public class StudentListController {

    // ── Table ─────────────────────────────────────────────────────────────────
    @FXML private TableView<Student>               studentTableView;
    @FXML private TableColumn<Student, String>     colId;
    @FXML private TableColumn<Student, String>     colFullName;
    @FXML private TableColumn<Student, String>     colCourse;
    @FXML private TableColumn<Student, String>     colYearLevel;
    @FXML private TableColumn<Student, String>     colContact;
    @FXML private TableColumn<Student, String>     colEmail;

    // ── CRUD action buttons ──────────────────────────────────────────────────
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    // ── Controls ──────────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Label     totalStudentsLabel;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final StudentDAO studentDAO = new StudentDAO();
    private ObservableList<Student> allStudents = FXCollections.observableArrayList();

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        studentTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        studentTableView.setMaxWidth(Double.MAX_VALUE);

        setupColumns();
        setupSearch();
        setupSelectionActions();
        loadStudents();
    }

    // ── Column setup ──────────────────────────────────────────────────────────
    private void setupColumns() {

        colId.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStudentId()));

        colFullName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFullName()));

        colCourse.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCourse()));

        colYearLevel.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getYearLevel()));

        colContact.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getContact()));

        colEmail.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEmail()));
    }

    // ── Selection-driven CRUD actions ────────────────────────────────────────
    private void setupSelectionActions() {
        editButton.setDisable(true);
        deleteButton.setDisable(true);

        studentTableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) -> {
                    boolean hasSelection = newSelection != null;
                    editButton.setDisable(!hasSelection);
                    deleteButton.setDisable(!hasSelection);
                });
    }

    // ── Search ────────────────────────────────────────────────────────────────
    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) ->
                applyFilter(newVal.trim().toLowerCase()));
    }

    private void applyFilter(String query) {
        if (query.isEmpty()) {
            studentTableView.setItems(allStudents);
        } else {
            ObservableList<Student> filtered = allStudents.stream()
                    .filter(s ->
                            s.getStudentId().toLowerCase().contains(query) ||
                                    s.getFullName().toLowerCase().contains(query))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            studentTableView.setItems(filtered);
        }
        totalStudentsLabel.setText(String.valueOf(studentTableView.getItems().size()));
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        studentTableView.setItems(allStudents);
        totalStudentsLabel.setText(String.valueOf(allStudents.size()));
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void loadStudents() {
        try {
            List<Student> students = studentDAO.findAll();
            allStudents = FXCollections.observableArrayList(students);
            studentTableView.setItems(allStudents);
            totalStudentsLabel.setText(String.valueOf(allStudents.size()));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not load students: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Handlers ─────────────────────────────────────────────────────────────
    @FXML
    private void handleEditSelected() {
        Student selected = studentTableView.getSelectionModel().getSelectedItem();
        if (selected != null) handleEdit(selected);
    }

    @FXML
    private void handleDeleteSelected() {
        Student selected = studentTableView.getSelectionModel().getSelectedItem();
        if (selected != null) handleDelete(selected);
    }

    private void handleEdit(Student student) {
        try {
            MainApp.setCurrentStudentId(student.getStudentId());
            MainApp.switchScene("edit-student.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not open edit form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDelete(Student student) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Student");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Permanently delete \"" + student.getFullName() + "\" (" +
                        student.getStudentId() + ")?\n\nThis will also remove all their attendance records."
        );

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    studentDAO.delete(student.getStudentId());
                    allStudents.remove(student);
                    applyFilter(searchField.getText().trim().toLowerCase());
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Delete Failed",
                            "Could not delete student: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    @FXML
    private void addStudent() {
        try {
            MainApp.switchScene("add-student.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        try {
            MainApp.switchScene("dashboard.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}