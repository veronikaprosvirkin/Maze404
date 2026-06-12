import enums.CellType;
import enums.Difficulty;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import logic.CollisionDetector;
import model.GameState;
import model.Grid;
import model.Player;
import model.Position;

public class EnemyTurnScheduler {
    private final CollisionDetector collisionDetector = new CollisionDetector();
    public Timeline createTimer(GameState gameState, Grid grid, Player player) {
        int[] tickCounter = {0};

        Timeline enemyTimer = new Timeline(
                new KeyFrame(Duration.seconds(0.2), e -> {
                    if (gameState.isGameOver() || gameState.isLevelComplete() || gameState.isPaused()) return;

                    tickCounter[0]++;

                    for (model.Enemy enemy : gameState.getEnemies()) {
                        if (enemy.getAi() != null) {
                            int requiredTicks = getRequiredTicks(enemy);

                            if (tickCounter[0] % requiredTicks != 0) {
                                continue;
                            }

                            Position next = enemy.getAi().computeNextMove(enemy, grid, player);
                            if (grid.isInBounds(next.getRow(), next.getCol()) &&
                                    grid.getCell(next.getRow(), next.getCol()).getType() != CellType.WALL) {
                                enemy.setRow(next.getRow());
                                enemy.setCol(next.getCol());
                            }
                        }
                    }
                    collisionDetector.checkCollisions(gameState);
                })
        );
        enemyTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        return enemyTimer;
    }

    private int getRequiredTicks(model.Enemy enemy) {
        if (enemy.getMode() == enums.EnemyMode.PATROL) {
            return 5;
        }
        if (enemy.getMode() == enums.EnemyMode.CHASE) {
            return switch (Difficulty.current) {
                case EASY -> 4;
                case MEDIUM -> 3;
                case HARD -> 2;
                default -> 3;
            };
        }
        return 4;
    }
}
