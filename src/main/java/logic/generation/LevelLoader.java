package logic.generation;

import com.google.gson.Gson;
import enums.Difficulty;
import logic.ArtifactSpawner;
import logic.EnemySpawner;
import model.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

        Player player = new Player(startPos.getRow(), startPos.getCol());

        Difficulty currentDifficulty = switch (levelNumber) {
            case 1 -> Difficulty.EASY;
            case 2 -> Difficulty.MEDIUM;
            default -> Difficulty.HARD;
        };

        Difficulty.current = currentDifficulty;

        List<Artifact> artifacts = artifactSpawner.spawnArtifacts(grid, currentDifficulty, startPos);

        List<Enemy> enemies = enemySpawner.spawnEnemies(grid, currentDifficulty, artifacts);

        return new GameState(grid, player, enemies, artifacts, levelNumber);
    }

    /**
     * Читає level{n}.json з ресурсів.
     * Якщо файл не знайдено — повертає хардкодований дефолт (LevelConfig.defaultFor).
     */
    private LevelConfig loadConfig(int levelNumber) {
        String path = "/levels/level" + levelNumber + ".json";
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.out.println("[LevelLoader] " + path + " not found, using default.");
                return LevelConfig.defaultFor(levelNumber);
            }
            Gson gson = new Gson();
            return gson.fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8),
                    LevelConfig.class
            );
        } catch (Exception e) {
            System.err.println("[LevelLoader] Error reading config: " + e.getMessage());
            return LevelConfig.defaultFor(levelNumber);
        }
    }
}
