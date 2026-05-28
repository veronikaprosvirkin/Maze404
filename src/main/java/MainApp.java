import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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

    public static void waitForStart() {
        try {
            START_LATCH.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Maze404 — JavaFX started");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 640, 480);

        primaryStage.setTitle("Maze404");
        primaryStage.setScene(scene);
        primaryStage.show();

        START_LATCH.countDown();
    }

    @Override
    public void stop() throws Exception {
        super.stop();

    }

    @Override
    public void init() throws Exception {
        super.init();
    }
}

