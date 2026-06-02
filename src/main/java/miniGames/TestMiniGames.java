package miniGames;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import enums.Difficulty;

public class TestMiniGames extends Application {

    @Override
    public void start(Stage primaryStage) {
        Button difficultyBtn = new Button("Difficulty: " + TestLauncher.difficulty);
        difficultyBtn.setOnAction(e -> {
            switch (TestLauncher.difficulty) {
                case EASY:
                    TestLauncher.difficulty = Difficulty.MEDIUM;
                    break;
                case MEDIUM:
                    TestLauncher.difficulty = Difficulty.HARD;
                    break;
                case HARD:
                    TestLauncher.difficulty = Difficulty.EASY;
                    break;
            }
            Difficulty.current = TestLauncher.difficulty;
            difficultyBtn.setText("Difficulty: " + TestLauncher.difficulty);
        });
        // Highlight the difficulty button visually
        difficultyBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #4A90E2; -fx-text-fill: white;");

        Button guessGameBtn = new Button("Test: Guess The Number");
        guessGameBtn.setOnAction(e -> GuessTheNumber.startNewGame(TestLauncher.difficulty, "Guess The Number Reward"));

        Button clickerGameBtn = new Button("Test: Clicker Game");
        clickerGameBtn.setOnAction(e -> ClickerMiniGame.startNewGame(TestLauncher.difficulty, "Clicker Game Reward"));

        Button memoryGameBtn = new Button("Test: Memory Game");
        memoryGameBtn.setOnAction(e -> MemoryGame.startNewGame(TestLauncher.difficulty, "Memory Game Reward"));

        Button echoGameBtn = new Button("Test: Echo Game");
        echoGameBtn.setOnAction(e -> EchoGame.startNewGame(TestLauncher.difficulty, "Echo Game Reward"));

        Button powerGame = new Button("Test: Power Game");
        powerGame.setOnAction(e -> PowerGridGame.startNewGame(TestLauncher.difficulty, "Power Grid Reward"));

        Button numberleGame = new Button("Test: Numberle Game");
        numberleGame.setOnAction(e -> Numberle.startNewGame(TestLauncher.difficulty, "Numberle Reward"));

        VBox root = new VBox(15, difficultyBtn, guessGameBtn, clickerGameBtn, memoryGameBtn, echoGameBtn, powerGame,
                numberleGame);
        root.setStyle("-fx-padding: 30px; -fx-alignment: center;");

        Scene scene = new Scene(root, 400, 420);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Mini Games Test");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}