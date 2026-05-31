package miniGames;

import enums.MiniGameResult;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ClickerMiniGame extends MiniGame {
    int clicks = 0;
    int clickLimit = 30;
    double timeLeft = 7.5;
    private Timeline timer;
    private StackPane wrapper;

    public static ClickerMiniGame startNewGame() {
        ClickerMiniGame game = new ClickerMiniGame();
        game.showWindow();
        return game;
    }

    private void showWindow() {
        Stage stage = new Stage();

        Label timerLabel = new Label("Press Start to begin");
        Button startButton = new Button("Start");
        Button clickButton = new Button("Click Me!");
        ProgressBar progressBar = new ProgressBar(0);

        timerLabel.setId("timer-label");
        startButton.setId("start-button");
        clickButton.setId("click-button");
        progressBar.setId("progress-bar");

        progressBar.setMaxWidth(Double.MAX_VALUE);
        clickButton.setMaxHeight(Double.MAX_VALUE);
        clickButton.setMaxWidth(Double.MAX_VALUE);
        clickButton.setDisable(true);

        startButton.setOnAction(e -> {
            if (timer != null || result != MiniGameResult.PENDING) {
                return;
            }

            timerLabel.setText(String.format("Time Left: %.1fs", timeLeft));
            clickButton.setDisable(false);
            startButton.setVisible(false);
            startButton.setManaged(false);
            startTimer(timerLabel, clickButton, wrapper);
        });

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
                timerLabel.getStyleClass().removeAll("danger");
                timerLabel.getStyleClass().add("success");
                showEndOverlay(wrapper, true);
            }
        });

        VBox root = new VBox(15, timerLabel, progressBar, startButton, clickButton);
        root.setId("game-container");
        root.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.layout.VBox.setVgrow(clickButton, javafx.scene.layout.Priority.ALWAYS);
        clickButton.setMaxHeight(Double.MAX_VALUE);
        clickButton.setMaxWidth(Double.MAX_VALUE);

        wrapper = new StackPane(root);
        Scene scene = new Scene(wrapper, width, height);

        setupWindow(stage, scene, "System Overload");
    }

    private void startTimer(Label timerLabel, Button clickButton, StackPane wrapper) {
        double initialTime = timeLeft;
        KeyFrame keyFrame = new KeyFrame(javafx.util.Duration.seconds(0.1), event -> {
            timeLeft -= 0.1;
            timerLabel.setText(String.format("Time Left: %.1fs", timeLeft));

            if (timeLeft <= initialTime * 0.3 && result == MiniGameResult.PENDING) {
                if (!timerLabel.getStyleClass().contains("danger")) {
                    timerLabel.getStyleClass().add("danger");
                }
            }

            if (timeLeft <= 0 && result == MiniGameResult.PENDING) {
                result = MiniGameResult.FAILURE;
                timer.stop();
                clickButton.setDisable(true);
                timerLabel.setText("System Locked! Time is up.");
                showEndOverlay(wrapper, false);
            }
        });

        timer = new Timeline(keyFrame);
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }
}