package miniGames;

import enums.Difficulty;
import enums.MiniGameResult;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MemoryGame extends MiniGame {
    private Button firstSelected;
    private Button secondSelected;
    private int pairsFound = 0;
    private int mistakes = 0;
    private javafx.scene.layout.HBox livesContainer;

    private int rows;
    private int cols;
    private double revealTime;
    private int mistakesLimit;

    public static MemoryGame startNewGame(Difficulty difficulty, String reward) {
        MemoryGame game = new MemoryGame();
        game.rewardName = reward;
        game.applyDifficulty(difficulty);
        game.showWindow();
        return game;
    }

    private void applyDifficulty(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                rows = 3; cols = 4;
                revealTime = 5.0;
                mistakesLimit = 4;
                this.height = 550;
                break;
            case MEDIUM:
                rows = 4; cols = 4;
                revealTime = 4.0;
                mistakesLimit = 4;
                this.height = 600;
                break;
            case HARD:
                rows = 4; cols = 5;
                revealTime = 6.0;
                mistakesLimit = 5;
                this.height = 650;
                break;
            default:
                rows = 4; cols = 4;
                revealTime = 4.0;
                mistakesLimit = 4;
                break;
        }
    }

    private void showWindow() {
        Stage stage = new Stage();

        StackPane wrapper = new StackPane();

        livesContainer = new javafx.scene.layout.HBox(8);
        livesContainer.setAlignment(Pos.CENTER);
        updateLivesUI();

        Label instructionLabel = new Label("Memory Game: Find all pairs!\nClick 'Start' to reveal cards for " + (int)revealTime + " seconds");
        instructionLabel.setWrapText(true);
        instructionLabel.setAlignment(javafx.geometry.Pos.CENTER);
        instructionLabel.setId("instruction-label");


        List<String> allIcons = Arrays.asList("🤖", "💎", "🔋", "🔥", "💻", "⚡", "🚀", "🛰️", "📡", "💾");
        List<String> hiddenValues = new ArrayList<>();
        int pairsNeeded = (rows * cols) / 2;

        for (int i = 0; i < pairsNeeded; i++) {
            hiddenValues.add(allIcons.get(i));
            hiddenValues.add(allIcons.get(i));
        }
        Collections.shuffle(hiddenValues);

        GridPane grid = new GridPane();
        grid.setId("memory-grid");
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(javafx.geometry.Pos.CENTER);

        int valueIndex = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {

                String secretValue = hiddenValues.get(valueIndex);

                Button card = new Button("?");
                card.setDisable(true);
                card.setId("memory-card");
                card.setMinSize(60, 60);
                card.setUserData(secretValue);
                valueIndex++;

                card.setOnAction(e -> {
                    if (result != MiniGameResult.PENDING || card == firstSelected || secondSelected != null) {
                        return;
                    }

                    card.setText(secretValue);

                    if (firstSelected == null) {
                        firstSelected = card;
                    } else {
                        secondSelected = card;

                        if (firstSelected.getUserData().equals(secondSelected.getUserData())) {
                            pairsFound++;
                            firstSelected.setDisable(true);
                            secondSelected.setDisable(true);
                            firstSelected = null;
                            secondSelected = null;

                            if (pairsFound == pairsNeeded) {
                                result = MiniGameResult.SUCCESS;
                                instructionLabel.setText("You Win! All pairs found!");
                                showEndOverlay(wrapper, true);
                            }
                        } else {
                            mistakes++;
                            updateLivesUI();

                            if (mistakes >= mistakesLimit) {
                                result = MiniGameResult.FAILURE;
                                instructionLabel.setText("Game Over! You lost");
                                disableAllCards(grid);
                                showEndOverlay(wrapper, false);
                            } else {
                                PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(1));
                                pause.setOnFinished(event -> {
                                    firstSelected.setText("?");
                                    secondSelected.setText("?");
                                    firstSelected = null;
                                    secondSelected = null;
                                });
                                pause.play();
                            }
                        }
                    }
                });

                grid.add(card, col, row);
            }
        }

        Scene scene = getScene(wrapper, grid, instructionLabel);
        setupWindow(stage, scene, "Memory Game");
    }

    private Scene getScene(StackPane wrapper, GridPane grid, Label instructionLabel) {
        Button startButton = new Button("Start");
        startButton.setId("start-button");
        startButton.setStyle("-fx-pref-width: 290px; -fx-max-width: 290px;");

        PauseTransition initialPause = new PauseTransition(Duration.seconds(revealTime));
        initialPause.setOnFinished(event -> {
            grid.getChildren().forEach(node -> {
                if (node instanceof Button) {
                    ((Button) node).setText("?");
                    ((Button) node).setDisable(false);
                }
            });
        });

        startButton.setOnAction(e -> {
            startButton.setVisible(false);
            startButton.setManaged(false);

            grid.getChildren().forEach(node -> {
                if (node instanceof Button) {
                    ((Button) node).setText((String) ((Button) node).getUserData());
                }
            });

            initialPause.play();
        });

        VBox root = new VBox(12, livesContainer, instructionLabel, startButton, grid);
        root.setAlignment(Pos.CENTER);
        root.setId("game-container");

        wrapper.getChildren().add(root);
        return new Scene(wrapper, width, height);
    }

    private void disableAllCards(GridPane grid) {
        grid.getChildren().forEach(node -> {
            if (node instanceof Button) {
                ((Button) node).setDisable(true);
            }
        });
    }

    private void updateLivesUI() {
        if (livesContainer == null)
            return;
        livesContainer.getChildren().clear();
        int livesLeft = mistakesLimit - mistakes;
        for (int i = 0; i < mistakesLimit; i++) {
            Label heart = new Label("❤");
            heart.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
            if (i < livesLeft) {
                heart.getStyleClass().add("memory-heart-active");
            } else {
                heart.getStyleClass().add("memory-heart-lost");
            }
            livesContainer.getChildren().add(heart);
        }
    }
}