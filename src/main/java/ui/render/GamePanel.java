package ui.render;

import enums.Difficulty;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import model.Grid;
import model.Player;

public class GamePanel extends Pane {
    private static final int TILE_SIZE = 32;
    private static final int MIST_RADIUS_CELLS = 2;

    private final Canvas canvas;
    private final GridRenderer gridRenderer;
    private final PlayerRenderer playerRenderer;
    private final Difficulty difficulty;
    private boolean mistEnabled = false;

    public GamePanel(Grid grid, Player player) {
        this(grid, player, Difficulty.current);
    }

    public GamePanel(Grid grid, Player player, Difficulty difficulty) {
        this.difficulty = difficulty != null ? difficulty : Difficulty.current;
        this.canvas = new Canvas(grid.getWidth() * TILE_SIZE, grid.getHeight() * TILE_SIZE);
        this.gridRenderer = new GridRenderer(new SpriteSheet(this.difficulty));
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
        if (mistEnabled) {
            drawMist(gc, grid, player);
        }
        playerRenderer.draw(gc, player, TILE_SIZE, this.difficulty);
    }

    public void setMistEnabled(boolean enabled) {
        this.mistEnabled = enabled;
    }

    public boolean isMistEnabled() {
        return mistEnabled;
    }

    private void drawMist(GraphicsContext gc, Grid grid, Player player) {
        int playerRow = player.getRow();
        int playerCol = player.getCol();
        int radiusSq = MIST_RADIUS_CELLS * MIST_RADIUS_CELLS;

        gc.setFill(Color.rgb(0, 0, 0, 0.6));
        for (int row = 0; row < grid.getHeight(); row++) {
            for (int col = 0; col < grid.getWidth(); col++) {
                int dRow = row - playerRow;
                int dCol = col - playerCol;
                if (dRow * dRow + dCol * dCol > radiusSq) {
                    gc.fillRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }
}
