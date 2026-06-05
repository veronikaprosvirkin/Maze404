package AI;

import enums.CellType;
import model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PatrolAI implements IEnemyAI {
    private final Random random = new Random();

    private Position lastPosition = null;

    @Override
    public Position computeNextMove(Enemy enemy, Grid grid, Player player) {
        int r = enemy.getRow();
        int c = enemy.getCol();

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        List<Position> possibleMoves = new ArrayList<>();

        for (int[] dir : directions) {
            int newRow = r + dir[0];
            int newCol = c + dir[1];

            if (grid.isInBounds(newRow, newCol) && grid.getCell(newRow, newCol).getType() != CellType.WALL) {
                possibleMoves.add(new Position(newRow, newCol));
            }
        }

        if (possibleMoves.isEmpty()) {
            return new Position(r, c);
        }

        List<Position> forwardMoves = new ArrayList<>();
        for (Position pos : possibleMoves) {
            if (lastPosition == null || (pos.getRow() != lastPosition.getRow() || pos.getCol() != lastPosition.getCol())) {
                forwardMoves.add(pos);
            }
        }

        Position chosenMove;

        if (forwardMoves.isEmpty()) {
            chosenMove = possibleMoves.get(0);
        } else {
            chosenMove = forwardMoves.get(random.nextInt(forwardMoves.size()));
        }
        lastPosition = new Position(r, c);

        return chosenMove;
    }
}