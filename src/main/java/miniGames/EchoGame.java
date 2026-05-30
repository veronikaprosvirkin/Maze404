package miniGames;

import enums.MiniGameResult;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EchoGame extends MiniGame {

    private List<Button> colorButtons = new ArrayList<>();
    private List<Button> sequence = new ArrayList<>();
    private int currentStep = 0;
    private int round = 1;
    private int maxRounds = 8;
    private Label timerLabel;
    private Label resultLabel;

    public static EchoGame startNewGame() {
        EchoGame game = new EchoGame();
        game.showWindow();
        return game;
    }

    private void showWindow() {
        Stage stage = new Stage();

        Label instructionLabel = new Label("Echo Game: Repeat the sequence!");
        instructionLabel.setWrapText(true);
        instructionLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        instructionLabel.setAlignment(Pos.CENTER);
        instructionLabel.setId("instruction-label");

        timerLabel = new Label("Press Start to begin");
        timerLabel.setAlignment(Pos.CENTER);
        timerLabel.setId("timer-label");

        resultLabel = new Label("");
        resultLabel.setAlignment(Pos.CENTER);
        resultLabel.setId("result-label");

        Button startButton = new Button("Start");
        startButton.setId("start-button");

        GridPane grid = getGridPane();

        startButton.setOnAction(e -> {
            if (result != MiniGameResult.PENDING) return;
            startButton.setVisible(false);
            startButton.setManaged(false);
            playNextRound();
        });

        VBox root = new VBox(20, instructionLabel, timerLabel, startButton, grid, resultLabel);
        root.setAlignment(Pos.CENTER);
        root.setId("game-container");

        Scene scene = new Scene(root, width, height);
        setupWindow(stage, scene, "Echo Game");
    }

    private void playNextRound() {
        timerLabel.setText("Round " + round + " / " + maxRounds);
        currentStep = 0;
        colorButtons.forEach(btn -> btn.setDisable(true));

        Random random = new Random();
        Button nextButton = colorButtons.get(random.nextInt(colorButtons.size()));
        sequence.add(nextButton);
        playSequence();
    }

    private void playSequence() {
        SequentialTransition seqTransition = new SequentialTransition();

        for (Button btn : sequence) {
            Timeline flash = new Timeline(
                    new KeyFrame(Duration.ZERO, event -> btn.setOpacity(1.0)),
                    new KeyFrame(Duration.seconds(0.4), event -> btn.setOpacity(0.3))
            );

            PauseTransition pause = new PauseTransition(Duration.seconds(0.2));
            seqTransition.getChildren().addAll(flash, pause);
        }

        seqTransition.setOnFinished(e -> {
            colorButtons.forEach(btn -> btn.setDisable(false));
        });
        seqTransition.play();
    }

    private GridPane getGridPane() {
        GridPane grid = new GridPane();
        grid.setId("echo-grid");
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        for (int i = 0; i < 4; i++) {
            Button btn = new Button("");
            btn.setMinSize(100, 100);
            btn.setDisable(true);
            btn.setOpacity(0.3);

            btn.getStyleClass().add("echo-button");
            btn.getStyleClass().add("echo-button-" + i);

            int col = i % 2;
            int row = i / 2;
            grid.add(btn, col, row);

            btn.setOnAction(e -> handlePlayerClick(btn));
            colorButtons.add(btn);
        }
        return grid;
    }

    private void handlePlayerClick(Button clickedButton) {
        if (result != MiniGameResult.PENDING) return;

        Timeline clickEffect = new Timeline(
                new KeyFrame(Duration.ZERO, event -> clickedButton.setOpacity(1.0)),
                new KeyFrame(Duration.seconds(0.2), event -> clickedButton.setOpacity(0.3))
        );
        clickEffect.play();

        if (clickedButton == sequence.get(currentStep)) {
            currentStep++;
            if (currentStep >= sequence.size()) {
                round++;
                if (round > maxRounds) {
                    result = MiniGameResult.SUCCESS;
                    timerLabel.setText("Success! All rounds completed.");
                    resultLabel.setText("YOU WIN!");
                    colorButtons.forEach(btn -> btn.setDisable(true));
                } else {
                    colorButtons.forEach(btn -> btn.setDisable(true));
                    PauseTransition roundPause = new PauseTransition(Duration.seconds(1));
                    roundPause.setOnFinished(event -> playNextRound());
                    roundPause.play();
                }
            }
        } else {
            result = MiniGameResult.FAILURE;
            resultLabel.setText("WRONG! GAME OVER.");
            colorButtons.forEach(btn -> btn.setDisable(true));
        }
    }
}