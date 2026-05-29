package miniGames;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Setter;

import java.util.Random;


public class GuessTheNumber {
    private int targetNumber;
    private int attempts;
    private boolean isStarted;
    private boolean isWon;
    @Setter
    private static int height = 300;
    @Setter
    private static int width = 400;

    public GuessTheNumber(int targetNumber) {
        this.targetNumber = targetNumber;
        this.attempts = 0;
        this.isStarted = true;
    }

    public static GuessTheNumber startNewGame() {
        Random random = new Random();
        int targetNumber = random.nextInt(100) + 1; // Random number between 1 and 100
        GuessTheNumber game = new GuessTheNumber(targetNumber);
        Stage stage = new Stage();
        stage.setTitle("Guess The Number");
        stage.setWidth(width);
        stage.setHeight(height);
        Scene scene = getScene();
        stage.setScene(scene);
        stage.show();
        return game;


    }

    private static Scene getScene() {
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


        Scene scene = new Scene(root, width ,height);
        return scene;
    }
}
