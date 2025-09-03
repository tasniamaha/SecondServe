package com.example.secondserve;

import com.example.secondserve.dto.AuthResponse;
import com.example.secondserve.dto.HotelDto;
import com.example.secondserve.dto.KitchenStaffDto;
import com.example.secondserve.dto.NgoDto;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

public class SignUpController {

    // --- FXML UI Components ---
    @FXML private ImageView logoImageView;
    @FXML private Label userTypeLabel;
    @FXML private Button registerButton;
    @FXML private VBox kitchenStaffForm, hotelManagerForm, ngoForm;
    @FXML private TextField staffNameField, staffEmailField, hotelCodeField, positionField;
    @FXML private PasswordField staffPasswordField;
    @FXML private TextField managerNameField, managerEmailField, hotelNameField, hotelAddressField, hotelLicenseField;
    @FXML private PasswordField managerPasswordField;
    @FXML private TextField adminNameField, ngoEmailField, ngoNameField, ngoAddressField, ngoContactField, ngoLicenseField;
    @FXML private PasswordField ngoPasswordField;

    private String userType;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @FXML
    public void initialize() {
        try {
            logoImageView.setImage(loadImage("/assets/SecondServe_logo.png"));
        } catch (Exception e) {
            System.err.println("Failed to load logo image: " + e.getMessage());
        }
        kitchenStaffForm.managedProperty().bind(kitchenStaffForm.visibleProperty());
        hotelManagerForm.managedProperty().bind(hotelManagerForm.visibleProperty());
        ngoForm.managedProperty().bind(ngoForm.visibleProperty());
    }

    private Image loadImage(String path) {
        return new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
    }

    public void setUserType(String userType) {
        this.userType = userType;
        if (userTypeLabel != null) {
            String labelText = "Register as ";
            VBox formToShow = null;
            switch (userType) {
                case "KITCHEN_STAFF":
                    labelText += "Kitchen Staff";
                    formToShow = kitchenStaffForm;
                    break;
                case "HOTEL_MANAGER":
                    labelText += "Hotel Manager";
                    formToShow = hotelManagerForm;
                    break;
                case "NGO":
                    labelText += "NGO Representative";
                    formToShow = ngoForm;
                    break;
            }
            userTypeLabel.setText(labelText);
            if (formToShow != null) showForm(formToShow);
        }
    }

    private void showForm(VBox formToShow) {
        kitchenStaffForm.setVisible(false);
        hotelManagerForm.setVisible(false);
        ngoForm.setVisible(false);
        formToShow.setVisible(true);
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        System.out.println("DEBUG: handleRegister method called.");


        if (userType == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user type was selected.");
            return;
        }
        switch (userType) {
            case "KITCHEN_STAFF": registerKitchenStaff(event); break;
            case "HOTEL_MANAGER": registerHotelManager(event); break;
            case "NGO": registerNGO(event); break;
        }

    }

    // --- Registration Logic ---
    private void registerKitchenStaff(ActionEvent event) {
        String name = staffNameField.getText(), email = staffEmailField.getText(), password = staffPasswordField.getText(), hotelCode = hotelCodeField.getText();
        if (name.trim().isEmpty() || email.trim().isEmpty() || password.isEmpty() || hotelCode.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill in all required fields.");
            return;
        }
        KitchenStaffDto staffDto = new KitchenStaffDto();
        staffDto.setStaffName(name);
        staffDto.setEmail(email);
        staffDto.setPassword(password);
        staffDto.setHotelCode(hotelCode);
        staffDto.setPosition(positionField.getText());
        sendRegistrationRequest(staffDto, "http://localhost:8080/api/staff/register", event);
    }

    private void registerHotelManager(ActionEvent event) {
        String name = managerNameField.getText(), email = managerEmailField.getText(), password = managerPasswordField.getText(), hotelName = hotelNameField.getText(), address = hotelAddressField.getText(), license = hotelLicenseField.getText();
        if (name.trim().isEmpty() || email.trim().isEmpty() || password.isEmpty() || hotelName.trim().isEmpty() || address.trim().isEmpty() || license.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill in all required fields.");
            return;
        }
        HotelDto hotelDto = new HotelDto();
        hotelDto.setManagerName(name);
        hotelDto.setEmail(email);
        hotelDto.setPassword(password);
        hotelDto.setHotelName(hotelName);
        hotelDto.setAddress(address);
        hotelDto.setHotelLicense(license);
        System.out.println("DEBUG: Preparing to send Hotel Manager registration request to the server...");
        sendRegistrationRequest(hotelDto, "http://localhost:8080/api/hotels/register", event);
    }

    private void registerNGO(ActionEvent event) {
        String adminName = adminNameField.getText(), email = ngoEmailField.getText(), password = ngoPasswordField.getText(), ngoName = ngoNameField.getText(), address = ngoAddressField.getText(), contact = ngoContactField.getText(), license = ngoLicenseField.getText();
        if (adminName.trim().isEmpty() || email.trim().isEmpty() || password.isEmpty() || ngoName.trim().isEmpty() || address.trim().isEmpty() || license.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill in all required fields.");
            return;
        }
        NgoDto ngoDto = new NgoDto();
        ngoDto.setContactPerson(adminName);
        ngoDto.setEmail(email);
        ngoDto.setPassword(password);
        ngoDto.setNgoName(ngoName);
        ngoDto.setAddress(address);
        ngoDto.setPhone(contact);
        ngoDto.setLicenseNumber(license);
        sendRegistrationRequest(ngoDto, "http://localhost:8080/api/ngos/register", event);
    }

    // --- API Communication ---
    private <T> void sendRegistrationRequest(T dto, String url, ActionEvent event) {
        registerButton.setDisable(true);
        try {
            String requestBody = objectMapper.writeValueAsString(dto);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> handleServerResponse(response, event)).exceptionally(this::handleConnectionError);
        } catch (IOException e) {
            System.err.println("DEBUG: FAILED before sending! Error converting DTO to JSON.");
            showAlert(Alert.AlertType.ERROR, "Application Error", "An error occurred while preparing the request.");
            registerButton.setDisable(false);
        }
    }

    private void handleServerResponse(HttpResponse<String> response, ActionEvent event) {
        System.out.println("DEBUG: Received a response from the server! Status code: " + response.statusCode());
        System.out.println("DEBUG: Response Body: " + response.body());
        Platform.runLater(() -> {
            if (response.statusCode() == 201) { // 201 Created
                try {
                    if (response.body() != null && response.body().contains("token")) {
                        AuthResponse authResponse = objectMapper.readValue(response.body(), AuthResponse.class);
                        SessionManager.createSession(authResponse);
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Registration successful! You are now logged in.");
                        navigateToDashboard(event);
                    } else {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Registration successful! You can now log in.");
                        navigateToLogin(event); // --- FIXED: This was the method that was missing ---
                    }
                } catch (IOException e) {
                    showAlert(Alert.AlertType.ERROR, "Application Error", "Could not process server response.");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Registration Failed", "Could not create account. The email, license, or hotel code may already be in use or invalid.");
            }
            registerButton.setDisable(false);
        });
    }

    private Void handleConnectionError(Throwable e) {
        Platform.runLater(() -> {
            showAlert(Alert.AlertType.ERROR, "Connection Error", "Could not connect to the server.");
            registerButton.setDisable(false);
        });
        System.err.println("DEBUG: The HTTP request failed entirely!");
        return null;
    }

    // --- Navigation ---
    @FXML private void handleBackButton(ActionEvent event) {
        navigateToView((Node) event.getSource(), "opening-view.fxml", "SecondServe - Choose Your Role");
    }

    @FXML private void handleLoginLink(ActionEvent event) {
        navigateToLogin(event);
    }

    // --- FIXED: ADDED THE MISSING navigateToLogin METHOD ---
    private void navigateToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/secondserve/login-fxml.fxml"));
            Parent loginRoot = loader.load();

            LoginController loginController = loader.getController();
            loginController.setUserType(this.userType);

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(new Scene(loginRoot, currentStage.getScene().getWidth(), currentStage.getScene().getHeight()));
            currentStage.setTitle("SecondServe - Login");
        } catch(IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Cannot load the login page.");
        }
    }

    private void navigateToDashboard(ActionEvent event) {
        AuthResponse session = SessionManager.getSession();
        if (session == null) return;
        String fxmlFile = null, title = "SecondServe";
        switch (session.getUserType()) {
            case "KITCHEN_STAFF": fxmlFile = "kitchen-main.fxml"; title = "Kitchen Interface"; break;
            case "HOTEL_MANAGER": fxmlFile = "HotelManager_Dashboard.fxml"; title = "Hotel Dashboard"; break;
            case "NGO": fxmlFile = "ngo-portal.fxml"; title = "NGO Portal"; break;
            default: showAlert(Alert.AlertType.ERROR, "Error", "Unknown user role."); return;
        }
        navigateToView(((Node) event.getSource()), fxmlFile, title);
    }

    private void navigateToView(Node sourceNode, String fxmlFile, String title) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/secondserve/" + fxmlFile)));
            Stage stage = (Stage) sourceNode.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle(title);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Cannot load page: " + fxmlFile);
            e.printStackTrace();
        }
    }

    /**
     * FIXED: This method now accepts an AlertType as the first argument.
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}