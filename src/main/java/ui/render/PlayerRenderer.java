package ui.render;

import enums.Difficulty;
import enums.PlayerSkin;
import events.EventBus;
import events.GameEvent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import model.Player;

public class PlayerRenderer {
    private static final double MOVEMENT_SMOOTHING_SECONDS = 0.18;
    private static final double SEMI_INVISIBLE_TRANSITION_SECONDS = 0.35;
    private static final double SEMI_INVISIBLE_OPACITY = 0.45;
    private static final long DAMAGE_FLASH_DURATION_NANOS = 1_000_000_000L;
    private static final Color MENU_SKIN_BASE = Color.web("#22103A");
    private static final Color MENU_SKIN_GLOW = Color.web("#8F55FF");
    private static final Color MENU_SKIN_BORDER = Color.web("#C9A7FF");
    private static final Color MENU_SKIN_HIGHLIGHT = Color.web("#F1E8FF");

    private double renderX;
    private double renderY;
    private double visibilityOpacity = 1.0;
    private boolean initialized = false;
    private long damageFlashUntilNanos = 0L;

    public PlayerRenderer() {
        EventBus.getInstance().subscribe(GameEvent.Type.PLAYER_DAMAGED, event ->
                damageFlashUntilNanos = System.nanoTime() + DAMAGE_FLASH_DURATION_NANOS
        );
    }

    public void draw(GraphicsContext gc, Player player, double tileSize) {
        draw(gc, player, tileSize, Difficulty.EASY);
    }

    public void draw(GraphicsContext gc, Player player, double tileSize, Difficulty diff) {
        update(player, tileSize, 1.0 / 60.0);
        drawCurrent(gc, player, tileSize, diff);
    }

    public void update(Player player, double tileSize, double deltaSeconds) {
        double targetX = player.getCol() * tileSize;
        double targetY = player.getRow() * tileSize;
        double targetOpacity = player.isSemiInvisible() ? SEMI_INVISIBLE_OPACITY : 1.0;

        if (!initialized) {
            renderX = targetX;
            renderY = targetY;
            visibilityOpacity = targetOpacity;
            initialized = true;
        } else {
            double smoothingFactor = 1.0 - Math.exp(-deltaSeconds / MOVEMENT_SMOOTHING_SECONDS);
            renderX += (targetX - renderX) * smoothingFactor;
            renderY += (targetY - renderY) * smoothingFactor;

            double opacitySmoothingFactor = 1.0 - Math.exp(-deltaSeconds / SEMI_INVISIBLE_TRANSITION_SECONDS);
            visibilityOpacity += (targetOpacity - visibilityOpacity) * opacitySmoothingFactor;
        }
    }

    public void drawCurrent(GraphicsContext gc, Player player, double tileSize, Difficulty diff) {
        if (diff == null) {
            diff = Difficulty.EASY;
        }

        double centerX = renderX + tileSize / 2.0;
        double centerY = renderY + tileSize / 2.0;
        double radius = tileSize * 0.4;
        gc.save();
        gc.setGlobalAlpha(visibilityOpacity);
        if (player.hasShield()) {
            drawShieldRing(gc, diff, centerX, centerY, radius);
        }
        drawSkin(gc, player.getSkin(), getCurrentPalette(diff), centerX, centerY, radius);
        gc.restore();
    }

    public double getRenderCenterX(double tileSize) {
        return renderX + tileSize / 2.0;
    }

    public double getRenderCenterY(double tileSize) {
        return renderY + tileSize / 2.0;
    }

    public static void drawPreview(GraphicsContext gc, PlayerSkin skin, Difficulty diff, double centerX, double centerY,
                                   double radius, double opacity) {
        drawSkin(gc, skin, diff, centerX, centerY, radius, opacity);
    }

    public static void drawMenuPreview(GraphicsContext gc, PlayerSkin skin, double centerX, double centerY,
                                       double radius, double opacity) {
        drawSkin(gc, skin, menuPalette(opacity), centerX, centerY, radius);
    }

    private static void drawSkin(GraphicsContext gc, PlayerSkin skin, Difficulty diff, double centerX, double centerY,
                                 double radius, double opacity) {
        drawSkin(gc, skin, paletteFor(diff != null ? diff : Difficulty.EASY, opacity), centerX, centerY, radius);
    }

    private PlayerPalette getCurrentPalette(Difficulty diff) {
        PlayerPalette basePalette = paletteFor(diff != null ? diff : Difficulty.EASY, 1.0);
        long now = System.nanoTime();
        if (now < damageFlashUntilNanos) {
            double elapsed = 1.0 - (double) (damageFlashUntilNanos - now) / DAMAGE_FLASH_DURATION_NANOS;
            double blend = Math.sin(Math.PI * clamp(elapsed, 0.0, 1.0));
            return blendPalettes(basePalette, damagePalette(1.0), blend);
        }
        return basePalette;
    }

    private static void drawSkin(GraphicsContext gc, PlayerSkin skin, PlayerPalette palette, double centerX,
                                 double centerY, double radius) {
        PlayerSkin effectiveSkin = skin != null ? skin : PlayerSkin.CIRCLE;

        drawOuterAura(gc, effectiveSkin, palette.outerAura(), centerX, centerY, radius);
        drawInnerGlow(gc, effectiveSkin, palette.innerGlow(), centerX, centerY, radius);

        drawBody(gc, effectiveSkin, palette.base(), centerX, centerY, radius);
        gc.setLineWidth(1.5);
        drawBorder(gc, effectiveSkin, palette.border(), centerX, centerY, radius);

        drawHighlight(gc, effectiveSkin, palette.highlight(), centerX, centerY, radius);
    }

    private static void drawOuterAura(GraphicsContext gc, PlayerSkin skin, Color fill, double centerX, double centerY,
                                      double radius) {
        gc.setFill(fill);
        switch (skin) {
            case RECTANGLE -> gc.fillRoundRect(centerX - radius * 1.7, centerY - radius * 1.7,
                    radius * 3.4, radius * 3.4, radius * 0.95, radius * 0.95);
            case TRIANGLE -> drawHexaStarFill(gc, centerX, centerY, radius * 1.62, radius * 0.7);
            default -> gc.fillOval(centerX - radius * 1.8, centerY - radius * 1.8, radius * 3.6, radius * 3.6);
        }
    }

    private static void drawInnerGlow(GraphicsContext gc, PlayerSkin skin, Color fill, double centerX, double centerY,
                                      double radius) {
        gc.setFill(fill);
        switch (skin) {
            case RECTANGLE -> gc.fillRoundRect(centerX - radius * 1.1, centerY - radius * 1.1,
                    radius * 2.2, radius * 2.2, radius * 0.72, radius * 0.72);
            case TRIANGLE -> drawHexaStarFill(gc, centerX, centerY, radius * 1.28, radius * 0.56);
            default -> gc.fillOval(centerX - radius * 1.3, centerY - radius * 1.3, radius * 2.6, radius * 2.6);
        }
    }

    private static void drawBody(GraphicsContext gc, PlayerSkin skin, Color fill, double centerX, double centerY,
                                 double radius) {
        gc.setFill(fill);
        switch (skin) {
            case RECTANGLE -> gc.fillRoundRect(centerX - radius * 1.02, centerY - radius * 0.88,
                    radius * 2.04, radius * 1.76, radius * 0.56, radius * 0.56);
            case TRIANGLE -> drawHexaStarFill(gc, centerX, centerY, radius, radius * 0.44);
            case DEMON -> {
                gc.fillPolygon(
                        new double[]{centerX - radius * 0.7, centerX - radius * 0.28, centerX - radius * 0.08},
                        new double[]{centerY - radius * 0.36, centerY - radius * 1.2, centerY - radius * 0.44},
                        3
                );
                gc.fillPolygon(
                        new double[]{centerX + radius * 0.7, centerX + radius * 0.28, centerX + radius * 0.08},
                        new double[]{centerY - radius * 0.36, centerY - radius * 1.2, centerY - radius * 0.44},
                        3
                );
                gc.fillOval(centerX - radius, centerY - radius * 0.82, radius * 2, radius * 1.82);
            }
            case DONUT -> {
                gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                gc.setFill(Color.rgb(17, 7, 29, 0.96));
                gc.fillOval(centerX - radius * 0.38, centerY - radius * 0.38, radius * 0.76, radius * 0.76);
            }
            default -> gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        }
    }

    private static void drawBorder(GraphicsContext gc, PlayerSkin skin, Color stroke, double centerX, double centerY,
                                   double radius) {
        gc.setStroke(stroke);
        switch (skin) {
            case RECTANGLE -> gc.strokeRoundRect(centerX - radius * 1.02, centerY - radius * 0.88,
                    radius * 2.04, radius * 1.76, radius * 0.56, radius * 0.56);
            case TRIANGLE -> drawHexaStarStroke(gc, centerX, centerY, radius, radius * 0.44);
            case DEMON -> {
                gc.strokePolygon(
                        new double[]{centerX - radius * 0.7, centerX - radius * 0.28, centerX - radius * 0.08},
                        new double[]{centerY - radius * 0.36, centerY - radius * 1.2, centerY - radius * 0.44},
                        3
                );
                gc.strokePolygon(
                        new double[]{centerX + radius * 0.7, centerX + radius * 0.28, centerX + radius * 0.08},
                        new double[]{centerY - radius * 0.36, centerY - radius * 1.2, centerY - radius * 0.44},
                        3
                );
                gc.strokeOval(centerX - radius, centerY - radius * 0.82, radius * 2, radius * 1.82);
            }
            case DONUT -> {
                gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                gc.strokeOval(centerX - radius * 0.38, centerY - radius * 0.38, radius * 0.76, radius * 0.76);
            }
            default -> gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        }
    }

    private static void drawHighlight(GraphicsContext gc, PlayerSkin skin, Color fill, double centerX, double centerY,
                                      double radius) {
        gc.setFill(fill);
        switch (skin) {
            case RECTANGLE -> gc.fillRoundRect(centerX - radius * 0.58, centerY - radius * 0.56,
                    radius * 0.7, radius * 0.42, radius * 0.2, radius * 0.2);
            case TRIANGLE -> drawHexaStarFill(gc, centerX - radius * 0.18, centerY - radius * 0.28,
                    radius * 0.34, radius * 0.15);
            case DEMON -> {
                Color eyeFill = Color.color(
                        Math.min(1.0, fill.getRed() * 1.18),
                        Math.min(1.0, fill.getGreen() * 1.18),
                        Math.min(1.0, fill.getBlue() * 1.18),
                        Math.min(0.92, fill.getOpacity() * 2.8)
                );
                gc.setFill(eyeFill);
                gc.fillOval(centerX - radius * 0.72, centerY - radius * 0.26, radius * 0.46, radius * 0.24);
                gc.fillOval(centerX + radius * 0.26, centerY - radius * 0.26, radius * 0.46, radius * 0.24);
            }
            case DONUT -> gc.fillArc(centerX - radius * 0.56, centerY - radius * 0.62,
                    radius * 0.94, radius * 0.78, 118, 80, javafx.scene.shape.ArcType.ROUND);
            default -> gc.fillOval(centerX - radius * 0.5, centerY - radius * 0.6, radius * 0.6, radius * 0.4);
        }
    }

    private static PlayerPalette paletteFor(Difficulty diff, double opacity) {
        return switch (diff) {
            case MEDIUM -> new PlayerPalette(
                    withOpacity(Color.web("#8FAA6A"), opacity),
                    Color.rgb(143, 170, 106, 0.35 * opacity),
                    Color.rgb(143, 170, 106, 0.2 * opacity),
                    withOpacity(Color.web("#B9D29A"), 0.6 * opacity),
                    Color.rgb(255, 255, 255, 0.25 * opacity)
            );
            case HARD -> new PlayerPalette(
                    withOpacity(Color.web("#E09070"), opacity),
                    Color.rgb(200, 100, 70, 0.45 * opacity),
                    Color.rgb(180, 60, 40, 0.2 * opacity),
                    withOpacity(Color.web("#F2B093"), 0.6 * opacity),
                    Color.rgb(255, 255, 255, 0.25 * opacity)
            );
            default -> new PlayerPalette(
                    withOpacity(Color.web("#9DC8D0"), opacity),
                    Color.rgb(122, 170, 176, 0.4 * opacity),
                    Color.rgb(100, 160, 190, 0.15 * opacity),
                    withOpacity(Color.web("#C6E5EB"), 0.6 * opacity),
                    Color.rgb(255, 255, 255, 0.25 * opacity)
            );
        };
    }

    private static PlayerPalette menuPalette(double opacity) {
        return new PlayerPalette(
                withOpacity(MENU_SKIN_BASE, opacity),
                withOpacity(MENU_SKIN_GLOW, 0.32 * opacity),
                withOpacity(MENU_SKIN_GLOW, 0.18 * opacity),
                withOpacity(MENU_SKIN_BORDER, 0.88 * opacity),
                withOpacity(MENU_SKIN_HIGHLIGHT, 0.28 * opacity)
        );
    }

    private static PlayerPalette damagePalette(double opacity) {
        return new PlayerPalette(
                withOpacity(Color.web("#F04444"), opacity),
                Color.rgb(255, 96, 96, 0.44 * opacity),
                Color.rgb(255, 48, 48, 0.24 * opacity),
                withOpacity(Color.web("#FFD0D0"), 0.9 * opacity),
                Color.rgb(255, 255, 255, 0.34 * opacity)
        );
    }

    private static PlayerPalette blendPalettes(PlayerPalette from, PlayerPalette to, double t) {
        double blend = clamp(t, 0.0, 1.0);
        return new PlayerPalette(
                blendColor(from.base(), to.base(), blend),
                blendColor(from.innerGlow(), to.innerGlow(), blend),
                blendColor(from.outerAura(), to.outerAura(), blend),
                blendColor(from.border(), to.border(), blend),
                blendColor(from.highlight(), to.highlight(), blend)
        );
    }

    private static Color blendColor(Color from, Color to, double t) {
        return Color.color(
                lerp(from.getRed(), to.getRed(), t),
                lerp(from.getGreen(), to.getGreen(), t),
                lerp(from.getBlue(), to.getBlue(), t),
                lerp(from.getOpacity(), to.getOpacity(), t)
        );
    }

    private static void drawShieldRing(GraphicsContext gc, Difficulty diff, double centerX, double centerY, double radius) {
        long nowNanos = System.nanoTime();
        double timeSeconds = nowNanos / 1_000_000_000.0;
        double pulse = 0.5 + 0.5 * Math.sin(timeSeconds * 3.2);
        double outerRadius = radius * (1.7 + pulse * 0.14);
        Color ringColor = switch (diff) {
            case MEDIUM -> Color.web("#E4D4B4");
            case HARD -> Color.web("#F0A0A8");
            default -> Color.web("#9FEAFF");
        };

        gc.save();
        gc.setStroke(Color.color(ringColor.getRed(), ringColor.getGreen(), ringColor.getBlue(), 0.82));
        gc.setLineWidth(radius * 0.22);
        gc.strokeOval(centerX - outerRadius, centerY - outerRadius, outerRadius * 2, outerRadius * 2);

        gc.setStroke(Color.color(ringColor.getRed(), ringColor.getGreen(), ringColor.getBlue(), 0.35));
        gc.setLineWidth(radius * 0.10);
        double haloRadius = outerRadius * 1.22;
        gc.strokeOval(centerX - haloRadius, centerY - haloRadius, haloRadius * 2, haloRadius * 2);
        gc.restore();
    }

    private static Color withOpacity(Color color, double opacity) {
        return Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
    }

    private static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void drawHexaStarFill(GraphicsContext gc, double centerX, double centerY,
                                         double outerRadius, double halfThickness) {
        gc.fillPolygon(
                new double[]{centerX, centerX + outerRadius, centerX - outerRadius},
                new double[]{centerY - outerRadius, centerY + halfThickness, centerY + halfThickness},
                3
        );
        gc.fillPolygon(
                new double[]{centerX, centerX + outerRadius, centerX - outerRadius},
                new double[]{centerY + outerRadius, centerY - halfThickness, centerY - halfThickness},
                3
        );
    }

    private static void drawHexaStarStroke(GraphicsContext gc, double centerX, double centerY,
                                           double outerRadius, double halfThickness) {
        gc.strokePolygon(
                new double[]{centerX, centerX + outerRadius, centerX - outerRadius},
                new double[]{centerY - outerRadius, centerY + halfThickness, centerY + halfThickness},
                3
        );
        gc.strokePolygon(
                new double[]{centerX, centerX + outerRadius, centerX - outerRadius},
                new double[]{centerY + outerRadius, centerY - halfThickness, centerY - halfThickness},
                3
        );
    }

    private record PlayerPalette(Color base, Color innerGlow, Color outerAura, Color border, Color highlight) {
    }
}
