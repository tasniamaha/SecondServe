package com.example.secondserve.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// This class perfectly matches the JSON the server expects to receive
public class FoodItemDto {
    private Long id;
    private String foodName;
    private BigDecimal quantity;
    private String unit = "kg"; // You can make this dynamic later
    private String category;    // e.g., "PREPARED_FOOD"
    private String condition;   // e.g., "FRESH"
    private String description;
    private LocalDate expiryDate;

    // Getters and Setters for all fields...
    // You can generate these automatically in your IDE
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = BigDecimal.valueOf(quantity); }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}