package logic;

import model.GameState;

import java.util.Objects;

public class PauseController {
    private final GameState gameState;

    public PauseController(GameState gameState) {
        this.gameState = Objects.requireNonNull(gameState, "gameState");
    }

    public boolean isPaused() {
        return gameState.isPaused();
    }

    public boolean canPause() {
        return !gameState.isGameOver() && !gameState.isLevelComplete();
    }

    public void pause() {
        if (!canPause()) {
            return;
        }
        gameState.setPaused(true);
    }

    public void resume() {
        gameState.setPaused(false);
    }

    public void toggle() {
        if (isPaused()) {
            resume();
            return;
        }
        pause();
    }
}
