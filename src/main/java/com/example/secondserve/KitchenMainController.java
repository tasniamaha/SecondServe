package com.example.secondserve;

import com.example.secondserve.dto.AuthResponse;
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
import javafx.geometry.Insets;

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
    @FXML private Label hotelName;
    @FXML private TextField foodItemField;
    @FXML private TextField quantityField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> conditionComboBox;
    @FXML private TextArea notesArea;
    @FXML private Button logLeftoverButton;
    @FXML private VBox todaysItemsContainer;
    @FXML private Label placeholderLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @FXML
    public void initialize() {
        objectMapper.registerModule(new JavaTimeModule());
        AuthResponse session = SessionManager.getSession();
        if (session != null) {

            // Use getOrganizationName() instead of getName()
            hotelName.setText(session.getOrganizationName());
        }
        categoryComboBox.setItems(FXCollections.observableArrayList("Prepared Food", "Ingredients"));
        conditionComboBox.setItems(FXCollections.observableArrayList("Fresh", "Good", "Near Expiry"));

        // Set proper spacing for the container
        todaysItemsContainer.setSpacing(10.0);

        // Load today's logged items from server
        loadTodaysItems();
    }

    private void loadTodaysItems() {
        String authToken = SessionManager.getAuthToken();
        if (authToken == null) return;

        Long hotelId = SessionManager.getSession() != null ? SessionManager.getSession().getUserId() : null;
        if (hotelId == null) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/food-items/hotel/" + hotelId + "/today"))
                .header("Authorization", authToken)
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(this::handleTodaysItemsResponse)
                .exceptionally(e -> {
                    Platform.runLater(() -> System.out.println("Could not load today's items: " + e.getMessage()));
                    return null;
                });
    }

    private void handleTodaysItemsResponse(HttpResponse<String> response) {
        Platform.runLater(() -> {
            if (response.statusCode() == 200) {
                try {
                    com.fasterxml.jackson.core.type.TypeReference<java.util.List<FoodItemDto>> typeRef =
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.List<FoodItemDto>>() {};
                    java.util.List<FoodItemDto> items = objectMapper.readValue(response.body(), typeRef);

                    if (items != null && !items.isEmpty()) {
                        // Remove placeholder
                        if (placeholderLabel != null) {
                            todaysItemsContainer.getChildren().remove(placeholderLabel);
                        }
                        todaysItemsContainer.setAlignment(javafx.geometry.Pos.TOP_LEFT);

                        // Add all items (newest first)
                        for (FoodItemDto item : items) {
                            HBox card = createLoggedItemCard(item);
                            todaysItemsContainer.getChildren().add(card);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error parsing today's items: " + e.getMessage());
                }
            }
        });
    }

    public void handleLogout(ActionEvent actionEvent) {
        SessionManager.clearSession();
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

        // --- 3. Convert UI text to the server's ENUM format ---
        foodItem.setCategory(category.toUpperCase().replace(" ", "_"));
        foodItem.setCondition(condition.toUpperCase().replace(" ", "_"));
        foodItem.setUnit("kg");

        // --- 4. Send the Data to the Server Asynchronously ---
        sendDataToServer(foodItem);
    }

    private void sendDataToServer(FoodItemDto foodItem) {
        String authToken = SessionManager.getAuthToken();
        if (authToken == null) {
            showAlert("Authentication Error", "You are not logged in. Please restart the application.");
            return;
        }

        try {
            String requestBody = objectMapper.writeValueAsString(foodItem);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/food-items"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", authToken)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            logLeftoverButton.setDisable(true);

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
            if (response.statusCode() == 201) {
                try {
                    FoodItemDto createdItem = objectMapper.readValue(response.body(), FoodItemDto.class);
                    showAlert("Success", "Food item has been logged and is awaiting manager approval.");
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
        // Remove placeholder if it exists
        if (placeholderLabel != null && todaysItemsContainer.getChildren().contains(placeholderLabel)) {
            todaysItemsContainer.getChildren().remove(placeholderLabel);
        }

        // Ensure container is aligned to top
        todaysItemsContainer.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        // Create the new card and add it at the TOP (index 0) for newest-first display
        HBox itemCard = createLoggedItemCard(newItem);
        todaysItemsContainer.getChildren().add(0, itemCard);

        // Debug output
        System.out.println("Total items in container: " + todaysItemsContainer.getChildren().size());
        System.out.println("Container spacing: " + todaysItemsContainer.getSpacing());
    }

    private HBox createLoggedItemCard(FoodItemDto item) {
        // VBox for the main text content
        VBox textContainer = new VBox(5.0);

        // Food name label
        Label nameLabel = new Label(item.getFoodName());
        nameLabel.getStyleClass().add("item-name");

        // Quantity and expiry details
        DateTimeFormatter expiryFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String details = String.format("Quantity: %.2f %s  |  Category: %s  |  Expires: %s",
                item.getQuantity(),
                item.getUnit(),
                formatEnumForDisplay(item.getCategory()),
                item.getExpiryDate().format(expiryFormatter));
        Label detailsLabel = new Label(details);
        detailsLabel.getStyleClass().add("item-details");

        textContainer.getChildren().addAll(nameLabel, detailsLabel);

        // Spacer to push timestamp to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Timestamp label
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        Label timestampLabel = new Label("Logged at " + item.getLoggedAt().format(timeFormatter));
        timestampLabel.getStyleClass().add("item-details");

        // Main card container
        HBox card = new HBox(15.0, textContainer, spacer, timestampLabel);
        card.getStyleClass().add("logged-item-card");
        card.setPadding(new Insets(15, 20, 15, 20));

        return card;
    }

    // Helper method to convert enum values back to readable format
    private String formatEnumForDisplay(String enumValue) {
        if (enumValue == null) return "";
        String[] words = enumValue.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    private void clearForm() {
        foodItemField.clear();
        quantityField.clear();
        categoryComboBox.setValue(null);
        conditionComboBox.setValue(null);
        notesArea.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}