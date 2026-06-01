package miniGames;

import enums.Difficulty;
import enums.MiniGameResult;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Random;

public class PowerGridGame extends MiniGame {

    private int targetPower;
    private int currentPower;
    private int movesLeft;
    private int tolerance;

    private Label statusLabel;
    private Label movesLabel;
    private Label resultLabel;
    private StackPane wrapper;

    public static PowerGridGame startNewGame(Difficulty difficulty) {
        PowerGridGame game = new PowerGridGame();
        game.applyDifficulty(difficulty);
        game.showWindow();
        return game;
    }

    private void applyDifficulty(Difficulty difficulty) {
        Random random = new Random();
        switch (difficulty) {
            case EASY:
                movesLeft = 8;
                tolerance = 5;
                currentPower = random.nextInt(15);
                break;
            case MEDIUM:
                movesLeft = 7;
                tolerance = 3;
                currentPower = random.nextInt(30) + 10;
                break;
            case HARD:
                movesLeft = 5;
                tolerance = 1;
                currentPower = random.nextInt(40) + 20;
                break;
            default:
                movesLeft = 7;
                tolerance = 3;
                currentPower = random.nextInt(20);
                break;
        }
    }

    private void showWindow() {
        Stage stage = new Stage();

        Random random = new Random();
        targetPower = currentPower + random.nextInt(60) + 20;

        Label instructionLabel = new Label("Power Grid: Calibrate voltage to " + targetPower + "V (±" + tolerance + "V)!");
        instructionLabel.setWrapText(true);
        instructionLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        instructionLabel.setAlignment(Pos.CENTER);
        instructionLabel.setId("instruction-label");

        statusLabel = new Label("Current Voltage: " + currentPower + "V");
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setId("timer-label");

        movesLabel = new Label("Moves left: " + movesLeft);
        movesLabel.setAlignment(Pos.CENTER);
        movesLabel.setId("moves-label");

        resultLabel = new Label("");
        resultLabel.setAlignment(Pos.CENTER);
        resultLabel.setId("result-label");

        GridPane grid = getOperationButtons();

        VBox root = new VBox(20, instructionLabel, statusLabel, movesLabel, grid, resultLabel);
        root.setAlignment(Pos.CENTER);
        root.setId("game-container");

        wrapper = new StackPane(root);
        Scene scene = new Scene(wrapper, width, height);
        setupWindow(stage, scene, "Power Grid Game");
    }

    private GridPane getOperationButtons() {
        GridPane grid = new GridPane();
        grid.setId("power-grid");
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        Button btnAdd10 = createButton("+ 10");
        btnAdd10.setOnAction(e -> handleOperation("+", 10));

        Button btnAdd3 = createButton("+ 3");
        btnAdd3.setOnAction(e -> handleOperation("+", 3));

        Button btnSub5 = createButton("- 5");
        btnSub5.setOnAction(e -> handleOperation("-", 5));

        Button btnMult2 = createButton("x 2");
        btnMult2.setOnAction(e -> handleOperation("*", 2));

        grid.add(btnAdd10, 0, 0);
        grid.add(btnAdd3, 1, 0);
        grid.add(btnSub5, 0, 1);
        grid.add(btnMult2, 1, 1);

        return grid;
    }

    private Button createButton(String text) {
        Button btn = new Button(text);
        btn.setMinSize(110, 110);
        btn.getStyleClass().add("power-button");
        return btn;
    }

    private void handleOperation(String operation, int value) {
        if (result != MiniGameResult.PENDING) return;

        switch (operation) {
            case "+": currentPower += value; break;
            case "-": currentPower -= value; break;
            case "*": currentPower *= value; break;
        }

        movesLeft--;
        updateLabels();
        checkWinCondition();
    }

    private void updateLabels() {
        statusLabel.setText("Current Voltage: " + currentPower + "V");
        movesLabel.setText("Moves left: " + movesLeft);
    }

    private void checkWinCondition() {
        if (currentPower >= targetPower - tolerance && currentPower <= targetPower + tolerance) {
            result = MiniGameResult.SUCCESS;
            resultLabel.setText("Perfect! You Win!");
            showEndOverlay(wrapper, true);
        } else if (movesLeft <= 0) {
            result = MiniGameResult.FAILURE;
            resultLabel.setText("No moves left! Game Over!");
            showEndOverlay(wrapper, false);
        } else if (currentPower > targetPower * 2 || currentPower < -50) {
            result = MiniGameResult.FAILURE;
            resultLabel.setText("Too much!");
            showEndOverlay(wrapper, false);
        }
    }
}