package com.example.secondserve.dto;

public class AuthResponse {
    private String token;
    private String userType;
    private Long userId;
    private String name;
    private String email;

    // A no-argument constructor is needed for the JSON library
    public AuthResponse() {}

    // Getters and Setters for all fields...
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}