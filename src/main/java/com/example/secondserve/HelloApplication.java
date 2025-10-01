package com.example.secondserve;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL; 
import java.util.Objects;

public class HelloApplication extends Application {
    @Override
    public void start(@SuppressWarnings("exports") Stage stage) throws IOException {

        System.out.println("--- Checking for resource ---");
        URL resourceUrl = HelloApplication.class.getResource("/assets/SecondServe_logo.png");
        if (resourceUrl == null) {
            System.err.println("FATAL: Resource '/assets/SecondServe_logo.png' was not found on the classpath!");
        } else {
            System.out.println("SUCCESS: Resource found at: " + resourceUrl);
        }
        System.out.println("---------------------------");
        


        // Load the FXML file
        Parent root = FXMLLoader.load(Objects.requireNonNull(HelloApplication.class.getResource("opening-view.fxml")));

        Scene scene = new Scene(root);
        stage.setTitle("SecondServe");
        stage.setScene(scene);
        stage.show();
    }
    
}