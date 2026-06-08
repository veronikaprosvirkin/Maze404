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
    private static final double VIEWPORT_ZOOM = 2.2;
    private static final double CAMERA_SMOOTHING_SECONDS = 0.3;
    private static final double INTRO_CAMERA_SMOOTHING_SECONDS = 0.55;
    private static final double LEVEL_INTRO_SECONDS = 2.6;
    private static final int EASY_MIST_RADIUS_CELLS = 6;
    private static final int MEDIUM_MIST_RADIUS_CELLS = 5;
    private static final int HARD_MIST_RADIUS_CELLS = 3;
    private static final int DEFAULT_MIST_SAMPLE_STEP = 2;
    private static final double MIST_ALPHA_CAP = 0.97;
    private static final double MIST_FOCUS_TRANSITION_SECONDS = 0.3;
    private static final double MIST_EDGE_FADE_BAND = TILE_SIZE * 2.2;
    private static final double MIST_OUTSIDE_FADE_BAND = TILE_SIZE * 3.4;

    private final Canvas canvas;
    private final GridRenderer gridRenderer;
    private final PlayerRenderer playerRenderer;
    private final EnemyRenderer enemyRenderer;
    private final Difficulty difficulty;
    private final List<Artifact> artifacts;
    private final double baseWidth;
    private final double baseHeight;
    private final int mistSampleStep;
    private double cameraX;
    private double cameraY;
    private boolean cameraInitialized = false;
    private boolean mistEnabled = false;
    private double mistAnimationTimeScale = 1.0;
    private double mistDensity = 1.0;
    private double gameVolume = 1.0;
    private long mistTimeNanos = 0L;
    private long lastFrameNanos = 0L;
    private double frameDeltaSeconds = 1.0 / 60.0;
    private double introElapsedSeconds = 0.0;
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
        this(grid, player, artifacts, difficulty, DEFAULT_MIST_SAMPLE_STEP, List.of());
    }

    public GamePanel(Grid grid, Player player, List<Artifact> artifacts, Difficulty difficulty, int mistSampleStep, List<model.Enemy> enemies) {
        this.difficulty = difficulty != null ? difficulty : Difficulty.current;
        this.artifacts = artifacts != null ? List.copyOf(artifacts) : List.of();
        this.baseWidth = grid.getWidth() * TILE_SIZE;
        this.baseHeight = grid.getHeight() * TILE_SIZE;
        this.mistSampleStep = Math.max(1, mistSampleStep);
        this.canvas = new Canvas(baseWidth, baseHeight);
        this.gridRenderer = new GridRenderer(new SpriteSheet(this.difficulty));
        this.playerRenderer = new PlayerRenderer();
        this.enemyRenderer = new EnemyRenderer();

        setMinSize(0, 0);
        setPrefSize(baseWidth, baseHeight);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        getChildren().add(canvas);
        widthProperty().addListener((obs, oldWidth, newWidth) -> updateCanvasSize());
        heightProperty().addListener((obs, oldHeight, newHeight) -> updateCanvasSize());

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
                introElapsedSeconds = Math.min(introElapsedSeconds + frameDeltaSeconds, LEVEL_INTRO_SECONDS);
                redraw(grid, player, enemies);
            }
        };
        timer.start();
    }

    private void updateCanvasSize() {
        double availableWidth = getWidth();
        double availableHeight = getHeight();
        if (availableWidth <= 0.0 || availableHeight <= 0.0) {
            return;
        }

        canvas.setWidth(availableWidth);
        canvas.setHeight(availableHeight);
        canvas.setTranslateX(0.0);
        canvas.setTranslateY(0.0);
    }

    public void redraw(Grid grid, Player player, List<model.Enemy> enemies) {
        if (canvas.getWidth() <= 0.0 || canvas.getHeight() <= 0.0) {
            updateCanvasSize();
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        playerRenderer.update(player, TILE_SIZE, frameDeltaSeconds);
        double currentZoom = getCurrentZoom();
        updateCamera(playerRenderer.getRenderCenterX(TILE_SIZE), playerRenderer.getRenderCenterY(TILE_SIZE), currentZoom);
        if (mistEnabled) {
            updateMistFocus(player);
        }

        double viewportWorldWidth = getViewportWorldWidth(currentZoom);
        double viewportWorldHeight = getViewportWorldHeight(currentZoom);
        int startCol = (int) Math.floor(cameraX / TILE_SIZE);
        int endCol = (int) Math.ceil((cameraX + viewportWorldWidth) / TILE_SIZE) + 1;
        int startRow = (int) Math.floor(cameraY / TILE_SIZE);
        int endRow = (int) Math.ceil((cameraY + viewportWorldHeight) / TILE_SIZE) + 1;

        gc.save();
        double viewportOffsetX = getViewportOffsetX(currentZoom);
        double viewportOffsetY = getViewportOffsetY(currentZoom);
        gc.setTransform(
                currentZoom, 0.0,
                0.0, currentZoom,
                viewportOffsetX - cameraX * currentZoom,
                viewportOffsetY - cameraY * currentZoom
        );
        gridRenderer.draw(gc, grid, startRow, endRow, startCol, endCol, mistTimeNanos / 1_000_000_000.0);
        drawArtifacts(gc, player, cameraX, cameraY, viewportWorldWidth, viewportWorldHeight);
        if (mistEnabled) {
            drawMist(gc, grid, cameraX, cameraY, viewportWorldWidth, viewportWorldHeight);
        }
        playerRenderer.drawCurrent(gc, player, TILE_SIZE, this.difficulty);

        if (enemies != null) {
            for (model.Enemy enemy : enemies) {
                if (enemy == null) {
                    continue;
                }

                double centerX = enemy.getCol() * TILE_SIZE + TILE_SIZE / 2.0;
                double centerY = enemy.getRow() * TILE_SIZE + TILE_SIZE / 2.0;
                if (!isInViewport(centerX, centerY, cameraX, cameraY, viewportWorldWidth, viewportWorldHeight, TILE_SIZE)) {
                    continue;
                }

                double visibility = getWorldVisibility(
                        player,
                        centerX,
                        centerY
                );
                if (visibility <= 0.01) {
                    continue;
                }

                gc.save();
                gc.setGlobalAlpha(visibility);
                enemyRenderer.draw(gc, enemy, TILE_SIZE, mistTimeNanos, this.difficulty, frameDeltaSeconds, visibility);
                gc.restore();
            }
        }
        gc.restore();
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

    public double getGameVolume() {
        return gameVolume;
    }

    public void setGameVolume(double gameVolume) {
        this.gameVolume = clamp(gameVolume, 0.0, 1.0);
    }

    private void drawMist(GraphicsContext gc, Grid grid, double viewX, double viewY,
                          double viewportWorldWidth, double viewportWorldHeight) {
        MistProfile profile = getMistProfile();
        double clearRadius = getCurrentClearRadius();
        double timeSeconds = (mistTimeNanos / 1_000_000_000.0) * mistAnimationTimeScale;
        double width = grid.getWidth() * TILE_SIZE;
        double height = grid.getHeight() * TILE_SIZE;
        int startX = alignToSampleStep((int) Math.floor(viewX) - (int) MIST_OUTSIDE_FADE_BAND - mistSampleStep);
        int endX = (int) Math.ceil(viewX + viewportWorldWidth) + (int) MIST_OUTSIDE_FADE_BAND + mistSampleStep;
        int startY = alignToSampleStep((int) Math.floor(viewY) - (int) MIST_OUTSIDE_FADE_BAND - mistSampleStep);
        int endY = (int) Math.ceil(viewY + viewportWorldHeight) + (int) MIST_OUTSIDE_FADE_BAND + mistSampleStep;

        for (int y = startY; y < endY; y += mistSampleStep) {
            for (int x = startX; x < endX; x += mistSampleStep) {
                double sampleX = x + mistSampleStep * 0.5;
                double sampleY = y + mistSampleStep * 0.5;
                double dx = sampleX - mistFocusX;
                double dy = sampleY - mistFocusY;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double signedNearestWorldEdgeDistance = Math.min(
                        Math.min(sampleX, width - sampleX),
                        Math.min(sampleY, height - sampleY)
                );

                double distanceOpacity = clearRadius <= 0.0
                        ? 1.0
                        : smoothStep(clearRadius - MIST_EDGE_FADE_BAND, clearRadius + MIST_EDGE_FADE_BAND, dist);
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
                double edgeNoise = ((drift1 * 0.28) + (drift2 * 0.24) + (drift3 * 0.28) + (drift4 * 0.20) + 1.0) * 0.5;
                double outsideDistance = Math.max(0.0, -signedNearestWorldEdgeDistance);
                double edgeOffset = lerp(-MIST_OUTSIDE_FADE_BAND * 0.35, MIST_OUTSIDE_FADE_BAND * 0.35, edgeNoise);
                double adjustedOutsideDistance = Math.max(0.0, outsideDistance + edgeOffset);
                double worldEdgeFade = 1.0 - smoothStep(0.0, MIST_OUTSIDE_FADE_BAND, adjustedOutsideDistance);
                double holeThreshold = lerp(0.72, 0.08, mistDensity);
                double densityMask = smoothStep(holeThreshold - 0.12, holeThreshold + 0.12, holeNoise);
                double accentMask = smoothStep(0.55, 0.95, ((drift2 * 0.55) + (drift4 * 0.45) + 1.0) * 0.5)
                        * profile.accentStrength();

                double alpha = clamp(distanceOpacity * worldEdgeFade * pulse * densityMask * (profile.baseAlpha() + swirl), 0.0,
                        MIST_ALPHA_CAP);
                if (alpha > 0.01) {
                    double colorR = lerp(profile.colorR(), profile.accentR(), accentMask);
                    double colorG = lerp(profile.colorG(), profile.accentG(), accentMask);
                    double colorB = lerp(profile.colorB(), profile.accentB(), accentMask);
                    gc.setFill(Color.color(colorR, colorG, colorB, alpha));
                    gc.fillRect(x, y, mistSampleStep, mistSampleStep);
                }
            }
        }
    }

    private void drawArtifacts(GraphicsContext gc, Player player, double viewX, double viewY,
                               double viewportWorldWidth, double viewportWorldHeight) {
        for (Artifact artifact : artifacts) {
            if (artifact == null || artifact.isCollected() || artifact.getPosition() == null) {
                continue;
            }

            double centerX = artifact.getPosition().getCol() * TILE_SIZE + TILE_SIZE / 2.0;
            double centerY = artifact.getPosition().getRow() * TILE_SIZE + TILE_SIZE / 2.0;
            if (!isInViewport(centerX, centerY, viewX, viewY, viewportWorldWidth, viewportWorldHeight, TILE_SIZE)) {
                continue;
            }

            double visibility = getWorldVisibility(player, centerX, centerY);
            if (visibility <= 0.01) {
                continue;
            }

            double timeSeconds = mistTimeNanos / 1_000_000_000.0;
            double phase = timeSeconds * 1.8
                    + artifact.getType().ordinal() * 0.78
                    + artifact.getPosition().getRow() * 0.29
                    + artifact.getPosition().getCol() * 0.17;
            ArtifactVisuals.drawArtifact(gc, artifact.getType(), centerX, centerY, TILE_SIZE, phase, visibility);
        }
    }

    private double getWorldVisibility(Player player, double targetCenterX, double targetCenterY) {
        if (!mistEnabled) {
            return 1.0;
        }

        double playerCenterX = (player.getCol() + 0.5) * TILE_SIZE;
        double playerCenterY = (player.getRow() + 0.5) * TILE_SIZE;
        double focusX = mistFocusInitialized ? mistFocusX : playerCenterX;
        double focusY = mistFocusInitialized ? mistFocusY : playerCenterY;
        double dx = targetCenterX - focusX;
        double dy = targetCenterY - focusY;
        double visibleRadius = getCurrentClearRadius();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (visibleRadius <= 0.0) {
            return 0.0;
        }

        return 1.0 - smoothStep(
                visibleRadius - MIST_EDGE_FADE_BAND,
                visibleRadius + MIST_EDGE_FADE_BAND,
                distance
        );
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

    private void updateCamera(double targetCenterX, double targetCenterY, double zoom) {
        double viewportWorldWidth = getViewportWorldWidth(zoom);
        double viewportWorldHeight = getViewportWorldHeight(zoom);
        double targetCameraX = clamp(targetCenterX - viewportWorldWidth / 2.0, 0.0, Math.max(0.0, baseWidth - viewportWorldWidth));
        double targetCameraY = clamp(targetCenterY - viewportWorldHeight / 2.0, 0.0, Math.max(0.0, baseHeight - viewportWorldHeight));

        if (!cameraInitialized) {
            cameraX = targetCameraX;
            cameraY = targetCameraY;
            cameraInitialized = true;
            return;
        }

        double smoothingSeconds = lerp(INTRO_CAMERA_SMOOTHING_SECONDS, CAMERA_SMOOTHING_SECONDS, getIntroProgress());
        double smoothingFactor = 1.0 - Math.exp(-frameDeltaSeconds / smoothingSeconds);
        cameraX += (targetCameraX - cameraX) * smoothingFactor;
        cameraY += (targetCameraY - cameraY) * smoothingFactor;
    }

    private double getViewportWorldWidth(double zoom) {
        return canvas.getWidth() / zoom;
    }

    private double getViewportWorldHeight(double zoom) {
        return canvas.getHeight() / zoom;
    }

    private double getViewportOffsetX(double zoom) {
        return Math.max(0.0, (canvas.getWidth() - baseWidth * zoom) / 2.0);
    }

    private double getViewportOffsetY(double zoom) {
        return Math.max(0.0, (canvas.getHeight() - baseHeight * zoom) / 2.0);
    }

    private double getCurrentZoom() {
        double introProgress = getZoomIntroProgress();
        double introZoom = getIntroZoom();
        return lerp(introZoom, VIEWPORT_ZOOM, introProgress);
    }

    private double getIntroZoom() {
        if (canvas.getWidth() <= 0.0 || canvas.getHeight() <= 0.0) {
            return VIEWPORT_ZOOM;
        }

        double fitZoom = Math.min(canvas.getWidth() / baseWidth, canvas.getHeight() / baseHeight);
        return Math.min(VIEWPORT_ZOOM, fitZoom);
    }

    private double getCurrentClearRadius() {
        double revealProgress = smoothStep(0.30, 1.0, getIntroProgress());
        return getMistRadiusCells() * TILE_SIZE * revealProgress;
    }

    private int getMistRadiusCells() {
        return switch (difficulty) {
            case EASY -> EASY_MIST_RADIUS_CELLS;
            case MEDIUM -> MEDIUM_MIST_RADIUS_CELLS;
            case HARD -> HARD_MIST_RADIUS_CELLS;
        };
    }

    private double getIntroProgress() {
        if (LEVEL_INTRO_SECONDS <= 0.0) {
            return 1.0;
        }
        double progress = clamp(introElapsedSeconds / LEVEL_INTRO_SECONDS, 0.0, 1.0);
        return smoothStep(0.0, 1.0, progress);
    }

    private double getZoomIntroProgress() {
        double progress = clamp((getIntroProgress() - 0.10) / 0.90, 0.0, 1.0);
        return easeInOutCubic(progress);
    }

    private double easeInOutCubic(double t) {
        double clamped = clamp(t, 0.0, 1.0);
        if (clamped < 0.5) {
            return 4.0 * clamped * clamped * clamped;
        }
        return 1.0 - Math.pow(-2.0 * clamped + 2.0, 3.0) / 2.0;
    }

    private int alignToSampleStep(int coordinate) {
        return coordinate - Math.floorMod(coordinate, mistSampleStep);
    }

    private boolean isInViewport(double x, double y, double viewX, double viewY,
                                 double viewportWorldWidth, double viewportWorldHeight, double margin) {
        return x >= viewX - margin
                && x <= viewX + viewportWorldWidth + margin
                && y >= viewY - margin
                && y <= viewY + viewportWorldHeight + margin;
    }
}
