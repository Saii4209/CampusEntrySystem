package com.campus;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * DatabaseInitializer
 *
 * Creates all tables and seeds default data on first run.
 * Call DatabaseInitializer.initialize() from MainApp.start() before showing any scene.
 *
 * Changes from original:
 *  - Faculty seed password is stored as a BCrypt hash, not plain text.
 *  - students table uses course_id INT + year_level SMALLINT (matches Java model).
 *  - courses table is created before students (foreign key dependency).
 */
public class DatabaseInitializer {

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // ── 1. students ─────────────────────────────────────────────────────
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS students (
                    student_id  VARCHAR(20)  PRIMARY KEY,
                    full_name   VARCHAR(100) NOT NULL,
                    course      VARCHAR(50)  NOT NULL,
                    year_level  VARCHAR(20)  NOT NULL,
                    contact     VARCHAR(20)  NOT NULL CONSTRAINT chk_contact CHECK (contact ~ '^[0-9]{10,15}$'),
                    email       VARCHAR(100) NOT NULL UNIQUE CONSTRAINT chk_email CHECK (email ~* '^[^@]+@[^@]+\\.[^@]+$'),
                    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
    )
""");

            // ── 2. faculty ───────────────────────────────────────────────────
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS faculty (
                    id         SERIAL       PRIMARY KEY,
                    username   VARCHAR(50)  NOT NULL UNIQUE,
                    password   VARCHAR(255) NOT NULL,
                    full_name  VARCHAR(100),
                    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
                )
            """);

            // Only hash + insert when the default account doesn't exist yet.
            String checkSql  = "SELECT 1 FROM faculty WHERE username = ?";
            String insertSql = """
                INSERT INTO faculty (username, password, full_name)
                VALUES (?, ?, 'System Administrator')
                """;

            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setString(1, "faculty");
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {                          // ← only runs once, ever
                        String hash = BCrypt.hashpw("admin123", BCrypt.gensalt());
                        try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                            ins.setString(1, "faculty");
                            ins.setString(2, hash);
                            ins.executeUpdate();
                        }
                    }
                }
            }

            // ── 3. attendance ────────────────────────────────────────────────
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS attendance (
                    id          BIGSERIAL    PRIMARY KEY,
                    student_id  VARCHAR(20)  NOT NULL
                                REFERENCES students(student_id) ON DELETE CASCADE,
                    action      VARCHAR(10)  NOT NULL
                                CHECK (action IN ('TIME_IN', 'TIME_OUT')),
                    timestamp   TIMESTAMP    NOT NULL DEFAULT NOW(),
                    exported    BOOLEAN      NOT NULL DEFAULT FALSE
                )
            """);

            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_attendance_student_date
                ON attendance (student_id, DATE(timestamp))
            """);

            // ── 4. export_log ────────────────────────────────────────────────
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS export_log (
                    id          BIGSERIAL   PRIMARY KEY,
                    exported_at TIMESTAMP   NOT NULL DEFAULT NOW(),
                    exported_by INT         REFERENCES faculty(id) ON DELETE SET NULL,
                    row_count   INT         NOT NULL DEFAULT 0,
                    file_name   VARCHAR(255)
                )
            """);

            // ── 5. export_log_attendance ─────────────────────────────────────
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS export_log_attendance (
                    export_id     BIGINT NOT NULL REFERENCES export_log(id) ON DELETE CASCADE,
                    attendance_id BIGINT NOT NULL REFERENCES attendance(id) ON DELETE CASCADE,
                    PRIMARY KEY (export_id, attendance_id)
                )
            """);

            System.out.println("[DB] Tables initialized successfully.");

        } catch (SQLException e) {
            System.err.println("[DB] Initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
