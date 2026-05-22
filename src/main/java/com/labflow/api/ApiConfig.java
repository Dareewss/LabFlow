package com.labflow.api;

public final class ApiConfig {
    public static final int DEFAULT_PORT = 8080;
    public static final String API_VERSION = "1.0";
    private static final String DEFAULT_API_KEY = "LABFLOW_LOCAL_API_KEY";

    private ApiConfig() {
    }

    public static int port() {
        String value = System.getProperty("labflow.api.port", System.getenv("LABFLOW_API_PORT"));
        if (value == null || value.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }

    public static String apiKey() {
        String value = System.getProperty("labflow.api.key", System.getenv("LABFLOW_API_KEY"));
        return value == null || value.isBlank() ? DEFAULT_API_KEY : value.trim();
    }
}
