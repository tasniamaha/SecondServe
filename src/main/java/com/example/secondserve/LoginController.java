package com.example.secondserve;

import com.example.secondserve.dto.AuthResponse;
import com.example.secondserve.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

public class LoginController {

    // --- FXML UI Components ---
    @FXML private ImageView logoImageView;
    @FXML private Label roleName;
    @FXML private Button backButton;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink signUpLink;

    private String userType;

    // --- For making API calls ---
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initializes the controller, called automatically after FXML is loaded.
     */
    @FXML
    public void initialize() {
        try {
            logoImageView.setImage(loadImage("/assets/SecondServe_logo.png"));
        } catch (Exception e) {
            System.err.println("Failed to load logo image: " + e.getMessage());
        }
    }

    /**
     * Helper method to load images safely.
     */
    private Image loadImage(String path) {
        return new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
    }

    /**
     * Receives the user type from the previous (RoleSelection) screen and updates the UI.
     */
    public void setUserType(String userType) {
        this.userType = userType;
        if (roleName != null) {
            switch (userType) {
                case "KITCHEN_STAFF":
                    roleName.setText("Login as Kitchen Staff");
                    break;
                case "HOTEL_MANAGER":
                    roleName.setText("Login as Hotel Manager");
                    break;
                case "NGO":
                    roleName.setText("Login as NGO Representative");
                    break;
            }
        }
    }

    /**
     * Handles the login button click, calling the backend server for authentication.
     */
    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.trim().isEmpty() || password.isEmpty()) {
            showAlert("Input Error", "Please enter both email and password.");
            return;
        }

        LoginRequest loginPayload = new LoginRequest(email, password, this.userType);

        try {
            String requestBody = objectMapper.writeValueAsString(loginPayload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/auth/login")) // Your server's endpoint
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            loginButton.setDisable(true); // Prevent user from clicking multiple times

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> handleServerResponse(response, event))
                    .exceptionally(this::handleConnectionError);

        } catch (IOException e) {
            showAlert("Application Error", "An unexpected error occurred. Please try again.");
            e.printStackTrace();
        }
    }

    /**
     * Processes the HTTP response from the server after a login attempt.
     */
    private void handleServerResponse(HttpResponse<String> response, ActionEvent event) {
        Platform.runLater(() -> {
            if (response.statusCode() == 200) {
                try {
                    AuthResponse authResponse = objectMapper.readValue(response.body(), AuthResponse.class);
                    SessionManager.createSession(authResponse); // Save user token and info
                    showAlert("Success", "Login successful! Welcome, " + authResponse.getName() + ".");
                    navigateToDashboard(event);
                } catch (IOException e) {
                    showAlert("Application Error", "Could not process the server's response.");
                }
            } else {
                showAlert("Login Failed", "Invalid email, password, or role. Please try again.");
            }
            loginButton.setDisable(false); // Re-enable the button
        });
    }

    /**
     * Handles network errors if the server cannot be reached.
     */
    private Void handleConnectionError(Throwable e) {
        Platform.runLater(() -> {
            System.err.println("Connection Error: " + e.getMessage());
            showAlert("Connection Error", "Could not connect to the server. Please ensure it is running.");
            loginButton.setDisable(false);
        });
        return null;
    }

    /**
     * Navigates to the appropriate dashboard based on the user's role from the session.
     */
    private void navigateToDashboard(ActionEvent event) {
        AuthResponse session = SessionManager.getSession();
        if (session == null) return;

        String fxmlFile = null;
        String title = "SecondServe";

        switch (session.getUserType()) {
            case "KITCHEN_STAFF": fxmlFile = "KitchenMain.fxml"; title = "Kitchen Interface"; break;
            case "HOTEL_MANAGER": fxmlFile = "HotelDashboard.fxml"; title = "Hotel Dashboard"; break;
            case "NGO": fxmlFile = "NgoPortal.fxml"; title = "NGO Portal"; break;
            default:
                showAlert("Error", "Unknown user role: " + session.getUserType());
                return;
        }

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/secondserve/" + fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(title);
            stage.centerOnScreen();
        } catch (IOException e) {
            showAlert("Navigation Error", "Could not load the dashboard view: " + fxmlFile);
            e.printStackTrace();
        }
    }

    /**
     * Handles the "Sign up" hyperlink click, navigating to the registration view.
     */
    @FXML
    private void handleSignUp(ActionEvent event) {
        navigateToSignUp(event);
    }

    /**
     * Handles the "Back" button click, navigating to the role selection view.
     */
    @FXML
    private void handleBackButton(ActionEvent event) {
        navigateToRoleSelection(event);
    }

    /**
     * A helper method for navigating to the Sign-Up screen while preserving window state.
     */
    private void navigateToSignUp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/secondserve/signup-fxml.fxml"));
            Parent signupRoot = loader.load();

            SignUpController signUpController = loader.getController();
            signUpController.setUserType(this.userType); // Pass the current role to the signup screen

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(new Scene(signupRoot, currentStage.getScene().getWidth(), currentStage.getScene().getHeight()));
            currentStage.setTitle("SecondServe - Sign Up");

        } catch (IOException e) {
            showAlert("Navigation Error", "Cannot load the signup page.");
            e.printStackTrace();
        }
    }

    /**
     * A helper method for navigating to the Role Selection screen while preserving window state.
     */
    private void navigateToRoleSelection(ActionEvent event) {
        try {
            Parent roleSelectionRoot = FXMLLoader.load(getClass().getResource("/com/example/secondserve/opening-view.fxml"));
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(new Scene(roleSelectionRoot, currentStage.getScene().getWidth(), currentStage.getScene().getHeight()));
            currentStage.setTitle("SecondServe - Choose Your Role");

        } catch (IOException e) {
            showAlert("Navigation Error", "Cannot load the role selection page.");
            e.printStackTrace();
        }
    }

    /**
     * A utility method for showing alerts to the user.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}