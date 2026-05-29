package ui.render;

import enums.CellType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
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
            }
        }
    }
}

