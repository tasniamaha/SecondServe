package com.example.secondserve;

import com.example.secondserve.dto.FoodItemDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.example.secondserve.dto.AuthResponse;
import com.example.secondserve.dto.DashboardStatsDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

public class HotelDashboardController {
    private Timeline refreshTimeline;
    // --- FXML UI Components ---
    @FXML private BorderPane mainBorderPane;
    @FXML private Label donatedValueLabel;
    @FXML private Label loggedValueLabel;
    @FXML private Label hotelCodeLabel;
    @FXML private Button dashboardButton;
    @FXML private Button profileButton;
    @FXML private Button requestsButton;
    @FXML private VBox newLeftoversContainer;
    @FXML private Label hotelName;
    // A reference to the initial dashboard view to easily return to it.
    private Node dashboardView;
    private Button activeButton;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @FXML
    public void initialize() {
        objectMapper.registerModule(new JavaTimeModule());
        AuthResponse session = SessionManager.getSession();
        if (session != null) {

            // Use getOrganizationName() instead of getName()
            hotelName.setText(session.getOrganizationName());
        }
        this.dashboardView = mainBorderPane.getCenter();

        // Set the "Main Dashboard" button as active visually.
        setActiveButton(dashboardButton);

        // Asynchronously load dynamic data from the server.
        loadDashboardStats();
        loadPendingLeftovers();
        setupAutoRefresh();
    }

    private void setupAutoRefresh() {
        refreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(30), event -> {
                    System.out.println("Auto-refreshing dashboard...");
                    loadDashboardStats();
                    loadPendingLeftovers();
                })
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
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
        loadViewIntoCenter("hotelManager_profile.fxml");
        setActiveButton(profileButton);
    }

    /**
     * Called when the "Donation Requests" button is clicked. Loads the requests view.
     */
    @FXML
    public void handleShowRequests(ActionEvent actionEvent) {
        loadViewIntoCenter("HotelManager_DonationReq.fxml"); // Ensure this FXML file exists
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
        String authToken = SessionManager.getAuthToken();
        Long hotelId = SessionManager.getHotelId(); // This method now exists and works
        if (authToken == null || hotelId == null) {
            System.err.println("Not logged in or hotelId is null. Cannot fetch leftovers.");
            return;
        }

        // We use the new endpoint created on the server
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/food-items/hotel/" + hotelId + "/pending"))
                .header("Authorization", authToken)
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(this::handleLeftoversResponse)
                .exceptionally(this::handleConnectionError);
    }

    private void handleLeftoversResponse(HttpResponse<String> response) {
        Platform.runLater(() -> {
            if (response.statusCode() == 200) {
                try {
                    // Use a TypeReference to parse a List of objects
                    List<FoodItemDto> pendingItems = objectMapper.readValue(response.body(), new TypeReference<List<FoodItemDto>>() {});
                    updateLeftoversUI(pendingItems);
                } catch (IOException e) {
                    System.err.println("Error parsing pending leftovers JSON: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("Failed to fetch pending leftovers. Status: " + response.statusCode());
            }
        });
    }

    private void updateLeftoversUI(List<FoodItemDto> pendingItems) {
        newLeftoversContainer.getChildren().clear(); // Clear old items

        if (pendingItems.isEmpty()) {
            Label placeholder = new Label("No new leftovers are awaiting review.");
            placeholder.getStyleClass().add("placeholder-text");
            newLeftoversContainer.getChildren().add(placeholder);
        } else {
            for (FoodItemDto item : pendingItems) {
                newLeftoversContainer.getChildren().add(createLeftoverCard(item));
            }
        }
    }

    private HBox createLeftoverCard(FoodItemDto item) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("leftover-card"); // For CSS styling

        Label nameLabel = new Label(item.getFoodName());
        nameLabel.setMinWidth(200);
        nameLabel.getStyleClass().add("leftover-name");

        Label quantityLabel = new Label(String.format("%.2f %s", item.getQuantity(), item.getUnit()));
        quantityLabel.setMinWidth(120);
        quantityLabel.getStyleClass().add("leftover-details");

        Label expiryLabel = new Label("Expires: " + item.getExpiryDate().toString());
        expiryLabel.getStyleClass().add("leftover-details");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button approveButton = new Button("Approve");
        approveButton.getStyleClass().add("approve-button");
        approveButton.setOnAction(e -> handleApprove(item.getId()));

        Button rejectButton = new Button("Reject");
        rejectButton.getStyleClass().add("reject-button");
        rejectButton.setOnAction(e -> handleReject(item.getId()));

        card.getChildren().addAll(nameLabel, quantityLabel, expiryLabel, spacer, approveButton, rejectButton);
        return card;
    }

    private void handleReject(Long foodItemId) {
        String authToken = SessionManager.getAuthToken();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/food-items/" + foodItemId))
                .header("Authorization", authToken)
                .DELETE() // DELETE request
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 204) { // 204 No Content is success for DELETE
                        Platform.runLater(this::loadPendingLeftovers); // Refresh the list
                    } else {
                        System.err.println("Failed to reject item. Status: " + response.statusCode());
                    }
                });
    }

    private void handleApprove(Long foodItemId) {
        String authToken = SessionManager.getAuthToken();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/food-items/" + foodItemId + "/approve"))
                .header("Authorization", authToken)
                .PUT(HttpRequest.BodyPublishers.noBody()) // PUT request
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        Platform.runLater(this::loadPendingLeftovers); // Refresh the list on success
                    } else {
                        System.err.println("Failed to approve item. Status: " + response.statusCode());
                    }
                });
    }

    private Void handleConnectionError(Throwable e) {
        Platform.runLater(() -> {
            System.err.println("Connection Error: " + e.getMessage());
            //showAlert("Connection Error", "Could not connect to the server.");
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