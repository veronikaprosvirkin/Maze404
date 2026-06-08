package ui.render;

import enums.ArtifactType;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public final class ArtifactVisuals {
    private static final Image KEY_IMAGE = loadKeyImage();

    private ArtifactVisuals() {
    }

    private enum ArtifactShape {
        ORB,
        ORB_WITH_CIRCLE,
        CRYSTAL_WITH_CIRCLE,
        CRYSTAL
    }

    private record ArtifactPalette(Color base, Color accent) {
    }

    public static Node createHudIcon(ArtifactType type, double size) {
        Canvas canvas = new Canvas(size, size);
        drawArtifact(canvas.getGraphicsContext2D(), type, size / 2.0, size / 2.0, size, 0.45, 1.0);
        return canvas;
    }

    public static void drawArtifact(
            GraphicsContext gc,
            ArtifactType type,
            double centerX,
            double centerY,
            double size,
            double phase,
            double visibility
    ) {
        if (type == null || visibility <= 0.0) {
            return;
        }

        if (type == ArtifactType.KEY && KEY_IMAGE != null) {
            gc.save();
            gc.setGlobalAlpha(visibility);
            double imgSize = size * 0.6;
            gc.drawImage(KEY_IMAGE, centerX - imgSize / 2.0, centerY - imgSize / 2.0, imgSize, imgSize);
            gc.restore();
            return;
        }

        ArtifactPalette palette = getArtifactPalette(type);
        ArtifactShape shape = getArtifactShape(type);
        double pulse = 0.5 + 0.5 * Math.sin(phase);
        double radius = size * (0.17 + pulse * 0.045);
        boolean crystal = shape == ArtifactShape.CRYSTAL || shape == ArtifactShape.CRYSTAL_WITH_CIRCLE;
        boolean circle = shape == ArtifactShape.ORB_WITH_CIRCLE || shape == ArtifactShape.CRYSTAL_WITH_CIRCLE;

        gc.save();
        gc.setGlobalAlpha((0.78 + pulse * 0.20) * visibility);
        gc.setEffect(new DropShadow(size * (0.55 + pulse * 0.25), withOpacity(palette.accent(), visibility)));
        gc.setFill(palette.base());
        if (crystal) {
            gc.fillPolygon(
                    new double[] {centerX, centerX + radius, centerX, centerX - radius},
                    new double[] {centerY - radius, centerY, centerY + radius, centerY},
                    4
            );
        } else {
            gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        }
        gc.restore();

        if (circle) {
            gc.setStroke(withOpacity(palette.accent(), (0.50 + pulse * 0.30) * visibility));
            gc.setLineWidth(Math.max(1.2, size * 0.035));
            gc.strokeOval(centerX - radius * 1.9, centerY - radius * 1.9, radius * 3.8, radius * 3.8);
        }

        gc.setFill(Color.rgb(255, 255, 255, (0.36 * visibility)));
        if (crystal) {
            gc.fillPolygon(
                    new double[] {centerX, centerX + radius * 0.30, centerX},
                    new double[] {centerY - radius * 0.58, centerY - radius * 0.06, centerY + radius * 0.18},
                    3
            );
        } else {
            gc.fillOval(centerX - radius * 0.45, centerY - radius * 0.55, radius * 0.55, radius * 0.40);
        }
    }

    public static void drawPlacedBeacon(
            GraphicsContext gc,
            double centerX,
            double centerY,
            double size,
            double phase,
            double visibility
    ) {
        if (gc == null || visibility <= 0.0 || size <= 0.0) {
            return;
        }

        ArtifactPalette palette = getArtifactPalette(ArtifactType.BEACON);
        double pulse = 0.5 + 0.5 * Math.sin(phase);
        double ringPulse = 0.5 + 0.5 * Math.sin(phase * 1.8 + 0.7);
        double baseRadius = size * 0.28;
        double mastHeight = size * (0.28 + pulse * 0.06);
        double mastWidth = Math.max(2.0, size * 0.08);
        double coreRadius = size * (0.12 + pulse * 0.03);
        double beaconY = centerY - size * 0.06;

        gc.save();
        gc.setGlobalAlpha(visibility);

        gc.setFill(Color.rgb(255, 182, 120, (0.18 + pulse * 0.10) * visibility));
        gc.fillOval(centerX - baseRadius * 1.15, centerY - baseRadius * 0.92, baseRadius * 2.3, baseRadius * 1.84);

        gc.setStroke(withOpacity(palette.accent(), (0.30 + ringPulse * 0.26) * visibility));
        gc.setLineWidth(Math.max(1.2, size * 0.045));
        double outerRingRadius = size * (0.32 + ringPulse * 0.08);
        gc.strokeOval(centerX - outerRingRadius, centerY - outerRingRadius, outerRingRadius * 2, outerRingRadius * 2);

        gc.setFill(Color.rgb(50, 27, 18, 0.92 * visibility));
        gc.fillRoundRect(centerX - baseRadius * 0.86, centerY + size * 0.12, baseRadius * 1.72, size * 0.12, size * 0.08, size * 0.08);

        gc.setFill(Color.rgb(102, 56, 28, 0.95 * visibility));
        gc.fillRoundRect(centerX - mastWidth / 2.0, beaconY - mastHeight * 0.44, mastWidth, mastHeight, mastWidth, mastWidth);

        gc.setEffect(new DropShadow(size * (0.35 + pulse * 0.16), withOpacity(palette.accent(), (0.55 + pulse * 0.20) * visibility)));
        gc.setFill(withOpacity(palette.base(), 0.96 * visibility));
        gc.fillOval(centerX - coreRadius, beaconY - mastHeight * 0.48 - coreRadius, coreRadius * 2, coreRadius * 2);

        gc.setFill(Color.rgb(255, 247, 234, (0.64 + pulse * 0.18) * visibility));
        gc.fillOval(
                centerX - coreRadius * 0.40,
                beaconY - mastHeight * 0.48 - coreRadius * 0.98,
                coreRadius * 0.72,
                coreRadius * 0.72
        );
        gc.restore();
    }

    private static ArtifactPalette getArtifactPalette(ArtifactType type) {
        return switch (type) {
            case CRYSTAL -> new ArtifactPalette(Color.web("#F0D66A"), Color.web("#FFF3A6"));
            case MINI_GAME -> new ArtifactPalette(Color.web("#FF73B7"), Color.web("#FFD4EA"));
            case SHIELD -> new ArtifactPalette(Color.web("#7DE4FF"), Color.web("#D7FAFF"));
            case RADAR -> new ArtifactPalette(Color.web("#65F2A0"), Color.web("#D4FFE3"));
            case BEACON -> new ArtifactPalette(Color.web("#FF8E52"), Color.web("#FFD2A8"));
            case ELIXIR -> new ArtifactPalette(Color.web("#C46BFF"), Color.web("#F0C8FF"));
            case KEY -> new ArtifactPalette(Color.web("#FFB86C"), Color.web("#FFE8C2"));
        };
    }

    private static ArtifactShape getArtifactShape(ArtifactType type) {
        return switch (type) {
            case CRYSTAL -> ArtifactShape.CRYSTAL;
            case SHIELD, BEACON -> ArtifactShape.ORB_WITH_CIRCLE;
            case RADAR, MINI_GAME -> ArtifactShape.CRYSTAL_WITH_CIRCLE;
            case ELIXIR -> ArtifactShape.ORB;
            case KEY -> ArtifactShape.ORB_WITH_CIRCLE;
        };
    }

    private static Color withOpacity(Color color, double opacity) {
        return Color.color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                Math.max(0.0, Math.min(1.0, opacity))
        );
    }

    private static Image loadKeyImage() {
        try {
            return new Image(ArtifactVisuals.class.getResourceAsStream("/icons/key.png"));
        } catch (Exception e) {
            return null;
        }
    }
}
