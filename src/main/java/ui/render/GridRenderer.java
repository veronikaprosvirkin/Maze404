package ui.render;

import enums.CellType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import model.Cell;
import model.Grid;

public class GridRenderer {
    private static final int TILE_SIZE = 32;
    private final SpriteSheet spriteSheet;

    public GridRenderer(SpriteSheet spriteSheet) {
        this.spriteSheet = spriteSheet;
    }

    public void draw(GraphicsContext gc, Grid grid) {
        for (int row = 0; row < grid.getHeight(); row++) {
            for (int col = 0; col < grid.getWidth(); col++) {
                Cell cell = grid.getCell(row, col);
                CellType type = cell.getType();
                Image tile = spriteSheet.getSprite(type);
                gc.drawImage(tile, col * TILE_SIZE, row * TILE_SIZE);

                if (type != CellType.WALL) {
                    gc.setStroke(Color.rgb(0, 0, 0, 0.2));
                    gc.setLineWidth(0.5);
                    gc.strokeRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }
}
