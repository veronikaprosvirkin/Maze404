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
        draw(gc, grid, 0, grid.getHeight(), 0, grid.getWidth(), 0.0);
    }

    public void draw(GraphicsContext gc, Grid grid, int startRow, int endRow, int startCol, int endCol) {
        draw(gc, grid, startRow, endRow, startCol, endCol, 0.0);
    }

    public void draw(GraphicsContext gc, Grid grid, int startRow, int endRow, int startCol, int endCol, double animationPhase) {
        int clampedStartRow = Math.max(0, startRow);
        int clampedEndRow = Math.min(grid.getHeight(), endRow);
        int clampedStartCol = Math.max(0, startCol);
        int clampedEndCol = Math.min(grid.getWidth(), endCol);

        for (int row = clampedStartRow; row < clampedEndRow; row++) {
            for (int col = clampedStartCol; col < clampedEndCol; col++) {
                Cell cell = grid.getCell(row, col);
                CellType type = cell.getType();
                Image tile = spriteSheet.getSprite(type, row, col);
                gc.drawImage(tile, col * TILE_SIZE, row * TILE_SIZE);

                if (type != CellType.WALL) {
                    gc.setStroke(Color.rgb(0, 0, 0, 0.2));
                    gc.setLineWidth(0.5);
                    gc.strokeRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }

                if (cell.isFlagged() && type != CellType.WALL) {
                    double phase = animationPhase + row * 0.55 + col * 0.35;
                    ArtifactVisuals.drawPlacedBeacon(
                            gc,
                            col * TILE_SIZE + TILE_SIZE / 2.0,
                            row * TILE_SIZE + TILE_SIZE / 2.0,
                            TILE_SIZE,
                            phase,
                            1.0
                    );
                }
            }
        }
    }
}
