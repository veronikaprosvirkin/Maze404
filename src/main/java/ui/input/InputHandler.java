package ui.input;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

import java.util.Objects;
import java.util.function.Consumer;

public class InputHandler {
    private static final long DEBOUNCE_MS = 150;

    private final Consumer<GameAction> actionConsumer;
    private long lastAcceptedAtMs = 0;

    public InputHandler(Consumer<GameAction> actionConsumer) {
        this.actionConsumer = Objects.requireNonNull(actionConsumer, "actionConsumer");
    }

    public void attachTo(Scene scene) {
        scene.setOnKeyPressed(event -> {
            long now = System.currentTimeMillis();
            if (now - lastAcceptedAtMs < DEBOUNCE_MS) {
                return;
            }

            GameAction action = mapKey(event.getCode());
            if (action != null) {
                lastAcceptedAtMs = now;
                actionConsumer.accept(action);
            }
        });
    }

    private GameAction mapKey(KeyCode code) {
        return switch (code) {
            case W, UP -> GameAction.MOVE_UP;
            case S, DOWN -> GameAction.MOVE_DOWN;
            case A, LEFT -> GameAction.MOVE_LEFT;
            case D, RIGHT -> GameAction.MOVE_RIGHT;
            case Q -> GameAction.SCAN;
            case B -> GameAction.BEACON;
            case R -> GameAction.RADAR;
            case E -> GameAction.SHIELD;
            default -> null;
        };
    }
}

