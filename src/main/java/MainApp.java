import enums.CellType;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.application.Application;
import model.Grid;
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
        StackPane root = new StackPane();
        root.setPrefSize(900, 600);
        Scene scene = new Scene(root, 900, 600);

        Grid grid = new Grid(15, 15);
        for (int row = 0; row < grid.getHeight(); row++) {
            for (int col = 0; col < grid.getWidth(); col++) {
                boolean isBorder = row == 0 || col == 0 || row == grid.getHeight() - 1 || col == grid.getWidth() - 1;
                grid.setType(row, col, isBorder ? CellType.WALL : CellType.FLOOR);
            }
        }

        GamePanel gamePanel = new GamePanel(grid);
        double baseWidth = grid.getWidth() * 32.0;
        double baseHeight = grid.getHeight() * 32.0;
        gamePanel.setPrefSize(baseWidth, baseHeight);
        gamePanel.redraw(grid);
        root.getChildren().add(gamePanel);

        Runnable updateScaleAndCenter = () -> {
            double rootHeight = root.getHeight();
            double rootWidth = root.getWidth();
            if (rootHeight <= 0 || rootWidth <= 0) {
                return;
            }

            double scale = rootHeight / baseHeight;
            gamePanel.setScaleX(scale);
            gamePanel.setScaleY(scale);

            double scaledWidth = baseWidth * scale;
            double scaledHeight = baseHeight * scale;
            gamePanel.setTranslateX((rootWidth - scaledWidth) / 2.0);
            gamePanel.setTranslateY((rootHeight - scaledHeight) / 2.0);
        };

        root.widthProperty().addListener((obs, oldWidth, newWidth) -> updateScaleAndCenter.run());
        root.heightProperty().addListener((obs, oldHeight, newHeight) -> updateScaleAndCenter.run());

        primaryStage.setTitle("Maze404");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.toFront();
        primaryStage.show();
        primaryStage.requestFocus();

        START_LATCH.countDown();
    }
}
