package miniGames;

import enums.Difficulty;
import enums.MiniGameResult;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Random;

public class GuessTheNumber extends MiniGame {
    private int maxRange;
    private int attempts = 0;
    private int maxAttempts;
    int targetNumber;

    public static GuessTheNumber startNewGame(Difficulty difficulty) {
        GuessTheNumber game = new GuessTheNumber();
        game.applyDifficulty(difficulty);
        game.showWindow();
        return game;
    }

    private void applyDifficulty(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                maxRange = 100;
                maxAttempts = 10;
                break;
            case MEDIUM:
                maxRange = 100;
                maxAttempts = 7;
                break;
            case HARD:
                maxRange = 200;
                maxAttempts = 7;
                break;
            default:
                maxRange = 100;
                maxAttempts = 7;
                break;
        }

        targetNumber = new Random().nextInt(maxRange) + 1;
    }

    private void showWindow() {
        Stage stage = new Stage();

        Image imgUp = loadIcon("/styles/icons/up.png");
        Image imgDown = loadIcon("/styles/icons/down.png");
        Image imgSuccess = loadIcon("/styles/icons/success.png");

        ImageView dirIcon = new ImageView();
        dirIcon.setFitWidth(16);
        dirIcon.setFitHeight(16);
        dirIcon.setPreserveRatio(true);
        dirIcon.setSmooth(true);

        StackPane dirIconBox = new StackPane(dirIcon);
        dirIconBox.setId("dir-icon-box");

        Label instructionLabel = new Label("Guess a number between 1 and " + maxRange + ":");
        TextField guessField = new TextField();
        Button guessButton = new Button("Guess");
        Label resultLabel = new Label();
        Label historyLabel = new Label("History of moves:");
        ListView<String> historyList = new ListView<>();

        HBox inputRow = new HBox(8, guessField, dirIconBox);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.setId("input-row");
        HBox.setHgrow(guessField, Priority.ALWAYS);

        VBox inputPanel = new VBox(10, instructionLabel, inputRow, guessButton, resultLabel);
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

        StackPane wrapper = new StackPane(root);

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

                if (guess < targetNumber) {
                    dirIcon.setImage(imgUp);
                    dirIconBox.setStyle("-fx-effect: dropshadow(gaussian, #9AB8C8, 15, 0.5, 0, 0);");
                } else if (guess > targetNumber) {
                    dirIcon.setImage(imgDown);
                    dirIconBox.setStyle("-fx-effect: dropshadow(gaussian, #B03878, 15, 0.5, 0, 0);");
                } else {
                    dirIcon.setImage(imgSuccess);
                    dirIconBox.setStyle("-fx-effect: dropshadow(gaussian, #9AB8C8, 15, 0.5, 0, 0);");
                }

                historyList.getItems().add(0, guess + " → " + outcome);
                guessField.clear();

                if (result == MiniGameResult.SUCCESS) {
                    inputPanel.getStyleClass().add("success");
                    resultLabel.getStyleClass().add("success");
                    showEndOverlay(wrapper, true);
                }

                if (result != MiniGameResult.PENDING) {
                    guessField.setDisable(true);
                    guessButton.setDisable(true);
                    if (result == MiniGameResult.FAILURE) {
                        showEndOverlay(wrapper, false);
                    }
                }
            } catch (NumberFormatException ex) {
                resultLabel.setText("Please enter a valid number.");
                historyList.getItems().add(0, guessText + " → invalid input");
            }
        };

        guessButton.setOnAction(e -> submitGuess.run());
        guessField.setOnAction(e -> submitGuess.run());

        Scene scene = new Scene(wrapper, width, height);
        setupWindow(stage, scene, "Guess The Number");
    }

    private Image loadIcon(String resourcePath) {
        var stream = getClass().getResourceAsStream(resourcePath);
        return stream != null ? new Image(stream) : null;
    }

    private String checkGuess(int guess) {
        this.attempts++;
        int attemptsLeft = this.maxAttempts - this.attempts;

        if (guess < this.targetNumber) {
            if (this.attempts >= this.maxAttempts) {
                this.result = MiniGameResult.FAILURE;
                return "Game Over! The number was: " + this.targetNumber;
            }
            return "Too low! Attempts left: " + attemptsLeft;
        } else if (guess > this.targetNumber) {
            if (this.attempts >= this.maxAttempts) {
                this.result = MiniGameResult.FAILURE;
                return "Game Over! The number was: " + this.targetNumber;
            }
            return "Too high! Attempts left: " + attemptsLeft;
        }

        this.result = MiniGameResult.SUCCESS;
        return "Correct! You guessed the number in " + this.attempts + " attempts.";
    }
}