package ui.render;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import model.Grid;

public class GamePanel extends Pane {
    private static final int TILE_SIZE = 32;

    private final Canvas canvas;
    private final GridRenderer gridRenderer;

    public GamePanel(Grid grid) {
        this.canvas = new Canvas(grid.getWidth() * TILE_SIZE, grid.getHeight() * TILE_SIZE);
        this.gridRenderer = new GridRenderer(new SpriteSheet());
        getChildren().add(canvas);
    }

    public void redraw(Grid grid) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gridRenderer.draw(gc, grid);
    }
}

