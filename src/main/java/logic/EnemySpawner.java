package logic;

import AI.ChaseAI;
import AI.PatrolAI;
import Utilities.Util;
import enums.ArtifactType;
import enums.Difficulty;
import enums.EnemyMode;
import model.Artifact;
import model.Enemy;
import model.Grid;
import model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnemySpawner {

    public List<Enemy> spawnEnemies(Grid grid, Difficulty difficulty, List<Artifact> artifacts) {
        int patrolEnemyCount = 0;

        switch (difficulty) {
            case EASY -> patrolEnemyCount = 2;
            case MEDIUM -> patrolEnemyCount = 4;
            case HARD -> patrolEnemyCount = 6;
        }

        List<Position> floorCells = Util.getFloorCells(grid);
        Collections.shuffle(floorCells);

        List<Enemy> enemies = new ArrayList<>();

        if (difficulty == Difficulty.MEDIUM || difficulty == Difficulty.HARD) {
            Position keyPosition = null;

            for (Artifact artifact : artifacts) {
                if (artifact.getType() == ArtifactType.KEY) {
                    keyPosition = artifact.getPosition();
                    break;
                }
            }

            if (keyPosition != null) {
                int guardsToSpawn = (difficulty == Difficulty.HARD) ? 3 : 2;
                int guardsSpawned = 0;

                for (Position cell : floorCells) {
                    if (guardsSpawned >= guardsToSpawn) break;

                    int distToKey = cell.manhattanDistance(keyPosition);

                    if (distToKey > 0 && distToKey <= 3) {
                        boolean isTooClose = false;

                        for (Enemy e : enemies) {
                            Position existingEnemyPos = new Position(e.getRow(), e.getCol());
                            if (cell.manhattanDistance(existingEnemyPos) < 2) {
                                isTooClose = true;
                                break;
                            }
                        }

                        if (!isTooClose) {
                            enemies.add(new Enemy(cell.getRow(), cell.getCol(), EnemyMode.CHASE, new ChaseAI()));
                            guardsSpawned++;
                        }
                    }
                }
            }
        }

        int spawnedPatrols = 0;

        for (Position candidate : floorCells) {
            if (spawnedPatrols >= patrolEnemyCount) {
                break;
            }

            boolean isTooClose = false;

            for (Enemy spawnedEnemy : enemies) {
                Position enemyPos = new Position(spawnedEnemy.getRow(), spawnedEnemy.getCol());

                if (candidate.manhattanDistance(enemyPos) < 3) {
                    isTooClose = true;
                    break;
                }
            }

            if (!isTooClose) {
                Enemy enemy = new Enemy(candidate.getRow(), candidate.getCol(), EnemyMode.PATROL, new PatrolAI());
                enemies.add(enemy);
                spawnedPatrols++;
            }
        }

        return enemies;
    }
}