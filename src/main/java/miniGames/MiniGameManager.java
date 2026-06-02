package miniGames;

import enums.Difficulty;
import events.EventBus;
import events.GameEvent;
import model.Player;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

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
        alert.setContentText("You've found a mini-game artifact! Do you want to spend 1 crystal to play a random mini-game?");

        DialogPane dialogPane = alert.getDialogPane();
        try {
            dialogPane.getStylesheets().add(getClass().getResource("/styles/minigames-cryo.css").toExternalForm());

        } catch (Exception e) {
            System.out.println("Could not load mini-game styles: " + e.getMessage());
        }

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
        errorAlert.setHeaderText(null);
        errorAlert.setContentText("You don't have any crystals to play the mini-game. Collect more crystals and try again!");

        try {
            errorAlert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/minigames-cryo.css").toExternalForm());
        } catch (Exception ignored) {}

        errorAlert.showAndWait();
    }

    private void launchRandomMiniGame() {
        int gameChoice = random.nextInt(6);

        Difficulty currentDiff = Difficulty.current;

        switch (gameChoice) {
            case 0 -> ClickerMiniGame.startNewGame(currentDiff);
            case 1 -> EchoGame.startNewGame(currentDiff);
            case 2 -> GuessTheNumber.startNewGame(currentDiff);
            case 3 -> MemoryGame.startNewGame(currentDiff);
            case 4 -> Numberle.startNewGame(currentDiff);
            case 5 -> PowerGridGame.startNewGame(currentDiff);
        }
    }
}