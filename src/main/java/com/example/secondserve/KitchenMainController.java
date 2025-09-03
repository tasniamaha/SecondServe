package com.example.secondserve;

import com.example.secondserve.dto.FoodItemDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

public class KitchenMainController {

    // --- FXML UI Components ---
    @FXML private TextField foodItemField;
    @FXML private TextField quantityField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> conditionComboBox;
    @FXML private TextArea notesArea;
    @FXML private Button logLeftoverButton; // Link to the button
    @FXML private VBox todaysItemsContainer;

    private boolean isFirstItemLogged = true;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @FXML
    public void initialize() {
        categoryComboBox.setItems(FXCollections.observableArrayList("Prepared Food", "Ingredients"));
        conditionComboBox.setItems(FXCollections.observableArrayList("Fresh", "Good", "Near Expiry"));
    }

    @FXML
    private void handleLogLeftoverItem() {
        // --- 1. Validate Form Inputs ---
        String foodName = foodItemField.getText();
        String category = categoryComboBox.getValue();
        String condition = conditionComboBox.getValue();

        if (foodName.trim().isEmpty() || category == null || condition == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please fill in Food Item, Category, and Condition.");
            return;
        }

        double quantity;
        try {
            quantity = Double.parseDouble(quantityField.getText());
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter a valid, positive quantity.");
            return;
        }

        // --- 2. Create the Data Transfer Object (DTO) ---
        FoodItemDto foodItem = new FoodItemDto();
        foodItem.setFoodName(foodName);
        foodItem.setQuantity(quantity);
        foodItem.setDescription(notesArea.getText());
        foodItem.setExpiryDate(calculateExpiryDate(condition));

        // --- 3. IMPORTANT: Convert UI text to the server's ENUM format ---
        foodItem.setCategory(category.toUpperCase().replace(" ", "_")); // "Prepared Food" -> "PREPARED_FOOD"
        foodItem.setCondition(condition.toUpperCase().replace(" ", "_")); // "Near Expiry" -> "NEAR_EXPIRY"
        foodItem.setUnit("kg"); // Set a default unit for now

        // --- 4. Send the Data to the Server Asynchronously ---
        sendDataToServer(foodItem);
    }

    private void sendDataToServer(FoodItemDto foodItem) {
        // --- Get the authorization token from the session ---
        String authToken = SessionManager.getAuthToken();
        if (authToken == null) {
            showAlert(Alert.AlertType.ERROR, "Authentication Error", "You are not logged in. Please restart the application.");
            return;
        }

        try {
            String requestBody = objectMapper.writeValueAsString(foodItem);

            // --- Build the HTTP request with the Authorization header ---
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/food-items"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", authToken) // Add the JWT token here
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            logLeftoverButton.setDisable(true); // Prevent double submission

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(this::handleServerResponse)
                    .exceptionally(this::handleConnectionError);

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred while preparing the request.");
            e.printStackTrace();
            logLeftoverButton.setDisable(false);
        }
    }

    private void handleServerResponse(HttpResponse<String> response) {
        Platform.runLater(() -> {
            if (response.statusCode() == 201) { // 201 Created
                showAlert(Alert.AlertType.INFORMATION, "Success", "Food item has been logged and is awaiting manager approval.");
                // We don't know the exact quantity string here without parsing the response,
                // so we will just use the name for the UI list.
                updateLoggedItemsList(foodItemField.getText());
                clearForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Server Error", "Failed to log item. Status: " + response.statusCode() + ". Check server logs.");
            }
            logLeftoverButton.setDisable(false);
        });
    }

    private Void handleConnectionError(Throwable e) {
        Platform.runLater(() -> {
            showAlert(Alert.AlertType.ERROR, "Connection Error", "Could not connect to the server. Please check if it is running.");
            logLeftoverButton.setDisable(false);
        });
        return null;
    }

    private LocalDate calculateExpiryDate(String condition) {
        LocalDate today = LocalDate.now();
        switch (condition) {
            case "Fresh": return today.plusDays(3);
            case "Good": return today.plusDays(2);
            case "Near Expiry": return today.plusDays(1);
            default: return today;
        }
    }

    private void updateLoggedItemsList(String name) {
        if (isFirstItemLogged) {
            todaysItemsContainer.getChildren().clear();
            isFirstItemLogged = false;
        }
        Label newItemLabel = new Label("Logged: " + name);
        todaysItemsContainer.getChildren().add(newItemLabel);
    }

    private void clearForm() {
        foodItemField.clear();
        quantityField.clear();
        categoryComboBox.setValue(null);
        conditionComboBox.setValue(null);
        notesArea.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}