package miniGames;

import enums.Difficulty;
import model.GameState;
import events.EventBus;
import events.GameEvent;
import model.Player;
import model.Position;
import ui.render.LevelIsland;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class MiniGameManager {
    private final Player player;
    private final LevelIsland levelIsland;
    private final Random random = new Random();
    private final GameState gameState;
    private final Consumer<GameEvent> miniGameTriggerListener;
    private final Consumer<GameEvent> playerMovedListener;
    private final List<Integer> availableGames = new ArrayList<>();
    private Position promptPosition;
    private boolean promptActive = false;

    public MiniGameManager(GameState gameState, Player player, LevelIsland levelIsland) {
        this.gameState = gameState;
        this.player = player;
        this.levelIsland = levelIsland;

        miniGameTriggerListener = event -> {
            askToPlay(event.getPosition());
        };
        playerMovedListener = event -> {
            if (promptActive && promptPosition != null && !promptPosition.equals(event.getPosition())) {
                rejectPrompt();
            }
        };
        EventBus.getInstance().subscribe(GameEvent.Type.MINI_GAME_TRIGGERED, miniGameTriggerListener);
        EventBus.getInstance().subscribe(GameEvent.Type.PLAYER_MOVED, playerMovedListener);
    }

    public void dispose() {
        EventBus.getInstance().unsubscribe(GameEvent.Type.MINI_GAME_TRIGGERED, miniGameTriggerListener);
        EventBus.getInstance().unsubscribe(GameEvent.Type.PLAYER_MOVED, playerMovedListener);
    }

    public boolean handlePromptKey(javafx.scene.input.KeyCode keyCode) {
        if (!promptActive) {
            return false;
        }

        if (keyCode == javafx.scene.input.KeyCode.ENTER) {
            acceptPrompt();
            return true;
        }

        if (keyCode == javafx.scene.input.KeyCode.ESCAPE) {
            rejectPrompt();
            return true;
        }

        return false;
    }

    private void askToPlay(Position position) {
        promptPosition = position;
        promptActive = true;
        levelIsland.showChoiceMessage(
                enums.ArtifactType.MINI_GAME,
                "Artifact recovered - spend 1 crystal? Crystals: " + player.getCrystals(),
                "Leave It",
                this::rejectPrompt,
                "Enter Challenge",
                this::acceptPrompt
        );
    }

    private void acceptPrompt() {
        if (!promptActive) {
            return;
        }

        promptActive = false;
        promptPosition = null;
        levelIsland.hideChoiceMessage();

        if (player.useCrystal()) {
            gameState.setPaused(true);
            launchRandomMiniGame();
            gameState.setPaused(false);
            return;
        }

        levelIsland.showArtifactMessage(enums.ArtifactType.CRYSTAL, "Need 1 crystal");
    }

    private void rejectPrompt() {
        if (!promptActive) {
            return;
        }

        promptActive = false;
        promptPosition = null;
        levelIsland.hideChoiceMessage();
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

        if (availableGames.isEmpty()) {
            for (int i = 0; i < 6; i++) {
                availableGames.add(i);
            }
            Collections.shuffle(availableGames, random);
        }
        int gameChoice = availableGames.remove(0);

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
