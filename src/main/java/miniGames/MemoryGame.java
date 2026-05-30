package miniGames;

import enums.MiniGameResult;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MemoryGame extends MiniGame {
    private Button firstSelected;
    private Button secondSelected;
    private int pairsFound = 0;
    private int mistakes = 0;
    private int mistakesLimit = 3;

    public static MemoryGame startNewGame() {
        MemoryGame game = new MemoryGame();
        game.showWindow();
        return game;
    }

    private void showWindow() {
        Stage stage = new Stage();

        Label instructionLabel = new Label("Memory Game: Find all pairs! Lives: " + "❤".repeat(mistakesLimit) +
                "\nClick 'Start' to reveal cards for 4 seconds");
        instructionLabel.setWrapText(true);
        instructionLabel.setAlignment(javafx.geometry.Pos.CENTER);
        instructionLabel.setId("instruction-label");

        List<String> hiddenValues = Arrays.asList(
                "🤖", "🤖", "💎", "💎", "🔋", "🔋", "🔥", "🔥", "💻", "💻", "⚡", "⚡"
        );
        Collections.shuffle(hiddenValues);

        GridPane grid = new GridPane();
        grid.setId("memory-grid");
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(javafx.geometry.Pos.CENTER);

        int valueIndex = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {

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

                            if (pairsFound == hiddenValues.size() / 2) {
                                result = MiniGameResult.SUCCESS;
                                instructionLabel.setText("You Win! All pairs found!");
                            }
                        } else {
                            mistakes++;
                            int livesLeft = mistakesLimit - mistakes;
                            instructionLabel.setText("Memory Game: Find all pairs! Lives: " + "❤".repeat(livesLeft));

                            if (mistakes >= mistakesLimit) {
                                result = MiniGameResult.FAILURE;
                                instructionLabel.setText("Game Over! You lost");
                                disableAllCards(grid);
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

        Scene scene = getScene(grid, instructionLabel);
        setupWindow(stage, scene, "Memory Game");
    }

    private Scene getScene(GridPane grid, Label instructionLabel) {
        Button startButton = new Button("Start");
        startButton.setId("start-button");

        PauseTransition initialPause = new PauseTransition(Duration.seconds(4));
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

        VBox root = new VBox(20, instructionLabel, startButton, grid);
        root.setAlignment(Pos.CENTER);
        root.setId("game-container");

        return new Scene(root, width, height);
    }

    private void disableAllCards(GridPane grid) {
        grid.getChildren().forEach(node -> {
            if (node instanceof Button) {
                ((Button) node).setDisable(true);
            }
        });
    }
}