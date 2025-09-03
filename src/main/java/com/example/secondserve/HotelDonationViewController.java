package com.example.secondserve;

import com.example.secondserve.dto.FoodItemDto;
import com.example.secondserve.dto.FoodRequestDto;
import com.example.secondserve.dto.HotelDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HotelDonationViewController {

    // --- FXML UI Components ---
    @FXML private Label hotelNameSubtitle, hotelNameLabel, hotelAddressLabel, hotelContactLabel;
    @FXML private TableView<FoodItemDto> foodItemsTableView;
    @FXML private TableColumn<FoodItemDto, String> itemNameColumn;
    @FXML private TableColumn<FoodItemDto, String> quantityColumn; // Display as String for formatting
    @FXML private TableColumn<FoodItemDto, String> conditionColumn;
    @FXML private TableColumn<FoodItemDto, LocalDate> expiryDateColumn;
    @FXML private TableColumn<FoodItemDto, Void> requestColumn;

    private long hotelId;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private NgoPortalController mainPortalController; // To handle "back" navigation

    /**
     * Called by the main portal controller to pass in data and a reference to itself.
     */
    public void initData(long hotelId, NgoPortalController mainPortalController) {
        this.hotelId = hotelId;
        this.mainPortalController = mainPortalController;
        setupTableColumns();
        loadHotelDetails();
        loadAvailableFoodItems();
    }

    private void setupTableColumns() {
        itemNameColumn.setCellValueFactory(new PropertyValueFactory<>("foodName"));
        conditionColumn.setCellValueFactory(new PropertyValueFactory<>("condition"));
        expiryDateColumn.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));

        // Custom cell factory to format quantity with its unit
        quantityColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getQuantity() + " " + cellData.getValue().getUnit()
                )
        );

        // --- The CellFactory for the dynamic "Request" button ---
        requestColumn.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Request");
            {
                btn.getStyleClass().add("request-button");
                btn.setOnAction(event -> {
                    FoodItemDto foodItem = getTableView().getItems().get(getIndex());
                    handleRequestItem(foodItem, btn); // Pass the button itself
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    /**
     * This method handles the logic for sending a donation request to the server.
     */
    private void handleRequestItem(FoodItemDto foodItem, Button button) {
        String authToken = SessionManager.getAuthToken();
        if (authToken == null) {
            showAlert("Authentication Error", "You are not logged in.");
            return;
        }

        // The user requests the full available amount for simplicity
        FoodRequestDto requestPayload = new FoodRequestDto();
        requestPayload.setFoodItemId(foodItem.getId());
        requestPayload.setRequestedQuantity(foodItem.getQuantity());

        try {
            String requestBody = objectMapper.writeValueAsString(requestPayload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/food-requests"))
                    .header("Authorization", authToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            button.setDisable(true); // Disable button immediately to prevent double-clicks

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 201) { // 201 Created
                            // --- SUCCESS: Update the UI to show "Pending" ---
                            button.setText("Pending");
                            button.getStyleClass().remove("request-button");
                            button.getStyleClass().add("pending-button");
                        } else {
                            // The request failed (e.g., item was just taken)
                            showAlert("Request Failed", "This item might no longer be available. Please refresh the list.");
                            button.setDisable(false); // Re-enable the button on failure
                        }
                    })).exceptionally(e -> {
                        // Handle network failure
                        Platform.runLater(() -> {
                            showAlert("Connection Error", "Could not send the request. Please check your connection.");
                            button.setDisable(false);
                        });
                        return null;
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Paste these full methods into your HotelDonationViewController.java file

    /**
     * Fetches the public details of the selected hotel from the server and populates the
     * top information card in the UI.
     */
    private void loadHotelDetails() {
        String authToken = SessionManager.getAuthToken();
        if (authToken == null) {
            showAlert("Authentication Error", "You are not logged in. Please restart and log in again.");
            return;
        }

        // Build the request to get a specific hotel's details
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/hotels/" + this.hotelId))
                .header("Authorization", authToken)
                .GET()
                .build();

        // Send the request asynchronously
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    // When the server responds, update the UI on the JavaFX Application Thread
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            try {
                                // Convert the JSON response into a HotelDto object
                                HotelDto hotel = objectMapper.readValue(response.body(), HotelDto.class);

                                // Update the UI labels with the fetched data
                                hotelNameSubtitle.setText("From " + hotel.getHotelName());
                                hotelNameLabel.setText(hotel.getHotelName());
                                hotelAddressLabel.setText(hotel.getAddress());
                                hotelContactLabel.setText("Contact: " + (hotel.getPhone() != null ? hotel.getPhone() : "Not provided"));

                            } catch (IOException e) {
                                e.printStackTrace();
                                showAlert("Application Error", "Failed to parse hotel details from the server.");
                            }
                        } else {
                            showAlert("Server Error", "Could not load hotel details. Status: " + response.statusCode());
                        }
                    });
                }).exceptionally(this::handleConnectionError);
    }

    /**
     * Fetches the list of currently available food items from the selected hotel
     * and populates the TableView in the UI.
     */
    private void loadAvailableFoodItems() {
        String authToken = SessionManager.getAuthToken();
        if (authToken == null) return; // Error was already shown in the other method

        // Build the request to get food items for a specific hotel
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/food-items/hotel/" + this.hotelId))
                .header("Authorization", authToken)
                .GET()
                .build();

        // Send the request asynchronously
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    // Update the UI on the JavaFX Application Thread
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            try {
                                // Convert the JSON array response into a List of FoodItemDto objects
                                List<FoodItemDto> foodItems = objectMapper.readValue(response.body(), new TypeReference<>() {});

                                // Populate the TableView with the fetched list
                                foodItemsTableView.setItems(FXCollections.observableArrayList(foodItems));
                            } catch (IOException e) {
                                e.printStackTrace();
                                showAlert("Application Error", "Failed to parse food item list from the server.");
                            }
                        } else {
                            showAlert("Server Error", "Could not load food items. Status: " + response.statusCode());
                        }
                    });
                }).exceptionally(this::handleConnectionError);
    }

    /**
     * Handles network errors if the server cannot be reached.
     */
    private Void handleConnectionError(Throwable e) {
        Platform.runLater(() -> {
            System.err.println("Connection Error: " + e.getMessage());
            showAlert("Connection Error", "Could not connect to the server. Please ensure it is running and accessible.");
        });
        return null;
    }

    /**
     * A utility method to show alerts.
     */


    @FXML
    public void handleGoBack(ActionEvent actionEvent) {
        // Use the reference to the main controller to go back to the hotel list view
        if (mainPortalController != null) {
            mainPortalController.showBrowseHotelsView();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}