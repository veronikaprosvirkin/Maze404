package ui.render;

import enums.Difficulty;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import lombok.Setter;
import model.Grid;
import model.Player;

public class GamePanel extends Pane {
    private static final int TILE_SIZE = 32;
    private static final int MIST_RADIUS_CELLS = 3;
    private static final int MIST_SAMPLE_STEP = 2;
    private static final double MIST_BASE_ALPHA = 0.88;
    private static final double MIST_SWIRL_ALPHA = 0.14;
    private static final double MIST_DRIFT_SPEED = 0.2;
    private static final double MIST_COLOR_R = 0.78;
    private static final double MIST_COLOR_G = 0.82;
    private static final double MIST_COLOR_B = 0.84;

    private final Canvas canvas;
    private final GridRenderer gridRenderer;
    private final PlayerRenderer playerRenderer;
    private final Difficulty difficulty;
    @Setter
    private boolean mistEnabled = false;
    private long mistTimeNanos = 0L;

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
                mistTimeNanos = now;
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

    public boolean isMistEnabled() {
        return mistEnabled;
    }

    private void drawMist(GraphicsContext gc, Grid grid, Player player) {
        double playerCenterX = (player.getCol() + 0.5) * TILE_SIZE;
        double playerCenterY = (player.getRow() + 0.5) * TILE_SIZE;
        double clearRadius = MIST_RADIUS_CELLS * TILE_SIZE;
        double fadeBand = TILE_SIZE * 2.2;
        double timeSeconds = mistTimeNanos / 1_000_000_000.0;
        double width = grid.getWidth() * TILE_SIZE;
        double height = grid.getHeight() * TILE_SIZE;

        for (int y = 0; y < height; y += MIST_SAMPLE_STEP) {
            for (int x = 0; x < width; x += MIST_SAMPLE_STEP) {
                double sampleX = x + MIST_SAMPLE_STEP * 0.5;
                double sampleY = y + MIST_SAMPLE_STEP * 0.5;
                double dx = sampleX - playerCenterX;
                double dy = sampleY - playerCenterY;
                double dist = Math.sqrt(dx * dx + dy * dy);

                double distanceOpacity = smoothStep(clearRadius - fadeBand, clearRadius + fadeBand, dist);

                // Layered low-frequency drift makes the fog feel natural and soft.
                double drift1 = Math.sin(sampleX * 0.018 + timeSeconds * MIST_DRIFT_SPEED)
                        * Math.cos(sampleY * 0.016 - timeSeconds * MIST_DRIFT_SPEED * 0.85);
                double drift2 = Math.sin(sampleX * 0.011 - timeSeconds * MIST_DRIFT_SPEED * 0.52)
                        * Math.sin(sampleY * 0.014 + timeSeconds * MIST_DRIFT_SPEED * 0.68);
                double drift3 = Math.cos(sampleX * 0.007 + sampleY * 0.009 + timeSeconds * MIST_DRIFT_SPEED * 0.38);
                double swirl = (drift1 * 0.45 + drift2 * 0.35 + drift3 * 0.20) * MIST_SWIRL_ALPHA;

                double alpha = clamp(distanceOpacity * (MIST_BASE_ALPHA + swirl), 0.0, 0.97);
                if (alpha > 0.01) {
                    gc.setFill(Color.color(MIST_COLOR_R, MIST_COLOR_G, MIST_COLOR_B, alpha));
                    gc.fillRect(x, y, MIST_SAMPLE_STEP, MIST_SAMPLE_STEP);
                }
            }
        }
    }

    private double smoothStep(double edge0, double edge1, double x) {
        double range = edge1 - edge0;
        if (range <= 0.0) {
            return x >= edge1 ? 1.0 : 0.0;
        }
        double t = clamp((x - edge0) / range, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
