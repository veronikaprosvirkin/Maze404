import enums.CellType;
import logic.generation.MazeGenerator;
import model.Grid;
import model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MazeGeneratorTest {
    @Test
    void testMazeGenerationBoundsAndExit() {
        MazeGenerator generator = new MazeGenerator();
        int rows = 11;
        int cols = 11;

        Grid grid = generator.generate(rows, cols);

        assertNotNull(grid);
        assertEquals(cols, grid.getWidth());
        assertEquals(rows, grid.getHeight());

        // Проходимо по сітці та шукаємо вихід
        int exitCount = 0;
        Position exitPos = null;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid.getCell(r, c).getType() == CellType.EXIT) {
                    exitCount++;
                    exitPos = new Position(r, c);
                }
            }
        }

        assertEquals(1, exitCount, "На карті має бути згенерований рівно один EXIT");
        assertNotNull(exitPos, "Координати виходу не повинні бути null");

        Position start = new Position(1, 1);
        int distance = start.manhattanDistance(exitPos);
        assertTrue(distance >= (rows + cols) / 2, "Вихід має бути розміщений далеко від старту");
    }

    @Test
    void testStartPointIsFloor() {
        MazeGenerator generator = new MazeGenerator();
        Grid grid = generator.generate(11, 11);

        assertEquals(CellType.FLOOR, grid.getCell(1, 1).getType());
    }
}
