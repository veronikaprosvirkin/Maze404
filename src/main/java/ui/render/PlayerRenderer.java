package ui.render;

import enums.Difficulty;
import miniGames.TestLauncher;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import model.Player;

public class PlayerRenderer {
    private static final double LERP_FACTOR = 0.2;

    private double renderX;
    private double renderY;
    private boolean initialized = false;

    public void draw(GraphicsContext gc, Player player, double tileSize) {
        draw(gc, player, tileSize, Difficulty.EASY);
    }

    public void draw(GraphicsContext gc, Player player, double tileSize, Difficulty diff) {
        double targetX = player.getCol() * tileSize;
        double targetY = player.getRow() * tileSize;

        if (!initialized) {
            renderX = targetX;
            renderY = targetY;
            initialized = true;
        } else {
            renderX += (targetX - renderX) * LERP_FACTOR;
            renderY += (targetY - renderY) * LERP_FACTOR;
        }

        if (diff == null) {
            diff = Difficulty.EASY;
        }

        Color playerColor;
        Color playerGlowColor;
        Color playerOuterRing;

        switch (diff) {
            case MEDIUM -> {
                playerColor = Color.web("#8FAA6A"); // Player base
                playerGlowColor = Color.rgb(143, 170, 106, 0.35); // Glow center
                playerOuterRing = Color.rgb(143, 170, 106, 0.2); // Outer aura
            }
            case HARD -> {
                playerColor = Color.web("#E09070"); // Player base
                playerGlowColor = Color.rgb(200, 100, 70, 0.45); // Glow center
                playerOuterRing = Color.rgb(180, 60, 40, 0.2); // Outer aura
            }
            default -> { // EASY
                playerColor = Color.web("#9DC8D0"); // Player base
                playerGlowColor = Color.rgb(122, 170, 176, 0.4); // Glow center
                playerOuterRing = Color.rgb(100, 160, 190, 0.15); // Outer aura
            }
        }

        double centerX = renderX + tileSize / 2.0;
        double centerY = renderY + tileSize / 2.0;
        double radius = tileSize * 0.4;

        // 1. Draw glowing auras first (behind the player)
        // Outer aura
        gc.setFill(playerOuterRing);
        gc.fillOval(centerX - radius * 1.8, centerY - radius * 1.8, radius * 3.6, radius * 3.6);

        // Inner glow ring
        gc.setFill(playerGlowColor);
        gc.fillOval(centerX - radius * 1.3, centerY - radius * 1.3, radius * 2.6, radius * 2.6);

        // 2. Draw the player body
        gc.setFill(playerColor);
        gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        // 3. Draw a subtle, sleek border around player
        gc.setStroke(playerColor.brighter().deriveColor(0, 1, 1, 0.6));
        gc.setLineWidth(1.5);
        gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        // 4. Draw a shiny highlight to make the player token look premium and desaturated
        gc.setFill(Color.rgb(255, 255, 255, 0.25));
        gc.fillOval(centerX - radius * 0.5, centerY - radius * 0.6, radius * 0.6, radius * 0.4);
    }
}

