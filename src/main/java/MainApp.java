import enums.CellType;
import enums.Difficulty;
import enums.PlayerSkin;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import logic.ArtifactSpawner;
import logic.ArtifactSystem;
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
        // FOR NOW - this is a difficulty changer for labyrinth
        Difficulty.current = Difficulty.EASY;

        String bgColor = switch (Difficulty.current) {
            case MEDIUM -> "#161210";
            case HARD -> "#150A0C";
            default -> "#111520";
        };
        root.setStyle("-fx-background-color: " + bgColor + ";");

        Grid grid = new Grid(15, 15);
        for (int row = 0; row < grid.getHeight(); row++) {
            for (int col = 0; col < grid.getWidth(); col++) {
                boolean isBorder = row == 0 || col == 0 || row == grid.getHeight() - 1 || col == grid.getWidth() - 1;
                grid.setType(row, col, isBorder ? CellType.WALL : CellType.FLOOR);
            }
        }

        Player player = new Player(7, 7);
        player.setSkin(settings.playerSkin() != null ? settings.playerSkin() : PlayerSkin.CIRCLE);

        ArtifactSpawner artifactSpawner = new ArtifactSpawner();
        List<Artifact> artifacts = artifactSpawner.spawnArtifacts(
                grid,
                Difficulty.current,
                new Position(player.getRow(), player.getCol())
        );

        GameState gameState = new GameState(grid, player, List.of(), artifacts, 1);
        ArtifactSystem artifactSystem = new ArtifactSystem();
        miniGames.MiniGameManager miniGameManager = new miniGames.MiniGameManager(player);

        GamePanel gamePanel = new GamePanel(grid, player, artifacts, Difficulty.current, settings.mistSampleStep());
        boolean mistEnabled = true;
        double mistAnimationTime = 1.2;
        double mistDensity = 1;
        gamePanel.setMistEnabled(mistEnabled);
        gamePanel.setMistAnimationTimeScale(mistAnimationTime);
        gamePanel.setMistDensity(mistDensity);
        gamePanel.setGameVolume(settings.gameVolume());
        root.getChildren().setAll(gamePanel);

        InputHandler inputHandler = new InputHandler(action -> {
            int deltaRow = 0;
            int deltaCol = 0;
            switch (action) {
                case MOVE_UP -> deltaRow = -1;
                case MOVE_DOWN -> deltaRow = 1;
                case MOVE_LEFT -> deltaCol = -1;
                case MOVE_RIGHT -> deltaCol = 1;
                default -> {
                }
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

        scene.getRoot().requestFocus();
    }
}
