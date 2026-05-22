package com.labflow.ui;

import com.labflow.model.CalendarEvent;
import com.labflow.service.CalendarService;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CalendarView extends VBox implements RefreshableView {
    private final CalendarService calendarService = new CalendarService();
    private final VBox content = new VBox(12);

    public CalendarView() {
        initializeUI();
    }

    private void initializeUI() {
        getStyleClass().add("page");
        setPadding(new Insets(20));
        setSpacing(14);
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> refresh());
        HBox header = UIComponents.headerWithActions("Calendar",
                "See borrow due dates, reservations and maintenance reminders.",
                refresh);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        getChildren().addAll(header, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        UIComponents.decorateButtonsIn(this);
        refresh();
    }

    private HBox spacer() {
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private void refresh() {
        content.getChildren().clear();
        List<CalendarEvent> events = calendarService.getEvents();
        addGroup("Today", events.stream().filter(event -> sameDay(event, LocalDate.now())).toList());
        addGroup("Tomorrow", events.stream().filter(event -> sameDay(event, LocalDate.now().plusDays(1))).toList());
        addGroup("This Week", events.stream()
                .filter(event -> event.dateTime().toLocalDate().isAfter(LocalDate.now().plusDays(1))
                        && !event.dateTime().toLocalDate().isAfter(LocalDate.now().plusDays(7)))
                .toList());
        addGroup("Later", events.stream()
                .filter(event -> event.dateTime().toLocalDate().isAfter(LocalDate.now().plusDays(7)))
                .toList());
        if (content.getChildren().isEmpty()) {
            content.getChildren().add(empty("No calendar events yet."));
        }
    }

    private boolean sameDay(CalendarEvent event, LocalDate date) {
        return event.dateTime() != null && event.dateTime().toLocalDate().equals(date);
    }

    private void addGroup(String title, List<CalendarEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        VBox panel = new VBox(8);
        panel.getStyleClass().add("panel");
        Label label = new Label(title);
        label.getStyleClass().add("subsection-title");
        panel.getChildren().add(label);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (CalendarEvent event : events) {
            Label row = new Label(formatter.format(event.dateTime()) + " - " + event.type() + " - " + event.title() + " - " + event.status());
            row.getStyleClass().add("small-muted-label");
            row.setWrapText(true);
            panel.getChildren().add(row);
        }
        content.getChildren().add(panel);
    }

    private Label empty(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("small-muted-label");
        return label;
    }

    @Override
    public void refreshFromExternalChange() {
        refresh();
    }
}
