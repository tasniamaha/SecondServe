package com.example.secondserve;

import com.example.secondserve.dto.FoodRequestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class HotelDonationreqController {

    // --- FXML UI Components ---
    @FXML private VBox requestsContainer;
    @FXML private Label hotelNameLabel;

    // --- API Communication ---
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @FXML
    public void initialize() {
        // Set the hotel name in the header from the logged-in user's session
        if (SessionManager.getSession() != null) {
            hotelNameLabel.setText(SessionManager.getSession().getName());
        }

        // Fetch the list of pending donation requests from the server
        loadPendingRequests();
    }

    private void loadPendingRequests() {
        String authToken = SessionManager.getAuthToken();
        Long hotelId = SessionManager.getSession() != null ? SessionManager.getSession().getUserId() : null;

        if (authToken == null || hotelId == null) {
            showAlert("Authentication Error", "Could not verify user. Please log in again.");
            return;
        }

        // Use the specific endpoint for pending requests for the logged-in hotel
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/food-requests/hotel/" + hotelId + "/pending"))
                .header("Authorization", authToken)
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(this::handleRequestsResponse)
                .exceptionally(this::handleConnectionError);
    }

    private void handleRequestsResponse(HttpResponse<String> response) {
        Platform.runLater(() -> {
            if (response.statusCode() == 200) {
                try {
                    List<FoodRequestDto> requests = objectMapper.readValue(response.body(), new TypeReference<>() {});
                    displayRequests(requests);
                } catch (IOException e) {
                    showAlert("Application Error", "Could not parse the list of requests from the server.");
                }
            } else {
                showAlert("Server Error", "Could not load donation requests. Status: " + response.statusCode());
            }
        });
    }

    private void displayRequests(List<FoodRequestDto> requests) {
        requestsContainer.getChildren().clear(); // Clear any existing content

        if (requests == null || requests.isEmpty()) {
            Label placeholder = new Label("There are no pending donation requests at this time.");
            placeholder.getStyleClass().add("placeholder-text");
            requestsContainer.getChildren().add(placeholder);
        } else {
            for (FoodRequestDto request : requests) {
                HBox card = createRequestCard(request);
                requestsContainer.getChildren().add(card);
            }
        }
    }

    private Void handleConnectionError(Throwable e) {
        Platform.runLater(() -> showAlert("Connection Error", "Could not connect to the server."));
        return null;
    }

    // --- Button Actions (Approve / Reject) ---

    private void handleApprove(FoodRequestDto requestDto, HBox cardNode) {
        updateRequestStatus(requestDto, "approve", cardNode);
    }

    private void handleReject(FoodRequestDto requestDto, HBox cardNode) {
        updateRequestStatus(requestDto, "reject", cardNode);
    }

    private void updateRequestStatus(FoodRequestDto requestDto, String action, HBox cardNode) {
        String authToken = SessionManager.getAuthToken();
        if (authToken == null) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/food-requests/" + requestDto.getId() + "/" + action))
                .header("Authorization", authToken)
                .PUT(HttpRequest.BodyPublishers.noBody()) // PUT request with no body
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        requestsContainer.getChildren().remove(cardNode); // Remove the card from the UI
                        showAlert("Success", "The request has been " + action + "d.");
                    } else {
                        showAlert("Error", "Could not update the request status. It may have already been processed.");
                    }
                })).exceptionally(this::handleConnectionError);
    }

    // --- UI Factory Method for Creating Cards ---

    private HBox createRequestCard(FoodRequestDto request) {
        // VBox for text
        VBox textContainer = new VBox();
        Label ngoNameLabel = new Label(request.getNgoName());
        ngoNameLabel.getStyleClass().add("ngo-name");

        String foodDetails = String.format("Requested: %s %s of %s",
                request.getRequestedQuantity().toString(), request.getUnit(), request.getFoodItemName());
        Label foodItemLabel = new Label(foodDetails);
        foodItemLabel.getStyleClass().add("food-item");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy 'at' hh:mm a");
        Label dateLabel = new Label("Received: " + request.getRequestDate().format(formatter));
        dateLabel.getStyleClass().add("date");
        textContainer.getChildren().addAll(ngoNameLabel, foodItemLabel, dateLabel);

        // Spacer region
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // HBox for buttons
        Button approveButton = new Button("Approve");
        approveButton.getStyleClass().add("approve-button");

        Button rejectButton = new Button("Reject");
        rejectButton.getStyleClass().add("reject-button");
        HBox buttonContainer = new HBox(10.0, approveButton, rejectButton);
        buttonContainer.getStyleClass().add("button-container");

        // Main card HBox
        HBox card = new HBox(textContainer, spacer, buttonContainer);
        card.getStyleClass().add("request-card");

        // --- IMPORTANT: Set actions here ---
        approveButton.setOnAction(event -> handleApprove(request, card));
        rejectButton.setOnAction(event -> handleReject(request, card));

        return card;
    }

    // --- Navigation (Implement as needed) ---
    @FXML public void handleShowProfile(ActionEvent actionEvent) { System.out.println("Navigate to Profile"); }
    @FXML public void handleLogout(ActionEvent actionEvent) { System.out.println("Log Out"); }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}