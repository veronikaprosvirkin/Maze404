package ui.render;

import enums.Difficulty;
import enums.EnemyMode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import model.Enemy;

import java.util.IdentityHashMap;
import java.util.Map;

public class EnemyRenderer {
    private static final double ENEMY_TICK_SECONDS = 0.2;
    private static final double CONTINUOUS_FLIGHT_FACTOR = 1.08;
    private static final long PATROL_TURNAROUND_PAUSE_NANOS = 1_000_000_000L;
    private static final Color CHASE_BODY = Color.web("#5A2E38");
    private static final Color CHASE_BODY_DARK = Color.web("#31141C");
    private static final Color CHASE_EDGE = Color.web("#F08996");
    private static final Color CHASE_GLOW = Color.rgb(255, 91, 123, 0.30);
    private static final Color CHASE_SENSOR = Color.web("#FFB1A1");
    private static final Color PATROL_SENSOR = Color.web("#9BE7FF");
    private static final Color PATROL_MARK = Color.web("#D8F6FF");
    private final Map<Enemy, RenderState> renderStates = new IdentityHashMap<>();

    public void draw(GraphicsContext gc, Enemy enemy, double tileSize, long nowNanos, Difficulty difficulty, double deltaSeconds,
                     double visibility) {
        if (enemy == null) {
            return;
        }

        EnemyMode mode = enemy.getMode() != null ? enemy.getMode() : EnemyMode.PATROL;
        DronePalette palette = paletteFor(mode, difficulty);
        RenderState state = renderStates.computeIfAbsent(enemy, key ->
                new RenderState(enemy.getCol() * tileSize, enemy.getRow() * tileSize));
        updateRenderState(state, enemy, tileSize, nowNanos, mode, difficulty);
        double x = state.renderX();
        double y = state.renderY();
        double centerX = x + tileSize / 2.0;
        double centerY = y + tileSize / 2.0;
        double time = nowNanos / 1_000_000_000.0;
        double bob = Math.sin(time * (mode == EnemyMode.CHASE ? 5.8 : 4.1) + enemyPhase(enemy)) * tileSize * 0.045;
        double rotorPhase = Math.sin(time * (mode == EnemyMode.CHASE ? 14.0 : 11.0) + enemyPhase(enemy) * 0.7);
        double bodyWidth = tileSize * 0.58;
        double bodyHeight = tileSize * 0.42;
        double bodyX = centerX - bodyWidth / 2.0;
        double bodyY = centerY - bodyHeight / 2.0 + bob;

        gc.save();
        gc.setEffect(new DropShadow(tileSize * 0.18, palette.glow()));
        gc.setFill(palette.glow());
        gc.fillOval(centerX - tileSize * 0.48, centerY - tileSize * 0.43 + bob, tileSize * 0.96, tileSize * 0.86);
        gc.restore();

        gc.save();
        gc.setGlobalAlpha(0.32);
        gc.setFill(Color.color(0.02, 0.03, 0.05, 0.65));
        gc.fillOval(centerX - tileSize * 0.34, y + tileSize * 0.70, tileSize * 0.68, tileSize * 0.16);
        gc.restore();

        drawRotor(gc, centerX - tileSize * 0.24, centerY - tileSize * 0.12 + bob, tileSize, rotorPhase, palette, false);
        drawRotor(gc, centerX + tileSize * 0.24, centerY - tileSize * 0.12 + bob, tileSize, rotorPhase, palette, true);

        gc.setStroke(palette.edge());
        gc.setLineWidth(Math.max(1.2, tileSize * 0.06));
        gc.strokeLine(centerX - tileSize * 0.15, bodyY + bodyHeight * 0.18, centerX - tileSize * 0.23, centerY - tileSize * 0.08 + bob);
        gc.strokeLine(centerX + tileSize * 0.15, bodyY + bodyHeight * 0.18, centerX + tileSize * 0.23, centerY - tileSize * 0.08 + bob);

        gc.setFill(palette.bodyDark());
        gc.fillRoundRect(bodyX, bodyY + tileSize * 0.05, bodyWidth, bodyHeight, tileSize * 0.18, tileSize * 0.18);
        gc.setFill(palette.body());
        gc.fillRoundRect(bodyX, bodyY, bodyWidth, bodyHeight, tileSize * 0.20, tileSize * 0.20);

        gc.setStroke(palette.edge());
        gc.setLineWidth(Math.max(1.4, tileSize * 0.07));
        gc.strokeRoundRect(bodyX, bodyY, bodyWidth, bodyHeight, tileSize * 0.20, tileSize * 0.20);

        gc.setFill(Color.color(1, 1, 1, 0.16));
        gc.fillRoundRect(bodyX + tileSize * 0.05, bodyY + tileSize * 0.05, bodyWidth * 0.55, bodyHeight * 0.22,
                tileSize * 0.12, tileSize * 0.12);

        if (mode == EnemyMode.PATROL) {
            double markAlpha = Math.max(0.0, Math.min(1.0, visibility * visibility));
            gc.setStroke(Color.color(
                    PATROL_MARK.getRed(),
                    PATROL_MARK.getGreen(),
                    PATROL_MARK.getBlue(),
                    markAlpha
            ));
            gc.setLineWidth(Math.max(1.3, tileSize * 0.055));
            gc.strokeLine(bodyX + tileSize * 0.10, bodyY + bodyHeight * 0.68, bodyX + bodyWidth - tileSize * 0.10, bodyY + bodyHeight * 0.68);
            gc.strokeLine(bodyX + tileSize * 0.13, bodyY + bodyHeight * 0.82, bodyX + bodyWidth - tileSize * 0.13, bodyY + bodyHeight * 0.82);
        }

        gc.save();
        gc.setEffect(new DropShadow(tileSize * 0.10, palette.sensor()));
        gc.setFill(palette.sensor());
        gc.fillOval(centerX - tileSize * 0.10, centerY - tileSize * 0.05 + bob, tileSize * 0.20, tileSize * 0.14);
        gc.restore();

        gc.setFill(Color.color(0.97, 0.99, 1.0, 0.82));
        gc.fillOval(centerX - tileSize * 0.04, centerY - tileSize * 0.03 + bob, tileSize * 0.05, tileSize * 0.05);

        double skidY = bodyY + bodyHeight + tileSize * 0.05;
        gc.setStroke(palette.edge());
        gc.setLineWidth(Math.max(1.2, tileSize * 0.05));
        gc.strokeArc(centerX - tileSize * 0.23, skidY - tileSize * 0.03, tileSize * 0.22, tileSize * 0.12, 200, 140, ArcType.OPEN);
        gc.strokeArc(centerX + tileSize * 0.01, skidY - tileSize * 0.03, tileSize * 0.22, tileSize * 0.12, 200, 140, ArcType.OPEN);
    }

    private void drawRotor(GraphicsContext gc, double centerX, double centerY, double tileSize, double rotorPhase,
                           DronePalette palette, boolean mirrored) {
        double tilt = mirrored ? -1.0 : 1.0;
        double armWidth = tileSize * 0.22;
        double armHeight = tileSize * 0.06;
        double rotorWidth = tileSize * (0.26 + (rotorPhase + 1.0) * 0.025);
        double rotorHeight = tileSize * 0.07;

        gc.setFill(palette.bodyDark());
        gc.fillRoundRect(centerX - armWidth / 2.0, centerY - armHeight / 2.0, armWidth, armHeight,
                tileSize * 0.05, tileSize * 0.05);

        gc.save();
        gc.setGlobalAlpha(0.55);
        gc.setStroke(Color.color(palette.glow().getRed(), palette.glow().getGreen(), palette.glow().getBlue(), 0.55));
        gc.setLineWidth(Math.max(1.4, tileSize * 0.05));
        gc.strokeLine(centerX - rotorWidth * 0.5, centerY - rotorHeight * tilt, centerX + rotorWidth * 0.5, centerY + rotorHeight * tilt);
        gc.strokeLine(centerX - rotorWidth * 0.5, centerY + rotorHeight * tilt, centerX + rotorWidth * 0.5, centerY - rotorHeight * tilt);
        gc.restore();

        gc.setFill(palette.edge());
        gc.fillOval(centerX - tileSize * 0.05, centerY - tileSize * 0.05, tileSize * 0.10, tileSize * 0.10);
    }

    private DronePalette paletteFor(EnemyMode mode, Difficulty difficulty) {
        if (mode == EnemyMode.CHASE) {
            return new DronePalette(CHASE_BODY, CHASE_BODY_DARK, CHASE_EDGE, CHASE_GLOW, CHASE_SENSOR);
        }
        return patrolPaletteFor(difficulty != null ? difficulty : Difficulty.EASY);
    }

    private DronePalette patrolPaletteFor(Difficulty difficulty) {
        return switch (difficulty) {
            case MEDIUM -> new DronePalette(
                    Color.web("#5D503E"),
                    Color.web("#342B21"),
                    Color.web("#E0C79B"),
                    Color.rgb(224, 199, 155, 0.22),
                    PATROL_SENSOR
            );
            case HARD -> new DronePalette(
                    Color.web("#6A3640"),
                    Color.web("#38161E"),
                    Color.web("#FF9A86"),
                    Color.rgb(255, 116, 91, 0.24),
                    PATROL_SENSOR
            );
            default -> new DronePalette(
                    Color.web("#3B4E68"),
                    Color.web("#243245"),
                    Color.web("#90B8E8"),
                    Color.rgb(117, 177, 255, 0.28),
                    PATROL_SENSOR
            );
        };
    }

    private void updateRenderState(RenderState state, Enemy enemy, double tileSize, long nowNanos,
                                   EnemyMode mode, Difficulty difficulty) {
        double targetX = enemy.getCol() * tileSize;
        double targetY = enemy.getRow() * tileSize;
        state.updateInterpolatedPosition(nowNanos);
        double distance = Math.abs(targetX - state.renderX()) + Math.abs(targetY - state.renderY());

        if (distance > tileSize * 2.5) {
            state.snapTo(targetX, targetY, nowNanos);
            return;
        }

        if (!state.isTrackingTarget(targetX, targetY)) {
            long durationNanos = moveDurationNanos(mode, difficulty);
            if (shouldPauseBeforeSegment(state, targetX, targetY, mode)) {
                state.scheduleTurnaround(targetX, targetY, nowNanos, durationNanos, PATROL_TURNAROUND_PAUSE_NANOS);
            } else {
                state.beginSegment(targetX, targetY, nowNanos, durationNanos, 0L, SegmentMotion.LINEAR);
            }
        } else {
            state.updateInterpolatedPosition(nowNanos);
        }
    }

    private long moveDurationNanos(EnemyMode mode, Difficulty difficulty) {
        int requiredTicks = 4;
        if (mode == EnemyMode.PATROL) {
            requiredTicks = 5;
        } else if (mode == EnemyMode.CHASE) {
            Difficulty effectiveDifficulty = difficulty != null ? difficulty : Difficulty.current;
            requiredTicks = switch (effectiveDifficulty != null ? effectiveDifficulty : Difficulty.EASY) {
                case EASY -> 4;
                case MEDIUM -> 3;
                case HARD -> 2;
            };
        }
        double seconds = requiredTicks * ENEMY_TICK_SECONDS * CONTINUOUS_FLIGHT_FACTOR;
        return (long) (seconds * 1_000_000_000L);
    }

    private boolean shouldPauseBeforeSegment(RenderState state, double nextTargetX, double nextTargetY, EnemyMode mode) {
        if (mode != EnemyMode.PATROL || !state.hasDirectionHistory()) {
            return false;
        }

        double currentDx = nextTargetX - state.targetX();
        double currentDy = nextTargetY - state.targetY();
        double previousDx = state.targetX() - state.startX();
        double previousDy = state.targetY() - state.startY();
        double currentLength = Math.hypot(currentDx, currentDy);
        double previousLength = Math.hypot(previousDx, previousDy);

        if (currentLength < 0.001 || previousLength < 0.001) {
            return false;
        }

        double dot = ((previousDx / previousLength) * (currentDx / currentLength))
                + ((previousDy / previousLength) * (currentDy / currentLength));
        return dot < -0.85;
    }

    private double enemyPhase(Enemy enemy) {
        return (enemy.getRow() * 0.73) + (enemy.getCol() * 1.17);
    }

    private record DronePalette(Color body, Color bodyDark, Color edge, Color glow, Color sensor) {
    }

    private enum SegmentMotion {
        LINEAR,
        EASE_IN,
        EASE_OUT
    }

    private static final class RenderState {
        private double renderX;
        private double renderY;
        private double startX;
        private double startY;
        private double targetX;
        private double targetY;
        private long segmentStartNanos;
        private long segmentDurationNanos;
        private long pauseUntilNanos;
        private SegmentMotion segmentMotion;
        private boolean queuedSegment;
        private double queuedTargetX;
        private double queuedTargetY;
        private long queuedDurationNanos;
        private SegmentMotion queuedMotion;

        private RenderState(double renderX, double renderY) {
            this.renderX = renderX;
            this.renderY = renderY;
            this.startX = renderX;
            this.startY = renderY;
            this.targetX = renderX;
            this.targetY = renderY;
            this.segmentStartNanos = 0L;
            this.segmentDurationNanos = 1L;
            this.pauseUntilNanos = 0L;
            this.segmentMotion = SegmentMotion.LINEAR;
            this.queuedSegment = false;
            this.queuedTargetX = renderX;
            this.queuedTargetY = renderY;
            this.queuedDurationNanos = 1L;
            this.queuedMotion = SegmentMotion.LINEAR;
        }

        private double renderX() {
            return renderX;
        }

        private void renderX(double renderX) {
            this.renderX = renderX;
        }

        private double renderY() {
            return renderY;
        }

        private void renderY(double renderY) {
            this.renderY = renderY;
        }

        private boolean isTrackingTarget(double nextTargetX, double nextTargetY) {
            boolean currentTargetMatches = Math.abs(targetX - nextTargetX) <= 0.001
                    && Math.abs(targetY - nextTargetY) <= 0.001;
            boolean queuedTargetMatches = queuedSegment
                    && Math.abs(queuedTargetX - nextTargetX) <= 0.001
                    && Math.abs(queuedTargetY - nextTargetY) <= 0.001;
            return currentTargetMatches || queuedTargetMatches;
        }

        private void beginSegment(double nextTargetX, double nextTargetY, long nowNanos, long durationNanos,
                                  long pauseNanos, SegmentMotion motion) {
            updateInterpolatedPosition(nowNanos);
            this.startX = this.renderX;
            this.startY = this.renderY;
            this.targetX = nextTargetX;
            this.targetY = nextTargetY;
            this.pauseUntilNanos = nowNanos + Math.max(0L, pauseNanos);
            this.segmentStartNanos = this.pauseUntilNanos;
            this.segmentDurationNanos = Math.max(1L, durationNanos);
            this.segmentMotion = motion;
            this.queuedSegment = false;
        }

        private void updateInterpolatedPosition(long nowNanos) {
            if (queuedSegment) {
                long segmentEndNanos = segmentStartNanos + segmentDurationNanos;
                if (nowNanos < segmentEndNanos) {
                    double progress = (nowNanos - segmentStartNanos) / (double) segmentDurationNanos;
                    double clampedProgress = Math.max(0.0, Math.min(1.0, progress));
                    double easedProgress = applyMotion(clampedProgress, segmentMotion);
                    renderX = startX + (targetX - startX) * easedProgress;
                    renderY = startY + (targetY - startY) * easedProgress;
                    return;
                }

                renderX = targetX;
                renderY = targetY;
                if (nowNanos < pauseUntilNanos) {
                    return;
                }

                if (nowNanos >= segmentEndNanos) {
                    renderX = targetX;
                    renderY = targetY;
                    startX = targetX;
                    startY = targetY;
                    targetX = queuedTargetX;
                    targetY = queuedTargetY;
                    segmentStartNanos = pauseUntilNanos;
                    segmentDurationNanos = Math.max(1L, queuedDurationNanos);
                    segmentMotion = queuedMotion;
                    pauseUntilNanos = segmentStartNanos;
                    queuedSegment = false;
                }
            }

            if (nowNanos < pauseUntilNanos) {
                renderX = startX;
                renderY = startY;
                return;
            }

            if (segmentDurationNanos <= 0L) {
                renderX = targetX;
                renderY = targetY;
                return;
            }

            double progress = (nowNanos - segmentStartNanos) / (double) segmentDurationNanos;
            double clampedProgress = Math.max(0.0, Math.min(1.0, progress));
            double easedProgress = applyMotion(clampedProgress, segmentMotion);
            renderX = startX + (targetX - startX) * easedProgress;
            renderY = startY + (targetY - startY) * easedProgress;
        }

        private void scheduleTurnaround(double nextTargetX, double nextTargetY, long nowNanos,
                                        long durationNanos, long pauseNanos) {
            updateInterpolatedPosition(nowNanos);
            this.segmentMotion = SegmentMotion.EASE_OUT;
            this.pauseUntilNanos = this.segmentStartNanos + this.segmentDurationNanos + Math.max(0L, pauseNanos);
            this.queuedTargetX = nextTargetX;
            this.queuedTargetY = nextTargetY;
            this.queuedDurationNanos = Math.max(1L, durationNanos);
            this.queuedMotion = SegmentMotion.EASE_IN;
            this.queuedSegment = true;
        }

        private void snapTo(double nextTargetX, double nextTargetY, long nowNanos) {
            this.renderX = nextTargetX;
            this.renderY = nextTargetY;
            this.startX = nextTargetX;
            this.startY = nextTargetY;
            this.targetX = nextTargetX;
            this.targetY = nextTargetY;
            this.segmentStartNanos = nowNanos;
            this.segmentDurationNanos = 1L;
            this.pauseUntilNanos = nowNanos;
            this.segmentMotion = SegmentMotion.LINEAR;
            this.queuedSegment = false;
        }

        private boolean hasDirectionHistory() {
            return Math.abs(targetX - startX) > 0.001 || Math.abs(targetY - startY) > 0.001;
        }

        private double startX() {
            return startX;
        }

        private double startY() {
            return startY;
        }

        private double targetX() {
            return targetX;
        }

        private double targetY() {
            return targetY;
        }

        private double applyMotion(double progress, SegmentMotion motion) {
            return switch (motion) {
                case EASE_IN -> progress * progress;
                case EASE_OUT -> 1.0 - Math.pow(1.0 - progress, 2.0);
                case LINEAR -> progress;
            };
        }
    }
}
