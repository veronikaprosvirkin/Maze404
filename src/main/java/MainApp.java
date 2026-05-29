import enums.CellType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import model.Grid;
import model.Player;
import ui.input.InputHandler;
import ui.render.GamePanel;

import java.util.concurrent.CountDownLatch;

/**
 * Main JavaFX application. Signals when the UI is ready via a latch so background threads
 * can wait for the toolkit to be up and the primary stage shown.
 */
public class MainApp extends Application {
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
        Pane root = new Pane();
        Scene scene = new Scene(root, 800, 600);

        Grid grid = new Grid(15, 15);
        for (int row = 0; row < grid.getHeight(); row++) {
            for (int col = 0; col < grid.getWidth(); col++) {
                boolean isBorder = row == 0 || col == 0 || row == grid.getHeight() - 1 || col == grid.getWidth() - 1;
                grid.setType(row, col, isBorder ? CellType.WALL : CellType.FLOOR);
            }
        }

        Player player = new Player(7, 7);

        GamePanel gamePanel = new GamePanel(grid, player);
        double baseWidth = grid.getWidth() * 32.0;
        double baseHeight = grid.getHeight() * 32.0;
        gamePanel.setPrefSize(baseWidth, baseHeight);
        root.getChildren().add(gamePanel);

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
                }
            }
        });
        inputHandler.attachTo(scene);

        Runnable updateScaleAndCenter = () -> {
            double rootHeight = root.getHeight();
            double rootWidth = root.getWidth();
            if (rootHeight <= 0 || rootWidth <= 0) {
                return;
            }

            double scale = Math.min(rootWidth / baseWidth, rootHeight / baseHeight);
            gamePanel.setScaleX(scale);
            gamePanel.setScaleY(scale);

            gamePanel.setTranslateX((rootWidth  - baseWidth)  / 2.0);
            gamePanel.setTranslateY((rootHeight - baseHeight) / 2.0);
        };

        root.widthProperty().addListener((obs, oldWidth, newWidth) -> updateScaleAndCenter.run());
        root.heightProperty().addListener((obs, oldHeight, newHeight) -> updateScaleAndCenter.run());

        primaryStage.setTitle("Maze404");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.toFront();
        primaryStage.show();
        primaryStage.requestFocus();
        Platform.runLater(updateScaleAndCenter);

        START_LATCH.countDown();
    }
}
