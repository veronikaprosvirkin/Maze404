import events.AudioManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ui.GameWindows;
import ui.render.StartMenuView;

import java.util.concurrent.CountDownLatch;

/**
 * JavaFX application shell. It owns startup, window setup, and high-level
 * navigation; gameplay orchestration lives in GameSession.
 */
public class MainApp extends Application {
    private static final double MIN_WINDOW_WIDTH = 1024;
    private static final double MIN_WINDOW_HEIGHT = 680;
    private static final CountDownLatch START_LATCH = new CountDownLatch(1);
    private final AudioManager audioManager = new AudioManager(true);

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
        GameWindows.setPrimaryStage(primaryStage);
        audioManager.setMusicVolume(0.75);
        audioManager.setEffectsVolume(0.75);

        StackPane root = new StackPane();
        Scene scene = new Scene(root, 1024, 680);
        showStartMenu(root, scene);

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

    private void showStartMenu(StackPane root, Scene scene) {
        audioManager.playMenuMusic();
        StartMenuView startMenu = new StartMenuView(
                settings -> new GameSession(root, scene, () -> showStartMenu(root, scene), audioManager).start(settings),
                Platform::exit,
                audioManager.getMusicVolume() * 100.0,
                audioManager::setMusicVolume,
                audioManager.getEffectsVolume() * 100.0,
                audioManager::setEffectsVolume
        );
        root.getChildren().setAll(startMenu);
        scene.getRoot().requestFocus();
    }
}
