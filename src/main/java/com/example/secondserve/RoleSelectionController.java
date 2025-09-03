package com.example.secondserve;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public class RoleSelectionController {

    @FXML private ImageView ngoIcon;
    @FXML private ImageView hotelManagerIcon;
    @FXML private ImageView kitchenStaffIcon;
    @FXML private Button ngoButton;
    @FXML private Button hotelManagerButton;
    @FXML private Button kitchenStaffButton;
    @FXML private ImageView logoImageView;

    public void initialize() {
        try {
            // Load and set ALL images here
            logoImageView.setImage(loadImage("/assets/SecondServe_logo.png"));
            kitchenStaffIcon.setImage(loadImage("/assets/kitchen_staff_logo.png"));
            hotelManagerIcon.setImage(loadImage("/assets/hotel_manager_logo.png"));
            ngoIcon.setImage(loadImage("/assets/ngo_logo.png"));

        } catch (Exception e) {
            System.err.println("Failed to load images from controller!");
            e.printStackTrace();
        }
    }

    private Image loadImage(String path) {
        return new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
    }

    public void handleKitchenStaff(ActionEvent actionEvent) {
        navigateToLogin("KITCHEN_STAFF", actionEvent);
    }

    public void handleHotelManager(ActionEvent actionEvent) {
        navigateToLogin("HOTEL_MANAGER", actionEvent);
    }

    public void handleNgoRepresentative(ActionEvent actionEvent) {
        navigateToLogin("NGO", actionEvent);
    }

    private void navigateToLogin(String userType, ActionEvent event) {
        try {
            URL fxmlUrl = getClass().getResource("/com/example/secondserve/login-fxml.fxml");

            if (fxmlUrl == null) {
                showAlert("Error", "Login form not found.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent loginRoot = loader.load();

            // Get the current stage and preserve its state
            Button sourceButton = (Button) event.getSource();
            Stage currentStage = (Stage) sourceButton.getScene().getWindow();

            // Store the current window state
            boolean wasMaximized = currentStage.isMaximized();
            double currentWidth = currentStage.getWidth();
            double currentHeight = currentStage.getHeight();
            double currentX = currentStage.getX();
            double currentY = currentStage.getY();

            // Create a new scene with the SAME dimensions as the current scene
            Scene currentScene = sourceButton.getScene();
            Scene loginScene = new Scene(loginRoot, currentScene.getWidth(), currentScene.getHeight());

            // Set the new scene on the current stage
            currentStage.setScene(loginScene);

            // Restore the window state
            if (wasMaximized) {
                currentStage.setMaximized(true);
            } else {
                currentStage.setWidth(currentWidth);
                currentStage.setHeight(currentHeight);
                currentStage.setX(currentX);
                currentStage.setY(currentY);
            }

            currentStage.setTitle("SecondServe - Login");

            // Get the login controller and pass the user type
            LoginController loginController = loader.getController();
            loginController.setUserType(userType);

        } catch (Exception e) {
            System.err.println("Failed to load login page: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Cannot load login page: " + e.getMessage());
        }
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}