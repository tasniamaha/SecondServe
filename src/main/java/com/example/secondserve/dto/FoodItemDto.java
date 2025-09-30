package com.example.secondserve.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true) // Safe fallback for any future fields
public class FoodItemDto {
    private Long id;

    // Add the missing fields from server response
    private Long hotelId;
    private String hotelName;
    private Boolean isAvailable;

    private String foodName;
    private BigDecimal quantity;
    private String unit;
    private String category;  // Keep as String (will receive enum name)
    private String condition; // Keep as String (will receive enum name)
    private String description;
    private LocalDate expiryDate;

    @JsonProperty("createdDate") // Map server's "createdDate" to this field
    private LocalDateTime loggedAt;
    private String currentUserRequestStatus;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }

    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

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

    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }

    public String getCurrentUserRequestStatus() {
        return currentUserRequestStatus;
    }

    public void setCurrentUserRequestStatus(String currentUserRequestStatus) {
        this.currentUserRequestStatus = currentUserRequestStatus;
    }
}