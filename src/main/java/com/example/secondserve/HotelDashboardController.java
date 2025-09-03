package com.example.secondserve;

import com.example.secondserve.dto.DashboardStatsDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

public class HotelDashboardController {

    // --- FXML UI Components ---
    @FXML private BorderPane mainBorderPane;
    @FXML private Label donatedValueLabel;
    @FXML private Label loggedValueLabel;
    @FXML private Label hotelCodeLabel;
    @FXML private Button dashboardButton;
    @FXML private Button profileButton;
    @FXML private Button requestsButton;
    @FXML private VBox newLeftoversContainer;

    // A reference to the initial dashboard view to easily return to it.
    private Node dashboardView;
    private Button activeButton;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @FXML
    public void initialize() {
        // Store the initial dashboard content before we navigate away from it.
        this.dashboardView = mainBorderPane.getCenter();

        // Set the "Main Dashboard" button as active visually.
        setActiveButton(dashboardButton);

        // Asynchronously load dynamic data from the server.
        loadDashboardStats();
        loadPendingLeftovers();
    }

    /**
     * Called when the "Main Dashboard" button is clicked. Restores the initial view.
     */
    @FXML
    public void handleShowDashboard(ActionEvent actionEvent) {
        mainBorderPane.setCenter(dashboardView);
        setActiveButton(dashboardButton);
    }

    /**
     * Called when the "Hotel Profile" button is clicked. Loads the profile view.
     */
    @FXML
    public void handleShowProfile(ActionEvent actionEvent) {
        loadViewIntoCenter("HotelProfileView.fxml");
        setActiveButton(profileButton);
    }

    /**
     * Called when the "Donation Requests" button is clicked. Loads the requests view.
     */
    @FXML
    public void handleShowRequests(ActionEvent actionEvent) {
        loadViewIntoCenter("DonationRequestsView.fxml"); // Ensure this FXML file exists
        setActiveButton(requestsButton);
    }

    /**
     * Called when the "Log Out" button is clicked. Clears the session and returns to the role selection screen.
     */
    @FXML
    public void handleLogout(ActionEvent actionEvent) {
        SessionManager.clearSession(); // Clears the stored JWT token and user info
        navigateToScene((Node) actionEvent.getSource(), "opening-view.fxml", "SecondServe - Choose Your Role");
    }

    // --- API Communication Logic ---

    private void loadDashboardStats() {
        String authToken = SessionManager.getAuthToken();
        if (authToken == null) {
            showAlert("Authentication Error", "You are not logged in.");
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/hotels/dashboard-stats"))
                .header("Authorization", authToken)
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(this::handleStatsResponse)
                .exceptionally(this::handleConnectionError);
    }

    private void handleStatsResponse(HttpResponse<String> response) {
        Platform.runLater(() -> {
            if (response.statusCode() == 200) {
                try {
                    DashboardStatsDto stats = objectMapper.readValue(response.body(), DashboardStatsDto.class);
                    // Update the UI labels with the fetched data
                    donatedValueLabel.setText(stats.getTotalDonatedThisWeek().toString());
                    loggedValueLabel.setText(stats.getTotalLoggedThisWeek().toString());
                    hotelCodeLabel.setText(stats.getHotelCode());
                } catch (IOException e) {
                    showAlert("Application Error", "Could not parse dashboard data from the server.");
                }
            } else {
                showAlert("Server Error", "Could not load dashboard stats. Status: " + response.statusCode());
            }
        });
    }

    private void loadPendingLeftovers() {
        // This is where you will call your server to get the list of leftovers that have is_available = false.
        // For each item, you will dynamically create an HBox "card" and add it to the newLeftoversContainer.
        // Example: newLeftoversContainer.getChildren().add(createLeftoverCard(item));
        System.out.println("Fetching pending leftovers from the server...");
    }

    private Void handleConnectionError(Throwable e) {
        Platform.runLater(() -> {
            System.err.println("Connection Error: " + e.getMessage());
            showAlert("Connection Error", "Could not connect to the server.");
        });
        return null;
    }

    // --- Navigation and Helper Methods ---

    private void loadViewIntoCenter(String fxmlFile) {
        try {
            Parent view = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/secondserve/" + fxmlFile)));
            mainBorderPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not load the view: " + fxmlFile);
        }
    }

    private void navigateToScene(Node sourceNode, String fxmlFile, String title) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/secondserve/" + fxmlFile)));
            Stage stage = (Stage) sourceNode.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(title);
            stage.centerOnScreen();
        } catch (IOException e) {
            showAlert("Navigation Error", "Could not load the page: " + fxmlFile);
        }
    }

    private void setActiveButton(Button newActiveButton) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        if (newActiveButton != null) {
            newActiveButton.getStyleClass().add("active");
        }
        this.activeButton = newActiveButton;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}