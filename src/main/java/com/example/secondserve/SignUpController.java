package com.example.secondserve;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public class SignUpController {

    @FXML private ImageView logoImageView;
    @FXML private Label userTypeLabel;
    @FXML private Button backButton;
    @FXML private Hyperlink loginLink;
    @FXML private Button registerButton;

    // Kitchen Staff fields
    @FXML private TextField staffNameField;
    @FXML private TextField staffEmailField;
    @FXML private PasswordField staffPasswordField;
    @FXML private TextField hotelCodeField;
    @FXML private TextField positionField;

    // Hotel Manager fields
    @FXML private TextField managerNameField;
    @FXML private TextField managerEmailField;
    @FXML private PasswordField managerPasswordField;
    @FXML private TextField hotelNameField;
    @FXML private TextField hotelAddressField;
    @FXML private TextField hotelLicenseField;

    // NGO fields
    @FXML private TextField adminNameField;
    @FXML private TextField ngoEmailField;
    @FXML private PasswordField ngoPasswordField;
    @FXML private TextField ngoNameField;
    @FXML private TextField ngoAddressField;
    @FXML private TextField ngoContactField;
    @FXML private TextField ngoLicenseField;

    // Form containers
    @FXML private VBox kitchenStaffForm;
    @FXML private VBox hotelManagerForm;
    @FXML private VBox ngoForm;

    private String userType;

    @FXML
    public void initialize() {
        try {
            logoImageView.setImage(loadImage("/assets/SecondServe_logo.png"));
        } catch (Exception e) {
            System.err.println("Failed to load images from controller!");
            e.printStackTrace();
        }

        // Initially hide all forms
        kitchenStaffForm.managedProperty().bind(kitchenStaffForm.visibleProperty());
        hotelManagerForm.managedProperty().bind(hotelManagerForm.visibleProperty());
        ngoForm.managedProperty().bind(ngoForm.visibleProperty());
    }

    private Image loadImage(String path) {
        return new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
    }

    // Method to set user type from previous screen
    public void setUserType(String userType) {
        this.userType = userType;
        System.out.println("User type set to: " + userType);

        // Update the label and show the appropriate form
        if (userTypeLabel != null) {
            switch (userType) {
                case "KITCHEN_STAFF":
                    userTypeLabel.setText("Register as Kitchen Staff");
                    showForm("KITCHEN_STAFF");
                    break;
                case "HOTEL_MANAGER":
                    userTypeLabel.setText("Register as Hotel Manager");
                    showForm("HOTEL_MANAGER");
                    break;
                case "NGO":
                    userTypeLabel.setText("Register as NGO Representative");
                    showForm("NGO");
                    break;
            }
        }
    }

    private void showForm(String userType) {
        // Hide all forms first
        if (kitchenStaffForm != null) kitchenStaffForm.setVisible(false);
        if (hotelManagerForm != null) hotelManagerForm.setVisible(false);
        if (ngoForm != null) ngoForm.setVisible(false);

        // Show the appropriate form
        switch (userType) {
            case "KITCHEN_STAFF":
                if (kitchenStaffForm != null) kitchenStaffForm.setVisible(true);
                break;
            case "HOTEL_MANAGER":
                if (hotelManagerForm != null) hotelManagerForm.setVisible(true);
                break;
            case "NGO":
                if (ngoForm != null) ngoForm.setVisible(true);
                break;
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        if (userType == null) {
            showAlert("Error", "Please select a user type.");
            return;
        }

        switch (userType) {
            case "KITCHEN_STAFF":
                registerKitchenStaff();
                break;
            case "HOTEL_MANAGER":
                registerHotelManager();
                break;
            case "NGO":
                registerNGO();
                break;
        }
    }

    @FXML
    private void handleBackButton(ActionEvent event) {
        navigateToRoleSelection(event);
    }

    @FXML
    private void handleLoginLink(ActionEvent event) {
        navigateToLogin(event);
    }

    private void registerKitchenStaff() {
        String name = staffNameField.getText();
        String email = staffEmailField.getText();
        String password = staffPasswordField.getText();
        String hotelCode = hotelCodeField.getText();
        String position = positionField.getText();

        // Validate required fields
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || hotelCode.isEmpty()) {
            showAlert("Error", "Please fill in all required fields.");
            return;
        }

        // Add your registration logic here
        System.out.println("Registering Kitchen Staff: " + name + ", " + email);
        showAlert("Success", "Kitchen Staff registration successful!");

        // Clear fields after successful registration
        staffNameField.clear();
        staffEmailField.clear();
        staffPasswordField.clear();
        hotelCodeField.clear();
        positionField.clear();
    }

    private void registerHotelManager() {
        String name = managerNameField.getText();
        String email = managerEmailField.getText();
        String password = managerPasswordField.getText();
        String hotelName = hotelNameField.getText();
        String hotelAddress = hotelAddressField.getText();
        String hotelLicense = hotelLicenseField.getText();

        // Validate required fields
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || hotelName.isEmpty() || hotelLicense.isEmpty()) {
            showAlert("Error", "Please fill in all required fields.");
            return;
        }

        // Add your registration logic here
        System.out.println("Registering Hotel Manager: " + name + ", " + email);
        showAlert("Success", "Hotel Manager registration successful!");

        // Clear fields after successful registration
        managerNameField.clear();
        managerEmailField.clear();
        managerPasswordField.clear();
        hotelNameField.clear();
        hotelAddressField.clear();
        hotelLicenseField.clear();
    }

    private void registerNGO() {
        String name = adminNameField.getText();
        String email = ngoEmailField.getText();
        String password = ngoPasswordField.getText();
        String ngoName = ngoNameField.getText();
        String ngoAddress = ngoAddressField.getText();
        String ngoContact = ngoContactField.getText();
        String ngoLicense = ngoLicenseField.getText();

        // Validate required fields
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || ngoName.isEmpty() || ngoLicense.isEmpty()) {
            showAlert("Error", "Please fill in all required fields.");
            return;
        }

        // Add your registration logic here
        System.out.println("Registering NGO: " + name + ", " + email);
        showAlert("Success", "NGO registration successful!");

        // Clear fields after successful registration
        adminNameField.clear();
        ngoEmailField.clear();
        ngoPasswordField.clear();
        ngoNameField.clear();
        ngoAddressField.clear();
        ngoContactField.clear();
        ngoLicenseField.clear();
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

            // Use current scene dimensions
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

    private void navigateToLogin(ActionEvent event) {
        try {
            URL fxmlUrl = getClass().getResource("/com/example/secondserve/login-fxml.fxml");
            if (fxmlUrl == null) {
                showAlert("Error", "Login form not found.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent loginRoot = loader.load();

            // Get current stage and preserve state
            Hyperlink sourceLink = (Hyperlink) event.getSource();
            Stage currentStage = (Stage) sourceLink.getScene().getWindow();

            boolean wasMaximized = currentStage.isMaximized();
            double currentWidth = currentStage.getWidth();
            double currentHeight = currentStage.getHeight();
            double currentX = currentStage.getX();
            double currentY = currentStage.getY();

            // Use current scene dimensions
            Scene currentScene = sourceLink.getScene();
            Scene loginScene = new Scene(loginRoot, currentScene.getWidth(), currentScene.getHeight());

            currentStage.setScene(loginScene);

            // Restore window state
            if (wasMaximized) {
                currentStage.setMaximized(true);
            } else {
                currentStage.setWidth(currentWidth);
                currentStage.setHeight(currentHeight);
                currentStage.setX(currentX);
                currentStage.setY(currentY);
            }

            currentStage.setTitle("SecondServe - Login");

        } catch (Exception e) {
            System.err.println("Failed to load login page: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Cannot load login page: " + e.getMessage());
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