package com.example.secondserve;

import com.example.secondserve.dto.AuthResponse;

public class SessionManager {
    private static AuthResponse currentSession = null;

    public static void createSession(AuthResponse session) {
        currentSession = session;
    }

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
}