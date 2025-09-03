package com.example.secondserve.dto;

import com.fasterxml.jackson.annotation.JsonProperty; // IMPORTANT: Add this import
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FoodItemDto {
    private Long id;
    private String foodName;
    private BigDecimal quantity; // Using BigDecimal to match your controller
    private String unit;
    private String category;
    private String condition;
    private String description;
    private LocalDate expiryDate;
    private LocalDateTime loggedAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getFoodName() {
        return foodName;
    }
    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }
    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    public String getCondition() {
        return condition;
    }
    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }
    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }

    // --- THIS IS THE FIX ---
    // This annotation tells the parser to map the JSON field "createdDate"
    // to this 'loggedAt' field in our Java class.
    @JsonProperty("createdDate")
    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }
}