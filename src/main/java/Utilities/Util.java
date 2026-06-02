package Utilities;

import enums.CellType;
import model.Grid;
import model.Position;

import java.util.ArrayList;
import java.util.List;

public class Util {
    public static List<Position> getFloorCells(Grid grid) {
        List<Position> floorCells = new ArrayList<>();

        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                if (grid.getCell(x, y).getType() == CellType.FLOOR) {
                    floorCells.add(new Position(x, y));
                }
            }
        }
        return floorCells;
    }
}
