package com.example.secondserve;

import com.example.secondserve.dto.FoodItemDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import com.fasterxml.jackson.databind.DeserializationFeature;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class KitchenMainController {

    @FXML private Button logoutButton;

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
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @FXML
    public void initialize() {
        categoryComboBox.setItems(FXCollections.observableArrayList("Prepared Food", "Ingredients"));
        conditionComboBox.setItems(FXCollections.observableArrayList("Fresh", "Good", "Near Expiry"));
    }
    public void handleLogout(ActionEvent actionEvent) {
        SessionManager.clearSession(); // Clears the stored JWT token and user info
        navigateToScene((Node) actionEvent.getSource(), "opening-view.fxml", "SecondServe - Choose Your Role");
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

    @FXML
    private void handleLogLeftoverItem() {
        // --- 1. Validate Form Inputs ---
        String foodName = foodItemField.getText();
        String category = categoryComboBox.getValue();
        String condition = conditionComboBox.getValue();

        if (foodName.trim().isEmpty() || category == null || condition == null) {
            showAlert("Validation Error", "Please fill in Food Item, Category, and Condition.");
            return;
        }

        double quantity;
        try {
            quantity = Double.parseDouble(quantityField.getText());
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter a valid, positive quantity.");
            return;
        }

        // --- 2. Create the Data Transfer Object (DTO) ---
        FoodItemDto foodItem = new FoodItemDto();
        foodItem.setFoodName(foodName);
        foodItem.setQuantity(BigDecimal.valueOf(quantity));
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
            showAlert("Authentication Error", "You are not logged in. Please restart the application.");
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
            showAlert("Error", "An unexpected error occurred while preparing the request.");
            e.printStackTrace();
            logLeftoverButton.setDisable(false);
        }
    }

    private void handleServerResponse(HttpResponse<String> response) {
        Platform.runLater(() -> {
            if (response.statusCode() == 201) { // 201 Created
                try {
                    // STEP 1: Parse the full FoodItemDto object from the server's response
                    FoodItemDto createdItem = objectMapper.readValue(response.body(), FoodItemDto.class);

                    showAlert("Success", "Food item has been logged and is awaiting manager approval.");

                    // STEP 2: Pass the complete object to the UI update method
                    updateLoggedItemsList(createdItem);
                    clearForm();

                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Parsing Error", "Successfully logged item, but could not parse the server's response.");
                }
            } else {
                showAlert("Server Error", "Failed to log item. Status: " + response.statusCode() + ". Check server logs.");
            }
            logLeftoverButton.setDisable(false);
        });
    }


    private Void handleConnectionError(Throwable e) {
        Platform.runLater(() -> {
            showAlert("Connection Error", "Could not connect to the server. Please check if it is running.");
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

    private void updateLoggedItemsList(FoodItemDto newItem) {
        if (isFirstItemLogged) {
            todaysItemsContainer.getChildren().clear();
            // Align items to the top instead of the center once there's content
            todaysItemsContainer.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            isFirstItemLogged = false;
        }

        // Create the new detailed card and add it to the container
        HBox itemCard = createLoggedItemCard(newItem);
        todaysItemsContainer.getChildren().add(itemCard);
    }

    // --- NEW HELPER METHOD ---
    /**
     * Creates a styled HBox to display the details of a logged food item.
     */
    // --- CORRECTED HELPER METHOD ---
    /**
     * Creates a styled HBox to display the details of a logged food item.
     */
    private HBox createLoggedItemCard(FoodItemDto item) {
        // VBox for the main text content (name, quantity, expiry)
        VBox textContainer = new VBox(5.0); // 5px spacing between text elements

        // Label for the food name (bold and larger)
        Label nameLabel = new Label(item.getFoodName());
        nameLabel.getStyleClass().add("item-name");

        // Label for quantity and expiry details (smaller and grey)
        DateTimeFormatter expiryFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String details = String.format("Quantity: %.2f %s  |  Expires: %s",
                item.getQuantity(),
                item.getUnit(),
                item.getExpiryDate().format(expiryFormatter));
        Label detailsLabel = new Label(details);
        detailsLabel.getStyleClass().add("item-details");

        textContainer.getChildren().addAll(nameLabel, detailsLabel);

        // A spacer to push the timestamp to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Label for the timestamp
        // THIS IS THE LINE THAT WAS CAUSING THE CRASH. Corrected pattern is "hh:mm a"
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        Label timestampLabel = new Label("Logged at " + item.getLoggedAt().format(timeFormatter));
        timestampLabel.getStyleClass().add("item-details");

        // The main HBox container for the card
        HBox card = new HBox(textContainer, spacer, timestampLabel);
        card.getStyleClass().add("logged-item-card");

        return card;
    }

    private void clearForm() {
        foodItemField.clear();
        quantityField.clear();
        categoryComboBox.setValue(null);
        conditionComboBox.setValue(null);
        notesArea.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}