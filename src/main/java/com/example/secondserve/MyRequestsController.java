package com.example.secondserve;

import com.example.secondserve.dto.FoodRequestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MyRequestsController {

    @FXML private TableColumn<FoodRequestDto, Void> actionColumn;
    @FXML private TableColumn<FoodRequestDto, String> quantityColumn;

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

        // Food item name only
        foodItemColumn.setCellValueFactory(new PropertyValueFactory<>("foodItemName"));

        // NEW: Separate quantity column
        quantityColumn.setCellValueFactory(cellData -> {
            FoodRequestDto request = cellData.getValue();
            String displayText = String.format("%.2f %s",
                    request.getRequestedQuantity(),
                    request.getUnit());
            return new SimpleStringProperty(displayText);
        });

        dateColumn.setCellValueFactory(cellData -> {
            LocalDateTime dateTime = cellData.getValue().getRequestDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            return new SimpleStringProperty(dateTime.format(formatter));
        });



        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (status == null || empty) {
                    setGraphic(null);
                } else {
                    Label statusLabel = new Label(status);
                    statusLabel.getStyleClass().add("status-label");
                    statusLabel.getStyleClass().add("status-" + status.toLowerCase());
                    setGraphic(statusLabel);
                }
            }
        });
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("requestStatus"));
        setupActionColumn();
    }

    private void setupActionColumn() {
        Callback<TableColumn<FoodRequestDto, Void>, TableCell<FoodRequestDto, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<FoodRequestDto, Void> call(final TableColumn<FoodRequestDto, Void> param) {
                return new TableCell<>() {
                    private final Button completeButton = new Button("Complete");
                    {
                        completeButton.getStyleClass().add("complete-button"); // Apply CSS
                        completeButton.setOnAction(event -> {
                            FoodRequestDto request = getTableView().getItems().get(getIndex());
                            handleCompleteDonation(request.getId());
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            FoodRequestDto request = getTableView().getItems().get(getIndex());
                            // Only show the button if the status is 'APPROVED'
                            if ("APPROVED".equalsIgnoreCase(request.getRequestStatus())) {
                                setGraphic(completeButton);
                                setAlignment(Pos.CENTER);
                            } else {
                                setGraphic(null);
                            }
                        }
                    }
                };
            }
        };
        actionColumn.setCellFactory(cellFactory);
    }

    private void handleCompleteDonation(Long requestId) {
        String authToken = SessionManager.getAuthToken();
        if (authToken == null) {
            showAlert("Authentication Error", "Could not verify user. Please log in again.");
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/food-requests/" + requestId + "/complete"))
                .header("Authorization", authToken)
                .PUT(HttpRequest.BodyPublishers.noBody()) // Using PUT to update the resource state
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        Platform.runLater(this::loadMyRequests); // Refresh the table on success
                    } else {
                        Platform.runLater(() -> showAlert("Update Failed", "Could not complete the donation. Server responded with status: " + response.statusCode()));
                    }
                })
                .exceptionally(this::handleConnectionError);
    }

    /**
     * Fetches the request history for the logged-in NGO from the server.
     */
    private void loadMyRequests() {
        String authToken = SessionManager.getAuthToken();
        Long ngoId = SessionManager.getSession() != null ? SessionManager.getSession().getUserId() : null;
        System.out.println("Loading requests for NGO ID: " + ngoId);
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
            System.out.println("Response Status: " + response.statusCode());
            System.out.println("Response Body: " + response.body());

            if (response.statusCode() == 200) {
                try {
                    List<FoodRequestDto> requests = objectMapper.readValue(response.body(), new TypeReference<>() {});
                    System.out.println("Parsed " + requests.size() + " requests");
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