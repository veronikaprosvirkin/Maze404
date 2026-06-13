package gameHistory;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;


import java.util.List;
import java.util.stream.Collectors;

public class VictoryHistoryView extends StackPane {

    public VictoryHistoryView(int levelNumber, Runnable backToMenu) {
        getStyleClass().add("start-menu");

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

        String protocolName = switch (levelNumber) {
            case 2 -> "STONE DEPTHS PROTOCOL";
            case 3 -> "INFERNO DEPTHS PROTOCOL";
            default -> "CRYO DEPTHS PROTOCOL";
        };

        setStyle("-fx-background-color: " + bgColor + ";");

        VBox mainLayout = new VBox(24);
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setPadding(new Insets(40));

        Label titleLabel = new Label("ARCHIVED VICTORIES");
        titleLabel.getStyleClass().add("pause-overlay-heading");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-text-fill: " + accentColor + ";");

        Label subtitleLabel = new Label(protocolName + " RECORDS");
        subtitleLabel.getStyleClass().add("victory-subtitle");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #8a95a5;");

        VBox recordsContainer = new VBox(12);
        recordsContainer.setAlignment(Pos.TOP_CENTER);
        recordsContainer.setPadding(new Insets(10, 0, 10, 0));

        ScrollPane scrollPane = new ScrollPane(recordsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().addAll("hud-card", "pause-menu-button", "pause-menu-secondary");
        backBtn.setOnAction(e -> backToMenu.run());

        mainLayout.getChildren().addAll(titleLabel, subtitleLabel, scrollPane, backBtn);
        getChildren().add(mainLayout);

        List<VictoryRecord> allRecords = VictoryHistoryManager.loadRecords();
        List<VictoryRecord> filtered = allRecords.stream()
                .filter(r -> r.getLevelNumber() == levelNumber)
                .sorted((r1, r2) -> Long.compare(r1.getTimeSeconds(), r2.getTimeSeconds())) // Speedrun сортування
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label noData = new Label("No successful extractions archived for this protocol.");
            noData.getStyleClass().add("victory-subtitle");
            noData.setStyle("-fx-padding: 40 0 0 0;");
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

            long mins = r.getTimeSeconds() / 60;
            long secs = r.getTimeSeconds() % 60;
            String timeStr = String.format("%02d:%02d", mins, secs);

            Label timeLabel = new Label("⏱ " + timeStr);
            timeLabel.getStyleClass().add("hud-card-value");

            int hp = r.getHealth();
            String stars;
            if (hp >= 3 || hp <= 0) stars = "★★★";
            else if (hp == 2) stars = "★★☆";
            else stars = "★☆☆";

            Label starsLabel = new Label(stars);
            starsLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16px; -fx-letter-spacing: 2px;"); // Золотий колір

            Label dateLabel = new Label("Date: " + r.getDate());
            dateLabel.getStyleClass().add("hud-card-title");

            StringBuilder artSb = new StringBuilder("Inventory: ");
            r.getCollectedArtifacts().forEach((name, count) -> {
                if (count > 0) artSb.append(name).append(" x").append(count).append(" | ");
            });
            String artStr = artSb.length() > 11 ? artSb.substring(0, artSb.length() - 3) : "No resources";

            Label artLabel = new Label(artStr);
            artLabel.getStyleClass().add("victory-subtitle");
            artLabel.setStyle("-fx-font-size: 13px;");

            HBox dateAndStars = new HBox(12, dateLabel, starsLabel);
            dateAndStars.setAlignment(Pos.CENTER_LEFT);

            VBox leftBox = new VBox(6, dateAndStars, artLabel);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            row.getChildren().addAll(leftBox, spacer, timeLabel);

            if (isBest) {
                Label recordBadge = new Label("★ BEST");
                recordBadge.setStyle("-fx-background-color: #FFD700; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-font-size: 11px;");
                row.getChildren().add(1, recordBadge);
                row.setStyle("-fx-border-color: #FFD700; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8;");
            }

            recordsContainer.getChildren().add(row);
        }
    }
}