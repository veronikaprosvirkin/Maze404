package AI;

import enums.CellType;
import model.Enemy;
import model.Grid;
import model.Player;
import model.Position;

import java.util.LinkedList;
import java.util.Queue;

public class ChaseAI implements IEnemyAI {
    private int visionRange;

    @Override
    public Position computeNextMove(Enemy enemy, Grid grid, Player player) {
        int er = enemy.getRow();
        int ec = enemy.getCol();
        int pr = player.getRow();
        int pc = player.getCol();

        int distanceToPlayer = Math.abs(er - pr) + Math.abs(ec - pc);
        if (distanceToPlayer > getVisionRange()) {
            return new Position(er, ec);
        }

        Position nextStep = findShortestPathStep(grid, new Position(er, ec), new Position(pr, pc));

        if (nextStep != null) {
            return nextStep;
        }

        return new Position(er, ec);

    }

    private Position findShortestPathStep(Grid grid, Position start, Position target) {
        int height = grid.getHeight();
        int width = grid.getWidth();

        Queue<Position> queue = new LinkedList<>();
        boolean[][] visited = new boolean[height][width];
        Position[][] cameFrom = new Position[height][width];

        queue.add(start);
        visited[start.getRow()][start.getCol()] = true;

        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        boolean found = false;

        while(!queue.isEmpty()) {
            Position current = queue.poll();
            if (current.equals(target)) {
                found = true;
                break;
            }

            for (int[] dir : directions) {
                int newRow = current.getRow() + dir[0];
                int newCol = current.getCol() + dir[1];

                if (grid.isInBounds(newRow, newCol) && grid.getCell(newRow, newCol).getType() != CellType.WALL &&
                    !visited[newRow][newCol]) {

                    Position neighbor = new Position(newRow, newCol);
                    queue.add(neighbor);
                    visited[newRow][newCol] = true;
                    cameFrom[newRow][newCol] = current;
                }
            }
        }
        if (!found) {
            return null;
        }
        Position current = target;
        Position firstStep = null;
        while (current != null && (current.getRow() != start.getRow() || current.getCol() != start.getCol())){
            firstStep = current;
            current = cameFrom[current.getRow()][current.getCol()];
        }
        return firstStep;
    }

    private int getVisionRange(){
        switch (enums.Difficulty.current) {
            case MEDIUM -> visionRange = 5;
            case HARD -> visionRange = 8;
        }
        return visionRange;
    }
}