package AI;

import model.*;

public interface IEnemyAI {

    Position computeNextMove(Enemy enemy, Grid grid, Player player);
}