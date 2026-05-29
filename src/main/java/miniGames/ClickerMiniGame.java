package miniGames;

import enums.MiniGameResult;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ClickerMiniGame extends MiniGame {
    int clicks = 0;
    int clickLimit = 20;
    double timeLeft = 10.0; // seconds
    private Timeline timer;

    public static ClickerMiniGame startNewGame() {
        ClickerMiniGame game = new ClickerMiniGame();
        game.showWindow();
        return game;
    }

    private void showWindow() {
        Stage stage = new Stage();

        Label timerLabel = new Label("Time Left: " + timeLeft + "s");
        Button clickButton = new Button("Click Me!");
        ProgressBar progressBar = new ProgressBar(0);

        timerLabel.setId("timer-label");
        clickButton.setId("click-button");
        progressBar.setId("progress-bar");

        clickButton.setOnAction(e -> {
            if (result != MiniGameResult.PENDING) {
                return;
            }
            clicks++;
            progressBar.setProgress((double) clicks / clickLimit);
            if (clicks >= clickLimit) {
                result = MiniGameResult.SUCCESS;
                if (timer != null) {
                    timer.stop();
                }
                clickButton.setDisable(true);
                timerLabel.setText("You Win!");
            }
        });

        startTimer(timerLabel, clickButton);
        VBox root = new VBox(15, timerLabel, progressBar, clickButton);
        root.setId("game-container");

        javafx.scene.layout.VBox.setVgrow(clickButton, javafx.scene.layout.Priority.ALWAYS);
        clickButton.setMaxHeight(Double.MAX_VALUE);
        clickButton.setMaxWidth(Double.MAX_VALUE);

        Scene scene = new Scene(root, width, height);

        setupWindow(stage, scene, "System Overload");
    }
    private void startTimer(Label timerLabel, Button clickButton) {
        KeyFrame keyFrame = new KeyFrame(javafx.util.Duration.seconds(0.1), event -> {
            timeLeft -= 0.1;
            timerLabel.setText(String.format("Time Left: %.1fs", timeLeft));

            if (timeLeft <= 0 && result == MiniGameResult.PENDING) {
                result = MiniGameResult.FAILURE;
                timer.stop();
                clickButton.setDisable(true);
                timerLabel.setText("System Locked! Time is up.");
            }
        });

        timer = new Timeline(keyFrame);
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }
}