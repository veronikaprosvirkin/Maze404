package miniGames;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TestMiniGames extends Application {

    @Override
    public void start(Stage primaryStage) {
        Button guessGameBtn = new Button("Test: Guess The Number");
        guessGameBtn.setOnAction(e -> GuessTheNumber.startNewGame());

        Button clickerGameBtn = new Button("Test: Clicker Game");
        clickerGameBtn.setOnAction(e -> ClickerMiniGame.startNewGame());

        Button memoryGameBtn = new Button("Test: Memory Game");
        memoryGameBtn.setOnAction(e -> MemoryGame.startNewGame());

        Button echoGameBtn = new Button("Test: Echo Game");
        echoGameBtn.setOnAction(e -> EchoGame.startNewGame());

        Button powerGame = new Button("Test: Power Game");
        powerGame.setOnAction(e -> PowerGridGame.startNewGame());

        VBox root = new VBox(20, guessGameBtn, clickerGameBtn, memoryGameBtn, echoGameBtn, powerGame);
        root.setStyle("-fx-padding: 50px; -fx-alignment: center;");

        Scene scene = new Scene(root, 400, 350);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Mini Games Test");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}