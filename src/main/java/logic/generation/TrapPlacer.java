package logic.generation;

import enums.CellType;
import model.Grid;
import model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrapPlacer {
    public void placeTraps(Grid grid, LevelConfig config, Position startPos) {
        List<Position> candidates = new ArrayList<>();
        for (int r = 0; r < grid.getHeight(); r++) {
            for (int c = 0; c < grid.getWidth(); c++) {
                if (grid.getCell(r, c).getType() != CellType.FLOOR) continue;
                Position p = new Position(r, c);
                if (startPos.manhattanDistance(p) < config.getMinTrapDistance()) continue;
                candidates.add(p);
            }
        }
        Collections.shuffle(candidates);
        int count = Math.min(config.getTrapCount(), candidates.size());
        for (int i = 0; i < count; i++)
            grid.setType(candidates.get(i).getRow(), candidates.get(i).getCol(), CellType.TRAP);
    }
}
