package com.labflow.api;

import com.google.gson.JsonObject;
import com.labflow.dao.ActivityLogDAO;
import com.labflow.dao.UserDAO;
import com.labflow.model.User;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuthApiHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuthApiHandler.class);
    private final UserDAO userDAO = new UserDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (ApiResponseUtil.handleOptions(exchange)) {
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                ApiResponseUtil.sendError(exchange, 400, "Invalid method");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (!"/api/auth/login".equals(path)) {
                ApiResponseUtil.sendError(exchange, 404, "Endpoint not found");
                return;
            }
            JsonObject body = ApiResponseUtil.readJson(exchange);
            String username = ApiResponseUtil.string(body, "username");
            String password = ApiResponseUtil.string(body, "password");
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                ApiResponseUtil.sendError(exchange, 400, "Username and password are required.");
                return;
            }
            userDAO.authenticate(username.trim(), password).ifPresentOrElse(user -> {
                try {
                    activityLogDAO.log(user.getId(), "MOBILE_LOGIN", "USER", user.getId(), "User logged in from companion app");
                    ApiResponseUtil.sendSuccess(exchange, userToMap(user));
                } catch (IOException e) {
                    logger.error("Could not send auth response", e);
                }
            }, () -> {
                try {
                    ApiResponseUtil.sendError(exchange, 401, "Invalid username or password.");
                } catch (IOException e) {
                    logger.error("Could not send auth error", e);
                }
            });
        } catch (Exception e) {
            logger.error("Auth API request failed", e);
            ApiResponseUtil.sendError(exchange, 500, "Server error");
        }
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("fullName", user.getFullName());
        data.put("role", user.getRole() == null ? null : user.getRole().name());
        data.put("displayName", user.toString());
        return data;
    }
}
