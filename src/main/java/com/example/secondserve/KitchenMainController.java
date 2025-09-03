package com.example.secondserve;

import com.example.secondserve.dto.FoodItemDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

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
    @FXML private VBox todaysItemsContainer;

    private boolean isFirstItemLogged = true;

    // For making HTTP requests to your backend server
    private final HttpClient httpClient = HttpClient.newHttpClient();
    // For converting our Java object to a JSON string
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @FXML
    public void initialize() {
        categoryComboBox.setItems(FXCollections.observableArrayList("Prepared Food", "Ingredients"));
        conditionComboBox.setItems(FXCollections.observableArrayList("Fresh", "Good", "Near Expiry"));
    }

    /**
     * Called when the "Log Leftover Item" button is clicked.
     * This method orchestrates the entire process.
     */
    @FXML
    private void handleLogLeftoverItem() {
        // --- 1. Gather & Validate Data ---
        String foodName = foodItemField.getText();
        String category = categoryComboBox.getValue();
        String condition = conditionComboBox.getValue();

        // Basic validation
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

        // --- 2. Create the Data Object (DTO) ---
        FoodItemDTO foodItem = new FoodItemDTO();
        foodItem.setFoodName(foodName);
        foodItem.setQuantity(quantity);
        foodItem.setCategory(category);
        foodItem.setCondition(condition);
        foodItem.setDescription(notesArea.getText());
        foodItem.setExpiryDate(calculateExpiryDate(condition));

        // --- 3. Send the Data to the Server (Asynchronously) ---
        // This runs on a background thread so the UI doesn't freeze.
        sendDataToServer(foodItem);
    }

    private void sendDataToServer(FoodItemDTO foodItem) {
        try {
            // Convert our Java object into a JSON string
            String requestBody = objectMapper.writeValueAsString(foodItem);

            // Build the HTTP POST request to your server's endpoint
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/food-items")) // IMPORTANT: Use your server's URL
                    .header("Content-Type", "application/json")
                    // .header("Authorization", "Bearer YOUR_JWT_TOKEN") // You will need this for security!
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Send the request asynchronously
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        // This code runs when the server responds
                        Platform.runLater(() -> {
                            if (response.statusCode() == 201) { // 201 Created is a typical success response
                                showAlert(Alert.AlertType.INFORMATION, "Success", "Food item logged successfully!");
                                updateLoggedItemsList(foodItem.getFoodName(), foodItem.getQuantity());
                                clearForm();
                            } else {
                                // Show an error message if something went wrong on the server
                                showAlert(Alert.AlertType.ERROR, "Server Error", "Failed to log item. Status code: " + response.statusCode());
                            }
                        });
                    }).exceptionally(e -> {
                        // This code runs if the connection fails entirely
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Connection Error", "Could not connect to the server. Please check if it is running.");
                        });
                        return null;
                    });

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Calculates an estimated expiry date based on the selected condition.
     */
    private LocalDate calculateExpiryDate(String condition) {
        LocalDate today = LocalDate.now();
        switch (condition) {
            case "Fresh": return today.plusDays(3);
            case "Good": return today.plusDays(2);
            case "Near Expiry": return today.plusDays(1);
            default: return today;
        }
    }

    private void updateLoggedItemsList(String name, double quantity) {
        if (isFirstItemLogged) {
            todaysItemsContainer.getChildren().clear();
            isFirstItemLogged = false;
        }
        Label newItemLabel = new Label(name + " - " + quantity + " kg");
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