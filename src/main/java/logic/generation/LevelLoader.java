package logic.generation;

import enums.Difficulty;
import logic.ArtifactSpawner;
import logic.EnemySpawner;
import model.*;

import java.io.InputStream;
import java.util.List;

public class LevelLoader {
    private final MazeGenerator    mazeGen;
    private final TrapPlacer       trapPlacer;
    private final EnemySpawner     enemySpawner;
    private final ArtifactSpawner  artifactSpawner;

    public LevelLoader(EnemySpawner enemySpawner, ArtifactSpawner artifactSpawner) {
        this.mazeGen         = new MazeGenerator();
        this.trapPlacer      = new TrapPlacer();
        this.enemySpawner    = enemySpawner;
        this.artifactSpawner = artifactSpawner;
    }


     // Головний метод — повертає повний стан гри для рівня levelNumber (1, 2 або 3)
    public GameState loadLevel(int levelNumber) {
        LevelConfig config = loadConfig(levelNumber);
        Grid grid = mazeGen.generate(config.getGridHeight(), config.getGridWidth());

        Position startPos = new Position(1, 1);
        trapPlacer.placeTraps(grid, config, startPos);

        Player         player    = new Player(startPos.getRow(), startPos.getCol());
        List<Enemy>    enemies   = enemySpawner.spawnEnemies(grid, Difficulty.MEDIUM);
        List<Artifact> artifacts = artifactSpawner.spawnArtifacts(grid, Difficulty.MEDIUM, startPos);

        return new GameState(grid, player, enemies, artifacts, levelNumber);
    }

    /**
     * Читає level{n}.json з ресурсів.
     * Якщо файл не знайдено — повертає хардкодований дефолт (LevelConfig.defaultFor).
     * TODO: підключити Gson/org.json для повноцінного парсингу після узгодження з командою.
     */
    private LevelConfig loadConfig(int levelNumber) {
        String path = "/levels/level" + levelNumber + ".json";
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.out.println("[LevelLoader] " + path + " не знайдено, використовується дефолт.");
                return LevelConfig.defaultFor(levelNumber);
            }
            // TODO: розпарсити JSON через Gson після узгодження з командою
            //Gson gson = new Gson();
            // return gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), LevelConfig.class);
            return LevelConfig.defaultFor(levelNumber);
        } catch (Exception e) {
            System.err.println("[LevelLoader] Помилка читання конфігу: " + e.getMessage());
            return LevelConfig.defaultFor(levelNumber);
        }
    }
}
