package com.example.secondserve;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class NgoProfileController {

    // View Mode Panes and Labels
    @FXML private GridPane viewPane;
    @FXML private Label nameLabel;
    @FXML private Label contactPersonLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label addressLabel;

    // Edit Mode Panes and TextFields
    @FXML private GridPane editPane;
    @FXML private TextField nameField;
    @FXML private TextField contactPersonField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;

    // Buttons
    @FXML private Button editButton;
    @FXML private HBox editButtonsBox;

    /**
     * Called when the FXML file is loaded.
     */
    @FXML
    public void initialize() {
        // In a real application, you would fetch this data from your server
        // and populate the labels. For now, the FXML has sample text.

        // Ensure the UI starts in view mode.
        switchToViewMode();
    }

    /**
     * Switches the UI to Edit Mode when the "Edit Profile" button is clicked.
     */
    @FXML
    private void handleEditProfile() {
        // Populate the TextFields with the current data from the Labels
        nameField.setText(nameLabel.getText());
        contactPersonField.setText(contactPersonLabel.getText());
        emailField.setText(emailLabel.getText());
        phoneField.setText(phoneLabel.getText());
        addressField.setText(addressLabel.getText());

        switchToEditMode();
    }

    /**
     * Saves the changes and switches back to View Mode.
     */
    @FXML
    private void handleSaveChanges() {
        // 1. Get the new values from the TextFields
        String newName = nameField.getText();
        String newContactPerson = contactPersonField.getText();
        String newPhone = phoneField.getText();
        String newAddress = addressField.getText();

        // 2. In a real application, send this data to your server via an API call
        //    to persist the changes in the database.

        // 3. Update the display Labels with the new data
        nameLabel.setText(newName);
        contactPersonLabel.setText(newContactPerson);
        phoneLabel.setText(newPhone);
        addressLabel.setText(newAddress);

        switchToViewMode();
    }

    /**
     * Discards any changes and switches back to View Mode.
     */
    @FXML
    private void handleCancel() {
        switchToViewMode();
    }

    /** Helper method to manage the visibility of panes and buttons for VIEW mode. */
    private void switchToViewMode() {
        viewPane.setVisible(true);
        viewPane.setManaged(true);
        editPane.setVisible(false);
        editPane.setManaged(false);

        editButton.setVisible(true);
        editButton.setManaged(true);
        editButtonsBox.setVisible(false);
        editButtonsBox.setManaged(false);
    }

    /** Helper method to manage the visibility of panes and buttons for EDIT mode. */
    private void switchToEditMode() {
        editPane.setVisible(true);
        editPane.setManaged(true);
        viewPane.setVisible(false);
        viewPane.setManaged(false);

        editButtonsBox.setVisible(true);
        editButtonsBox.setManaged(true);
        editButton.setVisible(false);
        editButton.setManaged(false);
    }
}