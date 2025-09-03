package com.example.secondserve;

import com.example.secondserve.dto.AuthResponse;
import com.example.secondserve.dto.HotelDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HotelProfileController {

    // --- FXML UI Components ---
    // View Mode Panes and Labels
    @FXML private GridPane viewPane;
    @FXML private Label hotelNameLabel, managerNameLabel, emailLabel, phoneLabel, addressLabel, licenseLabel;

    // Edit Mode Panes and TextFields
    @FXML private GridPane editPane;
    @FXML private TextField hotelNameField, managerNameField, emailField, phoneField, addressField, licenseField;

    // Buttons
    @FXML private Button editButton;
    @FXML private HBox editButtonsBox;

    // --- API Communication ---
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private HotelDto currentHotelData; // Store the currently displayed hotel data

    /**
     * Called when the FXML is loaded. Fetches the hotel's profile data from the server.
     */
    @FXML
    public void initialize() {
        loadProfileData();
        switchToViewMode(); // Ensure the UI starts in the correct state
    }

    private void loadProfileData() {
        AuthResponse session = SessionManager.getSession();
        if (session == null || session.getUserId() == null) {
            showAlert(Alert.AlertType.ERROR, "Authentication Error", "Could not find user session. Please log in again.");
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/hotels/" + session.getUserId()))
                .header("Authorization", SessionManager.getAuthToken())
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            try {
                                this.currentHotelData = objectMapper.readValue(response.body(), HotelDto.class);
                                populateViewLabels(currentHotelData);
                            } catch (IOException e) {
                                e.printStackTrace();
                                showAlert(Alert.AlertType.ERROR, "Data Error", "Failed to parse hotel profile data from server.");
                            }
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Server Error", "Failed to load profile. Status: " + response.statusCode());
                        }
                    });
                }).exceptionally(e -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Connection Error", "Could not connect to the server."));
                    return null;
                });
    }

    /**
     * A helper method to update the text of all the display Labels.
     */
    private void populateViewLabels(HotelDto hotel) {
        hotelNameLabel.setText(hotel.getHotelName());
        managerNameLabel.setText(hotel.getManagerName());
        emailLabel.setText(hotel.getEmail());
        phoneLabel.setText(hotel.getPhone());
        addressLabel.setText(hotel.getAddress());
        licenseLabel.setText(hotel.getHotelLicense());
    }

    /**
     * Switches the UI to Edit Mode when the "Edit Profile" button is clicked.
     */
    @FXML
    private void handleEditProfile(ActionEvent actionEvent) {
        if (currentHotelData == null) return;

        // Populate the TextFields with the current data
        hotelNameField.setText(currentHotelData.getHotelName());
        managerNameField.setText(currentHotelData.getManagerName());
        emailField.setText(currentHotelData.getEmail());
        phoneField.setText(currentHotelData.getPhone());
        addressField.setText(currentHotelData.getAddress());
        licenseField.setText(currentHotelData.getHotelLicense());

        switchToEditMode();
    }

    /**
     * Saves the changes by sending the updated data to the server.
     */
    @FXML
    private void handleSaveChanges(ActionEvent actionEvent) {
        // Create a new DTO with the updated values from the TextFields
        HotelDto updatedHotel = new HotelDto();
        updatedHotel.setHotelName(hotelNameField.getText());
        updatedHotel.setManagerName(managerNameField.getText());
        updatedHotel.setPhone(phoneField.getText());
        updatedHotel.setAddress(addressField.getText());
        // Email and License are not editable, so they are not included in the update payload.

        AuthResponse session = SessionManager.getSession();
        if (session == null) return;

        try {
            String requestBody = objectMapper.writeValueAsString(updatedHotel);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/hotels/" + session.getUserId()))
                    .header("Authorization", SessionManager.getAuthToken())
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            try {
                                this.currentHotelData = objectMapper.readValue(response.body(), HotelDto.class);
                                populateViewLabels(this.currentHotelData);
                                showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully.");
                                switchToViewMode();
                            } catch (IOException e) { e.printStackTrace();}
                        } else {
                            System.out.println("Update failed. Server responded with status code: " + response.statusCode());
                            System.out.println("Server error response body: " + response.body());
                            showAlert(Alert.AlertType.ERROR, "Update Failed", "Could not save changes to the server.");
                        }
                    })).exceptionally(e -> {  e.printStackTrace();
                        // -----------------------------------------
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Connection Error", "An unexpected error occurred.")); return null; });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Discards any changes and switches back to View Mode.
     */
    @FXML
    private void handleCancel(ActionEvent actionEvent) {
        switchToViewMode();
    }

    // --- UI State Management ---

    private void switchToViewMode() {
        viewPane.setVisible(true);
        editPane.setVisible(false);
        editPane.setManaged(false); // Make it not take up space

        editButton.setVisible(true);
        editButtonsBox.setVisible(false);
        editButtonsBox.setManaged(false);
    }

    private void switchToEditMode() {
        viewPane.setVisible(false);
        editPane.setVisible(true);
        editPane.setManaged(true);

        editButton.setVisible(false);
        editButtonsBox.setVisible(true);
        editButtonsBox.setManaged(true);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("Info");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}