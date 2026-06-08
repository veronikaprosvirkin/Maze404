package ui.render;

import enums.Difficulty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class GameAlerts {
    private GameAlerts() {
    }

    public static void showExitBlockedAlert() {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Exit Locked");
        styleAlert(
                alert,
                "exit-blocked-alert",
                "KEY REQUIRED",
                "Exit Seal Engaged",
                "The ancient door will not open yet.\nRecover the key, then return to unlock your escape."
        );

        Button continueButton = createAlertButton(alert, ButtonType.OK, "Find the Key", "artifact-confirm-button");
        setAlertActions(alert, continueButton);
        configureWindowClose(alert, ButtonType.OK);
        alert.showAndWait();
    }

    private static void styleAlert(Alert alert, String variantClass, String emblem, String title, String message) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().addAll("artifact-alert", variantClass);
        dialogPane.setHeaderText(null);
        dialogPane.setGraphic(null);
        dialogPane.setPrefWidth(420);
        dialogPane.setMinWidth(420);
        dialogPane.setPrefHeight(360);
        dialogPane.setMinHeight(360);

        Label emblemLabel = new Label(emblem);
        emblemLabel.setId("artifact-alert-emblem");

        Label titleLabel = new Label(title);
        titleLabel.setId("artifact-alert-title");

        Label difficultyLabel = new Label(getDifficultyLabel(Difficulty.current));
        difficultyLabel.setId("artifact-alert-difficulty");

        Label messageLabel = new Label(message);
        messageLabel.setId("artifact-alert-message");
        messageLabel.setWrapText(true);

        VBox content = new VBox(12, emblemLabel, titleLabel, difficultyLabel, messageLabel);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(34, 28, 28, 28));
        content.setMaxWidth(340);
        content.getStyleClass().add("artifact-alert-content");

        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("artifact-alert-layout");
        layout.setCenter(new StackPane(content));
        layout.setPrefHeight(360);
        layout.setMinHeight(360);
        dialogPane.setContent(layout);

        try {
            dialogPane.getStylesheets().add(GameAlerts.class.getResource(getDifficultyStylesheet()).toExternalForm());
        } catch (Exception e) {
            System.out.println("Could not load alert styles: " + e.getMessage());
        }
    }

    private static Button createAlertButton(Alert alert, ButtonType resultType, String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setOnAction(event -> {
            alert.setResult(resultType);
            alert.close();
        });
        return button;
    }

    private static void setAlertActions(Alert alert, Button... buttons) {
        HBox actions = new HBox(18);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(14, 18, 18, 18));
        actions.getStyleClass().add("artifact-alert-actions");
        actions.getChildren().addAll(buttons);

        DialogPane dialogPane = alert.getDialogPane();
        if (dialogPane.getContent() instanceof BorderPane layout) {
            layout.setBottom(actions);
        }
    }

    private static void configureWindowClose(Alert alert, ButtonType closeResult, ButtonType... supportedResults) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getButtonTypes().setAll(supportedResults);
        alert.setOnCloseRequest(event -> alert.setResult(closeResult));
    }

    private static String getDifficultyStylesheet() {
        return switch (Difficulty.current) {
            case MEDIUM -> "/styles/minigames-stone.css";
            case HARD -> "/styles/minigames-inferno.css";
            default -> "/styles/minigames-cryo.css";
        };
    }

    private static String getDifficultyLabel(Difficulty difficulty) {
        return switch (difficulty) {
            case MEDIUM -> "Stone Depths Lock";
            case HARD -> "Inferno Gate Lock";
            default -> "Cryo Vault Lock";
        };
    }
}
