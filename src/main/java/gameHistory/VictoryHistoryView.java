package gameHistory;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;


import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VictoryHistoryView extends StackPane {

    public VictoryHistoryView(int levelNumber, Runnable backToMenu) {
        getStyleClass().add("start-menu");
        getStyleClass().add(getMenuThemeClass(levelNumber));
        loadStyles();

        String bgColor = switch (levelNumber) {
            case 2 -> "#161210";    // Stone
            case 3 -> "#150A0C";    // Inferno
            default -> "#111520";   // Cryo
        };

        String accentColor = switch (levelNumber) {
            case 2 -> "#F0B030";
            case 3 -> "#F09040";
            default -> "#65F2A0";
        };

        String subtitleColor = switch (levelNumber) {
            case 2 -> "rgba(255, 241, 200, 0.80)";
            case 3 -> "rgba(255, 214, 220, 0.82)";
            default -> "rgba(214, 229, 255, 0.82)";
        };

        String titleColor = switch (levelNumber) {
            case 2 -> "#FFF8E6";
            case 3 -> "#FFF0F4";
            default -> "#F5FBFF";
        };

        String cardBorderColor = switch (levelNumber) {
            case 2 -> "rgba(217, 201, 168, 0.16)";
            case 3 -> "rgba(212, 168, 176, 0.16)";
            default -> "rgba(210, 234, 255, 0.16)";
        };

        String bestBadgeTextColor = switch (levelNumber) {
            case 2 -> "#2A1A08";
            case 3 -> "#2A1010";
            default -> "#07141A";
        };

        String protocolName = switch (levelNumber) {
            case 2 -> "STONE DEPTHS PROTOCOL";
            case 3 -> "INFERNO DEPTHS PROTOCOL";
            default -> "CRYO DEPTHS PROTOCOL";
        };

        setStyle("-fx-background-color: " + bgColor + ";");

        VBox mainLayout = new VBox(24);
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setPadding(new Insets(40, 40, 28, 40));
        mainLayout.setMaxWidth(760);
        mainLayout.setFillWidth(true);

        Label titleLabel = new Label("ARCHIVED VICTORIES");
        titleLabel.getStyleClass().add("pause-overlay-heading");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-text-fill: " + accentColor + ";");

        Label subtitleLabel = new Label(protocolName + " RECORDS");
        subtitleLabel.getStyleClass().add("victory-subtitle");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + subtitleColor + ";");

        VBox recordsContainer = new VBox(12);
        recordsContainer.setAlignment(Pos.TOP_CENTER);
        recordsContainer.setPadding(new Insets(10, 0, 10, 0));
        recordsContainer.setFillWidth(true);

        ScrollPane scrollPane = new ScrollPane(recordsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setMinHeight(360);
        scrollPane.setPrefViewportHeight(420);
        scrollPane.setMaxWidth(Double.MAX_VALUE);
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) ->
                recordsContainer.setPrefWidth(Math.max(0, newBounds.getWidth())));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().addAll("hud-card", "pause-menu-button", "pause-menu-secondary");
        backBtn.setOnAction(e -> backToMenu.run());

        mainLayout.getChildren().addAll(titleLabel, subtitleLabel, scrollPane, backBtn);
        getChildren().add(mainLayout);

        List<VictoryRecord> allRecords = VictoryHistoryManager.loadRecords();
        List<VictoryRecord> filtered = allRecords.stream()
                .filter(r -> r.getLevelNumber() == levelNumber)
                .sorted(Comparator.comparingLong(VictoryRecord::getTimeSeconds)
                        .thenComparing(VictoryRecord::getDate, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label noData = new Label("No successful extractions archived for this protocol.");
            noData.getStyleClass().add("victory-subtitle");
            noData.setStyle("-fx-padding: 40 0 0 0; -fx-text-fill: " + subtitleColor + ";");
            noData.setWrapText(true);
            noData.setMaxWidth(420);
            recordsContainer.getChildren().add(noData);
            return;
        }

        long bestTime = filtered.get(0).getTimeSeconds();

        for (VictoryRecord r : filtered) {
            boolean isBest = r.getTimeSeconds() == bestTime;

            HBox row = new HBox(20);
            row.getStyleClass().add("game-hud");
            row.setPadding(new Insets(14, 24, 14, 24));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(640);
            row.setFillHeight(true);

            long mins = r.getTimeSeconds() / 60;
            long secs = r.getTimeSeconds() % 60;
            String timeStr = String.format("%02d:%02d", mins, secs);

            Label timeLabel = new Label("⏱ " + timeStr);
            timeLabel.getStyleClass().add("hud-card-value");
            timeLabel.setStyle("-fx-text-fill: " + titleColor + ";");

            int hp = Math.max(0, Math.min(3, r.getHealth()));
            String stars;
            if (hp >= 3) stars = "★★★";
            else if (hp == 2) stars = "★★☆";
            else if (hp == 1) stars = "★☆☆";
            else stars = "☆☆☆";

            Label starsLabel = new Label(stars);
            starsLabel.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 16px; -fx-letter-spacing: 2px;");

            Label dateLabel = new Label("Date: " + safeText(r.getDate(), "Unknown"));
            dateLabel.getStyleClass().add("hud-card-title");
            dateLabel.setStyle("-fx-text-fill: " + subtitleColor + ";");

            StringBuilder artSb = new StringBuilder("Inventory: ");
            Map<String, Integer> artifacts = r.getCollectedArtifacts();
            if (artifacts != null) {
                artifacts.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> appendArtifact(artSb, entry.getKey(), entry.getValue()));
            }
            String artStr = artSb.length() > 11 ? artSb.substring(0, artSb.length() - 3) : "Inventory: No resources";

            Label artLabel = new Label(artStr);
            artLabel.getStyleClass().add("victory-subtitle");
            artLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + subtitleColor + ";");
            artLabel.setWrapText(true);
            artLabel.setMaxWidth(420);

            HBox dateAndStars = new HBox(12, dateLabel, starsLabel);
            dateAndStars.setAlignment(Pos.CENTER_LEFT);

            VBox leftBox = new VBox(6, dateAndStars, artLabel);
            leftBox.setAlignment(Pos.CENTER_LEFT);
            leftBox.setFillWidth(true);
            HBox.setHgrow(leftBox, Priority.ALWAYS);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            row.getChildren().addAll(leftBox, spacer, timeLabel);

            if (isBest) {
                Label recordBadge = new Label("★ BEST");
                recordBadge.setStyle("-fx-background-color: " + accentColor + "; -fx-text-fill: " + bestBadgeTextColor + "; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-font-size: 11px;");
                row.getChildren().add(1, recordBadge);
                row.setStyle("-fx-border-color: " + accentColor + "; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8;");
            } else {
                row.setStyle("-fx-border-color: " + cardBorderColor + "; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
            }

            recordsContainer.getChildren().add(row);
        }
    }

    private void loadStyles() {
        addStylesheet("/styles/start-menu.css");
        addStylesheet("/styles/game-cryo.css");
        addStylesheet("/styles/game-stone.css");
        addStylesheet("/styles/game-inferno.css");
    }

    private void addStylesheet(String resourcePath) {
        var stylesheet = VictoryHistoryView.class.getResource(resourcePath);
        if (stylesheet != null && !getStylesheets().contains(stylesheet.toExternalForm())) {
            getStylesheets().add(stylesheet.toExternalForm());
        }
    }

    private String getMenuThemeClass(int levelNumber) {
        return switch (levelNumber) {
            case 2 -> "start-menu-stone";
            case 3 -> "start-menu-inferno";
            default -> "start-menu-cryo";
        };
    }

    private static void appendArtifact(StringBuilder builder, String name, Integer count) {
        if (count != null && count > 0) {
            builder.append(name).append(" x").append(count).append(" | ");
        }
    }

    private static String safeText(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text;
    }
}
