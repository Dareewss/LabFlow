package com.labflow.api;

import com.sun.net.httpserver.HttpExchange;

public final class ApiSecurity {
    private ApiSecurity() {
    }

    public static boolean isAuthorized(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        return header != null && header.equals("Bearer " + ApiConfig.apiKey());
    }
}
