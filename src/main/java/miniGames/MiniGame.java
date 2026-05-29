package miniGames;

import enums.MiniGameResult;
import javafx.scene.Scene;
import javafx.stage.Stage;

public abstract class MiniGame {
    protected int width = 400;
    protected int height = 300;
    protected MiniGameResult result = MiniGameResult.PENDING;
    protected void setupWindow(Stage stage, Scene scene, String title){
        scene.getStylesheets().add(
            getClass().getResource("/styles/minigames.css").toExternalForm()
        );
        stage.setTitle(title);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setScene(scene);
        stage.show();
    }
    public MiniGameResult getResult() {
        return this.result;
    }

}
