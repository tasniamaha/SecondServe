package com.example.secondserve;

import com.example.secondserve.dto.NgoDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NgoDetailsController {

    // --- FXML UI Components ---
    @FXML private Label ngoNameSubtitle;
    @FXML private Label ngoNameLabel;
    @FXML private Label contactPersonLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label addressLabel;
    @FXML private Label licenseLabel;

    // --- For Navigation ---
    private BorderPane mainBorderPane; // A reference to the main dashboard's BorderPane

    // --- For API Communication ---
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private long ngoId;

    /**
     * This is the entry point for the controller. The previous screen (Donation Requests)
     * will call this method to provide the necessary data and context.
     *
     * @param ngoId The unique ID of the NGO to display.
     * @param mainBorderPane The main layout pane from the parent dashboard.
     */
    public void initData(long ngoId, BorderPane mainBorderPane) {
        this.ngoId = ngoId;
        this.mainBorderPane = mainBorderPane;
        loadNgoDetails();
    }

    /**
     * Makes an authenticated API call to the server to get the details of the specific NGO.
     */
    private void loadNgoDetails() {
        String authToken = SessionManager.getAuthToken();
        if (authToken == null) {
            showAlert("Authentication Error", "Could not verify user. Please log in again.");
            return;
        }

        // This is the endpoint to get a single NGO by their ID.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/ngos/" + this.ngoId))
                .header("Authorization", authToken)
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(this::handleServerResponse)
                .exceptionally(this::handleConnectionError);
    }

    private void handleServerResponse(HttpResponse<String> response) {
        Platform.runLater(() -> {
            if (response.statusCode() == 200) {
                try {
                    // Convert the JSON response into our NgoDto object
                    NgoDto ngo = objectMapper.readValue(response.body(), NgoDto.class);
                    // Populate the UI with the data from the DTO
                    populateLabels(ngo);
                } catch (IOException e) {
                    showAlert("Application Error", "Failed to parse NGO details from the server.");
                    e.printStackTrace();
                }
            } else {
                showAlert("Server Error", "Could not load NGO details. Status: " + response.statusCode());
            }
        });
    }

    private Void handleConnectionError(Throwable e) {
        Platform.runLater(() -> {
            System.err.println("Connection Error: " + e.getMessage());
            showAlert("Connection Error", "Could not connect to the server.");
        });
        return null;
    }

    /**
     * Helper method to set the text of all the labels from an NgoDto object.
     */
    private void populateLabels(NgoDto ngo) {
        ngoNameSubtitle.setText("Details for " + ngo.getNgoName());
        ngoNameLabel.setText(ngo.getNgoName());
        contactPersonLabel.setText(ngo.getContactPerson());
        emailLabel.setText(ngo.getEmail());
        phoneLabel.setText(ngo.getPhone() != null ? ngo.getPhone() : "Not Provided");
        addressLabel.setText(ngo.getAddress() != null ? ngo.getAddress() : "Not Provided");
        licenseLabel.setText(ngo.getLicenseNumber() != null ? ngo.getLicenseNumber() : "Not Provided");
    }

    /**
     * Handles the "Back" button click. This will reload the main Donation Requests view
     * into the center of the dashboard.
     */
    @FXML
    public void handleGoBack(ActionEvent actionEvent) {
        try {
            // Reload the requests view. A more advanced app might pass a reference to the
            // previous controller, but reloading is simpler and reliable.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/secondserve/DonationRequestsView.fxml"));
            Parent requestsView = loader.load();
            mainBorderPane.setCenter(requestsView);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not go back to the requests view.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}