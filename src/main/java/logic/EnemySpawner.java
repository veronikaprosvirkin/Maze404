package logic;

import Utilities.Util;
import enums.Difficulty;
import enums.EnemyMode;
import model.Enemy;
import model.Grid;
import model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnemySpawner {
    public List<Enemy> spawnEnemies(Grid grid, Difficulty difficulty){
        int enemyCount = 0;

        switch (difficulty) {
            case EASY ->
                enemyCount = 2;
            case MEDIUM ->
                enemyCount = 4;
            case HARD ->
                enemyCount = 6;
        }

        List<Position> floorCells = Util.getFloorCells(grid);
        Collections.shuffle(floorCells);

        List<Enemy> enemies = new ArrayList<>();

        for (Position candidate : floorCells) {
            if (enemies.size() >= enemyCount) {
                break;
            }

            boolean isTooClose = false;

            for (Enemy spawnedEnemy : enemies) {
                int distance = Math.abs(spawnedEnemy.getRow() - candidate.getRow()) +
                        Math.abs(spawnedEnemy.getCol() - candidate.getCol());


                if (distance < 3) {
                    isTooClose = true;
                    break;
                }
            }

            if (!isTooClose) {
                Enemy enemy = new Enemy(candidate.getRow(), candidate.getCol(), EnemyMode.PATROL, null);
                enemies.add(enemy);
            }
        }

        return enemies;
    }
}