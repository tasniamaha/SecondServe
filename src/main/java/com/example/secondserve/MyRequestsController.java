package com.example.secondserve;

import com.example.secondserve.dto.FoodRequestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MyRequestsController {

    // --- FXML UI Components ---
    @FXML private TableView<FoodRequestDto> requestsTableView;
    @FXML private TableColumn<FoodRequestDto, String> hotelNameColumn;
    @FXML private TableColumn<FoodRequestDto, String> foodItemColumn;
    @FXML private TableColumn<FoodRequestDto, String> dateColumn;
    @FXML private TableColumn<FoodRequestDto, String> statusColumn;

    // --- API Communication ---
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @FXML
    public void initialize() {
        setupTableColumns();
        loadMyRequests();
    }

    /**
     * Sets up how data is displayed in each column.
     * Includes special formatting for the date, food item, and status columns.
     */
    private void setupTableColumns() {
        hotelNameColumn.setCellValueFactory(new PropertyValueFactory<>("hotelName"));

        // Custom cell value factory to combine food name and quantity
        foodItemColumn.setCellValueFactory(cellData -> {
            FoodRequestDto request = cellData.getValue();
            String displayText = String.format("%s (%s %s)",
                    request.getFoodItemName(),
                    request.getRequestedQuantity().toString(),
                    request.getUnit());
            return new SimpleStringProperty(displayText);
        });

        // Custom cell value factory to format the date nicely
        dateColumn.setCellValueFactory(cellData -> {
            LocalDateTime dateTime = cellData.getValue().getRequestDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            return new SimpleStringProperty(dateTime.format(formatter));
        });

        // --- Custom Cell Factory for the Status Column ---
        // This is where the colored labels are created.
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (status == null || empty) {
                    setGraphic(null);
                } else {
                    Label statusLabel = new Label(status);
                    statusLabel.getStyleClass().add("status-label");
                    // Remove old styles before adding a new one
                    getStyleClass().removeAll("status-approved", "status-pending", "status-rejected", "status-completed");

                    // Add the correct style class based on the status text
                    statusLabel.getStyleClass().add("status-" + status.toLowerCase());

                    setGraphic(statusLabel);
                }
            }
        });
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("requestStatus"));
    }

    /**
     * Fetches the request history for the logged-in NGO from the server.
     */
    private void loadMyRequests() {
        String authToken = SessionManager.getAuthToken();
        Long ngoId = SessionManager.getSession() != null ? SessionManager.getSession().getUserId() : null;

        if (authToken == null || ngoId == null) {
            showAlert("Authentication Error", "Could not verify user. Please log in again.");
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/food-requests/ngo/" + ngoId))
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
                    // Convert the JSON array from the server into a List of DTOs
                    List<FoodRequestDto> requests = objectMapper.readValue(response.body(), new TypeReference<>() {});
                    // Populate the table with the fetched data
                    requestsTableView.setItems(FXCollections.observableArrayList(requests));
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Application Error", "Could not parse request history from the server.");
                }
            } else {
                showAlert("Server Error", "Failed to load request history. Status: " + response.statusCode());
            }
        });
    }

    private Void handleConnectionError(Throwable e) {
        Platform.runLater(() -> {
            System.err.println("Connection Error: " + e.getMessage());
            showAlert("Connection Error", "Could not connect to the server to fetch your requests.");
        });
        return null;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}