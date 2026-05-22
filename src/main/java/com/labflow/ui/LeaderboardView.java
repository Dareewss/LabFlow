package com.labflow.ui;

import com.labflow.dao.GamificationDAO;
import com.labflow.model.LeaderboardEntry;
import com.labflow.model.PointsHistoryEntry;
import com.labflow.util.LanguageManager;
import com.labflow.util.SessionManager;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class LeaderboardView extends VBox implements RefreshableView {
    private final GamificationDAO gamificationDAO = new GamificationDAO();
    private final SessionManager session = SessionManager.getInstance();
    private final TableView<LeaderboardEntry> leaderboardTable = new TableView<>();
    private final TableView<PointsHistoryEntry> historyTable = new TableView<>();
    private StackPane leaderboardShell;
    private StackPane historyShell;
    private EmptyStateView leaderboardEmpty;
    private EmptyStateView historyEmpty;

    public LeaderboardView() {
        initializeUi();
        refreshFromExternalChange();
    }

    private void initializeUi() {
        getStyleClass().add("page");
        setPadding(new Insets(20));
        setSpacing(14);

        HBox header = UIComponents.headerWithActions(
                t("leaderboard.title", "Leaderboard"),
                t("leaderboard.subtitle", "Track student points, positive behavior, and personal progress in this lab.")
        );

        HBox topCards = new HBox(12,
                summaryCard("1", t("leaderboard.firstPlace", "Top spot"), medalIcon(1), "success"),
                summaryCard(String.valueOf(session.getCurrentUserId() > 0 ? gamificationDAO.getPoints(session.getCurrentUserId(), session.getCurrentLabId()) : 0),
                        t("leaderboard.myPoints", "My points"), circleIcon("P", "info"), "info"),
                summaryCard(String.valueOf(gamificationDAO.getLeaderboard(session.getCurrentLabId()).size()),
                        t("leaderboard.activeMembers", "Active members"), circleIcon("U", "warning"), "warning")
        );

        leaderboardTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        leaderboardTable.getColumns().add(textColumn(t("leaderboard.rank", "Rank"), entry -> medal(entry.getRank())));
        leaderboardTable.getColumns().add(textColumn(t("leaderboard.username", "Username"), LeaderboardEntry::getUsername));
        leaderboardTable.getColumns().add(numberColumn(t("leaderboard.points", "Points"), LeaderboardEntry::getPoints));
        leaderboardTable.getColumns().get(0).setPrefWidth(120);
        leaderboardTable.getColumns().get(2).setPrefWidth(120);
        leaderboardTable.setRowFactory(view -> new TableRow<>() {
            @Override
            protected void updateItem(LeaderboardEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (session.getCurrentUser() != null && item.getUsername().equalsIgnoreCase(session.getCurrentUser().getUsername())) {
                    setStyle("-fx-background-color: rgba(107, 15, 26, 0.10); -fx-font-weight: 700;");
                    getStyleClass().add("leaderboard-row-current");
                } else {
                    setStyle("");
                    getStyleClass().remove("leaderboard-row-current");
                }
            }
        });
        @SuppressWarnings("unchecked")
        TableColumn<LeaderboardEntry, String> rankColumn = (TableColumn<LeaderboardEntry, String>) leaderboardTable.getColumns().get(0);
        rankColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getIndex() < 0 || getIndex() >= leaderboardTable.getItems().size()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                LeaderboardEntry entry = leaderboardTable.getItems().get(getIndex());
                setGraphic(rankGraphic(entry));
                setText(null);
            }
        });
        @SuppressWarnings("unchecked")
        TableColumn<LeaderboardEntry, Number> pointsColumn = (TableColumn<LeaderboardEntry, Number>) leaderboardTable.getColumns().get(2);
        pointsColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label value = UIComponents.badge(String.valueOf(item.intValue()), item.intValue() >= 0 ? "info" : "danger");
                setGraphic(value);
                setText(null);
            }
        });

        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        historyTable.getColumns().add(historyColumn(t("leaderboard.date", "Date"), entry -> entry.getCreatedAt() == null ? "" : entry.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        historyTable.getColumns().add(historyColumn(t("leaderboard.reason", "Reason"), PointsHistoryEntry::getReason));
        historyTable.getColumns().add(historyColumn(t("leaderboard.points", "Points"), entry -> (entry.getPointsDelta() > 0 ? "+" : "") + entry.getPointsDelta()));
        @SuppressWarnings("unchecked")
        TableColumn<PointsHistoryEntry, String> historyPointsColumn = (TableColumn<PointsHistoryEntry, String>) historyTable.getColumns().get(2);
        historyPointsColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getIndex() < 0 || getIndex() >= historyTable.getItems().size()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                PointsHistoryEntry entry = historyTable.getItems().get(getIndex());
                Label badge = UIComponents.badge(item, entry.getPointsDelta() >= 0 ? "success" : "danger");
                setGraphic(badge);
                setText(null);
            }
        });

        leaderboardEmpty = new EmptyStateView("\uD83C\uDFC6", t("leaderboard.noActivityTitle", "No activity yet"),
                t("leaderboard.noActivitySubtitle", "Start borrowing equipment to earn points."), null, null);
        leaderboardEmpty.setVisible(false);
        leaderboardEmpty.setManaged(false);
        leaderboardShell = new StackPane(leaderboardTable, leaderboardEmpty);
        VBox leaderboardCard = UIComponents.card(t("leaderboard.top10", "Top 10 in this lab"), leaderboardShell);
        VBox.setVgrow(leaderboardTable, Priority.ALWAYS);
        historyEmpty = new EmptyStateView("\uD83D\uDCC8", t("leaderboard.noHistoryTitle", "No points history yet"),
                t("leaderboard.noHistorySubtitle", "Your rewards and penalties will appear here."), null, null);
        historyEmpty.setVisible(false);
        historyEmpty.setManaged(false);
        historyShell = new StackPane(historyTable, historyEmpty);
        VBox historyCard = UIComponents.card(t("leaderboard.myHistory", "My points history"), historyShell);
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        HBox badges = new HBox(10,
                iconBadge("Z", t("leaderboard.zeroLateReturns", "Zero late returns"), "success"),
                iconBadge("R", t("leaderboard.topReturner", "Top Returner"), "info"),
                iconBadge("F", t("leaderboard.helpfulReporter", "Helpful Reporter"), "warning"));
        badges.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, topCards, badges, leaderboardCard, historyCard);
        VBox.setVgrow(leaderboardCard, Priority.ALWAYS);
        VBox.setVgrow(historyCard, Priority.ALWAYS);
    }

    @Override
    public void refreshFromExternalChange() {
        int labId = session.getCurrentLabId();
        int userId = session.getCurrentUserId();
        List<LeaderboardEntry> leaderboard = gamificationDAO.getLeaderboard(labId).stream().limit(10).toList();
        leaderboardTable.setItems(FXCollections.observableArrayList(leaderboard));
        historyTable.setItems(FXCollections.observableArrayList(gamificationDAO.getHistory(userId, labId)));
        boolean leaderboardIsEmpty = leaderboard.isEmpty();
        leaderboardEmpty.setVisible(leaderboardIsEmpty);
        leaderboardEmpty.setManaged(leaderboardIsEmpty);
        leaderboardTable.setVisible(!leaderboardIsEmpty);
        leaderboardTable.setManaged(!leaderboardIsEmpty);

        boolean historyIsEmpty = historyTable.getItems().isEmpty();
        historyEmpty.setVisible(historyIsEmpty);
        historyEmpty.setManaged(historyIsEmpty);
        historyTable.setVisible(!historyIsEmpty);
        historyTable.setManaged(!historyIsEmpty);
    }

    private TableColumn<LeaderboardEntry, String> textColumn(String title, java.util.function.Function<LeaderboardEntry, String> mapper) {
        TableColumn<LeaderboardEntry, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(mapper.apply(data.getValue())));
        return column;
    }

    private TableColumn<PointsHistoryEntry, String> historyColumn(String title, java.util.function.Function<PointsHistoryEntry, String> mapper) {
        TableColumn<PointsHistoryEntry, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(mapper.apply(data.getValue())));
        return column;
    }

    private TableColumn<LeaderboardEntry, Number> numberColumn(String title, java.util.function.ToIntFunction<LeaderboardEntry> mapper) {
        TableColumn<LeaderboardEntry, Number> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(mapper.applyAsInt(data.getValue())));
        return column;
    }

    private String medal(int rank) {
        return switch (rank) {
            case 1 -> "\uD83E\uDD47 1";
            case 2 -> "\uD83E\uDD48 2";
            case 3 -> "\uD83E\uDD49 3";
            default -> String.valueOf(rank);
        };
    }

    private VBox summaryCard(String value, String title, Node icon, String badgeType) {
        VBox card = UIComponents.card(null);
        card.getStyleClass().add("leaderboard-summary-card");
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(icon, spacer, UIComponents.badge(title, badgeType));
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        Label helper = new Label(title);
        helper.getStyleClass().add("stat-helper");
        card.getChildren().addAll(top, valueLabel, helper);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox rankGraphic(LeaderboardEntry entry) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(medalIcon(entry.getRank()));
        Label rank = new Label(entry.getRank() <= 3 ? t("leaderboard.top", "Top") + " " + entry.getRank() : "#" + entry.getRank());
        rank.getStyleClass().add("small-muted-label");
        row.getChildren().add(rank);
        return row;
    }

    private Node medalIcon(int rank) {
        return switch (rank) {
            case 1 -> circleIcon("\uD83E\uDD47", "success");
            case 2 -> circleIcon("\uD83E\uDD48", "info");
            case 3 -> circleIcon("\uD83E\uDD49", "warning");
            default -> circleIcon(String.valueOf(rank), "muted");
        };
    }

    private HBox iconBadge(String glyph, String text, String type) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(circleIcon(glyph, type), UIComponents.badge(text, type));
        return row;
    }

    private Label circleIcon(String glyph, String type) {
        Label icon = new Label(glyph);
        icon.getStyleClass().addAll("leaderboard-icon", "leaderboard-icon-" + type);
        icon.setAlignment(Pos.CENTER);
        icon.setMinSize(28, 28);
        icon.setPrefSize(28, 28);
        icon.setMaxSize(28, 28);
        if ("muted".equals(type)) {
            icon.setTextFill(Color.web("#6B7280"));
        }
        return icon;
    }

    private String t(String key, String fallback) {
        return LanguageManager.text(key, fallback);
    }
}
