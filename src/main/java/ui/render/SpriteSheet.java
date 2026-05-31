package ui.render;

import enums.CellType;
import enums.Difficulty;
import miniGames.TestLauncher;
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
 * Provides tile extraction via pixel slicing and automatic fallback colored
 * rectangles
 * when the spritesheet is missing or unavailable, ensuring robust development
 * rendering.
 * Maps {@link enums.CellType} values to sprite grid positions for seamless game
 * asset integration.
 */
public class SpriteSheet {
    private static final int TILE_SIZE = 32;
    private static final int WALL_VARIANT_COUNT = 4;
    private static final Map<CellType, int[]> SPRITE_POSITIONS = new EnumMap<>(CellType.class);

    static {
        // Mapping uses (col, row) coordinates on the spritesheet grid.
        SPRITE_POSITIONS.put(CellType.FLOOR, new int[] { 0, 0 });
        SPRITE_POSITIONS.put(CellType.WALL, new int[] { 1, 0 });
        SPRITE_POSITIONS.put(CellType.EXIT, new int[] { 2, 0 });
        SPRITE_POSITIONS.put(CellType.TRAP, new int[] { 3, 0 });
        SPRITE_POSITIONS.put(CellType.ARTIFACT, new int[] { 0, 2 });
        SPRITE_POSITIONS.put(CellType.EMPTY, new int[] { 0, 0 });
    }

    private final Image sheet;
    private final boolean sheetAvailable;
    private final Map<Difficulty, Map<CellType, Image>> fallbackCache = new EnumMap<>(Difficulty.class);
    private final Map<Difficulty, Image[]> wallVariantCache = new EnumMap<>(Difficulty.class);
    private final Difficulty difficulty;

    public SpriteSheet() {
        this(Difficulty.EASY);
    }

    public SpriteSheet(Difficulty difficulty) {
        this(difficulty, "/sprites/spritesheet.png");
    }

    public SpriteSheet(Difficulty difficulty, String resourcePath) {
        this.difficulty = difficulty != null ? difficulty : Difficulty.EASY;
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

    public Image getSprite(CellType type, int row, int col) {
        int[] position = SPRITE_POSITIONS.getOrDefault(type, SPRITE_POSITIONS.get(CellType.EMPTY));
        if (!sheetAvailable) {
            return getFallback(type, row, col);
        }
        return getSprite(position[0], position[1]);
    }

    private Image getFallback(CellType type) {
        return fallbackCache
                .computeIfAbsent(this.difficulty, d -> new EnumMap<>(CellType.class))
                .computeIfAbsent(type, t -> createFallback(t, this.difficulty));
    }

    private Image getFallback(CellType type, int row, int col) {
        if (type == CellType.WALL) {
            Image[] variants = wallVariantCache.computeIfAbsent(this.difficulty, this::createWallVariants);
            int variantIndex = variantIndexForCell(row, col);
            return variants[variantIndex];
        }
        return getFallback(type);
    }

    private Image[] createWallVariants(Difficulty diff) {
        Image[] variants = new Image[WALL_VARIANT_COUNT];
        for (int i = 0; i < WALL_VARIANT_COUNT; i++) {
            variants[i] = createFallback(CellType.WALL, diff, i);
        }
        return variants;
    }

    private int variantIndexForCell(int row, int col) {
        if (row < 0 || col < 0) {
            return 0;
        }
        int seed = (row * 73856093) ^ (col * 19349663);
        return Math.floorMod(seed, WALL_VARIANT_COUNT);
    }

    private Image createFallback(CellType type, Difficulty diff) {
        return createFallback(type, diff, 0);
    }

    private Image createFallback(CellType type, Difficulty diff, int variantIndex) {
        if (diff == null) {
            diff = Difficulty.EASY;
        }
        Color color;
        Color altColor = null;
        Color accentColor = null;

        switch (diff) {
            case MEDIUM -> {
                switch (type) {
                    case WALL -> {
                        color = Color.web("#231D17");
                        altColor = Color.web("#2E2620");
                    }
                    case FLOOR, EMPTY -> {
                        color = Color.web("#161210");
                        altColor = Color.web("#1A1512");
                    }
                    case EXIT -> {
                        color = Color.web("#3C5A35");
                        altColor = Color.web("#5A8248");
                    }
                    case ARTIFACT -> {
                        color = Color.web("#161210");
                        accentColor = Color.web("#5A8898");
                    }
                    case TRAP -> {
                        color = Color.web("#120A0A");
                        accentColor = Color.web("#6B2020");
                    }
                    default -> {
                        color = Color.web("#3D3228");
                    }
                }
            }
            case HARD -> {
                switch (type) {
                    case WALL -> {
                        color = Color.web("#281018");
                        altColor = Color.web("#381520");
                    }
                    case FLOOR, EMPTY -> {
                        color = Color.web("#150A0C");
                        altColor = Color.web("#1C0A10");
                    }
                    case EXIT -> {
                        color = Color.web("#382818");
                        altColor = Color.web("#6A5028");
                    }
                    case ARTIFACT -> {
                        color = Color.web("#150A0C");
                        accentColor = Color.web("#E06080");
                    }
                    case TRAP -> {
                        color = Color.web("#0E0404");
                        accentColor = Color.web("#CC2020");
                    }
                    default -> {
                        color = Color.web("#502030");
                    }
                }
            }
            default -> { // EASY
                switch (type) {
                    case WALL -> {
                        color = Color.web("#1C2232");
                        altColor = Color.web("#252D40");
                    }
                    case FLOOR, EMPTY -> {
                        color = Color.web("#111520");
                        altColor = Color.web("#141826");
                    }
                    case EXIT -> {
                        color = Color.web("#2A4A50");
                        altColor = Color.web("#3A6A72");
                    }
                    case ARTIFACT -> {
                        color = Color.web("#111520"); // floor background
                        accentColor = Color.web("#7AB4D0"); // crystal color
                    }
                    case TRAP -> {
                        color = Color.web("#0D080E");
                        accentColor = Color.web("#8A2860");
                    }
                    default -> {
                        color = Color.web("#2E3A52");
                    }
                }
            }
        }

        WritableImage image = new WritableImage(TILE_SIZE, TILE_SIZE);
        PixelWriter writer = image.getPixelWriter();

        // 1. WALL: masonry brick texture
        if (type == CellType.WALL) {
            Color crackColor = switch (diff) {
                case MEDIUM -> Color.rgb(80, 60, 30, 0.4);
                case HARD -> Color.rgb(160, 20, 20, 0.5);
                default -> Color.rgb(120, 180, 220, 0.45);
            };
            Color masonryLine = altColor != null ? altColor : color.brighter();

            for (int y = 0; y < TILE_SIZE; y++) {
                for (int x = 0; x < TILE_SIZE; x++) {
                    // Thin micro-bevel/brick borders
                    boolean isMasonryLine = (y == 0 || y == TILE_SIZE / 2 || y == TILE_SIZE - 1)
                            || (y < TILE_SIZE / 2 && (x == 0 || x == TILE_SIZE / 2 || x == TILE_SIZE - 1))
                            || (y >= TILE_SIZE / 2
                                    && (x == 0 || x == TILE_SIZE / 4 || x == 3 * TILE_SIZE / 4 || x == TILE_SIZE - 1));

                    // Some cracks
                    boolean isCrack = false;
                    if (diff == Difficulty.HARD) {
                        isCrack = (variantIndex == 0 && y == x - 4 && x > 8 && x < 24)
                                || (variantIndex == 1 && y == TILE_SIZE - x + 2 && x > 12 && x < 20)
                                || (variantIndex == 2 && y == x + 6 && x > 6 && x < 22);
                    } else if (diff == Difficulty.MEDIUM) {
                        isCrack = (variantIndex == 0 && y == 2 * x - 10 && x > 5 && x < 15)
                                || (variantIndex == 1 && y == 6 && x > 2 && x < 18)
                                || (variantIndex == 2 && y == TILE_SIZE - x - 6 && x > 8 && x < 24);
                    } else {
                        isCrack = (variantIndex == 0 && y == TILE_SIZE - x - 14 && x > 10 && x < 26)
                                || (variantIndex == 1 && y == x - 8 && x > 12 && x < 28)
                                || (variantIndex == 2 && y == TILE_SIZE - x + 4 && x > 6 && x < 22);
                    }

                    if (isCrack) {
                        writer.setColor(x, y, crackColor);
                    } else if (isMasonryLine) {
                        writer.setColor(x, y, masonryLine);
                    } else {
                        double noise = (Math.sin(x * 0.5) * Math.cos(y * 0.5) + 1.0) * 0.2;
                        writer.setColor(x, y, color.deriveColor(0, 1, 1 - noise, 1));
                    }
                }
            }
        }
        // 2. FLOOR / EMPTY: tiles with corner shadows and slight noise
        else if (type == CellType.FLOOR || type == CellType.EMPTY) {
            Color tileBorder = altColor != null ? altColor : color.darker();
            for (int y = 0; y < TILE_SIZE; y++) {
                for (int x = 0; x < TILE_SIZE; x++) {
                    boolean isBorder = x == 0 || y == 0 || x == TILE_SIZE - 1 || y == TILE_SIZE - 1;
                    if (isBorder) {
                        writer.setColor(x, y, tileBorder);
                    } else {
                        double centerX = TILE_SIZE / 2.0;
                        double centerY = TILE_SIZE / 2.0;
                        double dist = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
                        double maxDist = TILE_SIZE / 1.414;
                        double vignette = Math.min(1.0, dist / maxDist);
                        double factor = 1.0 - (vignette * 0.15);
                        writer.setColor(x, y, color.deriveColor(0, 1.0, factor, 1.0));
                    }
                }
            }
        }
        // 3. EXIT: double door shape
        else if (type == CellType.EXIT) {
            Color doorBorderColor = altColor != null ? altColor : color.brighter();
            Color handleColor = switch (diff) {
                case MEDIUM -> Color.web("#D9C9A8");
                case HARD -> Color.web("#D4A8B0");
                default -> Color.web("#D2DCE8");
            };
            for (int y = 0; y < TILE_SIZE; y++) {
                for (int x = 0; x < TILE_SIZE; x++) {
                    boolean isFrame = x < 4 || x >= TILE_SIZE - 4 || y < 4;
                    boolean isCenterLine = x == TILE_SIZE / 2 || x == TILE_SIZE / 2 - 1;
                    boolean isPanelBorder = (x >= 6 && x <= 10 && (y == 8 || y == 24))
                            || (x >= 21 && x <= 25 && (y == 8 || y == 24))
                            || ((x == 6 || x == 10) && y >= 8 && y <= 24)
                            || ((x == 21 || x == 25) && y >= 8 && y <= 24);

                    boolean isHandle = (y >= 14 && y <= 17) && (x == TILE_SIZE / 2 - 3 || x == TILE_SIZE / 2 + 2);

                    if (isHandle) {
                        writer.setColor(x, y, handleColor);
                    } else if (isCenterLine) {
                        writer.setColor(x, y, color.darker().darker());
                    } else if (isPanelBorder) {
                        writer.setColor(x, y, doorBorderColor);
                    } else if (isFrame) {
                        writer.setColor(x, y, doorBorderColor.darker());
                    } else {
                        writer.setColor(x, y, color);
                    }
                }
            }
        }
        // 4. ARTIFACT: Diamond crystal shape over floor background
        else if (type == CellType.ARTIFACT) {
            Color floorColor = color;
            Color crystalColor = accentColor != null ? accentColor : Color.GOLD;
            Color crystalGlow = crystalColor.deriveColor(0, 1.0, 1.2, 0.45);

            for (int y = 0; y < TILE_SIZE; y++) {
                for (int x = 0; x < TILE_SIZE; x++) {
                    int dx = Math.abs(x - TILE_SIZE / 2);
                    int dy = Math.abs(y - TILE_SIZE / 2);
                    int dist = dx + dy;

                    if (dist <= 7) {
                        if (dist == 7) {
                            writer.setColor(x, y, crystalColor.brighter());
                        } else {
                            double shine = (x == y || x == y - 1) ? 1.3 : 1.0;
                            writer.setColor(x, y, crystalColor.deriveColor(0, 1.0, shine, 1.0));
                        }
                    } else if (dist <= 11) {
                        double factor = (11.0 - dist) / 4.0;
                        Color mixed = floorColor.interpolate(crystalGlow, factor);
                        writer.setColor(x, y, mixed);
                    } else {
                        boolean isBorder = x == 0 || y == 0 || x == TILE_SIZE - 1 || y == TILE_SIZE - 1;
                        if (isBorder && altColor != null) {
                            writer.setColor(x, y, altColor);
                        } else {
                            writer.setColor(x, y, floorColor);
                        }
                    }
                }
            }
        }
        // 5. TRAP: Dark warning-patterned cell
        else if (type == CellType.TRAP) {
            Color trapLineColor = accentColor != null ? accentColor : Color.RED;
            for (int y = 0; y < TILE_SIZE; y++) {
                for (int x = 0; x < TILE_SIZE; x++) {
                    int dx = Math.abs(x - TILE_SIZE / 2);
                    int dy = Math.abs(y - TILE_SIZE / 2);
                    boolean isSymbol = (dx == 0 && dy <= 6) || (dy == 0 && dx <= 6)
                            || (dx == 1 && dy <= 4) || (dy == 1 && dx <= 4)
                            || (dx == 2 && dy <= 2) || (dy == 2 && dx <= 2);

                    if (isSymbol) {
                        writer.setColor(x, y, trapLineColor);
                    } else {
                        writer.setColor(x, y, color);
                    }
                }
            }
        }
        // DEFAULT fallbacks
        else {
            for (int y = 0; y < TILE_SIZE; y++) {
                for (int x = 0; x < TILE_SIZE; x++) {
                    writer.setColor(x, y, color);
                }
            }
        }

        return image;
    }
}
