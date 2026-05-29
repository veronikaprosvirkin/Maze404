import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.application.Application;

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
        root.setPrefSize(800, 600);
        Scene scene = new Scene(root, 800, 600);

        primaryStage.setTitle("Maze404");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        START_LATCH.countDown();
    }
}
