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
    private static final int MIST_RADIUS_CELLS = 3;
    private static final int MIST_SAMPLE_STEP = 2;
    private static final double MIST_ALPHA_CAP = 0.97;

    private final Canvas canvas;
    private final GridRenderer gridRenderer;
    private final PlayerRenderer playerRenderer;
    private final Difficulty difficulty;
    private boolean mistEnabled = false;
    private double mistAnimationTimeScale = 1.0;
    private long mistTimeNanos = 0L;

    private record MistProfile(
            double colorR,
            double colorG,
            double colorB,
            double baseAlpha,
            double swirlAlpha,
            double driftSpeed,
            double flowSpeedX,
            double flowSpeedY,
            double pulseSpeed,
            double pulseStrength,
            double lateralSwing,
            double verticalSwing,
            double noiseScaleX,
            double noiseScaleY
    ) {
    }

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

    public void setMistEnabled(boolean mistEnabled) {
        this.mistEnabled = mistEnabled;
    }

    public double getMistAnimationTimeScale() {
        return mistAnimationTimeScale;
    }

    public void setMistAnimationTimeScale(double mistAnimationTimeScale) {
        this.mistAnimationTimeScale = mistAnimationTimeScale > 0.0 ? mistAnimationTimeScale : 1.0;
    }

    private void drawMist(GraphicsContext gc, Grid grid, Player player) {
        MistProfile profile = getMistProfile();
        double playerCenterX = (player.getCol() + 0.5) * TILE_SIZE;
        double playerCenterY = (player.getRow() + 0.5) * TILE_SIZE;
        double clearRadius = MIST_RADIUS_CELLS * TILE_SIZE;
        double fadeBand = TILE_SIZE * 2.2;
        double timeSeconds = (mistTimeNanos / 1_000_000_000.0) * mistAnimationTimeScale;
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
                double pulse = 1.0 + Math.sin(timeSeconds * profile.pulseSpeed()) * profile.pulseStrength();

                double flowX = sampleX + timeSeconds * profile.flowSpeedX()
                        + Math.sin(timeSeconds * 0.35) * profile.lateralSwing();
                double flowY = sampleY + timeSeconds * profile.flowSpeedY()
                        + Math.cos(timeSeconds * 0.28) * profile.verticalSwing();

                // Layered low-frequency drift makes the fog feel natural and soft.
                double drift1 = Math.sin(flowX * profile.noiseScaleX() + timeSeconds * profile.driftSpeed())
                        * Math.cos(flowY * profile.noiseScaleY() - timeSeconds * profile.driftSpeed() * 0.85);
                double drift2 = Math.sin(flowX * profile.noiseScaleX() * 0.62 - timeSeconds * profile.driftSpeed() * 0.52)
                        * Math.sin(flowY * profile.noiseScaleY() * 0.88 + timeSeconds * profile.driftSpeed() * 0.68);
                double drift3 = Math.cos(flowX * profile.noiseScaleX() * 0.38 + flowY * profile.noiseScaleY() * 0.56
                        + timeSeconds * profile.driftSpeed() * 0.38);
                double drift4 = Math.sin((flowX + flowY) * profile.noiseScaleX() * 0.32
                        - timeSeconds * profile.driftSpeed() * 0.44);
                double swirl = (drift1 * 0.35 + drift2 * 0.30 + drift3 * 0.20 + drift4 * 0.15) * profile.swirlAlpha();

                double alpha = clamp(distanceOpacity * pulse * (profile.baseAlpha() + swirl), 0.0, MIST_ALPHA_CAP);
                if (alpha > 0.01) {
                    gc.setFill(Color.color(profile.colorR(), profile.colorG(), profile.colorB(), alpha));
                    gc.fillRect(x, y, MIST_SAMPLE_STEP, MIST_SAMPLE_STEP);
                }
            }
        }
    }

    private MistProfile getMistProfile() {
        return switch (difficulty) {
            case HARD -> new MistProfile(
                    0.69, 0.48, 0.53,   // purple-red flame storm from inferno mist/frost tones
                    0.92, 0.22,
                    1.10, 34.0, 20.0,
                    1.20, 0.09,
                    28.0, 18.0,
                    0.021, 0.018
            );
            case MEDIUM -> new MistProfile(
                    0.76, 0.64, 0.50,   // dusty parchment/bone tint for sanded stone storm
                    0.89, 0.18,
                    0.90, 30.0, 12.0,
                    0.95, 0.07,
                    24.0, 13.0,
                    0.016, 0.013
            );
            case EASY -> new MistProfile(
                    0.66, 0.72, 0.80,   // cryo mist blue-gray for blizzard feel
                    0.87, 0.20,
                    1.00, 36.0, 22.0,
                    1.05, 0.08,
                    30.0, 20.0,
                    0.023, 0.019
            );
        };
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
