package com.example.secondserve.dto;

public class HotelDto {
    private long id;
    private String managerName;
    private String email;
    private String password;
    private String hotelName;
    private String address;
    private String hotelLicense;
    private String phone;
    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getHotelLicense() {
        return hotelLicense;
    }

    public void setHotelLicense(String hotelLicense) {
        this.hotelLicense = hotelLicense;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
    // Getters and Setters for all fields...
}