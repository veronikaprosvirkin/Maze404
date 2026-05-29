package miniGames;

import enums.MiniGameResult;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Setter;
import java.util.Random;

public class GuessTheNumber extends MiniGame {
    private int targetNumber;
    private int attempts;
    private int tryLimit = 10;

    public GuessTheNumber(int targetNumber) {
        this.targetNumber = targetNumber;
        this.attempts = 0;
    }

    public static GuessTheNumber startNewGame() {
        Random random = new Random();
        int targetNumber = random.nextInt(100) + 1;
        GuessTheNumber game = new GuessTheNumber(targetNumber);
        game.showWindow();
        return game;
    }

    private void showWindow() {
        Stage stage = new Stage();

        Label instructionLabel = new Label("Guess a number between 1 and 100:");
        TextField guessField = new TextField();
        Button guessButton = new Button("Guess");
        Label resultLabel = new Label();

        VBox root = new VBox(10, instructionLabel, guessField, guessButton, resultLabel);
        root.setId("game-container");
        instructionLabel.setId("instruction-label");
        guessField.setId("guess-field");
        guessButton.setId("guess-button");
        resultLabel.setId("result-label");

        guessButton.setOnAction(e -> {
            if (result != MiniGameResult.PENDING) {
                return;
            }
            String guessText = guessField.getText();
            try {
                int guess = Integer.parseInt(guessText);
                checkGuess(guess, resultLabel);
                if (result != MiniGameResult.PENDING) {
                    guessField.setDisable(true);
                    guessButton.setDisable(true);
                }
            } catch (NumberFormatException ex) {
                resultLabel.setText("Please enter a valid number.");
            }
        });

        Scene scene = new Scene(root, width, height);
        setupWindow(stage, scene, "Guess The Number");
    }

    private void checkGuess(int guess, Label resultLabel) {
        this.attempts++;

        if (guess < this.targetNumber) {
            resultLabel.setText("Too low! Attempts: " + this.attempts);
        } else if (guess > this.targetNumber) {
            resultLabel.setText("Too high! Attempts: " + this.attempts);
        } else {
            resultLabel.setText("Correct! You guessed the number in " + this.attempts + " attempts.");
            this.result = MiniGameResult.SUCCESS;
        }

        if (this.attempts >= this.tryLimit && this.result != MiniGameResult.SUCCESS) {
            resultLabel.setText("Game Over! The number was: " + this.targetNumber);
            this.result = MiniGameResult.FAILURE;
        }
    }
}