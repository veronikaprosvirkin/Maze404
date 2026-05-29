package ui.render;

import enums.CellType;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Loads and manages sprite tiles from a spritesheet image resource.
 * Provides tile extraction via pixel slicing and automatic fallback colored rectangles
 * when the spritesheet is missing or unavailable, ensuring robust development rendering.
 * Maps {@link enums.CellType} values to sprite grid positions for seamless game asset integration.
 */
public class SpriteSheet {
    private static final int TILE_SIZE = 32;
    private static final Map<CellType, int[]> SPRITE_POSITIONS = new EnumMap<>(CellType.class);

    static {
        // Mapping uses (col, row) coordinates on the spritesheet grid.
        SPRITE_POSITIONS.put(CellType.FLOOR, new int[]{0, 0});
        SPRITE_POSITIONS.put(CellType.WALL, new int[]{1, 0});
        SPRITE_POSITIONS.put(CellType.EXIT, new int[]{2, 0});
        SPRITE_POSITIONS.put(CellType.TRAP, new int[]{3, 0});
        SPRITE_POSITIONS.put(CellType.ARTIFACT, new int[]{0, 2});
        SPRITE_POSITIONS.put(CellType.EMPTY, new int[]{0, 0});
    }

    private final Image sheet;
    private final boolean sheetAvailable;
    private final Map<CellType, Image> fallbackCache = new EnumMap<>(CellType.class);

    public SpriteSheet() {
        this("/sprites/spritesheet.png");
    }

    public SpriteSheet(String resourcePath) {
        InputStream stream = SpriteSheet.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            this.sheet = null;
            this.sheetAvailable = false;
        } else {
            this.sheet = new Image(stream);
            this.sheetAvailable = !this.sheet.isError();
        }
    }

    public Image getSprite(int col, int row) {
        if (!sheetAvailable || sheet == null) {
            return getFallback(CellType.EMPTY);
        }

        int x = col * TILE_SIZE;
        int y = row * TILE_SIZE;
        if (x + TILE_SIZE > sheet.getWidth() || y + TILE_SIZE > sheet.getHeight()) {
            return getFallback(CellType.EMPTY);
        }

        PixelReader reader = sheet.getPixelReader();
        if (reader == null) {
            return getFallback(CellType.EMPTY);
        }

        WritableImage tile = new WritableImage(TILE_SIZE, TILE_SIZE);
        tile.getPixelWriter().setPixels(0, 0, TILE_SIZE, TILE_SIZE, reader, x, y);
        return tile;
    }

    public Image getSprite(CellType type) {
        int[] position = SPRITE_POSITIONS.getOrDefault(type, SPRITE_POSITIONS.get(CellType.EMPTY));
        if (!sheetAvailable) {
            return getFallback(type);
        }
        return getSprite(position[0], position[1]);
    }

    private Image getFallback(CellType type) {
        return fallbackCache.computeIfAbsent(type, this::createFallback);
    }

    private Image createFallback(CellType type) {
        Color color;
        switch (type) {
            case WALL -> color = Color.rgb(45, 45, 55);
            case FLOOR -> color = Color.rgb(210, 200, 185);
            case EXIT -> color = Color.rgb(50, 205, 50);
            case ARTIFACT -> color = Color.rgb(255, 215, 0);
            case TRAP -> color = Color.rgb(180, 30, 30);
            case EMPTY -> color = Color.rgb(210, 200, 185);
            default -> color = Color.MAGENTA;
        }

        WritableImage image = new WritableImage(TILE_SIZE, TILE_SIZE);
        PixelWriter writer = image.getPixelWriter();
        Color borderColor = type == CellType.WALL
                ? new Color(color.getRed() * 0.85, color.getGreen() * 0.85, color.getBlue() * 0.85, color.getOpacity())
                : color;
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean isWallBorder = type == CellType.WALL
                        && (x < 2 || y < 2 || x >= TILE_SIZE - 2 || y >= TILE_SIZE - 2);
                writer.setColor(x, y, isWallBorder ? borderColor : color);
            }
        }
        return image;
    }
}
