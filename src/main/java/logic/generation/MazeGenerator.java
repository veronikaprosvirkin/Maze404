package logic.generation;
import enums.CellType;
import model.Grid;
import model.Position;

import java.util.*;

public class MazeGenerator implements IMazeGenerator {

    public Grid generate(int rows, int cols){
        Grid grid = new Grid(rows, cols);
        if (rows % 2 == 0) rows++;
        if (cols % 2 == 0) cols++;
        for(int r = 0; r<rows; r++){
            for(int c = 0; c<cols; c++){
                grid.setType(r, c, CellType.WALL);
            }
        }

        Random rnd = new Random();
        Deque<Position> stack = new ArrayDeque<>();
        Position start = new Position(1,1);
        grid.setType(1,1,CellType.FLOOR);
        stack.push(start);
        while(!stack.isEmpty()){
            Position curr = stack.peek();
            List<Position> unvisited = getUnvisitedNeighbors(grid, curr, rows, cols);
            if(unvisited.isEmpty()){
                stack.pop();
            }else{
                Position chosen = unvisited.get(rnd.nextInt(unvisited.size()));

                int wallRow = (curr.getRow() + chosen.getRow()) / 2;
                int wallCol = (curr.getCol() + chosen.getCol()) / 2;
                grid.setType(wallRow, wallCol, CellType.FLOOR);
                grid.setType(chosen.getRow(), chosen.getCol(), CellType.FLOOR);

                stack.push(chosen);
            }
        }

        Position exit = findExitPosition(grid, rows, cols);
        grid.setType(exit.getRow(), exit.getCol(), CellType.EXIT);
        return grid;
    }

    private Position findExitPosition(Grid grid, int rows, int cols) {
        Position start = new Position(1, 1);
        List<Position> candidates = new ArrayList<>();

        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if (grid.getCell(r, c).getType() != CellType.FLOOR) continue;
                Position p = new Position(r, c);
                // Тільки клітинки далеко від старту
                if (start.manhattanDistance(p) >= (rows + cols) / 2) {
                    candidates.add(p);
                }
            }
        }

        if (candidates.isEmpty()) {
            return new Position(rows - 2, cols - 2);
        }

        return candidates.get(new Random().nextInt(candidates.size()));
    }

    private List<Position> getUnvisitedNeighbors(Grid grid, Position p, int rows, int cols) {
        List<Position> result = new ArrayList<>();
        int[][] dirs = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        for(int[] d: dirs){
            int nr = p.getRow() + d[0];
            int nc = p.getCol() + d[1];
            if(nr >0 && nr <rows-1&&nc >0 && nc < cols-1){
                if(grid.getCell(nr,nc).getType() == CellType.WALL){
                    result.add(new Position(nr,nc));
                }
            }
        }
        return result;
    }
}
