package com.campus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * StudentDAO — CRUD operations for the students table.
 *
 *  CREATE  → insert(Student)
 *  READ    → findById(id), findAll(), exists(id)
 *  UPDATE  → update(Student)
 *  DELETE  → delete(id)
 */
public class StudentDAO {

    // ── CREATE ───────────────────────────────────────────────────────────────

    /**
     * Insert a new student.
     *
     * @throws IllegalArgumentException if any required field is blank or
     *         the student ID already exists.
     * @throws SQLException on database errors.
     */
    public void insert(Student s) throws SQLException {
        validate(s);

        if (exists(s.getStudentId())) {
            throw new IllegalArgumentException(
                    "Student ID " + s.getStudentId() + " is already registered.");
        }

        String sql = """
            INSERT INTO students
                (student_id, full_name, course, year_level, contact, email)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, s.getStudentId());
            ps.setString(2, s.getFullName());
            ps.setString(3, s.getCourse());
            ps.setString(4, s.getYearLevel());
            ps.setString(5, s.getContact());
            ps.setString(6, s.getEmail());

            ps.executeUpdate();
        }
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    /**
     * Find a student by their ID.
     */
    public Optional<Student> findById(String studentId) throws SQLException {
        String sql = "SELECT * FROM students WHERE student_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Return all students ordered by name.
     */
    public List<Student> findAll() throws SQLException {
        String sql = "SELECT * FROM students ORDER BY full_name";
        List<Student> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }
        }

        return list;
    }

    /**
     * Return true when a student with the given ID is in the table.
     */
    public boolean exists(String studentId) throws SQLException {
        String sql = "SELECT 1 FROM students WHERE student_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    /**
     * Update an existing student's details.
     *
     * @throws IllegalArgumentException if the student does not exist.
     */
    public void update(Student s) throws SQLException {
        validate(s);

        if (!exists(s.getStudentId())) {
            throw new IllegalArgumentException(
                    "Student ID " + s.getStudentId() + " not found.");
        }

        String sql = """
            UPDATE students
               SET full_name  = ?,
                   course  = ?,
                   year_level = ?,
                   contact    = ?,
                   email      = ?
             WHERE student_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, s.getFullName());
            ps.setString(2, s.getCourse());
            ps.setString(3, s.getYearLevel());
            ps.setString(4, s.getContact());
            ps.setString(5, s.getEmail());
            ps.setString(6, s.getStudentId());

            ps.executeUpdate();
        }
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    /**
     * Delete a student and all their attendance records (CASCADE).
     *
     * @throws IllegalArgumentException if the student does not exist.
     */
    public void delete(String studentId) throws SQLException {
        if (!exists(studentId)) {
            throw new IllegalArgumentException(
                    "Student ID " + studentId + " not found.");
        }

        String sql = "DELETE FROM students WHERE student_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, studentId);
            ps.executeUpdate();
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Map a ResultSet row to a Student object.
     */
    private Student map(ResultSet rs) throws SQLException {
        return new Student(
                rs.getString("student_id"),
                rs.getString("full_name"),
                rs.getString("course"),
                rs.getString("year_level"),
                rs.getString("contact"),
                rs.getString("email")
        );
    }

    /**
     * Validate all required fields.
     *
     * @throws IllegalArgumentException if any field is invalid.
     */
    private void validate(Student s) {
        if (s == null)
            throw new IllegalArgumentException("Student cannot be null.");

        assertNotBlank(s.getStudentId(), "Student ID");
        assertNotBlank(s.getFullName(), "Full Name");
        assertNotBlank(s.getCourse(), "Course");
        assertNotBlank(s.getYearLevel(), "Year Level");
        assertNotBlank(s.getContact(), "Contact Number");
        assertNotBlank(s.getEmail(), "Email Address");

        if (!s.getEmail().matches("^[^@]+@[^@]+\\.[^@]+$")) {
            throw new IllegalArgumentException("Invalid email format.");
        }

        if (!s.getContact().matches("^[0-9]{10,15}$")) {
            throw new IllegalArgumentException(
                    "Contact must be 10–15 digits.");
        }
    }

    private void assertNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be empty.");
        }
    }
}