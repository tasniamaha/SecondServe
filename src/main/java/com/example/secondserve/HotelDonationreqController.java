package com.example.secondserve;

import com.example.secondserve.dto.FoodRequestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;

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
    @FXML private BorderPane mainBorderPane; // Add this to your FXML root

    // --- API Communication ---
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @FXML
    public void initialize() {
        loadPendingRequests();
    }

    /**
     * Called by the dashboard controller to pass the main BorderPane reference
     */
    public void setMainBorderPane(BorderPane borderPane) {
        this.mainBorderPane = borderPane;
    }

    private void loadPendingRequests() {
        String authToken = SessionManager.getAuthToken();
        Long hotelId = SessionManager.getSession() != null ? SessionManager.getSession().getUserId() : null;
        System.out.println("Attempting to load requests for hotelId: " + hotelId);
        if (authToken == null || hotelId == null) {
            showAlert(Alert.AlertType.ERROR, "Authentication Error", "Could not verify user. Please log in again.");
            requestsContainer.getChildren().clear();
            Label placeholder = new Label("Could not load requests due to an authentication error.");
            placeholder.getStyleClass().add("placeholder-text");
            requestsContainer.getChildren().add(placeholder);
            return;
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/food-requests/hotel/" + hotelId + "?status=PENDING"))
                .header("Authorization", authToken)
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(this::handleRequestsResponse)
                .exceptionally(this::handleConnectionError);
    }

    private void handleRequestsResponse(HttpResponse<String> response) {
        Platform.runLater(() -> {
            System.out.println("Response Status: " + response.statusCode());
            System.out.println("Response Body: " + response.body());

            if (response.statusCode() == 200) {
                try {
                    List<FoodRequestDto> requests = objectMapper.readValue(response.body(), new TypeReference<>() {});
                    System.out.println("Number of requests loaded: " + (requests != null ? requests.size() : 0));
                    displayRequests(requests);
                } catch (IOException e) {
                    System.err.println("Parse error: " + e.getMessage());
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Application Error", "Could not parse the list of requests from the server.");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Server Error", "Could not load donation requests. Status: " + response.statusCode() + "\nBody: " + response.body());
            }
        });
    }

    private void displayRequests(List<FoodRequestDto> requests) {
        requestsContainer.getChildren().clear();
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

    // In HotelDonationreqController.java

    private HBox createRequestCard(FoodRequestDto request) {
        // Create card with consistent spacing
        HBox card = new HBox(15);  // Increased spacing slightly
        card.getStyleClass().add("request-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // 1. NGO Name Label (fixed width for alignment)
        Label ngoNameLabel = new Label(request.getNgoName());
        ngoNameLabel.getStyleClass().add("ngo-name-label");
        ngoNameLabel.setMinWidth(180);
        ngoNameLabel.setPrefWidth(180);

        // 2. Food Details (fixed width)
        String foodDetails = String.format("%.2f %s of %s",
                request.getRequestedQuantity(),
                request.getUnit(),
                request.getFoodItemName());
        Label foodDetailsLabel = new Label(foodDetails);
        foodDetailsLabel.getStyleClass().add("food-details-label");
        foodDetailsLabel.setMinWidth(240);
        foodDetailsLabel.setPrefWidth(240);

        // 3. Request Date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Label dateLabel = new Label("Received: " + request.getRequestDate().format(formatter));
        dateLabel.getStyleClass().add("date-label");

        // 4. Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 5. Buttons
        Button approveButton = new Button("Approve");
        approveButton.getStyleClass().add("approve-button");
        approveButton.setOnAction(event -> handleApprove(request, card));

        Button rejectButton = new Button("Reject");
        rejectButton.getStyleClass().add("reject-button");
        rejectButton.setOnAction(event -> handleReject(request, card));

        card.getChildren().addAll(ngoNameLabel, foodDetailsLabel, dateLabel, spacer, approveButton, rejectButton);

        // Click handler for NGO details
        card.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY &&
                    !(event.getTarget() instanceof Button)) {
                showNgoDetails(request.getNgoId());
            }
        });

        return card;
    }
    /**
     * Navigates to the NGO details view
     */
    private void showNgoDetails(Long ngoId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ngodetailview.fxml"));
            Parent ngoDetailsView = loader.load();

            // Get the controller and pass the necessary data
            NgoDetailsController controller = loader.getController();
            controller.initData(ngoId, mainBorderPane);

            // Replace the current center content with the NGO details view
            if (mainBorderPane != null) {
                mainBorderPane.setCenter(ngoDetailsView);
            } else {
                showAlert(Alert.AlertType.ERROR, "Navigation Error", "Cannot navigate - main pane not set.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load NGO details view.");
        }
    }

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
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        requestsContainer.getChildren().remove(cardNode);
                        showAlert(Alert.AlertType.INFORMATION, "Success", "The request has been " + (action.equals("approve") ? "approved." : "rejected."));
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Could not update the request status. It may have already been processed.");
                    }
                })).exceptionally(this::handleConnectionError);
    }

    // --- Navigation Methods ---

    @FXML
    public void handleShowDashboard(ActionEvent actionEvent) throws IOException {
        Parent dashboardView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("HotelManager_Dashboard.fxml")));
        Scene scene = ((Node) actionEvent.getSource()).getScene();
        scene.setRoot(dashboardView);
    }

    @FXML
    public void handleShowProfile(ActionEvent actionEvent) throws IOException {
        Parent profileView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("HotelManager_Profile.fxml")));
        Scene scene = ((Node) actionEvent.getSource()).getScene();
        scene.setRoot(profileView);
    }

    @FXML
    public void handleShowRequests(ActionEvent actionEvent) {
        loadPendingRequests();
    }

    @FXML
    public void handleLogout(ActionEvent actionEvent) throws IOException {
        SessionManager.clearSession();
        Parent loginView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Login.fxml")));
        Scene scene = ((Node) actionEvent.getSource()).getScene();
        scene.setRoot(loginView);
    }

    private Void handleConnectionError(Throwable e) {
        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Connection Error", "Could not connect to the server."));
        return null;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}