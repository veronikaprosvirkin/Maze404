package logic.generation;

public class LevelConfig {
    private int levelNumber;
    private int gridRows;
    private int gridCols;
    private int trapCount;
    private int minTrapDistance;
    private int crystalCount;
    private int droneCount;
    private int miniGameTriggers;
    private boolean fogEnabled;
    private int fogRadius;
    private int flashlightRadius;

    // Конструктор без аргументів
    public LevelConfig() {}

    public LevelConfig(int levelNumber, int gridRows, int gridCols,
                       int trapCount, int minTrapDistance, int crystalCount,
                       int droneCount, int miniGameTriggers,
                       boolean fogEnabled, int fogRadius, int flashlightRadius) {
        this.levelNumber= levelNumber;
        this.gridRows = gridRows;
        this.gridCols  = gridCols;
        this.trapCount = trapCount;
        this.minTrapDistance = minTrapDistance;
        this.crystalCount = crystalCount;
        this.droneCount = droneCount;
        this.miniGameTriggers = miniGameTriggers;
        this.fogEnabled  = fogEnabled;
        this.fogRadius  = fogRadius;
        this.flashlightRadius = flashlightRadius;
    }

    // Фабричний метод
    public static LevelConfig defaultFor(int level) {
        return switch (level) {
            case 1 -> new LevelConfig(1, 11, 11,  5, 3,  8, 0, 1, false, 0, 0);
            case 2 -> new LevelConfig(2, 15, 15, 10, 3, 12, 3, 2, true,  2, 4);
            case 3 -> new LevelConfig(3, 19, 19, 18, 2, 15, 5, 3, true,  2, 4);
            default -> throw new IllegalArgumentException("Unknown level: " + level);
        };
    }

    // ── Геттери ───────────────────────────────────────────────────────
    public int     getLevelNumber()      { return levelNumber; }
    public int     getGridHeight()       { return gridRows; }
    public int     getGridWidth()        { return gridCols; }
    public int     getTrapCount()        { return trapCount; }
    public int     getMinTrapDistance()  { return minTrapDistance; }
    public int     getCrystalCount()     { return crystalCount; }
    public int     getDroneCount()       { return droneCount; }
    public int     getMiniGameTriggers() { return miniGameTriggers; }
    public boolean isFogEnabled()        { return fogEnabled; }
    public int     getFogRadius()        { return fogRadius; }
    public int     getFlashlightRadius() { return flashlightRadius; }
}
