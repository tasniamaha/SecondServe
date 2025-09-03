package com.example.secondserve.dto;

import java.math.BigDecimal;

/**
 * This class is a Data Transfer Object (DTO) that represents the statistics
 * received from the server for the Hotel Manager's dashboard.
 * Its structure must match the JSON sent by the server.
 */
public class DashboardStatsDto {

    private BigDecimal totalDonatedThisWeek;
    private BigDecimal totalLoggedThisWeek;
    private String hotelCode;

    // A no-argument constructor is required for the Jackson JSON library to work.
    public DashboardStatsDto() {
    }

    // --- Getters and Setters ---
    // These allow the Jackson library and your controller to access the data.
    // You can generate these automatically in your IDE.

    public BigDecimal getTotalDonatedThisWeek() {
        return totalDonatedThisWeek;
    }

    public void setTotalDonatedThisWeek(BigDecimal totalDonatedThisWeek) {
        this.totalDonatedThisWeek = totalDonatedThisWeek;
    }

    public BigDecimal getTotalLoggedThisWeek() {
        return totalLoggedThisWeek;
    }

    public void setTotalLoggedThisWeek(BigDecimal totalLoggedThisWeek) {
        this.totalLoggedThisWeek = totalLoggedThisWeek;
    }

    public String getHotelCode() {
        return hotelCode;
    }

    public void setHotelCode(String hotelCode) {
        this.hotelCode = hotelCode;
    }
}