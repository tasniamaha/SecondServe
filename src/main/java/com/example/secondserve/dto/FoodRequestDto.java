package com.example.secondserve.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// This class represents a food request received FROM the server.
public class FoodRequestDto {
    private Long id;
    private String ngoName;
    private String foodItemName;
    private BigDecimal requestedQuantity;
    private String unit; // Assuming the server will send the unit
    private LocalDateTime requestDate;
    private String notes;
    private Long foodItemId;

    private String hotelName;




    private String requestStatus;
    // A no-argument constructor is needed for the JSON library
    public FoodRequestDto() {}

    // Getters and Setters for all fields...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNgoName() { return ngoName; }
    public void setNgoName(String ngoName) { this.ngoName = ngoName; }
    public String getFoodItemName() { return foodItemName; }
    public void setFoodItemName(String foodItemName) { this.foodItemName = foodItemName; }
    public BigDecimal getRequestedQuantity() { return requestedQuantity; }
    public void setRequestedQuantity(BigDecimal requestedQuantity) { this.requestedQuantity = requestedQuantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getFoodItemId() {
        return foodItemId;
    }

    public void setFoodItemId(Long foodItemId) {
        this.foodItemId = foodItemId;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }
}