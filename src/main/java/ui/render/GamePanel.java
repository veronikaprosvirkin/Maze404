package ui.render;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import model.Grid;
import model.Player;

public class GamePanel extends Pane {
    private static final int TILE_SIZE = 32;

    private final Canvas canvas;
    private final GridRenderer gridRenderer;
    private final PlayerRenderer playerRenderer;

    public GamePanel(Grid grid, Player player) {
        this.canvas = new Canvas(grid.getWidth() * TILE_SIZE, grid.getHeight() * TILE_SIZE);
        this.gridRenderer = new GridRenderer(new SpriteSheet());
        this.playerRenderer = new PlayerRenderer();
        getChildren().add(canvas);

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                redraw(grid, player);
            }
        };
        timer.start();
    }

    public void redraw(Grid grid, Player player) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gridRenderer.draw(gc, grid);
        playerRenderer.draw(gc, player, TILE_SIZE);
    }
}

