package AI;

import enums.CellType;
import model.Enemy;
import model.Grid;
import model.Player;
import model.Position;

public class ChaseAI implements IEnemyAI {

    @Override
    public Position computeNextMove(Enemy enemy, Grid grid, Player player) {
        int er = enemy.getRow();
        int ec = enemy.getCol();
        int pr = player.getRow();
        int pc = player.getCol();

        int rowDiff = Integer.compare(pr, er);
        int colDiff = Integer.compare(pc, ec);

        Position nextHorizontal = new Position(er, ec + colDiff);
        Position nextVertical = new Position(er + rowDiff, ec);


        boolean canMoveH = colDiff != 0 && grid.getCell(nextHorizontal.getRow(), nextHorizontal.getCol()).getType() != CellType.WALL;
        boolean canMoveV = rowDiff != 0 && grid.getCell(nextVertical.getRow(), nextVertical.getCol()).getType() != CellType.WALL;

        if (canMoveH && canMoveV) {
            return Math.abs(pr - er) > Math.abs(pc - ec) ? nextVertical : nextHorizontal;
        } else if (canMoveH) {
            return nextHorizontal;
        } else if (canMoveV) {
            return nextVertical;
        }

        return new Position(er, ec);
    }
}