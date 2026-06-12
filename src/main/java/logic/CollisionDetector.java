package logic;

import events.EventBus;
import events.GameEvent;
import model.*;

import java.util.HashMap;
import java.util.Map;

public class CollisionDetector {
    private static class Coords {
        int row, col;

        Coords(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    private Coords prevPlayerPos = null;
    private final Map<Enemy, Coords> prevEnemyPositions = new HashMap<>();

    /**
     * Головний метод перевірки всіх зіткнень.
     */
    public void checkCollisions(GameState gameState) {
        Player player = gameState.getPlayer();

        checkEnemyCollisions(player, gameState);
        // checkArtifactCollisions(player, gameState);

        updateHistory(gameState);
    }

    private void checkEnemyCollisions(Player player, GameState gameState) {
        for (Enemy enemy : gameState.getEnemies()) {
            // стоять на одній клітинці прямо зараз
            boolean isDirectCollision = (player.getRow() == enemy.getRow() && player.getCol() == enemy.getCol());

            //  помінялись місцями за один хід
            boolean isPassThroughCollision = false;
            Coords prevEnemy = prevEnemyPositions.get(enemy);

            if (prevPlayerPos != null && prevEnemy != null) {
                isPassThroughCollision =
                        (player.getRow() == prevEnemy.row && player.getCol() == prevEnemy.col) &&
                                (enemy.getRow() == prevPlayerPos.row && enemy.getCol() == prevPlayerPos.col);
            }

            // Якщо є будь-яке з двох зіткнень гравець отримує шкоду
            if (isDirectCollision || isPassThroughCollision) {
                player.takeDamage(1);
            }
        }
    }

    private void checkArtifactCollisions(Player player, GameState gameState) {
        Artifact collectedArtifact = null;

        for (Artifact artifact : gameState.getArtifacts()) {
            // Якщо артефакт вже зібрано, ігноруємо його
            if (artifact.isCollected()) continue;

            // Звертаємось до координат через об'єкт Position
            // ПРИМІТКА: Якщо Position це Java Record, використовуємо .row() та .col().
            // Якщо це звичайний клас, можливо доведеться змінити на .getRow() та .getCol()
            if (player.getRow() == artifact.getPosition().getRow() &&
                    player.getCol() == artifact.getPosition().getCol()) {

                collectedArtifact = artifact;
                break;
            }
        }

        // Обробляємо знайдений артефакт
        if (collectedArtifact != null) {
            collectedArtifact.collect();

            // 1. Даємо бонус гравцю залежно від типу артефакту
            switch (collectedArtifact.getType()) {
                case CRYSTAL -> player.addCrystals(1);
                case SHIELD -> player.addShield(1);
                // ... інші типи
            }

            // 2. Публікуємо івент для UI та звуку!
            Position pos = new Position(player.getRow(), player.getCol());
            EventBus.getInstance().publish(new GameEvent(GameEvent.Type.ARTIFACT_COLLECTED, pos));
        }
    }

    /**
     * Оновлює локальну мапу позицій після завершення перевірок
     */
    private void updateHistory(GameState gameState) {
        Player p = gameState.getPlayer();
        prevPlayerPos = new Coords(p.getRow(), p.getCol());

        prevEnemyPositions.clear();
        for (Enemy e : gameState.getEnemies()) {
            prevEnemyPositions.put(e, new Coords(e.getRow(), e.getCol()));
        }
    }



    /*public void checkCollisions(GameState state) {
        Player player = state.getPlayer();
        Position playerPos = new Position(player.getRow(), player.getCol());

        for (Enemy enemy : state.getEnemies()) {
            if (enemy.getRow() == player.getRow() && enemy.getCol() == player.getCol()) {
                player.takeDamage(1); // takeDamage сам перевіряє щит і публікує події
                break;
            }
        }
    }*/
}
