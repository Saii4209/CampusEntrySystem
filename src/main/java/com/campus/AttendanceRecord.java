package com.campus;

import java.time.LocalDateTime;

/**
 * AttendanceRecord model — mirrors one row of the attendance table.
 */
public class AttendanceRecord {

    private long id;
    private String studentId;
    private String action;       // "TIME_IN" | "TIME_OUT"
    private LocalDateTime timestamp;

    public AttendanceRecord() {}

    public AttendanceRecord(long id,
                            String studentId,
                            String action,
                            LocalDateTime timestamp) {

        this.id = id;
        this.studentId = studentId;
        this.action = action;
        this.timestamp = timestamp;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getId() {
        return id;
    }

    public void setId(long v) {
        this.id = v;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String v) {
        this.studentId = v;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String v) {
        this.action = v;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime v) {
        this.timestamp = v;
    }
}