package ui.render;

import enums.Difficulty;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import model.Artifact;
import model.Grid;
import model.Player;

import java.util.List;

public class GamePanel extends Pane {
    private static final int TILE_SIZE = 32;
    private static final int MIST_RADIUS_CELLS = 3;
    private static final int MIST_SAMPLE_STEP = 2;
    private static final double MIST_ALPHA_CAP = 0.97;
    private static final double MIST_FOCUS_TRANSITION_SECONDS = 0.3;

    private final Canvas canvas;
    private final GridRenderer gridRenderer;
    private final PlayerRenderer playerRenderer;
    private final Difficulty difficulty;
    private final List<Artifact> artifacts;
    private boolean mistEnabled = false;
    private double mistAnimationTimeScale = 1.0;
    private double mistDensity = 1.0;
    private long mistTimeNanos = 0L;
    private long lastFrameNanos = 0L;
    private double frameDeltaSeconds = 1.0 / 60.0;
    private double mistFocusX;
    private double mistFocusY;
    private double mistFocusStartX;
    private double mistFocusStartY;
    private double mistFocusTargetX;
    private double mistFocusTargetY;
    private double mistFocusElapsedSeconds = MIST_FOCUS_TRANSITION_SECONDS;
    private boolean mistFocusInitialized = false;

    private record MistProfile(
            double colorR,
            double colorG,
            double colorB,
            double accentR,
            double accentG,
            double accentB,
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
            double noiseScaleY,
            double accentStrength
    ) {
    }


    public GamePanel(Grid grid, Player player, Difficulty difficulty) {
        this(grid, player, List.of(), difficulty);
    }

    public GamePanel(Grid grid, Player player, List<Artifact> artifacts, Difficulty difficulty) {
        this.difficulty = difficulty != null ? difficulty : Difficulty.current;
        this.artifacts = artifacts != null ? List.copyOf(artifacts) : List.of();
        this.canvas = new Canvas(grid.getWidth() * TILE_SIZE, grid.getHeight() * TILE_SIZE);
        this.gridRenderer = new GridRenderer(new SpriteSheet(this.difficulty));
        this.playerRenderer = new PlayerRenderer();
        getChildren().add(canvas);

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameNanos == 0L) {
                    frameDeltaSeconds = 1.0 / 60.0;
                } else {
                    frameDeltaSeconds = (now - lastFrameNanos) / 1_000_000_000.0;
                }
                lastFrameNanos = now;
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
        drawArtifacts(gc);
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

    public double getMistDensity() {
        return mistDensity;
    }

    public void setMistDensity(double mistDensity) {
        this.mistDensity = clamp(mistDensity, 0.0, 1.0);
    }

    private void drawMist(GraphicsContext gc, Grid grid, Player player) {
        MistProfile profile = getMistProfile();
        updateMistFocus(player);
        double clearRadius = MIST_RADIUS_CELLS * TILE_SIZE;
        double fadeBand = TILE_SIZE * 2.2;
        double timeSeconds = (mistTimeNanos / 1_000_000_000.0) * mistAnimationTimeScale;
        double width = grid.getWidth() * TILE_SIZE;
        double height = grid.getHeight() * TILE_SIZE;

        for (int y = 0; y < height; y += MIST_SAMPLE_STEP) {
            for (int x = 0; x < width; x += MIST_SAMPLE_STEP) {
                double sampleX = x + MIST_SAMPLE_STEP * 0.5;
                double sampleY = y + MIST_SAMPLE_STEP * 0.5;
                double dx = sampleX - mistFocusX;
                double dy = sampleY - mistFocusY;
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
                double holeNoise = ((drift1 * 0.30) + (drift2 * 0.30) + (drift3 * 0.25) + (drift4 * 0.15) + 1.0) * 0.5;
                double holeThreshold = lerp(0.72, 0.08, mistDensity);
                double densityMask = smoothStep(holeThreshold - 0.12, holeThreshold + 0.12, holeNoise);
                double accentMask = smoothStep(0.55, 0.95, ((drift2 * 0.55) + (drift4 * 0.45) + 1.0) * 0.5)
                        * profile.accentStrength();

                double alpha = clamp(distanceOpacity * pulse * densityMask * (profile.baseAlpha() + swirl), 0.0,
                        MIST_ALPHA_CAP);
                if (alpha > 0.01) {
                    double colorR = lerp(profile.colorR(), profile.accentR(), accentMask);
                    double colorG = lerp(profile.colorG(), profile.accentG(), accentMask);
                    double colorB = lerp(profile.colorB(), profile.accentB(), accentMask);
                    gc.setFill(Color.color(colorR, colorG, colorB, alpha));
                    gc.fillRect(x, y, MIST_SAMPLE_STEP, MIST_SAMPLE_STEP);
                }
            }
        }
    }

    private void drawArtifacts(GraphicsContext gc) {
        for (Artifact artifact : artifacts) {
            if (artifact == null || artifact.isCollected() || artifact.getPosition() == null) {
                continue;
            }

            double centerX = artifact.getPosition().getCol() * TILE_SIZE + TILE_SIZE / 2.0;
            double centerY = artifact.getPosition().getRow() * TILE_SIZE + TILE_SIZE / 2.0;
            double radius = TILE_SIZE * 0.22;

            Color artifactColor;
            switch (artifact.getType()) {
                case CRYSTAL ->   artifactColor = Color.rgb(255, 215, 0, 0.92);
                case MINI_GAME -> artifactColor = Color.rgb(255, 20, 147, 0.95);
                case SHIELD ->    artifactColor = Color.rgb(0, 200, 255, 0.92);
                case RADAR ->     artifactColor = Color.rgb(0, 255, 100, 0.92);
                case BEACON ->    artifactColor = Color.rgb(255, 100, 0, 0.92);
                case ELIXIR ->    artifactColor = Color.rgb(200, 0, 255, 0.92);
                default ->        artifactColor = Color.WHITE;
            }

            gc.setFill(artifactColor);
            gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);


            gc.setStroke(artifactColor.brighter());
            gc.setLineWidth(1.2);
            gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            gc.setFill(Color.rgb(255, 255, 255, 0.45));
            gc.fillOval(centerX - radius * 0.45, centerY - radius * 0.55, radius * 0.55, radius * 0.4);
        }
    }

    private void updateMistFocus(Player player) {
        double playerCenterX = (player.getCol() + 0.5) * TILE_SIZE;
        double playerCenterY = (player.getRow() + 0.5) * TILE_SIZE;

        if (!mistFocusInitialized) {
            mistFocusX = playerCenterX;
            mistFocusY = playerCenterY;
            mistFocusStartX = playerCenterX;
            mistFocusStartY = playerCenterY;
            mistFocusTargetX = playerCenterX;
            mistFocusTargetY = playerCenterY;
            mistFocusElapsedSeconds = MIST_FOCUS_TRANSITION_SECONDS;
            mistFocusInitialized = true;
            return;
        }

        if (mistFocusTargetX != playerCenterX || mistFocusTargetY != playerCenterY) {
            mistFocusStartX = mistFocusX;
            mistFocusStartY = mistFocusY;
            mistFocusTargetX = playerCenterX;
            mistFocusTargetY = playerCenterY;
            mistFocusElapsedSeconds = 0.0;
        }

        if (mistFocusElapsedSeconds < MIST_FOCUS_TRANSITION_SECONDS) {
            mistFocusElapsedSeconds = Math.min(
                    mistFocusElapsedSeconds + frameDeltaSeconds,
                    MIST_FOCUS_TRANSITION_SECONDS
            );
            double progress = mistFocusElapsedSeconds / MIST_FOCUS_TRANSITION_SECONDS;
            double easedProgress = smoothStep(0.0, 1.0, progress);
            mistFocusX = lerp(mistFocusStartX, mistFocusTargetX, easedProgress);
            mistFocusY = lerp(mistFocusStartY, mistFocusTargetY, easedProgress);
        } else {
            mistFocusX = mistFocusTargetX;
            mistFocusY = mistFocusTargetY;
        }
    }

    private MistProfile getMistProfile() {
        return switch (difficulty) {
            case HARD -> new MistProfile(
                    0.48, 0.22, 0.26,   // deep ember-red base
                    0.93, 0.52, 0.20,   // hot flame highlight
                    0.93, 0.28,
                    1.35, 18.0, -34.0,  // strong upward flame pull
                    1.65, 0.12,
                    16.0, 36.0,
                    0.027, 0.020,
                    0.72
            );
            case MEDIUM -> new MistProfile(
                    0.56, 0.44, 0.31,   // dry earth base
                    0.79, 0.66, 0.48,   // lighter sand highlight
                    0.88, 0.14,
                    0.72, 38.0, 4.0,    // broad lateral drift for dust sweep
                    0.78, 0.04,
                    34.0, 9.0,
                    0.012, 0.009,
                    0.34
            );
            case EASY -> new MistProfile(
                    0.66, 0.72, 0.80,   // cryo mist blue-gray for blizzard feel
                    0.82, 0.88, 0.94,   // icy white flecks
                    0.87, 0.20,
                    1.00, 36.0, 22.0,
                    1.05, 0.08,
                    30.0, 20.0,
                    0.023, 0.019,
                    0.18
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

    private double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }
}
