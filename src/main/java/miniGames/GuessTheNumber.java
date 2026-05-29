package miniGames;

import enums.MiniGameResult;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Random;

public class GuessTheNumber extends MiniGame {
    private final int targetNumber;
    private int attempts;
    private static final int TRY_LIMIT = 10;

    public GuessTheNumber(int targetNumber) {
        this.targetNumber = targetNumber;
        this.attempts = 0;
        this.width = 400;
        this.height = 420;
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
        Label historyLabel = new Label("History of moves:");
        ListView<String> historyList = new ListView<>();
        VBox inputPanel = new VBox(10, instructionLabel, guessField, guessButton, resultLabel);
        VBox historyPanel = new VBox(10, historyLabel, historyList);

        historyList.setPrefHeight(120);

        VBox root = new VBox(15, inputPanel, historyPanel);
        root.setId("game-container");
        inputPanel.setId("input-panel");
        historyPanel.setId("history-panel");
        instructionLabel.setId("instruction-label");
        guessField.setId("guess-field");
        guessButton.setId("guess-button");
        resultLabel.setId("result-label");
        historyLabel.setId("history-label");
        historyList.setId("history-list");
        guessButton.setDefaultButton(true);
        guessField.requestFocus();

        Runnable submitGuess = () -> {
            if (result != MiniGameResult.PENDING) {
                return;
            }

            String guessText = guessField.getText().trim();
            if (guessText.isEmpty()) {
                resultLabel.setText("Please enter a valid number.");
                return;
            }

            try {
                int guess = Integer.parseInt(guessText);
                String outcome = checkGuess(guess);
                resultLabel.setText(outcome);
                historyList.getItems().add(0, guess + " -> " + outcome);

                guessField.clear();

                if (result != MiniGameResult.PENDING) {
                    guessField.setDisable(true);
                    guessButton.setDisable(true);
                }
            } catch (NumberFormatException ex) {
                resultLabel.setText("Please enter a valid number.");
                historyList.getItems().add(0, guessText + " -> Please enter a valid number.");
            }
        };

        guessButton.setOnAction(e -> submitGuess.run());
        guessField.setOnAction(e -> submitGuess.run());

        Scene scene = new Scene(root, width, height);
        setupWindow(stage, scene, "Guess The Number");
    }

    private String checkGuess(int guess) {
        this.attempts++;
        int attemptsLeft = TRY_LIMIT - this.attempts;

        if (guess < this.targetNumber) {
            if (this.attempts >= TRY_LIMIT) {
                this.result = MiniGameResult.FAILURE;
                return "Game Over! The number was: " + this.targetNumber;
            }
            return "Too low! Attempts left: " + attemptsLeft;
        } else if (guess > this.targetNumber) {
            if (this.attempts >= TRY_LIMIT) {
                this.result = MiniGameResult.FAILURE;
                return "Game Over! The number was: " + this.targetNumber;
            }
            return "Too high! Attempts left: " + attemptsLeft;
        }

        this.result = MiniGameResult.SUCCESS;
        return "Correct! You guessed the number in " + this.attempts + " attempts.";
    }
}