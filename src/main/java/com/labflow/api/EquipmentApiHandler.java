package com.labflow.api;

import com.google.gson.JsonObject;
import com.labflow.dao.ActivityLogDAO;
import com.labflow.model.ActivityLog;
import com.labflow.model.BorrowRecord;
import com.labflow.model.Equipment;
import com.labflow.service.EquipmentService;
import com.labflow.util.DesktopRefreshBus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EquipmentApiHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(EquipmentApiHandler.class);
    private final EquipmentService equipmentService = new EquipmentService();
    private final BorrowApiHandler borrowApiHandler = new BorrowApiHandler();
    private final FaultApiHandler faultApiHandler = new FaultApiHandler();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (ApiResponseUtil.handleOptions(exchange)) {
                return;
            }
            if (!ApiSecurity.isAuthorized(exchange)) {
                ApiResponseUtil.sendError(exchange, 401, "Cheie API invalida.");
                return;
            }
            route(exchange);
        } catch (IllegalArgumentException e) {
            ApiResponseUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            logger.error("API request failed", e);
            ApiResponseUtil.sendError(exchange, 500, "Server error");
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String base = "/api/equipment";
        String rest = path.length() <= base.length() ? "" : path.substring(base.length());
        if (rest.startsWith("/by-qr/") && "GET".equalsIgnoreCase(method)) {
            String qrCode = decode(rest.substring("/by-qr/".length()));
            Equipment equipment = equipmentService.getEquipmentByQrCode(qrCode).orElse(null);
            sendEquipment(exchange, equipment);
            return;
        }
        String[] parts = rest.split("/");
        if (parts.length < 2 || parts[1].isBlank()) {
            ApiResponseUtil.sendError(exchange, 404, "Endpoint not found");
            return;
        }
        int equipmentId = parseId(parts[1]);
        if (parts.length == 2 && "GET".equalsIgnoreCase(method)) {
            sendEquipment(exchange, equipmentService.getEquipmentById(equipmentId).orElse(null));
            return;
        }
        if (parts.length == 3 && "GET".equalsIgnoreCase(method) && "history".equals(parts[2])) {
            ApiResponseUtil.sendSuccess(exchange, history(equipmentId));
            return;
        }
        if (parts.length == 3 && "POST".equalsIgnoreCase(method) && "fault-report".equals(parts[2])) {
            JsonObject body = ApiResponseUtil.readJson(exchange);
            Map<String, Object> result = faultApiHandler.createFaultReport(equipmentId, body);
            DesktopRefreshBus.requestRefresh();
            ApiResponseUtil.sendSuccess(exchange, result);
            return;
        }
        if (parts.length == 3 && "POST".equalsIgnoreCase(method) && "borrow".equals(parts[2])) {
            JsonObject body = ApiResponseUtil.readJson(exchange);
            Map<String, Object> result = borrowApiHandler.borrow(equipmentId, body);
            DesktopRefreshBus.requestRefresh();
            ApiResponseUtil.sendSuccess(exchange, result);
            return;
        }
        if (parts.length == 3 && "POST".equalsIgnoreCase(method) && "return".equals(parts[2])) {
            JsonObject body = ApiResponseUtil.readJson(exchange);
            Map<String, Object> result = borrowApiHandler.returnEquipment(body);
            DesktopRefreshBus.requestRefresh();
            ApiResponseUtil.sendSuccess(exchange, result);
            return;
        }
        ApiResponseUtil.sendError(exchange, 404, "Endpoint not found");
    }

    private void sendEquipment(HttpExchange exchange, Equipment equipment) throws IOException {
        if (equipment == null) {
            ApiResponseUtil.sendError(exchange, 404, "Echipamentul nu a fost gasit.");
            return;
        }
        ApiResponseUtil.sendSuccess(exchange, equipmentToMap(equipment));
    }

    private Map<String, Object> equipmentToMap(Equipment equipment) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", equipment.getId());
        data.put("name", equipment.getName());
        data.put("category", equipment.getCategory());
        data.put("description", equipment.getDescription());
        data.put("location", equipment.getLocation());
        data.put("status", equipment.getStatus() == null ? null : equipment.getStatus().name());
        data.put("qrCode", equipment.getQrCode());
        data.put("qrCodePath", equipment.getQrCodePath());
        data.put("serialNumber", equipment.getSerialNumber());
        data.put("manufacturer", equipment.getManufacturer());
        data.put("model", equipment.getModel());
        data.put("purchaseDate", value(equipment.getPurchaseDate()));
        data.put("lastMaintenanceDate", value(equipment.getLastMaintenanceDate()));
        data.put("notes", equipment.getNotes());
        Optional<BorrowRecord> activeBorrow = borrowApiHandler.activeBorrowForEquipment(equipment.getId());
        activeBorrow.ifPresent(record -> {
            data.put("activeBorrowRecordId", record.getId());
            data.put("activeBorrowUserId", record.getUserId());
            data.put("activeBorrowUsername", record.getUsername());
        });
        return data;
    }

    private List<Map<String, Object>> history(int equipmentId) {
        return activityLogDAO.findEquipmentHistory(equipmentId, 20).stream().map(this::logToMap).toList();
    }

    private Map<String, Object> logToMap(ActivityLog log) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", log.getId());
        data.put("userId", log.getUserIdObject());
        data.put("username", log.getUsername());
        data.put("action", log.getAction());
        data.put("entityType", log.getEntityType());
        data.put("entityId", log.getEntityId());
        data.put("description", log.getDescription());
        data.put("timestamp", value(log.getTimestamp()));
        return data;
    }

    private int parseId(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid equipment id");
        }
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String value(Object value) {
        return value == null ? null : value.toString();
    }
}
