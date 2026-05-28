package AI;
import model.Enemy;
import model.GameState;
import model.Position;

import java.util.List;

public class PatrolAI implements IEnemyAI {
    private int routeIndex = 0;

    @Override
    public Position computeNextMove(GameState state, Enemy enemy) {
        List<Position> route = enemy.getPatrolRoute();
        if (route == null || route.isEmpty()) return new Position(enemy.getRow(), enemy.getCol());

        Position next = route.get(routeIndex % route.size());
        routeIndex++;
        return next;
    }
}