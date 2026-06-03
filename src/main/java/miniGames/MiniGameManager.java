package miniGames;

import enums.Difficulty;
import enums.MiniGameResult;
import events.EventBus;
import events.GameEvent;
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
import model.Player;

import java.util.Random;

public class MiniGameManager {

    private final Player player;
    private final Random random = new Random();

    public MiniGameManager(Player player) {
        this.player = player;

        EventBus.getInstance().subscribe(GameEvent.Type.MINI_GAME_TRIGGERED, event -> {
            askToPlay();
        });
    }

    private void askToPlay() {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Mini-game Challenge");
        styleAlert(
                alert,
                "artifact-challenge-alert",
                "✦",
                "Artifact Recovered",
                "A strange relic hums with unstable energy.\nSpend 1 crystal to enter a random mini-game?",
                player.getCrystals()
        );
        boolean[] challengeAccepted = {false};
        Button enterChallengeButton = createAlertButton(alert, ButtonType.OK, "✦ Enter Challenge", "artifact-confirm-button");
        enterChallengeButton.setOnAction(event -> {
            challengeAccepted[0] = true;
            alert.setResult(ButtonType.OK);
            alert.close();
        });
        Button leaveItButton = createAlertButton(alert, ButtonType.CANCEL, "Leave It", "artifact-cancel-button");
        setAlertActions(alert, leaveItButton, enterChallengeButton);
        configureWindowClose(alert, ButtonType.CANCEL, ButtonType.OK);

        alert.showAndWait();

        if (challengeAccepted[0]) {
            if (player.useCrystal()) {
                launchRandomMiniGame();
            } else {
                showNoCrystalsAlert();
            }
        }
    }

    private void showNoCrystalsAlert() {
        Alert errorAlert = new Alert(Alert.AlertType.NONE);
        errorAlert.setTitle("Not Enough Crystals");
        styleAlert(
                errorAlert,
                "artifact-warning-alert",
                "◇",
                "Crystal Reserve Empty",
                "You need at least 1 crystal to activate this artifact.\nCollect more and return when you're ready."
        );
        Button continueButton = createAlertButton(errorAlert, ButtonType.OK, "Continue Exploring", "artifact-warning-button");
        setAlertActions(errorAlert, continueButton);
        configureWindowClose(errorAlert, ButtonType.OK);

        errorAlert.showAndWait();
    }

    private void styleAlert(Alert alert, String variantClass, String emblem, String title, String message) {
        styleAlert(alert, variantClass, emblem, title, message, null);
    }

    private void styleAlert(Alert alert, String variantClass, String emblem, String title, String message, Integer crystalCount) {
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

        Label crystalsLabel = null;
        if (crystalCount != null) {
            crystalsLabel = new Label("◆ " + crystalCount);
            crystalsLabel.setId("artifact-alert-crystals");
        }

        Label messageLabel = new Label(message);
        messageLabel.setId("artifact-alert-message");
        messageLabel.setWrapText(true);

        VBox content = new VBox(12, emblemLabel, titleLabel, difficultyLabel, messageLabel);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(34, 28, 28, 28));
        content.setMaxWidth(340);
        content.getStyleClass().add("artifact-alert-content");

        StackPane body = new StackPane(content);
        if (crystalsLabel != null) {
            StackPane.setAlignment(crystalsLabel, Pos.TOP_RIGHT);
            StackPane.setMargin(crystalsLabel, new Insets(18, 22, 0, 0));
            body.getChildren().add(crystalsLabel);
        }

        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("artifact-alert-layout");
        layout.setCenter(body);
        layout.setPrefHeight(360);
        layout.setMinHeight(360);
        dialogPane.setContent(layout);

        try {
            dialogPane.getStylesheets().add(getClass().getResource(getDifficultyStylesheet()).toExternalForm());
        } catch (Exception e) {
            System.out.println("Could not load mini-game styles: " + e.getMessage());
        }
    }

    private Button createAlertButton(Alert alert, ButtonType resultType, String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setOnAction(event -> {
            alert.setResult(resultType);
            alert.close();
        });
        return button;
    }

    private void setAlertActions(Alert alert, Button... buttons) {
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

    private void configureWindowClose(Alert alert, ButtonType closeResult, ButtonType... supportedResults) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getButtonTypes().setAll(supportedResults);
        alert.setOnCloseRequest(event -> alert.setResult(closeResult));
    }

    private String getDifficultyStylesheet() {
        return switch (Difficulty.current) {
            case MEDIUM -> "/styles/minigames-stone.css";
            case HARD -> "/styles/minigames-inferno.css";
            default -> "/styles/minigames-cryo.css";
        };
    }

    private String getDifficultyLabel(Difficulty difficulty) {
        return switch (difficulty) {
            case MEDIUM -> "Stone Depths Protocol";
            case HARD -> "Inferno Depths Protocol";
            default -> "Cryo Depths Protocol";
        };
    }

    private void launchRandomMiniGame() {
        int rewardIndex = random.nextInt(4);
        String rewardText = switch (rewardIndex) {
            case 0 -> "Energy Shield";
            case 1 -> "Radar Charge";
            case 2 -> "Holo-Beacon";
            case 3 -> "Cyber-Elixir";
            default -> "Unknown Artifact";
        };

        int gameChoice = random.nextInt(6);
        Difficulty currentDiff = Difficulty.current;

        MiniGame game = switch (gameChoice) {
            case 0 -> ClickerMiniGame.startNewGame(currentDiff, rewardText);
            case 1 -> EchoGame.startNewGame(currentDiff, rewardText);
            case 2 -> GuessTheNumber.startNewGame(currentDiff, rewardText);
            case 3 -> MemoryGame.startNewGame(currentDiff, rewardText);
            case 4 -> Numberle.startNewGame(currentDiff, rewardText);
            case 5 -> PowerGridGame.startNewGame(currentDiff, rewardText);
            default -> throw new IllegalStateException();
        };

        if (game.getResult() == enums.MiniGameResult.SUCCESS) {
            switch (rewardIndex) {
                case 0 -> player.addShield(1);
                case 1 -> player.addRadarCharge(1);
                case 2 -> player.addBeacon(1);
                case 3 -> player.addElixir(1);
            }
        }
    }
}
