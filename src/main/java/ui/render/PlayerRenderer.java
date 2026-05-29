package ui.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import model.Player;

public class PlayerRenderer {
    private static final double LERP_FACTOR = 0.2;

    private double renderX;
    private double renderY;
    private boolean initialized = false;

    public void draw(GraphicsContext gc, Player player, double tileSize) {
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

        double radius = tileSize * 0.4;
        double offset = (tileSize - radius * 2) / 2.0;
        gc.setFill(Color.DODGERBLUE);
        gc.fillOval(renderX + offset, renderY + offset, radius * 2, radius * 2);
    }
}

