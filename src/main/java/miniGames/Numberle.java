package miniGames;

import enums.Difficulty;
import enums.MiniGameResult;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Random;

public class Numberle extends MiniGame {
    String targetPassword;
    String currentGuess = "";
    int currentRow = 0;
    private int maxAttempts;
    Label[][] gridLabels;
    Label statusLabel;
    private StackPane wrapper;
    private final int[] digitStates = new int[10];
    private Button[] digitButtons;
    private Timeline activeCellPulseTimeline;
    private double activeCellPulsePhase = 0.0;
    private static final double PULSE_FRAME_MS = 16.0;
    private static final double PULSE_PERIOD_MS = 1400.0;

    public Numberle() {
        this.width = 450;
        this.height = 540;
    }

    public static Numberle startNewGame(Difficulty difficulty) {
        Numberle game = new Numberle();
        game.applyDifficulty(difficulty);
        game.showWindow();
        return game;
    }

    private void applyDifficulty(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                maxAttempts = 8;
                this.height = 650;
                break;
            case MEDIUM:
                maxAttempts = 6;
                break;
            case HARD:
                maxAttempts = 4;
                break;
            default:
                maxAttempts = 6;
                break;
        }
    }

    private void showWindow() {
        Stage stage = new Stage();
        Label instructionLabel = new Label(
                "Numberle: Guess the 5-digit number! You have " + maxAttempts + " attempts.");
        instructionLabel.setWrapText(true);
        instructionLabel.setAlignment(javafx.geometry.Pos.CENTER);
        instructionLabel.setId("instruction-label");

        statusLabel = new Label("Attempt " + (currentRow + 1) + " of " + maxAttempts);
        statusLabel.setAlignment(javafx.geometry.Pos.CENTER);
        statusLabel.setId("timer-label");

        Random random = new Random();
        targetPassword = String.format("%05d", random.nextInt(100000));

        for (int i = 0; i < 10; i++) {
            digitStates[i] = 0;
        }

        GridPane grid = createGrid();
        startActiveCellPulse();
        refreshActiveTypingCell();

        // ── Legend Panel ──
        javafx.scene.layout.HBox legendBox = new javafx.scene.layout.HBox(8);
        legendBox.setAlignment(javafx.geometry.Pos.CENTER);

        Label itemGreen = new Label("  Correct Spot  ");
        itemGreen.getStyleClass().addAll("wordle-green");
        itemGreen.setStyle(
                "-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 9px; -fx-padding: 3 6 3 6; -fx-background-radius: 4; -fx-border-radius: 4;");

        Label itemYellow = new Label("  Wrong Spot  ");
        itemYellow.getStyleClass().addAll("wordle-yellow");
        itemYellow.setStyle(
                "-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 9px; -fx-padding: 3 6 3 6; -fx-background-radius: 4; -fx-border-radius: 4;");

        Label itemGray = new Label("  Incorrect  ");
        itemGray.getStyleClass().addAll("wordle-gray");
        itemGray.setStyle(
                "-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 9px; -fx-padding: 3 6 3 6; -fx-background-radius: 4; -fx-border-radius: 4;");

        legendBox.getChildren().addAll(itemGreen, itemYellow, itemGray);

        // ── Keypad Panel ──
        GridPane keypad = new GridPane();
        keypad.setHgap(4);
        keypad.setVgap(4);
        keypad.setAlignment(javafx.geometry.Pos.CENTER);

        digitButtons = new Button[10];
        for (int i = 0; i < 10; i++) {
            final int num = i;
            Button btn = new Button(String.valueOf(i));
            btn.setMinSize(28, 30);
            btn.setStyle(
                    "-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-radius: 4;");
            btn.getStyleClass().add("numberle-key");
            btn.setFocusTraversable(false);
            btn.setOnAction(e -> handleInput(String.valueOf(num)));
            digitButtons[i] = btn;
            keypad.add(btn, i, 0);
        }

        Button delBtn = new Button("DELETE");
        delBtn.setMinSize(70, 30);
        delBtn.setStyle(
                "-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-radius: 4;");
        delBtn.getStyleClass().add("numberle-key");
        delBtn.setFocusTraversable(false);
        delBtn.setOnAction(e -> handleInput("Del"));

        Button entBtn = new Button("ENTER");
        entBtn.setMinSize(70, 30);
        entBtn.setStyle(
                "-fx-font-family: 'IBM Plex Mono'; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-radius: 4;");
        entBtn.getStyleClass().add("numberle-key");
        entBtn.setFocusTraversable(false);
        entBtn.setOnAction(e -> handleInput("Ent"));

        javafx.scene.layout.HBox actionKeys = new javafx.scene.layout.HBox(8, delBtn, entBtn);
        actionKeys.setAlignment(javafx.geometry.Pos.CENTER);

        VBox keypadContainer = new VBox(6, keypad, actionKeys);
        keypadContainer.setAlignment(javafx.geometry.Pos.CENTER);

        VBox root = new VBox(10, instructionLabel, statusLabel, grid, legendBox, keypadContainer);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setId("game-container");

        wrapper = new StackPane(root);
        wrapper.setFocusTraversable(true);
        Scene scene = new Scene(wrapper, width, height);
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (result != MiniGameResult.PENDING)
                return;

            if (e.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) {
                handleInput("Del");
            } else if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleInput("Ent");
            } else if (e.getText().matches("\\d")) {
                handleInput(e.getText());
            }
        });

        setupWindow(stage, scene, "Numberle");
        Platform.runLater(wrapper::requestFocus);
    }

    private GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setId("numberle-grid");
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setAlignment(javafx.geometry.Pos.CENTER);

        gridLabels = new Label[maxAttempts][5];
        for (int row = 0; row < maxAttempts; row++) {
            for (int col = 0; col < 5; col++) {
                Label cell = new Label("");
                cell.setMinSize(34, 34);
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
        refreshActiveTypingCell();
    }

    private void checkGuess() {
        boolean[] targetUsed = new boolean[5];
        boolean[] guessUsed = new boolean[5];
        int correctCount = 0;

        for (int i = 0; i < 5; i++) {
            char gChar = currentGuess.charAt(i);
            int digit = gChar - '0';
            if (currentGuess.charAt(i) == targetPassword.charAt(i)) {
                gridLabels[currentRow][i].getStyleClass().add("wordle-green");
                targetUsed[i] = true;
                guessUsed[i] = true;
                correctCount++;
                digitStates[digit] = 3;
            }
        }

        for (int i = 0; i < 5; i++) {
            if (guessUsed[i])
                continue;
            char gChar = currentGuess.charAt(i);
            int digit = gChar - '0';
            boolean isYellow = false;

            for (int j = 0; j < 5; j++) {
                if (targetUsed[j])
                    continue;

                if (currentGuess.charAt(i) == targetPassword.charAt(j)) {
                    gridLabels[currentRow][i].getStyleClass().add("wordle-yellow");
                    targetUsed[j] = true;
                    isYellow = true;
                    if (digitStates[digit] < 3) {
                        digitStates[digit] = 2;
                    }
                    break;
                }
            }

            if (!isYellow) {
                gridLabels[currentRow][i].getStyleClass().add("wordle-gray");
                if (digitStates[digit] < 2) {
                    digitStates[digit] = 1;
                }
            }
        }

        updateKeypadStyles();

        if (correctCount == 5) {
            result = MiniGameResult.SUCCESS;
            statusLabel.setText("Congratulations! You've guessed the number!");
            stopActiveCellPulse();
            showEndOverlay(wrapper, true, "Code solved: " + targetPassword);
        } else {
            currentRow++;
            if (currentRow >= maxAttempts) {
                result = MiniGameResult.FAILURE;
                statusLabel.setText("Game Over! The number was: " + targetPassword);
                stopActiveCellPulse();
                showEndOverlay(wrapper, false, "Correct code: " + targetPassword);
            } else {
                statusLabel.setText("Attempt " + (currentRow + 1) + " of " + maxAttempts);
                currentGuess = "";
                refreshActiveTypingCell();
            }
        }
    }

    private void updateKeypadStyles() {
        for (int i = 0; i < 10; i++) {
            int state = digitStates[i];
            Button btn = digitButtons[i];
            btn.getStyleClass().removeAll("wordle-green", "wordle-yellow", "wordle-gray");
            btn.setDisable(state == 1);
            if (state == 3) {
                btn.getStyleClass().add("wordle-green");
            } else if (state == 2) {
                btn.getStyleClass().add("wordle-yellow");
            } else if (state == 1) {
                btn.getStyleClass().add("wordle-gray");
            }
        }
    }

    private void startActiveCellPulse() {
        activeCellPulseTimeline = new Timeline(new KeyFrame(Duration.millis(PULSE_FRAME_MS), e -> {
            activeCellPulsePhase += (Math.PI * 2.0 * PULSE_FRAME_MS) / PULSE_PERIOD_MS;
            if (activeCellPulsePhase > Math.PI * 2.0) {
                activeCellPulsePhase -= Math.PI * 2.0;
            }
            refreshActiveTypingCell();
        }));
        activeCellPulseTimeline.setCycleCount(Timeline.INDEFINITE);
        activeCellPulseTimeline.play();
    }

    private void stopActiveCellPulse() {
        if (activeCellPulseTimeline != null) {
            activeCellPulseTimeline.stop();
        }
        refreshActiveTypingCell();
    }

    private void refreshActiveTypingCell() {
        for (int row = 0; row < maxAttempts; row++) {
            for (int col = 0; col < 5; col++) {
                gridLabels[row][col].setStyle(null);
            }
        }

        if (result != MiniGameResult.PENDING || currentRow >= maxAttempts || currentGuess.length() >= 5) {
            return;
        }

        // Smooth pulse in [0..1] that continuously brightens/dims the current typing cell background.
        double pulse = 0.5 + 0.5 * Math.sin(activeCellPulsePhase);
        Color baseBg;
        Color peakBg;
        Color baseBorder;
        Color peakBorder;
        switch (Difficulty.current) {
            case MEDIUM -> {
                baseBg = Color.web("#161210");
                peakBg = Color.web("#2A2218");
                baseBorder = Color.web("#2E2620");
                peakBorder = Color.web("#5A4A2E");
            }
            case HARD -> {
                baseBg = Color.web("#150A0C");
                peakBg = Color.web("#2A1318");
                baseBorder = Color.web("#381520");
                peakBorder = Color.web("#6A3040");
            }
            default -> {
                baseBg = Color.web("#111520");
                peakBg = Color.web("#1B2435");
                baseBorder = Color.web("#252D40");
                peakBorder = Color.web("#3D4D68");
            }
        }

        Color blendedBg = baseBg.interpolate(peakBg, pulse);
        Color blendedBorder = baseBorder.interpolate(peakBorder, pulse);
        Label activeCell = gridLabels[currentRow][currentGuess.length()];
        activeCell.setStyle(
                "-fx-background-color: " + toRgba(blendedBg) + ";" +
                        "-fx-border-color: " + toRgba(blendedBorder) + ";");
    }

    private String toRgba(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return "rgba(" + r + ", " + g + ", " + b + ", 1.0)";
    }
}
