package com.labflow.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalApiServer {
    private static final Logger logger = LoggerFactory.getLogger(LocalApiServer.class);
    private HttpServer server;
    private ExecutorService executor;

    public void start() {
        if (server != null) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", ApiConfig.port()), 0);
            executor = Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "labflow-local-api");
                thread.setDaemon(true);
                return thread;
            });
            server.setExecutor(executor);
            server.createContext("/api/health", this::health);
            server.createContext("/api/auth", new AuthApiHandler());
            server.createContext("/api/equipment", new EquipmentApiHandler());
            server.createContext("/api/companion", new CompanionApiHandler());
            server.start();
            logger.info("LabFlow local API started on 0.0.0.0:{}", ApiConfig.port());
        } catch (IOException e) {
            logger.error("Could not start LabFlow local API", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void health(HttpExchange exchange) throws IOException {
        if (ApiResponseUtil.handleOptions(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            ApiResponseUtil.sendError(exchange, 400, "Invalid method");
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ok");
        data.put("app", "LabFlow");
        data.put("apiVersion", ApiConfig.API_VERSION);
        ApiResponseUtil.sendSuccess(exchange, data);
    }
}
