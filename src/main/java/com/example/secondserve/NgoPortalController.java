package com.yourcompany.yourapp; // Make sure this matches your project structure

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class NgoPortalController {

    @FXML private BorderPane mainBorderPane;
    @FXML private Button browseHotelsButton;
    @FXML private Button myRequestsButton;
    @FXML private Button ngoProfileButton;

    // A variable to keep track of the currently selected button
    private Button activeButton;

    @FXML
    public void initialize() {
        // The app starts with the "Browse Hotels" view active.
        activeButton = browseHotelsButton;
    }

    @FXML
    private void showBrowseHotelsView() {
        System.out.println("Showing Browse Hotels (already visible)");
        setActiveButton(browseHotelsButton);
        // You would reload the original view here if you wanted
        // loadView("InitialView.fxml");
    }

    @FXML
    private void showMyRequestsView() {
        System.out.println("Showing My Requests View");
        setActiveButton(myRequestsButton);
        // Example of loading another view
        // loadView("MyRequestsView.fxml");
    }

    /**
     * This is the method that gets called when you click "NGO Profile".
     */
    @FXML
    private void showNgoProfileView() {
        System.out.println("Loading NGO Profile View...");
        loadView("NgoProfileView.fxml"); // Loads the profile FXML
        setActiveButton(ngoProfileButton);
    }

    /**
     * Helper method to load an FXML file into the center of the main BorderPane.
     * This is the core logic that enables same-window navigation.
     */
    private void loadView(String fxmlFile) {
        try {
            // Load the FXML file for the desired view
            Node view = FXMLLoader.load(getClass().getResource(fxmlFile));
            // Replace the center content with the new view
            mainBorderPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
            // In a real application, show an error alert to the user.
        }
    }

    /**
     * Updates the CSS classes to show which navigation button is currently active.
     */
    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        button.getStyleClass().add("active");
        activeButton = button;
    }
}