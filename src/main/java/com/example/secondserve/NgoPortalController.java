package com.example.secondserve;

import com.example.secondserve.dto.AuthResponse;
import com.example.secondserve.dto.HotelDto;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
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
import java.util.List;
import java.util.Objects;

public class NgoPortalController {

    @FXML private Button logoutButton;
    @FXML private Label NGO_name;
    @FXML private VBox hotelCardsContainer;
    @FXML private BorderPane mainBorderPane;
    @FXML private Button browseHotelsButton;
    @FXML private Button myRequestsButton;
    @FXML private Button ngoProfileButton;

    private Node initialBrowseView;
    private Button activeButton;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @FXML
    public void initialize() {
        objectMapper.registerModule(new JavaTimeModule());
        AuthResponse session = SessionManager.getSession();
        if (session != null) {

            // Use getOrganizationName() instead of getName()
            NGO_name.setText(session.getOrganizationName());
        }
        this.initialBrowseView = mainBorderPane.getCenter();
        setActiveButton(browseHotelsButton);
        loadHotelsList(); // Call the simplified data loading method
    }

    // --- View Navigation ---
    @FXML
    void showBrowseHotelsView() {
        mainBorderPane.setCenter(initialBrowseView); // Restore the VBox container
        setActiveButton(browseHotelsButton);
        loadHotelsList(); // Refresh the list of hotels
    }
    @FXML private void showMyRequestsView() { loadView("MyRequestsView.fxml"); setActiveButton(myRequestsButton); }
    @FXML private void showNgoProfileView() { loadView("NgoProfileView.fxml"); setActiveButton(ngoProfileButton); }

    private void loadView(String fxmlFile) { /* ... (Unchanged) ... */ }
    private void setActiveButton(Button button) { /* ... (Unchanged) ... */ }

    // --- Data Loading and UI Building (Simplified) ---

    private void loadHotelsList() {
        String authToken = SessionManager.getAuthToken();
        if (authToken == null) return;

        // Use the existing, simple endpoint that returns all active hotels
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/hotels"))
                .header("Authorization", authToken)
                .GET().build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(this::handleHotelListResponse)
                .exceptionally(this::handleConnectionError);
    }

    private void handleHotelListResponse(HttpResponse<String> response) {
        Platform.runLater(() -> {
            if (response.statusCode() == 200) {
                try {
                    List<HotelDto> hotels = objectMapper.readValue(response.body(), new TypeReference<>() {});
                    displayHotelCards(hotels);
                } catch (IOException e) {
                    showAlert("Application Error", "Failed to parse hotel list.");
                }
            } else {
                showAlert("Server Error", "Failed to load hotels. Status: " + response.statusCode());
            }
        });
    }

    private void displayHotelCards(List<HotelDto> hotels) {

        hotelCardsContainer.getChildren().clear(); // Clear placeholder or old cards

        if (hotels == null || hotels.isEmpty()) {
            Label placeholder = new Label("No hotels are currently available for donation.");
            placeholder.getStyleClass().add("placeholder-text");
            hotelCardsContainer.getChildren().add(placeholder);
        } else {
            for (HotelDto hotel : hotels) {
                HBox card = createHotelCard(hotel);
                hotelCardsContainer.getChildren().add(card);
            }
        }
    }


    // In: NgoPortalController.java

    private HBox createHotelCard(HotelDto hotel) {
        // 1. Hotel Name (Title)
        Label nameLabel = new Label(hotel.getHotelName());
        nameLabel.getStyleClass().add("hotel-card-title");

        // 2. Hotel Location Details (New)
        // Combines city and state for a cleaner look. Handles nulls gracefully.
        String location = (hotel.getCity() != null ? hotel.getCity() : "") +
                (hotel.getState() != null ? ", " + hotel.getState() : "");
        Label detailsLabel = new Label(location);
        detailsLabel.getStyleClass().add("hotel-card-details");

        // 3. Vertical container for the text
        VBox textContainer = new VBox(5, nameLabel, detailsLabel); // 5px spacing between title and details
        textContainer.setAlignment(Pos.CENTER_LEFT);

        // 4. Spacer to push the button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 5. The "View" button
        Button viewButton = new Button("View Available Food");
        viewButton.getStyleClass().add("view-details-button");
        viewButton.setOnAction(event -> navigateToHotelDetails(hotel.getId()));

        // 6. The main HBox for the card
        HBox card = new HBox(textContainer, spacer, viewButton);
        card.getStyleClass().add("hotel-card"); // This class now has styles!
        card.setAlignment(Pos.CENTER); // Vertically align content in the card

        return card;
    }

    private void navigateToHotelDetails(long hotelId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/secondserve/HotelDonationView.fxml"));
            Parent root = loader.load();

            HotelDonationViewController controller = loader.getController();
            controller.initData(hotelId, this);

            mainBorderPane.setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not load the hotel details view.");
        }
    }

    private Void handleConnectionError(Throwable e) {
        Platform.runLater(() -> showAlert("Connection Error", "Could not connect to the server."));
        return null;
    }

    private void showAlert(String title, String message) { /* ... (Unchanged) ... */ }

    public void handleLogout(ActionEvent actionEvent) {
        // 1. Clear the stored session data
        SessionManager.clearSession();

        // 2. Navigate back to the Login screen
        navigateToLogin();
    }

    private void navigateToLogin() {
        try {
            // Load the LoginView.fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/secondserve/opening-view.fxml"));
            Parent root = loader.load();

            // Get the current stage (window) from any node in the current scene
            Stage stage = (Stage) mainBorderPane.getScene().getWindow();

            // Create a new scene with the login view
            Scene scene = new Scene(root);

            // Set the new scene on the stage and show it
            stage.setScene(scene);
            stage.setTitle("SecondServe - Login"); // Optional: Reset the window title
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not load the login screen.");
        }
    }
}