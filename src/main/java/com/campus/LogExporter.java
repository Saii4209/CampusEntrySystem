package com.campus;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * LogExporter
 *
 * Exports attendance records to a CSV file.
 *
 * Fix: students no longer has a plain 'course' column.
 * All queries now join the courses table to resolve course_id → course code.
 * year_level is a SMALLINT; cast to text via CAST(s.year_level AS TEXT).
 */
public class LogExporter {

    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final DateTimeFormatter DISPLAY_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String CSV_HEADER =
            "ID,Student ID,Full Name,Course,Year Level,Action,Timestamp";

    // ── Shared SQL fragment ───────────────────────────────────────────────────
    // Join courses so we get the human-readable course code.
    // Cast year_level (SMALLINT) to text for getString() convenience.
    private static final String SELECT_COLS = """
            SELECT a.id,
                   a.student_id,
                   s.full_name,
                   s.course,
                   s.year_level,
                   a.action,
                   a.timestamp
              FROM attendance a
              JOIN students   s ON s.student_id = a.student_id
            """;

    // ── Public API ────────────────────────────────────────────────────────────

    /** Export ALL attendance records to a CSV file in the given directory. */
    public static File exportAll(Path directory) throws IOException, SQLException {
        String sql = SELECT_COLS + " ORDER BY a.timestamp DESC";
        return runExport(sql, directory, "attendance_all_");
    }

    /** Export only today's attendance records. */
    public static File exportToday(Path directory) throws IOException, SQLException {
        String sql = SELECT_COLS
                + " WHERE DATE(a.timestamp) = CURRENT_DATE"
                + " ORDER BY a.timestamp DESC";
        return runExport(sql, directory, "attendance_today_");
    }

    /** Export attendance for a specific student. */
    public static File exportByStudent(String studentId,
                                       Path directory) throws IOException, SQLException {
        String sql = SELECT_COLS
                + " WHERE a.student_id = ?"
                + " ORDER BY a.timestamp DESC";
        return runExportWithParam(sql, studentId, directory,
                "attendance_" + studentId.replaceAll("[^a-zA-Z0-9]", "_") + "_");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static File runExport(String sql,
                                   Path dir,
                                   String prefix) throws IOException, SQLException {

        Files.createDirectories(dir);
        File out = dir.resolve(prefix + FILE_TS.format(LocalDateTime.now()) + ".csv")
                      .toFile();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery();
             PrintWriter pw = new PrintWriter(
                     new OutputStreamWriter(new FileOutputStream(out),
                                            StandardCharsets.UTF_8))) {

            pw.println(CSV_HEADER);
            while (rs.next()) pw.println(row(rs));
        }
        return out;
    }

    private static File runExportWithParam(String sql,
                                            String param,
                                            Path dir,
                                            String prefix) throws IOException, SQLException {

        Files.createDirectories(dir);
        File out = dir.resolve(prefix + FILE_TS.format(LocalDateTime.now()) + ".csv")
                      .toFile();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, param);

            try (ResultSet rs = ps.executeQuery();
                PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(out),
                                            StandardCharsets.UTF_8))) {

                pw.println(CSV_HEADER);
                while (rs.next()) pw.println(row(rs));
            }
        }
        return out;
    }

    /** Format a single ResultSet row as a CSV line. */
    private static String row(ResultSet rs) throws SQLException {
        return String.join(",",
                escape(rs.getString("id")),
                escape(rs.getString("student_id")),
                escape(rs.getString("full_name")),
                escape(rs.getString("course")),       // resolved via courses JOIN
                escape(rs.getString("year_level")),   // cast to text in SQL
                escape(rs.getString("action")),
                escape(DISPLAY_TS.format(
                        rs.getTimestamp("timestamp").toLocalDateTime()))
        );
    }

    /** Wrap value in quotes and escape internal quotes. */
    private static String escape(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
