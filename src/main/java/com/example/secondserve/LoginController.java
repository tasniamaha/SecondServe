package com.example.secondserve;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public class LoginController {

    @FXML private ImageView logoImageView;
    @FXML private Label roleName;
    @FXML private Button backButton;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink signUpLink;

    private String userType;

    public void initialize() {
        try {
            logoImageView.setImage(loadImage("/assets/SecondServe_logo.png"));
        } catch (Exception e) {
            System.err.println("Failed to load images from controller!");
            e.printStackTrace();
        }
    }

    private Image loadImage(String path) {
        return new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
    }

    public void setUserType(String userType) {
        this.userType = userType;
        System.out.println("User type set to: " + userType);

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

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please enter both email and password.");
            return;
        }

        System.out.println("Login attempted with email: " + email);

        if (authenticateUser(email, password)) {
            showAlert("Success", "Login successful!");
            // navigateToDashboard();
        } else {
            showAlert("Error", "Invalid email or password.");
        }
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        navigateToSignUp(event);
    }

    @FXML
    private void handleBackButton(ActionEvent event) {
        System.out.println("Back button clicked!");
        navigateToRoleSelection(event);
    }

    private boolean authenticateUser(String email, String password) {
        return !email.isEmpty() && !password.isEmpty();
    }

    private void navigateToSignUp(ActionEvent event) {
        try {
            URL fxmlUrl = getClass().getResource("/com/example/secondserve/signup-fxml.fxml");
            System.out.println("SignUp FXML URL: " + fxmlUrl); // Debug
            if (fxmlUrl == null) {
                showAlert("Error", "Signup form not found.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent signupRoot = loader.load();

            // FIX: Get the source as Hyperlink instead of Button
            Hyperlink sourceLink = (Hyperlink) event.getSource();
            Stage currentStage = (Stage) sourceLink.getScene().getWindow();

            boolean wasMaximized = currentStage.isMaximized();
            double currentWidth = currentStage.getWidth();
            double currentHeight = currentStage.getHeight();
            double currentX = currentStage.getX();
            double currentY = currentStage.getY();

            // Use current scene dimensions
            Scene currentScene = sourceLink.getScene();
            Scene signupScene = new Scene(signupRoot, currentScene.getWidth(), currentScene.getHeight());

            currentStage.setScene(signupScene);

            // Restore window state
            if (wasMaximized) {
                currentStage.setMaximized(true);
            } else {
                currentStage.setWidth(currentWidth);
                currentStage.setHeight(currentHeight);
                currentStage.setX(currentX);
                currentStage.setY(currentY);
            }

            currentStage.setTitle("SecondServe - Sign Up");

            // Get the controller and set user type if needed
            SignUpController signUpController = loader.getController();
            signUpController.setUserType(userType);

        } catch (Exception e) {
            System.err.println("Failed to load signup page: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Cannot load signup page: " + e.getMessage());
        }
    }
    private void navigateToRoleSelection(ActionEvent event) {
        try {
            URL fxmlUrl = getClass().getResource("/com/example/secondserve/opening-view.fxml");
            if (fxmlUrl == null) {
                showAlert("Error", "Role selection form not found.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent roleSelectionRoot = loader.load();

            // Get current stage and preserve state
            Button sourceButton = (Button) event.getSource();
            Stage currentStage = (Stage) sourceButton.getScene().getWindow();

            boolean wasMaximized = currentStage.isMaximized();
            double currentWidth = currentStage.getWidth();
            double currentHeight = currentStage.getHeight();
            double currentX = currentStage.getX();
            double currentY = currentStage.getY();

            // FIX: Use the current scene dimensions instead of default
            Scene currentScene = sourceButton.getScene();
            Scene roleSelectionScene = new Scene(roleSelectionRoot, currentScene.getWidth(), currentScene.getHeight());

            currentStage.setScene(roleSelectionScene);

            // Restore window state
            if (wasMaximized) {
                currentStage.setMaximized(true);
            } else {
                currentStage.setWidth(currentWidth);
                currentStage.setHeight(currentHeight);
                currentStage.setX(currentX);
                currentStage.setY(currentY);
            }

            currentStage.setTitle("SecondServe - Choose Your Role");

        } catch (Exception e) {
            System.err.println("Failed to load role selection page: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Cannot load role selection page: " + e.getMessage());
        }
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}