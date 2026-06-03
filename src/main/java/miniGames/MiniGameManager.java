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
import javafx.scene.layout.VBox;
import model.Player;

import java.util.Optional;
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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Mini-game Challenge");
        styleAlert(
                alert,
                "artifact-challenge-alert",
                "✦",
                "Artifact Recovered",
                "A strange relic hums with unstable energy.\nSpend 1 crystal to enter a random mini-game?"
        );
        configureButton(alert, ButtonType.OK, "Enter Challenge", "artifact-confirm-button");
        configureButton(alert, ButtonType.CANCEL, "Leave It", "artifact-cancel-button");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {

            if (player.useCrystal()) {
                launchRandomMiniGame();
            } else {
                showNoCrystalsAlert();
            }
        }
    }

    private void showNoCrystalsAlert() {
        Alert errorAlert = new Alert(Alert.AlertType.WARNING);
        errorAlert.setTitle("Not Enough Crystals");
        styleAlert(
                errorAlert,
                "artifact-warning-alert",
                "◇",
                "Crystal Reserve Empty",
                "You need at least 1 crystal to activate this artifact.\nCollect more and return when you're ready."
        );
        configureButton(errorAlert, ButtonType.OK, "Continue Exploring", "artifact-warning-button");

        errorAlert.showAndWait();
    }

    private void styleAlert(Alert alert, String variantClass, String emblem, String title, String message) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().addAll("artifact-alert", variantClass);
        dialogPane.setHeaderText(null);
        dialogPane.setGraphic(null);
        dialogPane.setPrefWidth(420);
        dialogPane.setMinWidth(420);

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
        content.setPadding(new Insets(8, 0, 4, 0));
        content.setMaxWidth(340);
        dialogPane.setContent(content);

        try {
            dialogPane.getStylesheets().add(getClass().getResource(getDifficultyStylesheet()).toExternalForm());
        } catch (Exception e) {
            System.out.println("Could not load mini-game styles: " + e.getMessage());
        }
    }

    private void configureButton(Alert alert, ButtonType buttonType, String text, String styleClass) {
        Button button = (Button) alert.getDialogPane().lookupButton(buttonType);
        if (button == null) {
            return;
        }
        button.setText(text);
        button.getStyleClass().add(styleClass);
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
