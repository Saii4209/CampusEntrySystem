package com.campus;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.sql.Statement;

/**
 * AttendanceDAO — entry-log operations for the attendance table.
 *
 * All write methods verify the student exists before touching the DB.
 */
public class AttendanceDAO {

    private final StudentDAO studentDAO = new StudentDAO();

    // ── Log Time-In ──────────────────────────────────────────────────────────

    /**
     * Record a TIME_IN for today.
     *
     * Rules enforced:
     *  • Student must exist.
     *  • No duplicate TIME_IN for the same calendar day.
     *
     * @return the auto-generated attendance ID.
     * @throws IllegalArgumentException on business-rule violation.
     * @throws IllegalStateException    if student is not registered.
     */
    public long logTimeIn(String studentId) throws SQLException {
        requireStudentExists(studentId);

        LocalDate today = LocalDate.now();
        if (hasActionToday(studentId, "TIME_IN", today)) {
            throw new IllegalArgumentException(
                    "You have already timed in today. Please time out first.");
        }
        return insertLog(studentId, "TIME_IN", LocalDateTime.now());
    }

    // ── Log Time-Out ─────────────────────────────────────────────────────────

    /**
     * Record a TIME_OUT for today.
     *
     * Rules enforced:
     *  • Student must exist.
     *  • A TIME_IN must exist for today.
     *  • No duplicate TIME_OUT for the same calendar day.
     */
    public long logTimeOut(String studentId) throws SQLException {
        requireStudentExists(studentId);

        LocalDate today = LocalDate.now();
        if (!hasActionToday(studentId, "TIME_IN", today)) {
            throw new IllegalArgumentException(
                    "No Time In found for today. Please time in first.");
        }
        if (hasActionToday(studentId, "TIME_OUT", today)) {
            throw new IllegalArgumentException(
                    "You have already timed out for today.");
        }
        return insertLog(studentId, "TIME_OUT", LocalDateTime.now());
    }

    // ── Status check ─────────────────────────────────────────────────────────

    /**
     * Return the student's current campus status for today.
     *
     * @return "IN"  – timed in but not out yet
     *         "OUT" – timed out already
     *         "NONE"– no record today
     */
    public String getStatusToday(String studentId) throws SQLException {
        LocalDate today = LocalDate.now();
        boolean timedIn = hasActionToday(studentId, "TIME_IN", today);
        boolean timedOut = hasActionToday(studentId, "TIME_OUT", today);

        if (timedIn && !timedOut) return "IN";
        if (timedIn) return "OUT";
        return "NONE";
    }

    // ── Query helpers ─────────────────────────────────────────────────────────

    /**
     * All attendance records for a specific student, newest first.
     */
    public List<AttendanceRecord> findByStudent(String studentId) throws SQLException {
        String sql = """
            SELECT id, student_id, action, timestamp
              FROM attendance
             WHERE student_id = ?
             ORDER BY timestamp DESC
            """;
        return query(sql, studentId);
    }

    /**
     * All attendance records for today (all students), newest first.
     */
    public List<AttendanceRecord> findAllToday() throws SQLException {
        String sql = """
            SELECT id, student_id, action, timestamp
              FROM attendance
             WHERE DATE(timestamp) = CURRENT_DATE
             ORDER BY timestamp DESC
            """;
        return queryAll(sql);
    }

    /**
     * All attendance records (all students, all dates), newest first.
     */
    public List<AttendanceRecord> findAll() throws SQLException {
        String sql = """
            SELECT id, student_id, action, timestamp
              FROM attendance
             ORDER BY timestamp DESC
            """;
        return queryAll(sql);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean hasActionToday(String studentId,
                                   String action,
                                   LocalDate date) throws SQLException {

        String sql = """
            SELECT 1 FROM attendance
             WHERE student_id = ?
               AND action     = ?
               AND DATE(timestamp) = ?
            LIMIT 1
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, studentId);
            ps.setString(2, action);
            ps.setDate(3, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private long insertLog(String studentId,
                           String action,
                           LocalDateTime ts) throws SQLException {

        String sql = """
            INSERT INTO attendance (student_id, action, timestamp)
            VALUES (?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, studentId);
            ps.setString(2, action);
            ps.setTimestamp(3, Timestamp.valueOf(ts));

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void requireStudentExists(String studentId) throws SQLException {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("Student ID cannot be empty.");
        }

        if (!studentDAO.exists(studentId)) {
            throw new IllegalStateException(
                    "Student ID " + studentId + " is not registered in the system.");
        }
    }

    private List<AttendanceRecord> query(String sql,
                                         String studentId) throws SQLException {

        List<AttendanceRecord> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }

        return list;
    }

    private List<AttendanceRecord> queryAll(String sql) throws SQLException {
        List<AttendanceRecord> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }
        }

        return list;
    }

    private AttendanceRecord map(ResultSet rs) throws SQLException {
        return new AttendanceRecord(
                rs.getLong("id"),
                rs.getString("student_id"),
                rs.getString("action"),
                rs.getTimestamp("timestamp").toLocalDateTime()
        );
    }
}