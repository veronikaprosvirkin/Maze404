package miniGames;

import enums.MiniGameResult;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Random;

public class Numberle extends MiniGame {
    String targetPassword;
    String currentGuess = "";
    int currentRow = 0;
    private static final int MAX_ATTEMPTS = 6;
    Label[][] gridLabels;
    Label statusLabel;
    private StackPane wrapper;

    public static Numberle startNewGame() {
        Numberle game = new Numberle();
        game.showWindow();
        return game;
    }

    private void showWindow() {
        Stage stage = new Stage();
        Label instructionLabel = new Label("Numberle: Guess the 5-digit number! You have " + MAX_ATTEMPTS + " attempts.");
        instructionLabel.setWrapText(true);
        instructionLabel.setAlignment(javafx.geometry.Pos.CENTER);
        instructionLabel.setId("instruction-label");

        statusLabel = new Label("Attempt " + (currentRow + 1) + " of " + MAX_ATTEMPTS);
        statusLabel.setAlignment(javafx.geometry.Pos.CENTER);
        statusLabel.setId("timer-label");

        Random random = new Random();
        targetPassword = String.format("%05d", random.nextInt(100000));

        GridPane grid = createGrid();
        VBox root = new VBox(20, instructionLabel, statusLabel, grid);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setId("game-container");

        wrapper = new StackPane(root);
        Scene scene = new Scene(wrapper, width, height);
        scene.setOnKeyPressed(e -> {
            if (result != MiniGameResult.PENDING) return;

            if (e.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) {
                handleInput("Del");
            } else if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleInput("Ent");
            } else if (e.getText().matches("\\d")) {
                handleInput(e.getText());
            }
        });

        setupWindow(stage, scene, "Numberle");
    }

    private GridPane createGrid(){
        GridPane grid = new GridPane();
        grid.setId("numberle-grid");
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(javafx.geometry.Pos.CENTER);

        gridLabels = new Label[MAX_ATTEMPTS][5];
        for (int row = 0; row < MAX_ATTEMPTS; row++) {
            for (int col = 0; col < 5; col++) {
                Label cell = new Label("");
                cell.setMinSize(40, 40);
                cell.setAlignment(javafx.geometry.Pos.CENTER);
                cell.setId("numberle-cell");
                gridLabels[row][col] = cell;
                grid.add(cell, col, row);
            }
        }
        return grid;
    }

    private void handleInput(String input) {
        if (input.equals("Ent")) {
            if (currentGuess.length() == 5) {
                checkGuess();
            }
        } else if (input.equals("Del")) {
            if (!currentGuess.isEmpty()) {
                currentGuess = currentGuess.substring(0, currentGuess.length() - 1);
                updateGrid();
            }
        } else {
            if (currentGuess.length() < 5) {
                currentGuess += input;
                updateGrid();
            }
        }
    }

    private void updateGrid() {
        for (int col = 0; col < 5; col++) {
            if (col < currentGuess.length()) {
                gridLabels[currentRow][col].setText(String.valueOf(currentGuess.charAt(col)));
            } else {
                gridLabels[currentRow][col].setText("");
            }
        }
    }

    private void checkGuess() {
        boolean[] targetUsed = new boolean[5];
        boolean[] guessUsed = new boolean[5];
        int correctCount = 0;

        for (int i = 0; i < 5; i++) {
            if (currentGuess.charAt(i) == targetPassword.charAt(i)) {
                gridLabels[currentRow][i].getStyleClass().add("wordle-green");
                targetUsed[i] = true;
                guessUsed[i] = true;
                correctCount++;
            }
        }

        for (int i = 0; i < 5; i++) {
            if (guessUsed[i]) continue;
            boolean isYellow = false;

            for (int j = 0; j < 5; j++) {
                if (targetUsed[j]) continue;

                if (currentGuess.charAt(i) == targetPassword.charAt(j)) {
                    gridLabels[currentRow][i].getStyleClass().add("wordle-yellow");
                    targetUsed[j] = true;
                    isYellow = true;
                    break;
                }
            }

            if (!isYellow) {
                gridLabels[currentRow][i].getStyleClass().add("wordle-gray");
            }
        }


        if (correctCount == 5) {
            result = MiniGameResult.SUCCESS;
            statusLabel.setText("Congratulations! You've guessed the number!");
            showEndOverlay(wrapper, true);
        } else {
            currentRow++;
            if (currentRow >= MAX_ATTEMPTS) {
                result = MiniGameResult.FAILURE;
                statusLabel.setText("Game Over! The number was: " + targetPassword);
                showEndOverlay(wrapper, false);
            } else {
                statusLabel.setText("Attempt " + (currentRow + 1) + " of " + MAX_ATTEMPTS);
                currentGuess = "";
            }
        }
    }

}
