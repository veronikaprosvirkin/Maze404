package miniGames;

import enums.MiniGameResult;
import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EchoGame extends MiniGame {

    private List<Button> colorButtons;
    private List<Button> sequence = new ArrayList<>();
    private int currentStep = 0;
    private int round = 1;
    private int maxRounds = 5;

    public static EchoGame startNewGame() {
        EchoGame game = new EchoGame();
        game.showWindow();
        return game;
    }

    private void showWindow() {
        Stage stage = new Stage();

        Label instructionLabel = new Label("Echo Game: Repeat the sequence of colors!");
        instructionLabel.setWrapText(true);
        instructionLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        instructionLabel.setId("instruction-label");

        Label roundLabel = new Label("Press Start to begin");
        roundLabel.setId("timer-label");

        Button startButton = new Button("Start");
        startButton.setId("start-button");

        Button btn1 = new Button("");
        Button btn2 = new Button("");
        Button btn3 = new Button("");
        Button btn4 = new Button("");

        colorButtons = Arrays.asList(btn1, btn2, btn3, btn4);

        GridPane grid = getGridPane();

        startButton.setOnAction(e -> {
            if (result != MiniGameResult.PENDING) return;

            startButton.setVisible(false);
            startButton.setManaged(false);

            playNextRound(roundLabel);
        });

        VBox root = new VBox(20, instructionLabel, roundLabel, startButton, grid);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, width, height);
        setupWindow(stage, scene, "Echo Game");
    }

    private void playNextRound(Label roundLabel) {
        roundLabel.setText("Round " + round);
        currentStep = 0;
        colorButtons.forEach(btn -> btn.setDisable(true));

        Random random = new Random();
        Button nextButton = colorButtons.get(random.nextInt(colorButtons.size()));
        sequence.add(nextButton);
        playSequence();
    }

    private void playSequence(){
        SequentialTransition seqTransition = new SequentialTransition();
        for (Button btn : sequence){
            FadeTransition fadeIn = new FadeTransition(javafx.util.Duration.seconds(0.5), btn);
            fadeIn.setFromValue(1.0);
            fadeIn.setToValue(0.3);
            seqTransition.getChildren().add(fadeIn);
        }
        seqTransition.setOnFinished(e -> {
            colorButtons.forEach(btn -> btn.setDisable(false));
        });
        seqTransition.play();
    }

    private GridPane getGridPane() {
        String[] colors = {"#ff0055", "#00ffcc", "#bfff00", "#ffaa00"};

        GridPane grid = new GridPane();
        grid.setId("echo-grid");
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        for (int i = 0; i < colorButtons.size(); i++) {
            Button btn = colorButtons.get(i);
            btn.setMinSize(100, 100);
            btn.setDisable(true);


            btn.setStyle("-fx-background-color: " + colors[i] + "; -fx-opacity: 0.3; -fx-border-color: white; -fx-border-width: 2px;");

            int col = i % 2;
            int row = i / 2;
            grid.add(btn, col, row);
        }
        return grid;
    }
}