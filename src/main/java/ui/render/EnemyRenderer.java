package ui.render;

import enums.Difficulty;
import enums.EnemyMode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import model.Enemy;

public class EnemyRenderer {
    private static final Color CHASE_BODY = Color.web("#5A2E38");
    private static final Color CHASE_BODY_DARK = Color.web("#31141C");
    private static final Color CHASE_EDGE = Color.web("#F08996");
    private static final Color CHASE_GLOW = Color.rgb(255, 91, 123, 0.30);
    private static final Color CHASE_SENSOR = Color.web("#FFB1A1");
    private static final Color PATROL_SENSOR = Color.web("#9BE7FF");
    private static final Color PATROL_MARK = Color.web("#D8F6FF");

    public void draw(GraphicsContext gc, Enemy enemy, double tileSize, long nowNanos, Difficulty difficulty) {
        if (enemy == null) {
            return;
        }

        EnemyMode mode = enemy.getMode() != null ? enemy.getMode() : EnemyMode.PATROL;
        DronePalette palette = paletteFor(mode, difficulty);
        double x = enemy.getCol() * tileSize;
        double y = enemy.getRow() * tileSize;
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
            gc.setStroke(PATROL_MARK);
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

    private double enemyPhase(Enemy enemy) {
        return (enemy.getRow() * 0.73) + (enemy.getCol() * 1.17);
    }

    private record DronePalette(Color body, Color bodyDark, Color edge, Color glow, Color sensor) {
    }
}
