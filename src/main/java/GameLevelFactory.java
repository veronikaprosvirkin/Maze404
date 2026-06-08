import enums.Difficulty;
import enums.PlayerSkin;
import logic.ArtifactSpawner;
import logic.EnemySpawner;
import logic.generation.MazeGenerator;
import model.Artifact;
import model.Enemy;
import model.GameState;
import model.Grid;
import model.Player;
import model.Position;

import java.util.List;

public class GameLevelFactory {
    public LevelContext create(Difficulty difficulty, PlayerSkin playerSkin) {
        Grid grid = new MazeGenerator().generate(21, 21);

        Player player = new Player(7, 7);
        player.setSkin(playerSkin != null ? playerSkin : PlayerSkin.CIRCLE);
        Position dynamicPlayerPos = new Position(player.getRow(), player.getCol());

        List<Artifact> artifacts = new ArtifactSpawner().spawnArtifacts(
                grid,
                difficulty,
                dynamicPlayerPos
        );
        List<Enemy> enemies = new EnemySpawner().spawnEnemies(grid, difficulty, artifacts, dynamicPlayerPos);
        GameState gameState = new GameState(grid, player, enemies, artifacts, 1);

        return new LevelContext(gameState, grid, player, artifacts, enemies);
    }

    public record LevelContext(
            GameState gameState,
            Grid grid,
            Player player,
            List<Artifact> artifacts,
            List<Enemy> enemies
    ) {
    }
}
