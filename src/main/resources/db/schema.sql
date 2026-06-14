-- =============================================================
-- CampusEntrySystem — schema
-- =============================================================

-- 1. students
-- =============================================================
CREATE TABLE IF NOT EXISTS students (
    student_id  VARCHAR(20)  PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    course      VARCHAR(50)  NOT NULL,
    year_level  VARCHAR(20)  NOT NULL,
    contact     VARCHAR(20)  NOT NULL
    CONSTRAINT chk_contact CHECK (contact ~ '^[0-9]{10,15}$'),
    email       VARCHAR(100) NOT NULL UNIQUE
    CONSTRAINT chk_email CHECK (email ~* '^[^@]+@[^@]+\.[^@]+$'),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
    );

COMMENT ON TABLE  students IS 'Registered students in the campus system.';
COMMENT ON COLUMN students.student_id  IS 'Primary key — e.g. 2024-0001.';
COMMENT ON COLUMN students.course      IS 'Plain text course code — e.g. BSIT, BSCS.';
COMMENT ON COLUMN students.year_level  IS 'Plain text year level — e.g. 2nd Year.';
COMMENT ON COLUMN students.contact     IS 'Digits only, 10–15 characters.';

-- 2. faculty
-- =============================================================
CREATE TABLE IF NOT EXISTS faculty (
    id         SERIAL       PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,   -- BCrypt hash
    full_name  VARCHAR(100),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
    );

-- 3. attendance
-- =============================================================
CREATE TABLE IF NOT EXISTS attendance (
    id          BIGSERIAL    PRIMARY KEY,
    student_id  VARCHAR(20)  NOT NULL
    REFERENCES students(student_id) ON DELETE CASCADE,
    action      VARCHAR(10)  NOT NULL
    CONSTRAINT chk_action CHECK (action IN ('TIME_IN', 'TIME_OUT')),
    timestamp   TIMESTAMP    NOT NULL DEFAULT NOW(),
    exported    BOOLEAN      NOT NULL DEFAULT FALSE
    );

COMMENT ON TABLE  attendance IS 'Campus entry/exit log.';
COMMENT ON COLUMN attendance.action   IS 'TIME_IN or TIME_OUT.';
COMMENT ON COLUMN attendance.exported IS 'Marked TRUE after CSV export.';

CREATE INDEX IF NOT EXISTS idx_attendance_student_date
    ON attendance (student_id, DATE(timestamp));

-- 4. export_log
-- =============================================================
CREATE TABLE IF NOT EXISTS export_log (
    id          BIGSERIAL    PRIMARY KEY,
    exported_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    exported_by INT          REFERENCES faculty(id) ON DELETE SET NULL,
    row_count   INT          NOT NULL DEFAULT 0,
    file_name   VARCHAR(255)
    );

-- 5. export_log_attendance  (join table)
-- =============================================================
CREATE TABLE IF NOT EXISTS export_log_attendance (
    export_id     BIGINT NOT NULL REFERENCES export_log(id)   ON DELETE CASCADE,
    attendance_id BIGINT NOT NULL REFERENCES attendance(id)   ON DELETE CASCADE,
    PRIMARY KEY (export_id, attendance_id)
    );