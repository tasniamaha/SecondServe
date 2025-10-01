package com.example.secondserve;

import com.example.secondserve.dto.AuthResponse;

public class SessionManager {
    private static AuthResponse currentSession = null;

    @SuppressWarnings("exports")
    public static void createSession(AuthResponse session) {
        currentSession = session;
    }

    @SuppressWarnings("exports")
    public static AuthResponse getSession() {
        return currentSession;
    }

    // A helper to get the token ready for an HTTP header
    public static String getAuthToken() {
        if (currentSession != null && currentSession.getToken() != null) {
            return "Bearer " + currentSession.getToken();
        }
        return null;
    }

    public static void clearSession() {
        currentSession = null;
    }
    public static Long getHotelId() {

        if (currentSession != null && "HOTEL_MANAGER".equals(currentSession.getUserType())) {
            return currentSession.getUserId(); // The userId IS the hotelId in this context
        }
        return null; // Return null if not logged in or not a hotel manager
    }
}