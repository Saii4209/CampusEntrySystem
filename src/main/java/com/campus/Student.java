package com.campus;

/**
 * Student model — mirrors the students table.
 */
public class Student {

    private String studentId;
    private String fullName;
    private String course;
    private String yearLevel;
    private String contact;
    private String email;

    public Student() {}

    public Student(String studentId,
                   String fullName,
                   String course,
                   String yearLevel,
                   String contact,
                   String email) {

        this.studentId = studentId;
        this.fullName = fullName;
        this.course = course;
        this.yearLevel = yearLevel;
        this.contact = contact;
        this.email = email;
    }

    // ── Getters & Setters ─────────────────────────────────────────────

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getYearLevel() {
        return yearLevel;
    }

    public void setYearLevel(String yearLevel) {
        this.yearLevel = yearLevel;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return studentId + " - " + fullName;
    }
}