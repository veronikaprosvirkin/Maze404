package logic.system;

import model.Enemy;
import model.GameState;
import model.Player;
import model.Position;

public class CollisionDetector {
    public void checkEnemyCollisions(GameState state) {
        Player player = state.getPlayer();
        Position playerPos = new Position(player.getRow(), player.getCol());

        for (Enemy enemy : state.getEnemies()) {
            if (enemy.getRow() == player.getRow() && enemy.getCol() == player.getCol()) {
                player.takeEnemyDamage(1);
                break;
            }
        }
    }
}
