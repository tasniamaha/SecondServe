package com.example.secondserve;

import com.example.secondserve.dto.AuthResponse;
import com.example.secondserve.dto.NgoDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
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

public class NgoProfileController {

    // --- FXML UI Components ---
    @FXML private GridPane viewPane;
    @FXML private Label nameLabel, contactPersonLabel, emailLabel, phoneLabel, addressLabel;

    @FXML private GridPane editPane;
    @FXML private TextField nameField, contactPersonField, emailField, phoneField, addressField;

    @FXML private Button editButton;
    @FXML private HBox editButtonsBox;

    // --- API Communication ---
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private NgoDto currentNgoData; // Store the currently displayed profile data

    @FXML
    public void initialize() {
        // Fetch the profile data from the server as soon as the view is loaded.
        loadProfileData();
        switchToViewMode();
    }

    private void loadProfileData() {
        AuthResponse session = SessionManager.getSession();
        if (session == null || session.getUserId() == null) {
            showAlert(Alert.AlertType.ERROR, "Authentication Error", "User session not found. Please log in again.");
            return;
        }

        // The endpoint to get a single NGO by their ID.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/ngos/" + session.getUserId()))
                .header("Authorization", SessionManager.getAuthToken())
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(this::handleLoadResponse)
                .exceptionally(this::handleConnectionError);
    }

    private void handleLoadResponse(HttpResponse<String> response) {
        Platform.runLater(() -> {
            System.out.println("NGO Profile Response Status: " + response.statusCode());
            System.out.println("NGO Profile Response Body: " + response.body());

            if (response.statusCode() == 200) {
                try {
                    this.currentNgoData = objectMapper.readValue(response.body(), NgoDto.class);
                    populateViewLabels(currentNgoData);
                } catch (IOException e) {
                    System.err.println("Parse error: " + e.getMessage());
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Data Error", "Failed to parse profile data from the server.");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Server Error", "Failed to load profile. Status: " + response.statusCode());
            }
        });
    }

    private void populateViewLabels(NgoDto ngo) {
        nameLabel.setText(ngo.getNgoName());
        contactPersonLabel.setText(ngo.getContactPerson());
        emailLabel.setText(ngo.getEmail());
        phoneLabel.setText(ngo.getPhone());
        addressLabel.setText(ngo.getAddress());
    }

    @FXML
    private void handleEditProfile() {
        if (currentNgoData == null) return;

        nameField.setText(currentNgoData.getNgoName());
        contactPersonField.setText(currentNgoData.getContactPerson());
        emailField.setText(currentNgoData.getEmail());
        phoneField.setText(currentNgoData.getPhone());
        addressField.setText(currentNgoData.getAddress());

        switchToEditMode();
    }

    @FXML
    private void handleSaveChanges() {
        // Create a DTO with only the fields that are allowed to be updated.
        NgoDto updatedNgo = new NgoDto();
        updatedNgo.setNgoName(nameField.getText());
        updatedNgo.setContactPerson(contactPersonField.getText());
        updatedNgo.setPhone(phoneField.getText());
        updatedNgo.setAddress(addressField.getText());

        AuthResponse session = SessionManager.getSession();
        if (session == null) return;

        try {
            String requestBody = objectMapper.writeValueAsString(updatedNgo);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/ngos/" + session.getUserId()))
                    .header("Authorization", SessionManager.getAuthToken())
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            Platform.runLater(() -> {
                                try {
                                    this.currentNgoData = objectMapper.readValue(response.body(), NgoDto.class);
                                    populateViewLabels(this.currentNgoData);
                                    showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully.");
                                    switchToViewMode();
                                } catch (IOException e) {
                                    showAlert(Alert.AlertType.ERROR, "Data Error", "Failed to parse updated profile data.");
                                }
                            });
                        } else {
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Update Failed", "Could not save changes to the server."));
                        }
                    })
                    .exceptionally(this::handleConnectionError);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        switchToViewMode();
    }

    private Void handleConnectionError(Throwable e) {
        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Connection Error", "Could not connect to the server."));
        return null;
    }

    // --- UI State Management ---
    private void switchToViewMode() {
        viewPane.setVisible(true);
        editPane.setVisible(false); editPane.setManaged(false);
        editButton.setVisible(true);
        editButtonsBox.setVisible(false); editButtonsBox.setManaged(false);
    }

    private void switchToEditMode() {
        viewPane.setVisible(false);
        editPane.setVisible(true); editPane.setManaged(true);
        editButton.setVisible(false);
        editButtonsBox.setVisible(true); editButtonsBox.setManaged(true);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("Info");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}