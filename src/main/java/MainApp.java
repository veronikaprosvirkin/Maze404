import enums.CellType;
import enums.Difficulty;
import enums.PlayerSkin;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import logic.ArtifactSpawner;
import logic.ArtifactSystem;
import logic.generation.MazeGenerator;
import model.Artifact;
import model.GameState;
import model.Grid;
import model.Player;
import model.Position;
import ui.input.InputHandler;
import ui.render.GamePanel;
import ui.render.StartMenuView;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Main JavaFX application. Signals when the UI is ready via a latch so
 * background threads can wait for the toolkit to be up and the primary stage shown.
 */
public class MainApp extends Application {
    private static final double MIN_WINDOW_WIDTH = 1024;
    private static final double MIN_WINDOW_HEIGHT = 680;

    private static final CountDownLatch START_LATCH = new CountDownLatch(1);


    @SuppressWarnings("unused")
    public static void waitForStart() {
        try {
            START_LATCH.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 1024, 680);
        StartMenuView startMenu = new StartMenuView(
                settings -> startGame(root, scene, settings),
                Platform::exit
        );

        root.getChildren().setAll(startMenu);
        primaryStage.setTitle("Maze404");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(MIN_WINDOW_WIDTH);
        primaryStage.setMinHeight(MIN_WINDOW_HEIGHT);
        primaryStage.toFront();
        primaryStage.show();
        primaryStage.requestFocus();

        START_LATCH.countDown();
    }

    private void startGame(StackPane root, Scene scene, StartMenuView.MenuSettings settings) {
        Difficulty.current = settings.difficulty() != null ? settings.difficulty() : Difficulty.EASY;

        String bgColor = switch (Difficulty.current) {
            case MEDIUM -> "#161210";
            case HARD -> "#150A0C";
            default -> "#111520";
        };
        root.setStyle("-fx-background-color: " + bgColor + ";");

        Grid grid = new MazeGenerator().generate(21, 21);

        Player player = new Player(7, 7);
        player.setSkin(settings.playerSkin() != null ? settings.playerSkin() : PlayerSkin.CIRCLE);

        ArtifactSpawner artifactSpawner = new ArtifactSpawner();
        List<Artifact> artifacts = artifactSpawner.spawnArtifacts(
                grid,
                Difficulty.current,
                new Position(player.getRow(), player.getCol())
        );

        logic.EnemySpawner enemySpawner = new logic.EnemySpawner();
        List<model.Enemy> enemies = enemySpawner.spawnEnemies(grid, Difficulty.current, artifacts);

        GameState gameState = new GameState(grid, player, enemies, artifacts, 1);
        ArtifactSystem artifactSystem = new ArtifactSystem();
        miniGames.MiniGameManager miniGameManager = new miniGames.MiniGameManager(gameState, player);

        GamePanel gamePanel = new GamePanel(grid, player, artifacts, Difficulty.current, settings.mistSampleStep(), enemies);
        boolean mistEnabled = true;
        double mistAnimationTime = 2;
        double mistDensity = 1;
        gamePanel.setMistEnabled(mistEnabled);
        gamePanel.setMistAnimationTimeScale(mistAnimationTime);
        gamePanel.setMistDensity(mistDensity);
        gamePanel.setGameVolume(settings.gameVolume());
        root.getChildren().setAll(gamePanel);

        int[] turnCounter = {0};

        InputHandler inputHandler = new InputHandler(action -> {
            int deltaRow = 0;
            int deltaCol = 0;
            switch (action) {
                case MOVE_UP -> deltaRow = -1;
                case MOVE_DOWN -> deltaRow = 1;
                case MOVE_LEFT -> deltaCol = -1;
                case MOVE_RIGHT -> deltaCol = 1;
                default -> { }
            }

            if (deltaRow != 0 || deltaCol != 0) {
                int targetRow = player.getRow() + deltaRow;
                int targetCol = player.getCol() + deltaCol;
                if (grid.isInBounds(targetRow, targetCol)
                        && grid.getCell(targetRow, targetCol).getType() != CellType.WALL) {
                    player.setRow(targetRow);
                    player.setCol(targetCol);
                    artifactSystem.processArtifacts(gameState);
                }
            }
        });
        inputHandler.attachTo(scene);

        Timeline enemyTimer = getEnemyTimer(gameState, grid, player);
        enemyTimer.play();

        scene.getRoot().requestFocus();
    }

    private static Timeline getEnemyTimer(GameState gameState, Grid grid, Player player) {
        int[] tickCounter = {0};

        Timeline enemyTimer = new Timeline(
                new KeyFrame(Duration.seconds(0.2), e -> {

                    if (gameState.isGameOver() || gameState.isLevelComplete() || gameState.isPaused()) return;

                    tickCounter[0]++;

                    for (model.Enemy enemy : gameState.getEnemies()) {
                        if (enemy.getAi() != null) {

                            int requiredTicks = 4;
                            if (enemy.getMode() == enums.EnemyMode.PATROL) {
                                requiredTicks = 5;
                            } else if (enemy.getMode() == enums.EnemyMode.CHASE) {
                                requiredTicks = switch (Difficulty.current) {
                                    case EASY -> 4;
                                    case MEDIUM -> 3;
                                    case HARD -> 2;
                                    default -> 3;
                                };
                            }

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
                })
        );
        enemyTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        return enemyTimer;
    }
}
