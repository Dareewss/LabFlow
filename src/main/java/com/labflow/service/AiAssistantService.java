package com.labflow.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.labflow.dao.PreferencesDAO;
import com.labflow.model.Equipment;
import com.labflow.model.Lab;
import com.labflow.model.Role;
import com.labflow.model.Reservation;
import com.labflow.model.User;
import com.labflow.model.WeeklyStats;
import com.labflow.util.DesktopRefreshBus;
import com.labflow.util.SessionManager;
import com.labflow.util.ThemeManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AiAssistantService {
    private static final Gson GSON = new Gson();
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/";
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String PREF_PROVIDER = "ai.provider";
    private static final String PREF_API_KEY = "ai.apiKey";
    private static final String PREF_BASE_URL = "ai.baseUrl";
    private static final String PREF_MODEL = "ai.model";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();
    private final PreferencesDAO preferencesDAO = new PreferencesDAO();
    private final LabService labService = new LabService();
    private final TestKitImportService testKitImportService = new TestKitImportService();
    private final EquipmentService equipmentService = new EquipmentService();
    private final ReservationService reservationService = new ReservationService();
    private final SessionManager session = SessionManager.getInstance();

    public boolean isConfigured() {
        return !apiKey().isBlank();
    }

    public String configurationHint() {
        return "Open Settings > AI Assistant to save a provider, API key, base URL and model. Environment variables still work as fallback.";
    }

    public String getSavedProvider() {
        return preferencesDAO.get(PREF_PROVIDER, "Gemini");
    }

    public String getSavedApiKey() {
        return preferencesDAO.get(PREF_API_KEY, "");
    }

    public String getResolvedApiKey() {
        return apiKey();
    }

    public String getSavedBaseUrl() {
        return preferencesDAO.get(PREF_BASE_URL, "");
    }

    public String getResolvedBaseUrl() {
        return rawBaseUrl();
    }

    public String getSavedModel() {
        return preferencesDAO.get(PREF_MODEL, "");
    }

    public String getResolvedModel() {
        return model();
    }

    public void saveConfiguration(String provider, String apiKey, String baseUrl, String model) {
        preferencesDAO.set(PREF_PROVIDER, blank(provider) ? "Gemini" : provider.trim());
        preferencesDAO.set(PREF_API_KEY, apiKey == null ? "" : apiKey.trim());
        preferencesDAO.set(PREF_BASE_URL, baseUrl == null ? "" : baseUrl.trim());
        preferencesDAO.set(PREF_MODEL, model == null ? "" : model.trim());
    }

    public void testConnection() {
        testConnection(apiKey(), rawBaseUrl(), model());
    }

    public void testConnection(String apiKey, String baseUrl, String model) {
        String resolvedApiKey = firstNonBlank(apiKey);
        String resolvedBaseUrl = firstNonBlank(baseUrl, DEFAULT_BASE_URL);
        String resolvedModel = firstNonBlank(model, DEFAULT_MODEL);
        if (resolvedApiKey.isBlank()) {
            throw new IllegalArgumentException("Add an API key first.");
        }
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", resolvedModel);
            body.addProperty("temperature", 0.0);
            JsonArray messages = new JsonArray();
            JsonObject user = new JsonObject();
            user.addProperty("role", "user");
            user.addProperty("content", "Reply with OK.");
            messages.add(user);
            body.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chatCompletionsUrl(resolvedBaseUrl)))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + resolvedApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode() + " from provider.");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Connection test failed. " + ex.getMessage(), ex);
        }
    }

    public AiPlan draftPlan(String userRequest) {
        if (userRequest == null || userRequest.isBlank()) {
            return AiPlan.empty("Tell me what you want me to change.");
        }
        if (!isConfigured()) {
            return AiPlan.empty("AI is not configured yet. " + configurationHint());
        }
        String content = callLlm(userRequest.trim());
        return parsePlan(content);
    }

    public List<ActionResult> executePlan(AiPlan plan) {
        List<ActionResult> results = new ArrayList<>();
        if (plan == null || plan.actions().isEmpty()) {
            results.add(new ActionResult(false, "No executable actions in this plan."));
            return results;
        }
        for (AiAction action : plan.actions()) {
            try {
                results.add(execute(action));
            } catch (Exception ex) {
                results.add(new ActionResult(false, action.label() + ": " + ex.getMessage()));
            }
        }
        DesktopRefreshBus.requestRefresh();
        return results;
    }

    public String generateWeeklyReport(Lab lab, User currentUser, WeeklyStats stats) {
        if (lab == null || currentUser == null || stats == null) {
            return "Weekly report is unavailable because the lab context is incomplete.";
        }
        if (!isConfigured()) {
            return fallbackWeeklyReport(stats);
        }
        String prompt = """
                You are a lab assistant. Generate a brief weekly summary (max 150 words) for lab "%s".
                Stats for last 7 days: %s.
                Respond in the same language as the user's locale (%s). Be concise and actionable.
                """.formatted(lab.getName(), GSON.toJson(stats), Locale.getDefault().toLanguageTag());
        try {
            String content = callLlm(prompt);
            return content == null || content.isBlank() ? fallbackWeeklyReport(stats) : content.trim();
        } catch (Exception ex) {
            return fallbackWeeklyReport(stats);
        }
    }

    private String callLlm(String userRequest) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", model());
            body.addProperty("temperature", 0.1);
            body.add("messages", messages(userRequest));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chatCompletionsUrl()))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("LLM request failed: HTTP " + response.statusCode());
            }
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            return root.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        } catch (Exception ex) {
            throw new IllegalStateException("AI could not create a plan. " + ex.getMessage(), ex);
        }
    }

    private JsonArray messages(String userRequest) {
        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", systemPrompt());
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", context() + "\n\nUser request:\n" + userRequest);
        messages.add(system);
        messages.add(user);
        return messages;
    }

    private String systemPrompt() {
        return """
                You are LabFlow's in-app AI operator. Return ONLY valid JSON, no markdown.
                You may produce these action types:
                SET_ROLE {username, role}
                ADD_MEMBER {username, role}
                REMOVE_MEMBER {username}
                CREATE_FULL_LAB {labName, theme, importTestKits}
                IMPORT_TEST_KITS {}
                RENAME_LAB {labName}
                CHANGE_THEME {theme}
                CREATE_GUEST {username, password, fullName, role}
                ARCHIVE_EQUIPMENT {equipmentId}
                RESTORE_EQUIPMENT {equipmentId}
                RETIRE_EQUIPMENT {equipmentId}
                MARK_MAINTENANCE_COMPLETE {equipmentId, notes}
                ADD_STOCK {equipmentId, quantity, notes}
                CONSUME_STOCK {equipmentId, quantity, notes}
                APPROVE_RESERVATION {reservationId}
                REJECT_RESERVATION {reservationId}
                CANCEL_RESERVATION {reservationId}
                COMPLETE_RESERVATION {reservationId}
                SUMMARIZE_LAB {}
                Roles: ADMIN, PROFESSOR, TECHNICIAN, STUDENT, GUEST.
                Themes: RED, GREEN, PURPLE, BLUE.
                Use CREATE_FULL_LAB for requests to make a complete lab; set importTestKits true unless the user says not to.
                Use CREATE_GUEST only when the user explicitly asks to create accounts.
                Use equipmentId and reservationId from the provided context whenever an equipment or reservation action is requested.
                You may return multiple actions when the user asks for a sequence.
                Respect the current user's role and lab scope. Do not produce actions outside the current lab context.
                Do not invent existing usernames. If a username is not provided for member operations, produce no action and explain what is missing.
                Response schema:
                {"reply":"short friendly summary","actions":[{"type":"...","username":"...","role":"...","labName":"...","theme":"...","importTestKits":true,"password":"...","fullName":"...","equipmentId":0,"reservationId":0,"quantity":0,"notes":"..."}]}
                """;
    }

    private String context() {
        Lab lab = session.getCurrentLab();
        StringBuilder builder = new StringBuilder();
        builder.append("Current user: ").append(session.getCurrentUser() == null ? "none" : session.getCurrentUser().getUsername()).append('\n');
        builder.append("Current role: ").append(session.getEffectiveRole()).append('\n');
        builder.append("Is lab owner: ").append(session.isLabOwner()).append('\n');
        builder.append("Current lab: ").append(lab == null ? "none" : lab.getName()).append('\n');
        if (lab != null) {
            builder.append("Members:\n");
            for (User member : labService.getMembers(lab.getId())) {
                builder.append("- ").append(member.getUsername()).append(" role=").append(member.getRole()).append('\n');
            }
            builder.append("Equipment in current lab (id | name | status | type | location):\n");
            equipmentService.getAllEquipment().stream()
                    .limit(40)
                    .forEach(equipment -> builder.append("- ")
                            .append(equipment.getId()).append(" | ")
                            .append(equipment.getName()).append(" | ")
                            .append(equipment.getStatus()).append(" | ")
                            .append(equipment.getItemType()).append(" | ")
                            .append(equipment.getLocation()).append('\n'));
            builder.append("Reservations in current lab (id | equipment | user | status | start -> end):\n");
            reservationService.getAllReservations().stream()
                    .limit(25)
                    .forEach(reservation -> builder.append("- ")
                            .append(reservation.getId()).append(" | ")
                            .append(reservation.getEquipmentName()).append(" | ")
                            .append(reservation.getUsername()).append(" | ")
                            .append(reservation.getStatus()).append(" | ")
                            .append(reservation.getStartDateTime()).append(" -> ")
                            .append(reservation.getEndDateTime()).append('\n'));
        }
        return builder.toString();
    }

    private AiPlan parsePlan(String content) {
        try {
            String json = cleanJson(content);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String reply = text(root, "reply", "I created a plan.");
            List<AiAction> actions = new ArrayList<>();
            JsonArray array = root.has("actions") && root.get("actions").isJsonArray()
                    ? root.getAsJsonArray("actions")
                    : new JsonArray();
            for (JsonElement element : array) {
                if (element.isJsonObject()) {
                    AiAction.fromJson(element.getAsJsonObject()).ifPresent(actions::add);
                }
            }
            return new AiPlan(reply, actions);
        } catch (Exception ex) {
            return AiPlan.empty("AI returned a response I could not safely execute. Try wording the request more directly.");
        }
    }

    private ActionResult execute(AiAction action) {
        return switch (action.type()) {
            case SET_ROLE -> setRole(action);
            case ADD_MEMBER -> addMember(action);
            case REMOVE_MEMBER -> removeMember(action);
            case CREATE_FULL_LAB -> createFullLab(action);
            case IMPORT_TEST_KITS -> importTestKits();
            case RENAME_LAB -> renameLab(action);
            case CHANGE_THEME -> changeTheme(action);
            case CREATE_GUEST -> createGuest(action);
            case ARCHIVE_EQUIPMENT -> archiveEquipment(action);
            case RESTORE_EQUIPMENT -> restoreEquipment(action);
            case RETIRE_EQUIPMENT -> retireEquipment(action);
            case MARK_MAINTENANCE_COMPLETE -> markMaintenanceComplete(action);
            case ADD_STOCK -> addStock(action);
            case CONSUME_STOCK -> consumeStock(action);
            case APPROVE_RESERVATION -> approveReservation(action);
            case REJECT_RESERVATION -> rejectReservation(action);
            case CANCEL_RESERVATION -> cancelReservation(action);
            case COMPLETE_RESERVATION -> completeReservation(action);
            case SUMMARIZE_LAB -> summarizeLab();
            case UNKNOWN -> new ActionResult(false, "Unknown action: " + action.label());
        };
    }

    private ActionResult setRole(AiAction action) {
        int labId = requireLab();
        User user = findMember(action.username())
                .orElseThrow(() -> new IllegalArgumentException("No member found with username @" + action.username()));
        Role role = parseRole(action.role());
        labService.updateMemberRole(labId, user.getId(), role);
        return new ActionResult(true, "Set @" + user.getUsername() + " to " + role.getDisplayName() + ".");
    }

    private ActionResult addMember(AiAction action) {
        int labId = requireLab();
        Role role = parseRole(action.role());
        labService.addExistingMemberByUsername(labId, action.username(), role);
        return new ActionResult(true, "Added @" + action.username() + " as " + role.getDisplayName() + ".");
    }

    private ActionResult removeMember(AiAction action) {
        int labId = requireLab();
        User user = findMember(action.username())
                .orElseThrow(() -> new IllegalArgumentException("No member found with username @" + action.username()));
        labService.removeMember(labId, user.getId());
        return new ActionResult(true, "Removed @" + user.getUsername() + " from the lab.");
    }

    private ActionResult createFullLab(AiAction action) {
        int userId = session.getCurrentUserId();
        if (userId <= 0) {
            throw new IllegalArgumentException("Log in before creating labs.");
        }
        String name = blank(action.labName()) ? "AI Lab" : action.labName().trim();
        Lab lab = labService.createLab(userId, name)
                .orElseThrow(() -> new IllegalArgumentException("Could not create lab."));
        session.setCurrentLab(lab);
        ThemeManager.ColorPalette palette = parsePalette(action.theme());
        if (palette != null) {
            labService.updateColorPalette(lab.getId(), palette);
            ThemeManager.setCurrentLabColorPalette(palette);
        }
        StringBuilder message = new StringBuilder("Created and selected lab \"" + lab.getName() + "\".");
        if (action.importTestKits()) {
            TestKitImportService.ImportSummary summary = testKitImportService.importDefaultKits();
            message.append(" Added ").append(summary.equipmentCount()).append(" kit items.");
        }
        return new ActionResult(true, message.toString());
    }

    private ActionResult importTestKits() {
        requireLab();
        TestKitImportService.ImportSummary summary = testKitImportService.importDefaultKits();
        return new ActionResult(true, "Kit import complete: added " + summary.equipmentCount()
                + ", repaired " + summary.repairedContainerCount()
                + ", skipped " + summary.skippedEquipmentCount() + ".");
    }

    private ActionResult renameLab(AiAction action) {
        int labId = requireLab();
        if (blank(action.labName())) {
            throw new IllegalArgumentException("Lab name is missing.");
        }
        labService.renameLab(labId, action.labName());
        session.getCurrentLab().setName(action.labName().trim());
        return new ActionResult(true, "Renamed current lab to \"" + action.labName().trim() + "\".");
    }

    private ActionResult changeTheme(AiAction action) {
        int labId = requireLab();
        ThemeManager.ColorPalette palette = parsePalette(action.theme());
        if (palette == null) {
            throw new IllegalArgumentException("Theme must be RED, GREEN, PURPLE or BLUE.");
        }
        labService.updateColorPalette(labId, palette);
        ThemeManager.setCurrentLabColorPalette(palette);
        return new ActionResult(true, "Changed lab theme to " + palette.getDisplayName() + ".");
    }

    private ActionResult createGuest(AiAction action) {
        int labId = requireLab();
        if (blank(action.username()) || blank(action.password())) {
            throw new IllegalArgumentException("Guest username and password are required.");
        }
        User user = labService.createGuestUser(labId, action.username(), action.password(), action.fullName())
                .orElseThrow(() -> new IllegalArgumentException("Could not create guest."));
        if (!blank(action.role())) {
            Role role = parseRole(action.role());
            labService.updateMemberRole(labId, user.getId(), role);
        }
        return new ActionResult(true, "Created guest @" + user.getUsername() + ".");
    }

    private ActionResult archiveEquipment(AiAction action) {
        if (action.equipmentId() <= 0) {
            throw new IllegalArgumentException("equipmentId is required for archive.");
        }
        Equipment equipment = equipmentService.getEquipmentById(action.equipmentId())
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));
        equipmentService.archiveEquipment(action.equipmentId());
        return new ActionResult(true, "Archived equipment " + equipment.getName() + ".");
    }

    private ActionResult restoreEquipment(AiAction action) {
        if (action.equipmentId() <= 0) {
            throw new IllegalArgumentException("equipmentId is required for restore.");
        }
        Equipment equipment = equipmentService.getEquipmentById(action.equipmentId())
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));
        equipmentService.restoreEquipment(action.equipmentId());
        return new ActionResult(true, "Restored equipment " + equipment.getName() + ".");
    }

    private ActionResult retireEquipment(AiAction action) {
        if (action.equipmentId() <= 0) {
            throw new IllegalArgumentException("equipmentId is required for retire.");
        }
        Equipment equipment = equipmentService.getEquipmentById(action.equipmentId())
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));
        equipmentService.retireEquipment(action.equipmentId());
        return new ActionResult(true, "Retired equipment " + equipment.getName() + ".");
    }

    private ActionResult markMaintenanceComplete(AiAction action) {
        if (action.equipmentId() <= 0) {
            throw new IllegalArgumentException("equipmentId is required for maintenance.");
        }
        Equipment equipment = equipmentService.getEquipmentById(action.equipmentId())
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));
        equipmentService.markMaintenanceCompleted(action.equipmentId(), action.notes());
        return new ActionResult(true, "Marked maintenance completed for " + equipment.getName() + ".");
    }

    private ActionResult addStock(AiAction action) {
        if (action.equipmentId() <= 0 || action.quantity() <= 0) {
            throw new IllegalArgumentException("equipmentId and positive quantity are required for stock add.");
        }
        Equipment equipment = equipmentService.getEquipmentById(action.equipmentId())
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));
        equipmentService.addStock(action.equipmentId(), action.quantity(), action.notes());
        return new ActionResult(true, "Added " + action.quantity() + " stock units to " + equipment.getName() + ".");
    }

    private ActionResult consumeStock(AiAction action) {
        if (action.equipmentId() <= 0 || action.quantity() <= 0) {
            throw new IllegalArgumentException("equipmentId and positive quantity are required for stock consume.");
        }
        Equipment equipment = equipmentService.getEquipmentById(action.equipmentId())
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));
        equipmentService.consumeStock(action.equipmentId(), action.quantity(), action.notes());
        return new ActionResult(true, "Consumed " + action.quantity() + " stock units from " + equipment.getName() + ".");
    }

    private ActionResult approveReservation(AiAction action) {
        Reservation reservation = requireReservation(action.reservationId());
        reservationService.approveReservation(action.reservationId());
        return new ActionResult(true, "Approved reservation #" + reservation.getId() + " for " + reservation.getEquipmentName() + ".");
    }

    private ActionResult rejectReservation(AiAction action) {
        Reservation reservation = requireReservation(action.reservationId());
        reservationService.rejectReservation(action.reservationId());
        return new ActionResult(true, "Rejected reservation #" + reservation.getId() + " for " + reservation.getEquipmentName() + ".");
    }

    private ActionResult cancelReservation(AiAction action) {
        Reservation reservation = requireReservation(action.reservationId());
        reservationService.cancelReservation(action.reservationId());
        return new ActionResult(true, "Cancelled reservation #" + reservation.getId() + " for " + reservation.getEquipmentName() + ".");
    }

    private ActionResult completeReservation(AiAction action) {
        Reservation reservation = requireReservation(action.reservationId());
        reservationService.convertToBorrowing(action.reservationId());
        return new ActionResult(true, "Converted reservation #" + reservation.getId() + " into a borrowing.");
    }

    private ActionResult summarizeLab() {
        int labId = requireLab();
        List<User> members = labService.getMembers(labId);
        long admins = members.stream().filter(user -> user.getRole() == Role.ADMIN).count();
        long technicians = members.stream().filter(user -> user.getRole() == Role.TECHNICIAN).count();
        return new ActionResult(true, "Current lab has " + members.size() + " members, " + admins + " admins and " + technicians + " technicians.");
    }

    private Optional<User> findMember(String username) {
        if (blank(username)) {
            return Optional.empty();
        }
        int labId = requireLab();
        return labService.getMembers(labId).stream()
                .filter(user -> user.getUsername() != null && user.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst();
    }

    private Reservation requireReservation(int reservationId) {
        if (reservationId <= 0) {
            throw new IllegalArgumentException("reservationId is required.");
        }
        return reservationService.getAllReservations().stream()
                .filter(reservation -> reservation.getId() == reservationId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));
    }

    private int requireLab() {
        int labId = session.getCurrentLabId();
        if (labId <= 0) {
            throw new IllegalArgumentException("Select a lab first.");
        }
        return labId;
    }

    private Role parseRole(String value) {
        if (blank(value)) {
            return Role.STUDENT;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "OWNER", "ADMINISTRATOR", "ADMIN" -> Role.ADMIN;
            case "PROF", "TEACHER", "PROFESSOR" -> Role.PROFESSOR;
            case "TECH", "TECHNICIAN" -> Role.TECHNICIAN;
            case "ELEV", "STUDENT" -> Role.STUDENT;
            case "GUEST", "VIEWER" -> Role.GUEST;
            default -> throw new IllegalArgumentException("Unknown role: " + value);
        };
    }

    private ThemeManager.ColorPalette parsePalette(String value) {
        if (blank(value)) {
            return null;
        }
        try {
            return ThemeManager.ColorPalette.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return null;
        }
    }

    private String cleanJson(String content) {
        String value = content == null ? "" : content.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return value.substring(start, end + 1);
        }
        return value;
    }

    private static String text(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }

    private String apiKey() {
        return firstNonBlank(
                getSavedApiKey(),
                System.getProperty("labflow.ai.apiKey"),
                System.getenv("LABFLOW_AI_API_KEY"),
                System.getenv("GEMINI_API_KEY"),
                System.getenv("OPENAI_API_KEY"),
                System.getenv("OPENROUTER_API_KEY"),
                System.getenv("GROQ_API_KEY")
        );
    }

    private String model() {
        return firstNonBlank(getSavedModel(), System.getProperty("labflow.ai.model"), System.getenv("LABFLOW_AI_MODEL"), DEFAULT_MODEL);
    }

    private String chatCompletionsUrl() {
        return chatCompletionsUrl(rawBaseUrl());
    }

    private String rawBaseUrl() {
        return firstNonBlank(getSavedBaseUrl(), System.getProperty("labflow.ai.baseUrl"), System.getenv("LABFLOW_AI_BASE_URL"), DEFAULT_BASE_URL);
    }

    private String chatCompletionsUrl(String base) {
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        return base.replaceAll("/+$", "") + "/chat/completions";
    }

    private String fallbackWeeklyReport(WeeklyStats stats) {
        return "Weekly summary: "
                + stats.getNewBorrows() + " new borrows, "
                + stats.getReturnsOnTime() + " on-time returns, "
                + stats.getLateReturns() + " late returns, "
                + stats.getNewFaults() + " new faults, "
                + stats.getResolvedFaults() + " resolved faults, "
                + stats.getLowStockItems() + " low-stock items, "
                + stats.getOverdueEquipment() + " overdue maintenance items. Focus on late returns and unresolved operational risk.";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public enum ActionType {
        SET_ROLE,
        ADD_MEMBER,
        REMOVE_MEMBER,
        CREATE_FULL_LAB,
        IMPORT_TEST_KITS,
        RENAME_LAB,
        CHANGE_THEME,
        CREATE_GUEST,
        ARCHIVE_EQUIPMENT,
        RESTORE_EQUIPMENT,
        RETIRE_EQUIPMENT,
        MARK_MAINTENANCE_COMPLETE,
        ADD_STOCK,
        CONSUME_STOCK,
        APPROVE_RESERVATION,
        REJECT_RESERVATION,
        CANCEL_RESERVATION,
        COMPLETE_RESERVATION,
        SUMMARIZE_LAB,
        UNKNOWN
    }

    public record AiPlan(String reply, List<AiAction> actions) {
        public static AiPlan empty(String reply) {
            return new AiPlan(reply, List.of());
        }
    }

    public record AiAction(
            ActionType type,
            String username,
            String role,
            String labName,
            String theme,
            boolean importTestKits,
            String password,
            String fullName,
            int equipmentId,
            int reservationId,
            int quantity,
            String notes
    ) {
        static Optional<AiAction> fromJson(JsonObject json) {
            String typeName = text(json, "type", "UNKNOWN").trim().toUpperCase(Locale.ROOT);
            ActionType type;
            try {
                type = ActionType.valueOf(typeName);
            } catch (Exception ex) {
                type = ActionType.UNKNOWN;
            }
            return Optional.of(new AiAction(
                    type,
                    text(json, "username", ""),
                    text(json, "role", ""),
                    text(json, "labName", ""),
                    text(json, "theme", ""),
                    json.has("importTestKits") && json.get("importTestKits").getAsBoolean(),
                    text(json, "password", ""),
                    text(json, "fullName", ""),
                    json.has("equipmentId") && !json.get("equipmentId").isJsonNull() ? json.get("equipmentId").getAsInt() : 0,
                    json.has("reservationId") && !json.get("reservationId").isJsonNull() ? json.get("reservationId").getAsInt() : 0,
                    json.has("quantity") && !json.get("quantity").isJsonNull() ? json.get("quantity").getAsInt() : 0,
                    text(json, "notes", "")
            ));
        }

        public String label() {
            return switch (type) {
                case SET_ROLE -> "Set @" + username + " role to " + role;
                case ADD_MEMBER -> "Add @" + username + " as " + role;
                case REMOVE_MEMBER -> "Remove @" + username;
                case CREATE_FULL_LAB -> "Create full lab \"" + labName + "\"";
                case IMPORT_TEST_KITS -> "Import test kits";
                case RENAME_LAB -> "Rename lab to \"" + labName + "\"";
                case CHANGE_THEME -> "Change theme to " + theme;
                case CREATE_GUEST -> "Create guest @" + username;
                case ARCHIVE_EQUIPMENT -> "Archive equipment #" + equipmentId;
                case RESTORE_EQUIPMENT -> "Restore equipment #" + equipmentId;
                case RETIRE_EQUIPMENT -> "Retire equipment #" + equipmentId;
                case MARK_MAINTENANCE_COMPLETE -> "Complete maintenance for equipment #" + equipmentId;
                case ADD_STOCK -> "Add " + quantity + " stock to equipment #" + equipmentId;
                case CONSUME_STOCK -> "Consume " + quantity + " stock from equipment #" + equipmentId;
                case APPROVE_RESERVATION -> "Approve reservation #" + reservationId;
                case REJECT_RESERVATION -> "Reject reservation #" + reservationId;
                case CANCEL_RESERVATION -> "Cancel reservation #" + reservationId;
                case COMPLETE_RESERVATION -> "Convert reservation #" + reservationId + " to borrowing";
                case SUMMARIZE_LAB -> "Summarize current lab";
                case UNKNOWN -> "Unknown action";
            };
        }
    }

    public record ActionResult(boolean success, String message) {
    }
}
