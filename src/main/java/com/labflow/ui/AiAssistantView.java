package com.labflow.ui;

import com.labflow.service.AiAssistantService;
import com.labflow.service.AiAssistantService.ActionResult;
import com.labflow.service.AiAssistantService.AiAction;
import com.labflow.service.AiAssistantService.AiPlan;
import com.labflow.util.NotificationUtil;
import com.labflow.util.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AiAssistantView extends VBox implements RefreshableView {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AiAssistantService assistantService = new AiAssistantService();
    private final Runnable shellRefresh;
    private final TextArea prompt = new TextArea();
    private final VBox conversation = new VBox(14);
    private final Button generateButton = UIComponents.primaryButton("Send to AI");
    private final Button runButton = UIComponents.secondaryButton("Run Plan");
    private final Label statusLabel = new Label("The AI can draft a plan first, then execute only the actions you approve.");
    private final LoadingOverlay loadingOverlay = new LoadingOverlay();
    private ScrollPane conversationScroll;
    private Node typingRow;
    private AiPlan currentPlan = AiPlan.empty("No plan yet.");

    public AiAssistantView(Runnable shellRefresh) {
        this.shellRefresh = shellRefresh;
        initializeUI();
    }

    private void initializeUI() {
        getStyleClass().addAll("page", "ai-chat-page");
        setPadding(new Insets(22));
        setSpacing(18);
        setAlignment(Pos.TOP_CENTER);

        if (!SessionManager.getInstance().isLoggedIn()) {
            getChildren().add(UIComponents.emptyState("AI Helper", "Log in before using the assistant.", null));
            return;
        }

        Label title = new Label("LabFlow AI");
        title.getStyleClass().add("ai-hero-title");
        Label subtitle = new Label("Talk to the assistant naturally. It will draft a safe plan before it changes anything.");
        subtitle.getStyleClass().add("small-muted-label");
        subtitle.setWrapText(true);

        FlowPane suggestions = new FlowPane(10, 10,
                quickPrompt("Make a full robotics lab with green theme"),
                quickPrompt("Set alex role to technician"),
                quickPrompt("Import test kits into this lab"),
                quickPrompt("Summarize this lab"));
        suggestions.setAlignment(Pos.CENTER_LEFT);

        prompt.setPromptText("Ask LabFlow AI to help with this lab...");
        prompt.setPrefRowCount(3);
        prompt.setWrapText(true);
        prompt.getStyleClass().add("ai-chat-input");
        prompt.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                generatePlan();
            }
        });

        generateButton.setOnAction(event -> generatePlan());
        runButton.setDisable(true);
        runButton.setOnAction(event -> runPlan());
        statusLabel.getStyleClass().add("ai-helper-text");
        statusLabel.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, statusLabel, spacer, runButton, generateButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox composer = new VBox(12, prompt, actions);
        composer.getStyleClass().add("ai-composer");
        composer.setMaxWidth(900);

        conversation.getStyleClass().add("ai-conversation");
        conversation.setMaxWidth(900);
        conversationScroll = new ScrollPane(conversation);
        conversationScroll.getStyleClass().add("ai-conversation-scroll");
        conversationScroll.setFitToWidth(true);
        conversationScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        conversationScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        conversationScroll.setMaxWidth(940);
        VBox.setVgrow(conversationScroll, Priority.ALWAYS);

        VBox hero = new VBox(12, title, subtitle, suggestions, composer);
        hero.getStyleClass().add("ai-hero");
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setMaxWidth(940);

        StackPane shell = new StackPane(new VBox(18, hero, conversationScroll), loadingOverlay);
        VBox.setVgrow(shell, Priority.ALWAYS);
        getChildren().add(shell);
        appendAssistantMessage(assistantService.isConfigured()
                ? "Ready when you are. Tell me what you want changed in the current lab."
                : assistantService.configurationHint(), null, false);
        UIComponents.decorateButtonsIn(this);
    }

    private Button quickPrompt(String text) {
        Button button = UIComponents.secondaryButton(text);
        button.getStyleClass().add("ai-suggestion");
        button.setOnAction(event -> {
            prompt.setText(text);
            prompt.requestFocus();
            prompt.positionCaret(prompt.getText().length());
        });
        return button;
    }

    private void generatePlan() {
        String request = prompt.getText() == null ? "" : prompt.getText().trim();
        if (request.isBlank()) {
            NotificationUtil.showWarning("Write a command first.");
            return;
        }
        appendUserMessage(request);
        prompt.clear();
        showTyping("LabFlow AI is drafting a plan...");
        loadingOverlay.show("Drafting an AI plan...");
        setBusy(true);

        Task<AiPlan> task = new Task<>() {
            @Override
            protected AiPlan call() {
                return assistantService.draftPlan(request);
            }
        };
        task.setOnSucceeded(event -> {
            hideTyping();
            loadingOverlay.hide();
            setBusy(false);
            refreshPlan(task.getValue());
        });
        task.setOnFailed(event -> {
            hideTyping();
            loadingOverlay.hide();
            setBusy(false);
            String message = task.getException() == null ? "AI failed." : task.getException().getMessage();
            appendAssistantMessage("AI failed: " + message, null, true);
        });
        Thread thread = new Thread(task, "labflow-ai-plan");
        thread.setDaemon(true);
        thread.start();
    }

    private void runPlan() {
        if (currentPlan == null || currentPlan.actions().isEmpty()) {
            NotificationUtil.showWarning("Generate a plan with actions first.");
            return;
        }

        showTyping("Running the approved plan...");
        loadingOverlay.show("Running approved AI actions...");
        setBusy(true);
        Task<List<ActionResult>> task = new Task<>() {
            @Override
            protected List<ActionResult> call() {
                return assistantService.executePlan(currentPlan);
            }
        };
        task.setOnSucceeded(event -> {
            hideTyping();
            loadingOverlay.hide();
            setBusy(false);
            refreshResults(task.getValue());
            if (shellRefresh != null) {
                Platform.runLater(shellRefresh);
            }
        });
        task.setOnFailed(event -> {
            hideTyping();
            loadingOverlay.hide();
            setBusy(false);
            String message = task.getException() == null ? "Plan execution failed." : task.getException().getMessage();
            appendAssistantMessage(message, null, true);
        });
        Thread thread = new Thread(task, "labflow-ai-execute");
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshPlan(AiPlan plan) {
        currentPlan = plan == null ? AiPlan.empty("No plan.") : plan;
        VBox details = null;
        if (!currentPlan.actions().isEmpty()) {
            details = new VBox(8);
            Label planTitle = new Label("Planned actions");
            planTitle.getStyleClass().add("ai-bubble-title");
            details.getChildren().add(planTitle);
            FlowPane chips = new FlowPane(8, 8);
            for (AiAction action : currentPlan.actions()) {
                Label chip = new Label(action.label());
                chip.getStyleClass().add("ai-plan-chip");
                chip.setWrapText(true);
                chips.getChildren().add(chip);
            }
            details.getChildren().add(chips);
        }
        appendAssistantMessage(currentPlan.reply(), details, false);
        runButton.setDisable(currentPlan.actions().isEmpty());
        statusLabel.setText(currentPlan.actions().isEmpty()
                ? "No executable actions yet. Ask more directly if you want the assistant to do something."
                : "Plan ready. Review it, then run it when it looks right.");
    }

    private void refreshResults(List<ActionResult> results) {
        VBox details = new VBox(8);
        Label title = new Label("Execution results");
        title.getStyleClass().add("ai-bubble-title");
        details.getChildren().add(title);
        if (results == null || results.isEmpty()) {
            details.getChildren().add(resultLabel(false, "No results."));
        } else {
            for (ActionResult result : results) {
                details.getChildren().add(resultLabel(result.success(), result.message()));
            }
        }
        appendAssistantMessage("Done. Here is what changed.", details, false);
    }

    private void appendUserMessage(String message) {
        conversation.getChildren().add(messageRow("You", message, "ai-user-bubble", Pos.CENTER_RIGHT, false, null));
        stickConversationToBottom();
    }

    private void appendAssistantMessage(String message, Node details, boolean error) {
        conversation.getChildren().add(messageRow("LabFlow AI", message, error ? "ai-error-bubble" : "ai-assistant-bubble",
                Pos.CENTER_LEFT, true, details));
        stickConversationToBottom();
    }

    private HBox messageRow(String author, String message, String bubbleClass, Pos alignment, boolean assistant, Node details) {
        Label authorLabel = new Label(author + " • " + LocalTime.now().format(TIME_FORMAT));
        authorLabel.getStyleClass().add("ai-message-author");

        Label messageLabel = new Label(message == null ? "" : message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("ai-message-text");

        VBox bubble = new VBox(8, authorLabel, messageLabel);
        if (details != null) {
            bubble.getChildren().add(details);
        }
        bubble.getStyleClass().addAll("ai-message-bubble", bubbleClass);
        bubble.setMaxWidth(assistant ? 700 : 640);

        HBox row = new HBox(bubble);
        row.setFillHeight(true);
        row.setAlignment(alignment);
        row.getStyleClass().addAll("ai-message-row", assistant ? "ai-message-row-assistant" : "ai-message-row-user");
        return row;
    }

    private Label resultLabel(boolean success, String message) {
        Label label = new Label((success ? "OK: " : "Failed: ") + message);
        label.setWrapText(true);
        label.getStyleClass().addAll("ai-result-row", success ? "ai-result-success" : "ai-result-error");
        return label;
    }

    private void showTyping(String message) {
        hideTyping();
        typingRow = messageRow("LabFlow AI", message, "ai-assistant-bubble", Pos.CENTER_LEFT, true, null);
        typingRow.getStyleClass().add("ai-typing-row");
        conversation.getChildren().add(typingRow);
        stickConversationToBottom();
    }

    private void hideTyping() {
        if (typingRow != null) {
            conversation.getChildren().remove(typingRow);
            typingRow = null;
        }
    }

    private void setBusy(boolean busy) {
        prompt.setDisable(busy);
        generateButton.setDisable(busy);
        runButton.setDisable(busy || currentPlan == null || currentPlan.actions().isEmpty());
    }

    private void stickConversationToBottom() {
        Platform.runLater(() -> conversationScroll.setVvalue(1.0));
    }

    @Override
    public void refreshFromExternalChange() {
        runButton.setDisable(currentPlan == null || currentPlan.actions().isEmpty());
    }
}
