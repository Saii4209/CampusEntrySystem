package com.campus;

import javafx.fxml.FXML;

public class RoleSelectionController {

    /**
     * Open Student Entry Portal
     */
    @FXML
    private void openStudent() {

        try {

            MainApp.switchScene("student-entry.fxml");

        } catch (Exception e) {

            e.printStackTrace();

        }
    }

    /**
     * Open Faculty Login Portal
     */
    @FXML
    private void openFaculty() {

        try {

            MainApp.switchScene("faculty-login.fxml");

        } catch (Exception e) {

            e.printStackTrace();

        }
    }
}